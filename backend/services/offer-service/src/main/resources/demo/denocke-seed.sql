-- Demo-Seed (Denocke): historische Angebote + Rechnungen ueber Jan-Mai 2026.
-- Wird vom DemoDataSeeder beim Start EINMALIG ausgefuehrt (idempotenter Guard
-- ueber die Seed-Offer-IDs 9001-9013). KEIN BEGIN/COMMIT: die Transaktion
-- steuert der Seeder. Statements sind durch ';' getrennt; Werte enthalten keine
-- Semikolons. Spaltennamen exakt nach DB-Schema (businesskey/einzelpreis/... klein).
-- handwerker_id (Denocke): c7b384fc-d787-4e49-a021-23c7c0b4a569 , Stundensatz 100 EUR/h.

INSERT INTO offer
  (id, created_at, updated_at, annahmetoken, businesskey, customer_id,
   gesamt_preis, geschaetzte_arbeitsdauer_stunden, handwerker_id, speech_snippet, status)
VALUES
  (9001, '2026-01-13 09:20:00', '2026-01-15 14:00:00', NULL, 'angebot-8068e94b-480e-4aae-b458-6a5f1e7c8181', '51', 0, 3,   'c7b384fc-d787-4e49-a021-23c7c0b4a569', 'Im Wohnzimmer brauche ich fuenf neue Steckdosen mit Leitung und Dosen, ungefaehr drei Stunden Arbeit.', 'ANGENOMMEN'),
  (9002, '2026-01-22 10:10:00', '2026-01-24 11:00:00', NULL, 'angebot-b56f7493-94df-4168-b9ef-aed2ed908b8f', '53', 0, 1.5, 'c7b384fc-d787-4e49-a021-23c7c0b4a569', 'Fuer den Flur zwei Dimmer und zwei Schalter einbauen.', 'ABGELEHNT'),
  (9003, '2026-02-04 08:40:00', '2026-02-06 16:30:00', NULL, 'angebot-a5dd35d3-3500-4436-b19f-89bcd0169398', '52', 0, 4,   'c7b384fc-d787-4e49-a021-23c7c0b4a569', 'Die Unterverteilung im Keller erneuern, neuer Kleinverteiler mit Sicherungen und FI.', 'ANGENOMMEN'),
  (9004, '2026-02-17 13:15:00', '2026-02-18 09:00:00', NULL, 'angebot-faeba9a5-b2d3-49e6-acdc-84bf2fc7f0b8', '55', 0, 2.5, 'c7b384fc-d787-4e49-a021-23c7c0b4a569', 'Auf der Terrasse zwei Aussensteckdosen setzen, Zuleitung dazu.', 'VERSENDET'),
  (9005, '2026-02-26 11:50:00', '2026-02-28 10:20:00', NULL, 'angebot-1107f4a0-b378-4886-b362-4852d3e1cc61', '54', 0, 3,   'c7b384fc-d787-4e49-a021-23c7c0b4a569', 'In der Kueche acht LED-Spots mit Dimmer installieren.', 'ANGENOMMEN'),
  (9006, '2026-03-09 07:55:00', '2026-03-12 15:40:00', NULL, 'angebot-5635c580-c0f1-4eea-9625-2a4c64eeb3c2', '51', 0, 5,   'c7b384fc-d787-4e49-a021-23c7c0b4a569', 'Wallbox in der Garage anschliessen, mit FI Typ B und Zuleitung.', 'ANGENOMMEN'),
  (9007, '2026-03-23 14:05:00', '2026-03-25 09:30:00', NULL, 'angebot-5d238f9c-388f-4ef8-b756-9bfb18c985c9', '52', 0, 3,   'c7b384fc-d787-4e49-a021-23c7c0b4a569', 'Smart-Home Beleuchtung mit vier Dimmern und Schaltern.', 'ABGELEHNT'),
  (9008, '2026-04-07 09:10:00', '2026-04-08 12:00:00', NULL, 'angebot-1dc1a16f-3340-46ad-ad63-b64c1a4c6831', '53', 0, 8,   'c7b384fc-d787-4e49-a021-23c7c0b4a569', 'Altbauwohnung, Leitungen komplett erneuern, acht Steckdosen neu.', 'VERSENDET'),
  (9009, '2026-04-15 10:25:00', '2026-04-17 14:10:00', NULL, 'angebot-b1ecfd75-8fac-461a-96d6-ff84226c0b36', '54', 0, 4.5, 'c7b384fc-d787-4e49-a021-23c7c0b4a569', 'Zaehlerschrank erweitern, mehr Sicherungen und zweiter FI.', 'ANGENOMMEN'),
  (9010, '2026-04-28 08:30:00', '2026-04-30 17:00:00', NULL, 'angebot-f5a5bcf4-1fc9-433d-8886-6cc73191c756', '52', 0, 5,   'c7b384fc-d787-4e49-a021-23c7c0b4a569', 'Im Buero zehn Steckdosen und Netzwerkverkabelung.', 'ANGENOMMEN'),
  (9011, '2026-05-12 09:45:00', '2026-05-14 13:20:00', NULL, 'angebot-cb3f812c-8f02-4dad-9545-ebfb5a869fff', '51', 0, 4,   'c7b384fc-d787-4e49-a021-23c7c0b4a569', 'Bad renoviert, drei Steckdosen, sechs Einbaustrahler, FI nachruesten.', 'ANGENOMMEN'),
  (9012, '2026-05-21 15:30:00', '2026-05-22 10:15:00', NULL, 'angebot-d2a3e231-91e2-4a83-a8c3-709be3327eff', '55', 0, 4,   'c7b384fc-d787-4e49-a021-23c7c0b4a569', 'Gartenhaus mit Strom versorgen, Erdkabel und Aussensteckdose.', 'VERSENDET'),
  (9013, '2026-05-29 11:20:00', '2026-05-31 09:50:00', NULL, 'angebot-bafe7852-137a-45ca-9912-514f19f7befe', '54', 0, 2,   'c7b384fc-d787-4e49-a021-23c7c0b4a569', 'Carport mit vier Strahlern beleuchten, Schalter dazu.', 'ABGELEHNT');

