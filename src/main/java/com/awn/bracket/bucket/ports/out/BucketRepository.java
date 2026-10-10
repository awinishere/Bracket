package com.awn.bracket.bucket.ports.out;

import com.awn.bracket.bucket.domain.Bucket;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BucketRepository {

    Bucket save(Bucket bucket);

    Optional<Bucket> findById(UUID id);

    Optional<Bucket> findByName(String name);

    List<Bucket> findAll();

    boolean delete(UUID id);
}
