package com.awn.bracket.bucket.support;

import com.awn.bracket.bucket.domain.Bucket;
import com.awn.bracket.bucket.ports.out.BucketRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class InMemoryBucketRepository implements BucketRepository {

    private final Map<UUID, Bucket> store = new LinkedHashMap<>();

    public InMemoryBucketRepository seed(Bucket... buckets) {
        for (Bucket b : buckets) {
            if (b.id() == null) {
                throw new IllegalArgumentException("seed buckets must have an id");
            }
            store.put(b.id(), b);
        }
        return this;
    }

    @Override
    public Bucket save(Bucket bucket) {
        LocalDateTime now = LocalDateTime.now();
        UUID id = bucket.id() != null ? bucket.id() : UUID.randomUUID();
        LocalDateTime createdAt = bucket.createdAt() != null ? bucket.createdAt() : now;
        Bucket persisted = Bucket.restore(
                id,
                bucket.name(),
                bucket.visibility(),
                bucket.region(),
                createdAt,
                now
        );
        store.put(id, persisted);
        return persisted;
    }

    @Override
    public Optional<Bucket> findById(UUID id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Optional<Bucket> findByName(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        return store.values().stream()
                .filter(b -> name.equals(b.name()))
                .findFirst();
    }

    @Override
    public List<Bucket> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public boolean delete(UUID id) {
        if (id == null) {
            return false;
        }
        return store.remove(id) != null;
    }

    public int size() {
        return store.size();
    }

    public void clear() {
        store.clear();
    }
}
