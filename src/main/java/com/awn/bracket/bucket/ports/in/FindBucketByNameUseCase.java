package com.awn.bracket.bucket.ports.in;

import com.awn.bracket.bucket.domain.Bucket;

import java.util.Optional;

public interface FindBucketByNameUseCase {

    Optional<Bucket> find(String name);
}
