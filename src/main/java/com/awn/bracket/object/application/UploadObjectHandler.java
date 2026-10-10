package com.awn.bracket.object.application;

import com.awn.bracket.object.domain.Object;
import com.awn.bracket.object.ports.in.BucketLookup;
import com.awn.bracket.object.ports.in.UploadObjectUseCase;
import com.awn.bracket.object.ports.out.BlobStorage;
import com.awn.bracket.object.ports.out.ObjectRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.InputStream;
import java.math.BigInteger;

@ApplicationScoped
public class UploadObjectHandler implements UploadObjectUseCase {

    @Inject
    ObjectRepository objectRepository;

    @Inject
    BlobStorage blobStorage;

    @Inject
    BucketLookup bucketLookup;

    @Override
    public Object upload(String bucketName,
                         String name,
                         String contentType,
                         long contentLength,
                         InputStream content,
                         String checksum) {
        if (!bucketLookup.exists(bucketName)) {
            throw new IllegalStateException("Bucket '" + bucketName + "' does not exist");
        }

        blobStorage.put(storageKey(bucketName, name), content, contentLength, contentType);

        Object object = Object.newObject(
                bucketName,
                name,
                contentType,
                BigInteger.valueOf(contentLength),
                checksum);

        return objectRepository.save(object);
    }

    private String storageKey(String bucketName, String name) {
        return bucketName + "/" + name;
    }
}