INSERT INTO offer_position
  (id, created_at, updated_at, beschreibung, bezeichnung, einheit, einzelpreis,
   hersteller, katalogproduktid, menge, positionspreis, reihenfolge, type, offer_id)
VALUES
  (91001, '2026-01-13 09:20:00', '2026-01-13 09:20:00', 'Schutzkontakt-Steckdose System 55 reinweiss', 'Gira SCHUKO Steckdose System 55', 'Stk.', 6.90,  'Gira',     NULL, 5,  5*6.90,   1, 'MATERIAL',    9001),
  (91002, '2026-01-13 09:20:00', '2026-01-13 09:20:00', 'Geraetedose tief 60mm Unterputz',           'Geraetedose tief Unterputz 1-fach','Stk.', 0.45,  'Kaiser',   NULL, 5,  5*0.45,   2, 'MATERIAL',    9001),
  (91003, '2026-01-13 09:20:00', '2026-01-13 09:20:00', 'Mantelleitung NYM-J 3x1,5mm2',              'Installationsleitung NYM-J 3x1,5mm2','m', 0.85,  'Lapp',     NULL, 25, 25*0.85,  3, 'MATERIAL',    9001),
  (91004, '2026-01-13 09:20:00', '2026-01-13 09:20:00', NULL,                                        'Arbeitszeit',                      'h',    100.00, NULL,       NULL, 3,  3*100.00, 4, 'ARBEITSZEIT', 9001),
  (91005, '2026-01-22 10:10:00', '2026-01-22 10:10:00', 'Universal-Dimmeinsatz System 55',           'Gira Universaldimmer System 55',   'Stk.', 38.90, 'Gira',     NULL, 2,  2*38.90,  1, 'MATERIAL',    9002),
  (91006, '2026-01-22 10:10:00', '2026-01-22 10:10:00', 'Wippschalter System 55 reinweiss',          'Gira Wippschalter System 55',      'Stk.', 7.20,  'Gira',     NULL, 2,  2*7.20,   2, 'MATERIAL',    9002),
  (91007, '2026-01-22 10:10:00', '2026-01-22 10:10:00', NULL,                                        'Arbeitszeit',                      'h',    100.00, NULL,       NULL, 1.5,1.5*100.00,3,'ARBEITSZEIT', 9002),
  (91008, '2026-02-04 08:40:00', '2026-02-04 08:40:00', 'Kleinverteiler 12TE Unterputz',             'Hager Kleinverteiler 12TE UP',     'Stk.', 24.50, 'Hager',    NULL, 1,  1*24.50,  1, 'MATERIAL',    9003),
  (91009, '2026-02-04 08:40:00', '2026-02-04 08:40:00', 'Leitungsschutzschalter B16 1-polig',        'Hager Leitungsschutzschalter B16', 'Stk.', 4.80,  'Hager',    NULL, 6,  6*4.80,   2, 'MATERIAL',    9003),
  (91010, '2026-02-04 08:40:00', '2026-02-04 08:40:00', 'FI-Schutzschalter Typ A 40A 30mA',          'Doepke FI-Schalter Typ A 40A/30mA','Stk.', 31.50, 'Doepke',   NULL, 1,  1*31.50,  3, 'MATERIAL',    9003),
  (91011, '2026-02-04 08:40:00', '2026-02-04 08:40:00', NULL,                                        'Arbeitszeit',                      'h',    100.00, NULL,       NULL, 4,  4*100.00, 4, 'ARBEITSZEIT', 9003),
  (91012, '2026-02-17 13:15:00', '2026-02-17 13:15:00', 'Aussensteckdose IP44 Aufputz 2-fach',       'Aussensteckdose IP44 Aufputz 2-fach','Stk.',12.90,'Jung',     NULL, 2,  2*12.90,  1, 'MATERIAL',    9004),
  (91013, '2026-02-17 13:15:00', '2026-02-17 13:15:00', 'Mantelleitung NYM-J 3x1,5mm2',              'Installationsleitung NYM-J 3x1,5mm2','m', 0.85,  'Lapp',     NULL, 30, 30*0.85,  2, 'MATERIAL',    9004),
  (91014, '2026-02-17 13:15:00', '2026-02-17 13:15:00', NULL,                                        'Arbeitszeit',                      'h',    100.00, NULL,       NULL, 2.5,2.5*100.00,3,'ARBEITSZEIT', 9004),
  (91015, '2026-02-26 11:50:00', '2026-02-26 11:50:00', 'LED-Einbaustrahler dimmbar warmweiss',      'LED-Einbaustrahler dimmbar',       'Stk.', 9.40,  'Paulmann', NULL, 8,  8*9.40,   1, 'MATERIAL',    9005),
  (91016, '2026-02-26 11:50:00', '2026-02-26 11:50:00', 'Universal-Dimmeinsatz System 55',           'Gira Universaldimmer System 55',   'Stk.', 38.90, 'Gira',     NULL, 1,  1*38.90,  2, 'MATERIAL',    9005),
  (91017, '2026-02-26 11:50:00', '2026-02-26 11:50:00', 'Mantelleitung NYM-J 3x1,5mm2',              'Installationsleitung NYM-J 3x1,5mm2','m', 0.85,  'Lapp',     NULL, 20, 20*0.85,  3, 'MATERIAL',    9005),
  (91018, '2026-02-26 11:50:00', '2026-02-26 11:50:00', NULL,                                        'Arbeitszeit',                      'h',    100.00, NULL,       NULL, 3,  3*100.00, 4, 'ARBEITSZEIT', 9005),
  (91019, '2026-03-09 07:55:00', '2026-03-09 07:55:00', 'Wallbox 22kW Typ 2 mit Ladekabel',          'Wallbox 22kW Typ 2',               'Stk.', 690.00,'ABL',      NULL, 1,  1*690.00, 1, 'MATERIAL',    9006),
  (91020, '2026-03-09 07:55:00', '2026-03-09 07:55:00', 'FI-Schutzschalter Typ B 40A 30mA',          'Doepke FI-Schalter Typ B 40A/30mA','Stk.', 145.00,'Doepke',   NULL, 1,  1*145.00, 2, 'MATERIAL',    9006),
  (91021, '2026-03-09 07:55:00', '2026-03-09 07:55:00', 'Mantelleitung NYM-J 5x2,5mm2',              'Installationsleitung NYM-J 5x2,5mm2','m', 1.65,  'Lapp',     NULL, 15, 15*1.65,  3, 'MATERIAL',    9006),
  (91022, '2026-03-09 07:55:00', '2026-03-09 07:55:00', 'Leitungsschutzschalter C16 1-polig',        'Hager Leitungsschutzschalter C16', 'Stk.', 4.80,  'Hager',    NULL, 1,  1*4.80,   4, 'MATERIAL',    9006),
  (91023, '2026-03-09 07:55:00', '2026-03-09 07:55:00', NULL,                                        'Arbeitszeit',                      'h',    100.00, NULL,       NULL, 5,  5*100.00, 5, 'ARBEITSZEIT', 9006),
  (91024, '2026-03-23 14:05:00', '2026-03-23 14:05:00', 'Universal-Dimmeinsatz System 55',           'Gira Universaldimmer System 55',   'Stk.', 38.90, 'Gira',     NULL, 4,  4*38.90,  1, 'MATERIAL',    9007),
  (91025, '2026-03-23 14:05:00', '2026-03-23 14:05:00', 'Wippschalter System 55 reinweiss',          'Gira Wippschalter System 55',      'Stk.', 7.20,  'Gira',     NULL, 4,  4*7.20,   2, 'MATERIAL',    9007),
  (91026, '2026-03-23 14:05:00', '2026-03-23 14:05:00', NULL,                                        'Arbeitszeit',                      'h',    100.00, NULL,       NULL, 3,  3*100.00, 3, 'ARBEITSZEIT', 9007),
  (91027, '2026-04-07 09:10:00', '2026-04-07 09:10:00', 'Mantelleitung NYM-J 3x1,5mm2',              'Installationsleitung NYM-J 3x1,5mm2','m', 0.85,  'Lapp',     NULL, 60, 60*0.85,  1, 'MATERIAL',    9008),
  (91028, '2026-04-07 09:10:00', '2026-04-07 09:10:00', 'Schutzkontakt-Steckdose System 55 reinweiss','Gira SCHUKO Steckdose System 55', 'Stk.', 6.90,  'Gira',     NULL, 8,  8*6.90,   2, 'MATERIAL',    9008),
  (91029, '2026-04-07 09:10:00', '2026-04-07 09:10:00', 'Geraetedose tief 60mm Unterputz',           'Geraetedose tief Unterputz 1-fach','Stk.', 0.45,  'Kaiser',   NULL, 12, 12*0.45,  3, 'MATERIAL',    9008),
  (91030, '2026-04-07 09:10:00', '2026-04-07 09:10:00', NULL,                                        'Arbeitszeit',                      'h',    100.00, NULL,       NULL, 8,  8*100.00, 4, 'ARBEITSZEIT', 9008),
  (91031, '2026-04-15 10:25:00', '2026-04-15 10:25:00', 'Kleinverteiler 12TE Unterputz',             'Hager Kleinverteiler 12TE UP',     'Stk.', 24.50, 'Hager',    NULL, 1,  1*24.50,  1, 'MATERIAL',    9009),
  (91032, '2026-04-15 10:25:00', '2026-04-15 10:25:00', 'Leitungsschutzschalter B16 1-polig',        'Hager Leitungsschutzschalter B16', 'Stk.', 4.80,  'Hager',    NULL, 8,  8*4.80,   2, 'MATERIAL',    9009),
  (91033, '2026-04-15 10:25:00', '2026-04-15 10:25:00', 'FI-Schutzschalter Typ A 40A 30mA',          'Doepke FI-Schalter Typ A 40A/30mA','Stk.', 31.50, 'Doepke',   NULL, 2,  2*31.50,  3, 'MATERIAL',    9009),
  (91034, '2026-04-15 10:25:00', '2026-04-15 10:25:00', NULL,                                        'Arbeitszeit',                      'h',    100.00, NULL,       NULL, 4.5,4.5*100.00,4,'ARBEITSZEIT', 9009),
  (91035, '2026-04-28 08:30:00', '2026-04-28 08:30:00', 'Schutzkontakt-Steckdose System 55 reinweiss','Gira SCHUKO Steckdose System 55', 'Stk.', 6.90,  'Gira',     NULL, 10, 10*6.90,  1, 'MATERIAL',    9010),
  (91036, '2026-04-28 08:30:00', '2026-04-28 08:30:00', 'Geraetedose tief 60mm Unterputz',           'Geraetedose tief Unterputz 1-fach','Stk.', 0.45,  'Kaiser',   NULL, 10, 10*0.45,  2, 'MATERIAL',    9010),
  (91037, '2026-04-28 08:30:00', '2026-04-28 08:30:00', 'Mantelleitung NYM-J 3x1,5mm2',              'Installationsleitung NYM-J 3x1,5mm2','m', 0.85,  'Lapp',     NULL, 40, 40*0.85,  3, 'MATERIAL',    9010),
  (91038, '2026-04-28 08:30:00', '2026-04-28 08:30:00', NULL,                                        'Arbeitszeit',                      'h',    100.00, NULL,       NULL, 5,  5*100.00, 4, 'ARBEITSZEIT', 9010),
  (91039, '2026-05-12 09:45:00', '2026-05-12 09:45:00', 'Schutzkontakt-Steckdose System 55 reinweiss','Gira SCHUKO Steckdose System 55', 'Stk.', 6.90,  'Gira',     NULL, 3,  3*6.90,   1, 'MATERIAL',    9011),
  (91040, '2026-05-12 09:45:00', '2026-05-12 09:45:00', 'LED-Einbaustrahler dimmbar warmweiss',      'LED-Einbaustrahler dimmbar',       'Stk.', 9.40,  'Paulmann', NULL, 6,  6*9.40,   2, 'MATERIAL',    9011),
  (91041, '2026-05-12 09:45:00', '2026-05-12 09:45:00', 'FI-Schutzschalter Typ A 40A 30mA',          'Doepke FI-Schalter Typ A 40A/30mA','Stk.', 31.50, 'Doepke',   NULL, 1,  1*31.50,  3, 'MATERIAL',    9011),
  (91042, '2026-05-12 09:45:00', '2026-05-12 09:45:00', NULL,                                        'Arbeitszeit',                      'h',    100.00, NULL,       NULL, 4,  4*100.00, 4, 'ARBEITSZEIT', 9011),
  (91043, '2026-05-21 15:30:00', '2026-05-21 15:30:00', 'Mantelleitung NYM-J 5x2,5mm2',              'Installationsleitung NYM-J 5x2,5mm2','m', 1.65,  'Lapp',     NULL, 35, 35*1.65,  1, 'MATERIAL',    9012),
  (91044, '2026-05-21 15:30:00', '2026-05-21 15:30:00', 'Aussensteckdose IP44 Aufputz 2-fach',       'Aussensteckdose IP44 Aufputz 2-fach','Stk.',12.90,'Jung',     NULL, 1,  1*12.90,  2, 'MATERIAL',    9012),
  (91045, '2026-05-21 15:30:00', '2026-05-21 15:30:00', 'Leitungsschutzschalter B16 1-polig',        'Hager Leitungsschutzschalter B16', 'Stk.', 4.80,  'Hager',    NULL, 2,  2*4.80,   3, 'MATERIAL',    9012),
  (91046, '2026-05-21 15:30:00', '2026-05-21 15:30:00', NULL,                                        'Arbeitszeit',                      'h',    100.00, NULL,       NULL, 4,  4*100.00, 4, 'ARBEITSZEIT', 9012),
  (91047, '2026-05-29 11:20:00', '2026-05-29 11:20:00', 'LED-Einbaustrahler dimmbar warmweiss',      'LED-Einbaustrahler dimmbar',       'Stk.', 9.40,  'Paulmann', NULL, 4,  4*9.40,   1, 'MATERIAL',    9013),
  (91048, '2026-05-29 11:20:00', '2026-05-29 11:20:00', 'Wippschalter System 55 reinweiss',          'Gira Wippschalter System 55',      'Stk.', 7.20,  'Gira',     NULL, 2,  2*7.20,   2, 'MATERIAL',    9013),
  (91049, '2026-05-29 11:20:00', '2026-05-29 11:20:00', 'Mantelleitung NYM-J 3x1,5mm2',              'Installationsleitung NYM-J 3x1,5mm2','m', 0.85,  'Lapp',     NULL, 18, 18*0.85,  3, 'MATERIAL',    9013),
  (91050, '2026-05-29 11:20:00', '2026-05-29 11:20:00', NULL,                                        'Arbeitszeit',                      'h',    100.00, NULL,       NULL, 2,  2*100.00, 4, 'ARBEITSZEIT', 9013);

