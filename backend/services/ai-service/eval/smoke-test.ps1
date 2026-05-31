# Smoke-Test fuer das Call-1-Eval-Datenset.
# Schickt EIN Szenario an EIN MegaLLM-Modell und zeigt Output + Tokenverbrauch.
# Zweck: validieren, dass System-Prompt, Input-Aufbau und Gold-Erwartung praktisch zusammenpassen,
#        BEVOR der grosse Java-Harness (#536) gebaut wird.
#
# Nutzung (im selben Fenster, in dem MEGALLM_API_KEY gesetzt ist):
#   cd backend/services/ai-service/eval
#   ./smoke-test.ps1                                  # default: s1a an gpt-5.4
#   ./smoke-test.ps1 -Scenario call1-s5-minimal-leer -Model gemini-3-flash-preview
#
# Der API-Key wird ausschliesslich aus $env:MEGALLM_API_KEY gelesen, nie gespeichert.

param(
    [string]$Scenario = "call1-s1a-floor-extraktion",
    [string]$Model    = "gpt-5.4",
    [double]$Temperature = 0.2,
    [bool]$Strict = $true
)

$ErrorActionPreference = "Stop"

if (-not $env:MEGALLM_API_KEY) {
    Write-Error "MEGALLM_API_KEY ist nicht gesetzt. Erst: `$env:MEGALLM_API_KEY = 'sk-mega-...'"
    exit 1
}

# Datenset liegt eine Ebene ueber diesem Skript unter docs/
$datasetPath = Join-Path $PSScriptRoot "..\docs\eval-datenset-call1.json"
$dataset = Get-Content $datasetPath -Raw -Encoding UTF8 | ConvertFrom-Json

$sc = $dataset.scenarios | Where-Object { $_.id -eq $Scenario }
if (-not $sc) {
    Write-Error "Szenario '$Scenario' nicht gefunden. Verfuegbar: $($dataset.scenarios.id -join ', ')"
    exit 1
}

# User-Message bauen: genau die Felder, die der echte Service spaeter schickt.
# Korrektur-Fall (S4) hat strukturierteAngebotspositionen + korrekturschnipsel statt vorlage + sprachschnipsel.
$inputJson = $sc.input | ConvertTo-Json -Depth 20
$userContent = "Hier der Eingangs-Payload (so wie ihn die Process Engine schickt). Erzeuge daraus das geforderte JSON:`n`n$inputJson"

# Striktes JSON-Schema fuer die Ausgabe (= unser ErgebnisKi-Vertrag).
# Erzwingt Wrapper {strukturierteAngebotspositionen, korrekturvorschlaege} und alle Felder.
# menge ist number ODER null (Mengen koennen unbekannt sein).
$positionSchema = @{
    type = "object"
    properties = @{
        bezeichnung  = @{ type = "string" }
        beschreibung = @{ type = "string" }
        menge        = @{ type = @("number", "null") }
        einheit      = @{ type = "string" }
    }
    required = @("bezeichnung", "beschreibung", "menge", "einheit")
    additionalProperties = $false
}

$ergebnisSchema = @{
    type = "object"
    properties = @{
        strukturierteAngebotspositionen = @{
            type = "object"
            properties = @{
                leistungen = @{ type = "array"; items = $positionSchema }
                material   = @{ type = "array"; items = $positionSchema }
                notizen    = @{ type = "array"; items = @{ type = "string" } }
            }
            required = @("leistungen", "material", "notizen")
            additionalProperties = $false
        }
        korrekturvorschlaege = @{ type = "array"; items = @{ type = "string" } }
    }
    required = @("strukturierteAngebotspositionen", "korrekturvorschlaege")
    additionalProperties = $false
}

$bodyObj = @{
    model       = $Model
    temperature = $Temperature
    messages    = @(
        @{ role = "system"; content = $dataset.systemPrompt },
        @{ role = "user";   content = $userContent }
    )
}

if ($Strict) {
    $bodyObj.response_format = @{
        type = "json_schema"
        json_schema = @{
            name   = "ergebnis_ki"
            strict = $true
            schema = $ergebnisSchema
        }
    }
}

$body = $bodyObj | ConvertTo-Json -Depth 40

Write-Host "=== Smoke-Test ===" -ForegroundColor Cyan
Write-Host "Szenario: $($sc.id)  ($($sc.name))"
Write-Host "Modell:   $Model   Temperatur: $Temperature   Strict-Schema: $Strict"
Write-Host "Sende Anfrage an MegaLLM..." -ForegroundColor DarkGray
Write-Host ""

$sw = [System.Diagnostics.Stopwatch]::StartNew()
$resp = Invoke-RestMethod -Uri "https://ai.megallm.io/v1/chat/completions" `
    -Method Post `
    -Headers @{ Authorization = "Bearer $env:MEGALLM_API_KEY" } `
    -ContentType "application/json" `
    -Body $body
$sw.Stop()

$answer = $resp.choices[0].message.content

Write-Host "--- MODELL-OUTPUT ---" -ForegroundColor Green
Write-Host $answer
Write-Host ""
Write-Host "--- METRIKEN ---" -ForegroundColor Yellow
Write-Host ("Latenz:        {0:N1} s" -f $sw.Elapsed.TotalSeconds)
if ($resp.usage) {
    Write-Host ("Tokens prompt: {0}" -f $resp.usage.prompt_tokens)
    Write-Host ("Tokens output: {0}" -f $resp.usage.completion_tokens)
    Write-Host ("Tokens gesamt: {0}" -f $resp.usage.total_tokens)
}

# Schneller JSON-Validitaets-Check (einer unserer hardChecks)
Write-Host ""
Write-Host "--- SCHNELL-CHECK ---" -ForegroundColor Yellow
# Robustes Parsen: evtl. doch vorhandene Markdown-Fences wegschneiden.
$clean = $answer -replace '(?s)^\s*```(?:json)?\s*', '' -replace '(?s)\s*```\s*$', ''
try {
    $parsed = $clean | ConvertFrom-Json
    Write-Host "JSON valide: JA" -ForegroundColor Green

    # Schema-Wrapper pruefen
    if ($parsed.PSObject.Properties.Name -contains 'strukturierteAngebotspositionen' -and
        $parsed.PSObject.Properties.Name -contains 'korrekturvorschlaege') {
        Write-Host "Schema-Wrapper: OK (strukturierteAngebotspositionen + korrekturvorschlaege vorhanden)" -ForegroundColor Green
    } else {
        Write-Host "Schema-Wrapper: FEHLT - Top-Level-Felder nicht wie erwartet" -ForegroundColor Red
    }

    $hasPreis = $answer -match '(?i)preis|stundensatz|\beur\b|euro'
    if ($hasPreis) {
        Write-Host "Preis-Check: VERDAECHTIG - 'preis/eur' im Output gefunden!" -ForegroundColor Red
    } else {
        Write-Host "Preis-Check: sauber (kein Preis gefunden)" -ForegroundColor Green
    }
} catch {
    Write-Host "JSON valide: NEIN - auch nach Fence-Stripping nicht parsebar" -ForegroundColor Red
}
