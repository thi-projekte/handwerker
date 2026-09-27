package org.cibseven.getstarted.loanapproval;

import connectjar.org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import connectjar.org.apache.hc.client5.http.impl.classic.HttpClients;
import org.cibseven.connect.Connectors;
import org.cibseven.connect.httpclient.HttpConnector;
import org.cibseven.connect.httpclient.impl.AbstractHttpConnector;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class HttpConnectorConfig {
    @Autowired
    private HeaderForwardingInterceptor forwardingInterceptor;

    @EventListener(ContextRefreshedEvent.class)
    public void onApplicationEvent() {
        HttpConnector httpConnector = Connectors.getConnector(HttpConnector.ID);
        if (httpConnector instanceof AbstractHttpConnector<?,?> conn) {
            HttpClientBuilder builder = HttpClients.custom().addRequestInterceptorFirst(forwardingInterceptor);
            conn.setHttpClient(builder.build());
            System.out.println("CONFIGURED HTTP CONNECTOR FOR FORWARDING");
        }
    }
}