INSERT INTO offer_status_history
  (id, created_at, updated_at, notiz, status, zeitpunkt, offer_id)
VALUES
  (92001, '2026-01-13 09:20:00', '2026-01-13 09:20:00', NULL, 'ERFASST',    '2026-01-13 09:20:00', 9001),
  (92002, '2026-01-15 14:00:00', '2026-01-15 14:00:00', 'Vom Kunden angenommen', 'ANGENOMMEN', '2026-01-15 14:00:00', 9001),
  (92003, '2026-01-22 10:10:00', '2026-01-22 10:10:00', NULL, 'ERFASST',    '2026-01-22 10:10:00', 9002),
  (92004, '2026-01-24 11:00:00', '2026-01-24 11:00:00', 'Vom Kunden abgelehnt',  'ABGELEHNT',  '2026-01-24 11:00:00', 9002),
  (92005, '2026-02-04 08:40:00', '2026-02-04 08:40:00', NULL, 'ERFASST',    '2026-02-04 08:40:00', 9003),
  (92006, '2026-02-06 16:30:00', '2026-02-06 16:30:00', 'Vom Kunden angenommen', 'ANGENOMMEN', '2026-02-06 16:30:00', 9003),
  (92007, '2026-02-17 13:15:00', '2026-02-17 13:15:00', NULL, 'ERFASST',    '2026-02-17 13:15:00', 9004),
  (92008, '2026-02-18 09:00:00', '2026-02-18 09:00:00', 'An den Kunden versendet','VERSENDET', '2026-02-18 09:00:00', 9004),
  (92009, '2026-02-26 11:50:00', '2026-02-26 11:50:00', NULL, 'ERFASST',    '2026-02-26 11:50:00', 9005),
  (92010, '2026-02-28 10:20:00', '2026-02-28 10:20:00', 'Vom Kunden angenommen', 'ANGENOMMEN', '2026-02-28 10:20:00', 9005),
  (92011, '2026-03-09 07:55:00', '2026-03-09 07:55:00', NULL, 'ERFASST',    '2026-03-09 07:55:00', 9006),
  (92012, '2026-03-12 15:40:00', '2026-03-12 15:40:00', 'Vom Kunden angenommen', 'ANGENOMMEN', '2026-03-12 15:40:00', 9006),
  (92013, '2026-03-23 14:05:00', '2026-03-23 14:05:00', NULL, 'ERFASST',    '2026-03-23 14:05:00', 9007),
  (92014, '2026-03-25 09:30:00', '2026-03-25 09:30:00', 'Vom Kunden abgelehnt',  'ABGELEHNT',  '2026-03-25 09:30:00', 9007),
  (92015, '2026-04-07 09:10:00', '2026-04-07 09:10:00', NULL, 'ERFASST',    '2026-04-07 09:10:00', 9008),
  (92016, '2026-04-08 12:00:00', '2026-04-08 12:00:00', 'An den Kunden versendet','VERSENDET', '2026-04-08 12:00:00', 9008),
  (92017, '2026-04-15 10:25:00', '2026-04-15 10:25:00', NULL, 'ERFASST',    '2026-04-15 10:25:00', 9009),
  (92018, '2026-04-17 14:10:00', '2026-04-17 14:10:00', 'Vom Kunden angenommen', 'ANGENOMMEN', '2026-04-17 14:10:00', 9009),
  (92019, '2026-04-28 08:30:00', '2026-04-28 08:30:00', NULL, 'ERFASST',    '2026-04-28 08:30:00', 9010),
  (92020, '2026-04-30 17:00:00', '2026-04-30 17:00:00', 'Vom Kunden angenommen', 'ANGENOMMEN', '2026-04-30 17:00:00', 9010),
  (92021, '2026-05-12 09:45:00', '2026-05-12 09:45:00', NULL, 'ERFASST',    '2026-05-12 09:45:00', 9011),
  (92022, '2026-05-14 13:20:00', '2026-05-14 13:20:00', 'Vom Kunden angenommen', 'ANGENOMMEN', '2026-05-14 13:20:00', 9011),
  (92023, '2026-05-21 15:30:00', '2026-05-21 15:30:00', NULL, 'ERFASST',    '2026-05-21 15:30:00', 9012),
  (92024, '2026-05-22 10:15:00', '2026-05-22 10:15:00', 'An den Kunden versendet','VERSENDET', '2026-05-22 10:15:00', 9012),
  (92025, '2026-05-29 11:20:00', '2026-05-29 11:20:00', NULL, 'ERFASST',    '2026-05-29 11:20:00', 9013),
  (92026, '2026-05-31 09:50:00', '2026-05-31 09:50:00', 'Vom Kunden abgelehnt',  'ABGELEHNT',  '2026-05-31 09:50:00', 9013);

