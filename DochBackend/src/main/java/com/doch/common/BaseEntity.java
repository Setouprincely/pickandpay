package com.doch.common;

import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@MappedSuperclass
public abstract class BaseEntity {
    @Getter
    @Setter
    private Boolean isDeleted = false;
    private Date createdAt = new Date();
    private Date updatedAt = new Date();

    @PrePersist
    public void prePersist() {
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = new Date();
    }

    // public Boolean getIsDeleted() {
    //     return isDeleted;
    // }


    public Date getCreatedAt() {
        return createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }
}
