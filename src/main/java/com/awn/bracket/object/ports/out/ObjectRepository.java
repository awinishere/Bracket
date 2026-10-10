package com.awn.bracket.object.ports.out;

import com.awn.bracket.object.domain.Object;

import java.util.Optional;
import java.util.UUID;

public interface ObjectRepository {
    Object save(Object object);
    Optional<Object> findById(UUID id);
    boolean delete(UUID id);
}
