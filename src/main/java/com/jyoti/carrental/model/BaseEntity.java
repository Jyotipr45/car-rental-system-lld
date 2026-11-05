package com.jyoti.carrental.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Common parent of every persisted object.
 *
 * <p><b>Why an abstract class (and not an interface)?</b> Every entity shares the same
 * <i>data</i> (id, audit timestamps) and the same <i>code</i> (equals/hashCode by id).
 * An interface cannot hold fields, so the shared state lives here. It is abstract because
 * a "BaseEntity" on its own means nothing and must never be created directly.</p>
 *
 * <p>In a Spring Boot / JPA version this becomes a {@code @MappedSuperclass}.</p>
 *
 * @author Jyoti Prakash Parida
 */
public abstract class BaseEntity {

    private Long id;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    /** Assigned once by the repository on first save. */
    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    /** Two entities are equal when they are the same persisted row (same type and id). */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BaseEntity other = (BaseEntity) o;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass(), id);
    }
}
