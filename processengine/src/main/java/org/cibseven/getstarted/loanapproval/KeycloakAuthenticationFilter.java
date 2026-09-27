package org.cibseven.getstarted.loanapproval;

import jakarta.servlet.*;
import org.cibseven.bpm.engine.IdentityService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
public class KeycloakAuthenticationFilter implements Filter {

    private final IdentityService identityService;

    public KeycloakAuthenticationFilter(IdentityService identityService) {
        this.identityService = identityService;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            String username = jwt.getClaimAsString("preferred_username");
            try {
                identityService.setAuthentication(username, List.of("camunda-admin"));
                chain.doFilter(request, response);
            } finally {
                identityService.clearAuthentication();
            }

        } else {
            chain.doFilter(request, response);
        }
    }
}
