package com.awn.bracket.object.domain;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.UUID;

public record Object(
        UUID id,
        String bucketName,
        String name,
        String contentType,
        BigInteger sizeBytes,
        String checksum,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
){
    public Object {
        if (bucketName == null || bucketName.isBlank()){
            throw new IllegalArgumentException("Bucket name must not null");
        }
        if (name == null || name.isBlank()){
            throw new IllegalArgumentException("Name must not null");
        }
        if (sizeBytes == null ){
            throw new IllegalArgumentException("size bytes must not null");
        }
        if (checksum == null || checksum.isBlank()){
            throw new IllegalArgumentException("checksum must not null");
        }
    }

    public static Object newObject(String bucketName,
                                   String name,
                                   String contentType,
                                   BigInteger sizeBytes,
                                   String checksum) {
        return new Object(null, bucketName, name, contentType, sizeBytes, checksum, null, null);
    }

    public static Object restore(UUID id,
                                 String bucketName,
                                 String name,
                                 String contentType,
                                 BigInteger sizeBytes,
                                 String checksum,
                                 LocalDateTime createdAt,
                                 LocalDateTime updatedAt) {
        return new Object(id, bucketName, name, contentType, sizeBytes, checksum, createdAt, updatedAt);
    }

    public Object withId(UUID newId) {
        return new Object(newId, bucketName, name, contentType, sizeBytes, checksum, createdAt, updatedAt);
    }

    public Object withTimestamps(LocalDateTime newCreatedAt, LocalDateTime newUpdatedAt) {
        return new Object(id, bucketName, name, contentType, sizeBytes, checksum, newCreatedAt, newUpdatedAt);
    }
}
