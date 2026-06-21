package org.cibseven.getstarted.loanapproval;

import connectjar.org.apache.hc.core5.http.EntityDetails;
import connectjar.org.apache.hc.core5.http.HttpException;
import connectjar.org.apache.hc.core5.http.HttpRequest;
import connectjar.org.apache.hc.core5.http.HttpRequestInterceptor;
import connectjar.org.apache.hc.core5.http.protocol.HttpContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;

@Component
public class HeaderForwardingInterceptor implements HttpRequestInterceptor {
    @Override
    public void process(HttpRequest httpRequest, EntityDetails entityDetails, HttpContext httpContext) throws HttpException, IOException {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        assert attributes != null;
        HttpServletRequest request = attributes.getRequest();
        if (request.getHeader("Authorization") != null) {
            httpRequest.addHeader("Authorization", request.getHeader("Authorization"));
        }
    }
}
