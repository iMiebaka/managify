package com.managify.config.auth;

/**
 * SpEL expressions for @PreAuthorize. Role names are Keycloak realm roles, lowercased by
 * KeycloakJwtRolesConverter. Add roles here as endpoints are built.
 */
public final class Roles {

    private Roles() {}

    public static final String ADMIN = "admin";

    public static final String USER = "user";

    public static final String HAS_ADMIN = "hasRole('admin')";

    public static final String HAS_USER = "hasRole('user')";

    public static final String HAS_USER_OR_ADMIN = "hasAnyRole('user', 'admin')";
}
