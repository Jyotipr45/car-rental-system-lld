package com.jyoti.carrental.repository;

import com.jyoti.carrental.model.BaseEntity;

import java.util.List;
import java.util.Optional;

/**
 * Generic data-access contract, a small copy of Spring Data's {@code CrudRepository}.
 *
 * <p><b>Why an interface?</b> Services depend on this contract, not on how data is stored.
 * Today the implementation is in-memory; tomorrow it can be JPA/MySQL without touching
 * a single service (Dependency Inversion, the "D" in SOLID).</p>
 *
 * @param <T> entity type
 * @author Jyoti Prakash Parida
 */
public interface CrudRepository<T extends BaseEntity> {

    /** Inserts when id is null (assigning a new id), otherwise updates. */
    T save(T entity);

    Optional<T> findById(Long id);

    List<T> findAll();

    boolean existsById(Long id);

    void deleteById(Long id);

    long count();
}
