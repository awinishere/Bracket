package com.awn.bracket.object.adapters.bucketlink;

import com.awn.bracket.bucket.ports.in.FindBucketByNameUseCase;
import com.awn.bracket.object.ports.in.BucketLookup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class BucketLookupAdapter implements BucketLookup {

    @Inject
    FindBucketByNameUseCase findBucketByName;

    @Override
    public boolean exists(String bucketName) {
        if (bucketName == null || bucketName.isBlank()) {
            return false;
        }
        return findBucketByName.find(bucketName).isPresent();
    }
}
