package com.awn.bracket.bucket.adapters.out.persistence;

import com.awn.bracket.bucket.domain.Bucket;
import com.awn.bracket.bucket.ports.out.BucketRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class BucketRepositoryAdapter implements BucketRepository {

    @PersistenceContext
    EntityManager em;

    @Override
    @Transactional
    public Bucket save(Bucket bucket) {
        BucketEntity entity = BucketEntity.from(bucket);
        if (entity.id == null) {
            em.persist(entity);
        } else {
            entity = em.merge(entity);
        }

        em.flush();
        return entity.toDomain();
    }

    @Override
    public Optional<Bucket> findById(UUID id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(em.find(BucketEntity.class, id))
                       .map(BucketEntity::toDomain);
    }

    @Override
    public Optional<Bucket> findByName(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        return em.createQuery(
                        "SELECT b FROM BucketEntity b WHERE b.name = :name", BucketEntity.class)
                .setParameter("name", name)
                .setMaxResults(1)
                .getResultStream()
                .findFirst()
                .map(BucketEntity::toDomain);
    }

    @Override
    public List<Bucket> findAll() {
        return em.createQuery(
                        "SELECT b FROM BucketEntity b ORDER BY b.createdAt DESC", BucketEntity.class)
                .getResultStream()
                .map(BucketEntity::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public boolean delete(UUID id) {
        if (id == null) {
            return false;
        }
        int removed = em.createQuery("DELETE FROM BucketEntity b WHERE b.id = :id")
                        .setParameter("id", id)
                        .executeUpdate();
        return removed > 0;
    }
}
