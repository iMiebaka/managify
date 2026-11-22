package com.managify.config.auth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Maps a Keycloak JWT to authorities:
 * <ul>
 *   <li>realm roles ({@code realm_access.roles}) as {@code role}</li>
 *   <li>client roles ({@code resource_access.<client>.roles}) as {@code client:role}</li>
 *   <li>groups ({@code groups}) as {@code group:name}</li>
 * </ul>
 * All lowercased, no {@code ROLE_} prefix (see {@code WebSecurityConfig#grantedAuthorityDefaults}).
 */
@Slf4j
@Component
public class KeycloakJwtRolesConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = Stream.of(
                extractRealmRoles(jwt),
                extractResourceRoles(jwt),
                extractGroups(jwt)
        ).flatMap(Collection::stream).collect(Collectors.toSet());
        log.debug("Extracted authorities: {}", authorities);
        return new JwtAuthenticationToken(jwt, authorities);
    }

    private Collection<GrantedAuthority> extractRealmRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess == null || !(realmAccess.get("roles") instanceof List<?> roles)) {
            return Collections.emptyList();
        }
        return roles.stream()
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority(String.valueOf(role).toLowerCase(Locale.ROOT)))
                .toList();
    }

    private Collection<GrantedAuthority> extractResourceRoles(Jwt jwt) {
        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
        if (resourceAccess == null) {
            return Collections.emptyList();
        }
        return resourceAccess.entrySet().stream()
                .filter(entry -> entry.getValue() instanceof Map)
                .flatMap(entry -> {
                    String resource = entry.getKey().toLowerCase(Locale.ROOT);
                    Object roles = ((Map<?, ?>) entry.getValue()).get("roles");
                    if (!(roles instanceof List<?> list)) return Stream.empty();
                    return list.stream().map(role -> (GrantedAuthority) new SimpleGrantedAuthority(
                            resource + ":" + String.valueOf(role).toLowerCase(Locale.ROOT)));
                })
                .toList();
    }

    private Collection<GrantedAuthority> extractGroups(Jwt jwt) {
        List<String> groups = jwt.getClaimAsStringList("groups");
        if (groups == null) {
            return Collections.emptyList();
        }
        return groups.stream()
                .map(group -> (GrantedAuthority) new SimpleGrantedAuthority("group:" + group.toLowerCase(Locale.ROOT)))
                .toList();
    }
}
