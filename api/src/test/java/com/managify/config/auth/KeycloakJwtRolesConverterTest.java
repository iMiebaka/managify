package com.managify.config.auth;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakJwtRolesConverterTest {

    private final KeycloakJwtRolesConverter converter = new KeycloakJwtRolesConverter();

    @Test
    void mapsRealmRolesClientRolesAndGroups() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("realm_access", Map.of("roles", List.of("Admin", "user")))
                .claim("resource_access", Map.of("Managify-Web", Map.of("roles", List.of("Viewer"))))
                .claim("groups", List.of("Operations"))
                .build();

        List<String> authorities = converter.convert(jwt).getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).toList();

        assertThat(authorities).containsExactlyInAnyOrder(
                "admin", "user", "managify-web:viewer", "group:operations");
    }

    @Test
    void tokenWithoutRoleClaimsHasNoAuthorities() {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").claim("sub", "someone").build();

        assertThat(converter.convert(jwt).getAuthorities()).isEmpty();
    }
}
