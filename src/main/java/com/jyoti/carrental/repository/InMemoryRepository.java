package com.jyoti.carrental.repository;

import com.jyoti.carrental.model.BaseEntity;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

/**
 * Thread-safe in-memory implementation shared by all repositories.
 *
 * <p><b>Template / code reuse via abstract class:</b> the CRUD mechanics (id generation,
 * timestamps, storage) are written once here. Concrete repositories only add their own
 * finder methods, exactly like {@code interface BookingRepository extends JpaRepository}
 * in Spring Data.</p>
 *
 * <p>{@link ConcurrentHashMap} + {@link AtomicLong} make it safe to call from many threads.</p>
 *
 * @param <T> entity type
 * @author Jyoti Prakash Parida
 */
public abstract class InMemoryRepository<T extends BaseEntity> implements CrudRepository<T> {

    private final Map<Long, T> store = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();
    private final Clock clock;

    protected InMemoryRepository(Clock clock) {
        this.clock = clock;
    }

    @Override
    public T save(T entity) {
        LocalDateTime now = LocalDateTime.now(clock);
        if (entity.getId() == null) {
            entity.setId(sequence.incrementAndGet());
            entity.setCreatedAt(now);
        }
        entity.setUpdatedAt(now);
        store.put(entity.getId(), entity);
        return entity;
    }

    @Override
    public Optional<T> findById(Long id) {
        return id == null ? Optional.empty() : Optional.ofNullable(store.get(id));
    }

    @Override
    public List<T> findAll() {
        return findWhere(e -> true);
    }

    @Override
    public boolean existsById(Long id) {
        return id != null && store.containsKey(id);
    }

    @Override
    public void deleteById(Long id) {
        store.remove(id);
    }

    @Override
    public long count() {
        return store.size();
    }

    /**
     * Helper for subclasses: the in-memory equivalent of a SQL {@code WHERE} clause.
     * Results are sorted by id so output is stable.
     */
    protected List<T> findWhere(Predicate<T> condition) {
        List<T> result = new ArrayList<>();
        for (T entity : store.values()) {
            if (condition.test(entity)) {
                result.add(entity);
            }
        }
        result.sort(Comparator.comparing(BaseEntity::getId));
        return result;
    }
}
