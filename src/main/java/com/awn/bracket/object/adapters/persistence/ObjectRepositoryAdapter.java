package com.awn.bracket.object.adapters.persistence;

import com.awn.bracket.object.domain.Object;
import com.awn.bracket.object.ports.out.ObjectRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ObjectRepositoryAdapter implements ObjectRepository {

    @PersistenceContext
    EntityManager em;

    @Override
    @Transactional
    public Object save(Object object) {
        ObjectEntity entity = ObjectEntity.from(object);
        if (entity.id == null) {
            em.persist(entity);
        } else {
            entity = em.merge(entity);
        }

        em.flush();
        return entity.toDomain();
    }

    @Override
    public Optional<Object> findById(UUID id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(em.find(ObjectEntity.class, id))
                       .map(ObjectEntity::toDomain);
    }

    @Override
    @Transactional
    public boolean delete(UUID id) {
        if (id == null) {
            return false;
        }
        int removed = em.createQuery("DELETE FROM ObjectEntity o WHERE o.id = :id")
                        .setParameter("id", id)
                        .executeUpdate();
        return removed > 0;
    }
}
