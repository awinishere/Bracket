package com.awn.bracket.bucket.application;

import com.awn.bracket.bucket.domain.Bucket;
import com.awn.bracket.bucket.ports.in.ListBucketsUseCase;
import com.awn.bracket.bucket.ports.out.BucketRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

@ApplicationScoped
public class ListBucketsHandler implements ListBucketsUseCase {

    private final BucketRepository repository;

    @Inject
    public ListBucketsHandler(BucketRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Bucket> list() {
        return repository.findAll();
    }
}
