package com.managify.projects.model;

import com.managify.model.common.generic.FullyAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Someone who can be assigned tasks and tasklets. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "workers")
public class Worker extends FullyAuditableEntity {

    @Column(nullable = false, length = 150)
    private String fullName;

    @Column(nullable = false, length = 200)
    private String email;

    /** Optional link to the worker's Keycloak account (the token's {@code sub}). */
    @Column(length = 64)
    private String keycloakUserId;

    /** Optional http(s) link to the worker's profile image. */
    @Column(length = 1000)
    private String imageUrl;

    @Column(nullable = false)
    private boolean active = true;
}
