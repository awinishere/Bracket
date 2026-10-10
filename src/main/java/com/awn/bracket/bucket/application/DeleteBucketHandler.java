package com.awn.bracket.bucket.application;

import com.awn.bracket.bucket.ports.in.DeleteBucketUseCase;
import com.awn.bracket.bucket.ports.out.BucketRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.UUID;

@ApplicationScoped
public class DeleteBucketHandler implements DeleteBucketUseCase {

    @Inject
    BucketRepository repository;

    @Override
    public boolean delete(UUID id) {
        if (id == null) {
            return false;
        }
        return repository.delete(id);
    }
}
