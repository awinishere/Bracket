package com.awn.bracket.bucket.adapters.http.dto;

import com.awn.bracket.bucket.domain.Bucket;
import com.awn.bracket.bucket.domain.VisibilityType;

import java.time.LocalDateTime;
import java.util.UUID;

public record BucketResponse(
        UUID id,
        String name,
        VisibilityType visibility,
        String region,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static BucketResponse from(Bucket b) {
        return new BucketResponse(
                b.id(),
                b.name(),
                b.visibility(),
                b.region(),
                b.createdAt(),
                b.updatedAt()
        );
    }
}
