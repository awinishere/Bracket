package com.awn.bracket.bucket.application;

import com.awn.bracket.bucket.domain.Bucket;
import com.awn.bracket.bucket.domain.VisibilityType;
import com.awn.bracket.bucket.ports.in.CreateBucketUseCase;
import com.awn.bracket.bucket.ports.out.BucketRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class CreateBucketHandler implements CreateBucketUseCase {

    @Inject
    BucketRepository repository;

    @Override
    public Bucket create(String name, VisibilityType visibility, String region) {
        if (repository.findByName(name).isPresent()) {
            throw new IllegalStateException("Bucket with name '" + name + "' already exists");
        }
        return repository.save(Bucket.newBucket(name, visibility, region));
    }
}
