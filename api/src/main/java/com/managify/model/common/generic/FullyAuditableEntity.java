package com.managify.model.common.generic;

import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;

/** Superclass for entities that also record who created and last modified them. */
@Getter
@Setter
@NoArgsConstructor
@MappedSuperclass
public class FullyAuditableEntity extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    private String createdBy;

    private String lastModifiedBy;
}
