package com.managify.model.common.audit;

import com.managify.model.common.generic.BaseEntity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import java.time.LocalDateTime;

public class AuditListener {

    @PrePersist
    public void onSave(Object o) {
        setAuditSection(o);
    }

    @PreUpdate
    public void onUpdate(Object o) {
        setAuditSection(o);
    }

    private void setAuditSection(Object o) {
        if (o instanceof BaseEntity item) {
            item.setDateLastUpdated(LocalDateTime.now());
            if (item.getDateCreated() == null) {
                item.setDateCreated(LocalDateTime.now());
            }
        }
    }
}
