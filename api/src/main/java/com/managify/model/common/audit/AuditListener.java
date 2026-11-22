package com.managify.model.common.audit;

import com.managify.model.common.generic.BaseEntity;
import com.managify.model.common.generic.FullyAuditableEntity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.LocalDateTime;

/** Fills the audit columns: timestamps, and for {@link FullyAuditableEntity} the user from the JWT. */
public class AuditListener {

    @PrePersist
    public void onSave(Object o) {
        setAuditSection(o, true);
    }

    @PreUpdate
    public void onUpdate(Object o) {
        setAuditSection(o, false);
    }

    private void setAuditSection(Object o, boolean creating) {
        if (o instanceof BaseEntity item) {
            item.setDateLastUpdated(LocalDateTime.now());
            if (item.getDateCreated() == null) {
                item.setDateCreated(LocalDateTime.now());
            }
        }
        if (o instanceof FullyAuditableEntity audited) {
            String user = currentUser();
            if (user != null) {
                if (creating && audited.getCreatedBy() == null) {
                    audited.setCreatedBy(user);
                }
                audited.setLastModifiedBy(user);
            }
        }
    }

    /** preferred_username from the Keycloak token, else the principal name; null outside a request. */
    private static String currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        if (auth instanceof JwtAuthenticationToken jwt) {
            String username = jwt.getToken().getClaimAsString("preferred_username");
            if (username != null && !username.isBlank()) {
                return username;
            }
        }
        return auth.getName();
    }
}
