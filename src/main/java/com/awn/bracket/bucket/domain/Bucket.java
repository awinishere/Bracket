package com.awn.bracket.bucket.domain;

import java.time.LocalDateTime;
import java.util.UUID;

public record Bucket(
        UUID id,
        String name,
        VisibilityType visibility,
        String region,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public Bucket {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Bucket name must not be blank");
        }
        if (visibility == null) {
            throw new IllegalArgumentException("Bucket visibility must not be null");
        }
        if (region == null || region.isBlank()) {
            throw new IllegalArgumentException("Bucket region must not be blank");
        }
    }

    public static Bucket newBucket(String name, VisibilityType visibility, String region) {
        return new Bucket(null, name, visibility, region, null, null);
    }

    public static Bucket restore(UUID id,
                                 String name,
                                 VisibilityType visibility,
                                 String region,
                                 LocalDateTime createdAt,
                                 LocalDateTime updatedAt) {
        return new Bucket(id, name, visibility, region, createdAt, updatedAt);
    }

    public Bucket withId(UUID newId) {
        return new Bucket(newId, name, visibility, region, createdAt, updatedAt);
    }

    public Bucket withTimestamps(LocalDateTime newCreatedAt, LocalDateTime newUpdatedAt) {
        return new Bucket(id, name, visibility, region, newCreatedAt, newUpdatedAt);
    }
}