INSERT INTO invoice
  (id, created_at, updated_at, gesamt_preis,
   kunde_vorname, kunde_nachname, kunde_email, kunde_strasse, kunde_hausnummer, kunde_plz, kunde_ort,
   offer_business_key, rechnungsnummer)
VALUES
  (9001, '2026-01-16 09:00:00', '2026-01-16 09:00:00', 0, 'Lennart','Moog',  'moog@example.de',   'Hans-Kuhn-Str.',   '24', '85051','Ingolstadt','angebot-8068e94b-480e-4aae-b458-6a5f1e7c8181','RE-2026-007'),
  (9002, '2026-02-07 09:00:00', '2026-02-07 09:00:00', 0, 'Emanuel','Mrazek','mrazek@example.de', 'Muenchnerstrasse', '132','85051','Ingolstadt','angebot-a5dd35d3-3500-4436-b19f-89bcd0169398','RE-2026-008'),
  (9003, '2026-03-01 09:00:00', '2026-03-01 09:00:00', 0, 'Felix',  'Bartel','bartel@example.de', 'Nuernbergerstrasse','21', '85054','Ingolstadt','angebot-1107f4a0-b378-4886-b362-4852d3e1cc61','RE-2026-009'),
  (9004, '2026-03-13 09:00:00', '2026-03-13 09:00:00', 0, 'Lennart','Moog',  'moog@example.de',   'Hans-Kuhn-Str.',   '24', '85051','Ingolstadt','angebot-5635c580-c0f1-4eea-9625-2a4c64eeb3c2','RE-2026-010'),
  (9005, '2026-04-18 09:00:00', '2026-04-18 09:00:00', 0, 'Felix',  'Bartel','bartel@example.de', 'Nuernbergerstrasse','21', '85054','Ingolstadt','angebot-b1ecfd75-8fac-461a-96d6-ff84226c0b36','RE-2026-011'),
  (9006, '2026-05-01 09:00:00', '2026-05-01 09:00:00', 0, 'Emanuel','Mrazek','mrazek@example.de', 'Muenchnerstrasse', '132','85051','Ingolstadt','angebot-f5a5bcf4-1fc9-433d-8886-6cc73191c756','RE-2026-012'),
  (9007, '2026-05-15 09:00:00', '2026-05-15 09:00:00', 0, 'Lennart','Moog',  'moog@example.de',   'Hans-Kuhn-Str.',   '24', '85051','Ingolstadt','angebot-cb3f812c-8f02-4dad-9545-ebfb5a869fff','RE-2026-013');

INSERT INTO invoice_position
  (id, created_at, updated_at, bezeichnung, einheit, einzelpreis, hersteller,
   katalogproduktid, menge, positionspreis, reihenfolge, type, invoice_id)
SELECT 93000 + ROW_NUMBER() OVER (ORDER BY inv.id, op.reihenfolge),
       op.created_at, op.updated_at, op.bezeichnung, op.einheit, op.einzelpreis, op.hersteller,
       op.katalogproduktid, op.menge, op.positionspreis, op.reihenfolge, op.type, inv.id
FROM invoice inv
JOIN offer o            ON o.businesskey = inv.offer_business_key
JOIN offer_position op  ON op.offer_id = o.id
WHERE inv.id BETWEEN 9001 AND 9007;

UPDATE offer o
   SET gesamt_preis = (SELECT COALESCE(SUM(positionspreis),0) FROM offer_position WHERE offer_id = o.id)
 WHERE o.id BETWEEN 9001 AND 9013;

UPDATE invoice i
   SET gesamt_preis = (SELECT COALESCE(SUM(positionspreis),0) FROM invoice_position WHERE invoice_id = i.id)
 WHERE i.id BETWEEN 9001 AND 9007;
