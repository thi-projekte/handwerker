package de.winfprojekt.craftvoice.offerservice.demo;

import de.winfprojekt.craftvoice.offerservice.offer.Offer;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.jboss.logging.Logger;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * DEMO-SEED (vor Produktivbetrieb wieder entfernbar).
 *
 * <p>Laedt beim Service-Start <b>einmalig</b> historische Demo-Angebote und
 * Rechnungen fuer den Denocke-Demo-Account aus {@code demo/denocke-seed.sql},
 * damit Dashboard, Monatsdiagramm und Rechnungsliste ueber mehrere Monate
 * gefuellt aussehen.
 *
 * <p><b>Idempotent:</b> Es wird nur geseedet, wenn noch keine Angebote mit
 * {@code businessKey LIKE 'angebot-demo-%'} existieren. Mehrfaches Deployen ist
 * also gefahrlos.
 *
 * <p><b>Robust:</b> Schlaegt der Seed fehl, wird der Fehler nur geloggt und
 * <b>nie</b> geworfen — der Service startet immer normal weiter.
 */
@ApplicationScoped
public class DemoDataSeeder {

    private static final Logger LOG = Logger.getLogger(DemoDataSeeder.class);
    private static final String SEED_RESOURCE = "demo/denocke-seed.sql";

    @Inject
    EntityManager em;

    void onStart(@Observes StartupEvent ev) {
        try {
            QuarkusTransaction.requiringNew().run(this::seedIfEmpty);
        } catch (Exception e) {
            LOG.error("Demo-Seed fehlgeschlagen — wird ignoriert, Start laeuft normal weiter.", e);
        }
    }

    private void seedIfEmpty() {
        long vorhanden = Offer.count("businessKey like ?1", "angebot-demo-%");
        if (vorhanden > 0) {
            LOG.infof("Demo-Seed uebersprungen (%d Demo-Angebote bereits vorhanden).", vorhanden);
            return;
        }

        String script = readScript();
        if (script == null) {
            return;
        }

        int ausgefuehrt = 0;
        // Kommentare ZUERST entfernen (ein ';' in einem Kommentar wuerde sonst das
        // Statement-Splitting verfaelschen), dann an ';' in einzelne Statements teilen.
        for (String teil : stripComments(script).split(";")) {
            String stmt = teil.trim();
            if (stmt.isEmpty()) {
                continue;
            }
            em.createNativeQuery(stmt).executeUpdate();
            ausgefuehrt++;
        }
        LOG.infof("Demo-Seed eingespielt: %d Statements ausgefuehrt.", ausgefuehrt);
    }

    private String readScript() {
        try (InputStream in = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(SEED_RESOURCE)) {
            if (in == null) {
                LOG.warnf("Demo-Seed-Datei '%s' nicht im Classpath gefunden — uebersprungen.", SEED_RESOURCE);
                return null;
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            LOG.error("Demo-Seed-Datei konnte nicht gelesen werden.", e);
            return null;
        }
    }

    /** Entfernt {@code --}-Zeilenkommentare, sodass reine SQL-Statements uebrig bleiben. */
    private static String stripComments(String sql) {
        StringBuilder sb = new StringBuilder();
        for (String line : sql.split("\n", -1)) {
            int idx = line.indexOf("--");
            sb.append(idx >= 0 ? line.substring(0, idx) : line).append('\n');
        }
        return sb.toString();
    }
}
