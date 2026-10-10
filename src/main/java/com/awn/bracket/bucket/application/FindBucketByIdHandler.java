package com.awn.bracket.bucket.application;

import com.awn.bracket.bucket.domain.Bucket;
import com.awn.bracket.bucket.ports.in.FindBucketByIdUseCase;
import com.awn.bracket.bucket.ports.out.BucketRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class FindBucketByIdHandler implements FindBucketByIdUseCase {

    @Inject
    BucketRepository repository;

    @Override
    public Optional<Bucket> find(UUID id) {
        if (id == null) {
            return Optional.empty();
        }
        return repository.findById(id);
    }
}
