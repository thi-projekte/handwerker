package org.cibseven.getstarted.loanapproval;

import org.cibseven.bpm.spring.boot.starter.event.PostDeployEvent;
import org.cibseven.connect.Connectors;
import org.cibseven.connect.spi.Connector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ConnectorChecker {

    private static final Logger log = LoggerFactory.getLogger(ConnectorChecker.class);

    @EventListener
    public void checkConnectors(PostDeployEvent event) {
        log.info("=== DISCOVERED CIB SEVEN CONNECTORS ===");
// Retrieve all registered connectors
        java.util.Set<Connector<?>> availableConnectors = Connectors.getAvailableConnectors();
        if (availableConnectors.isEmpty()) {
            log.warn("No connectors found! Check your pom.xml/build.gradle dependencies.");
        } else {
            for (Connector<?> connector : availableConnectors) {
                log.info("Connector ID: '{}' | Implementation: {}",
                        connector.getId(),
                        connector.getClass().getName());
            }
        }
        log.info("=======================================");
    }
}
