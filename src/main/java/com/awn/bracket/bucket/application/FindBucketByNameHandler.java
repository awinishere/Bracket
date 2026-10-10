package com.awn.bracket.bucket.application;

import com.awn.bracket.bucket.domain.Bucket;
import com.awn.bracket.bucket.ports.in.FindBucketByNameUseCase;
import com.awn.bracket.bucket.ports.out.BucketRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Optional;

@ApplicationScoped
public class FindBucketByNameHandler implements FindBucketByNameUseCase {

    @Inject
    BucketRepository repository;

    @Override
    public Optional<Bucket> find(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        return repository.findByName(name);
    }
}
