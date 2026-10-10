package com.awn.bracket.bucket.ports.in;

import com.awn.bracket.bucket.domain.Bucket;

import java.util.Optional;
import java.util.UUID;

public interface FindBucketByIdUseCase {

    Optional<Bucket> find(UUID id);
}
