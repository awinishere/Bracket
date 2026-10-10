package com.awn.bracket.bucket.application;

import com.awn.bracket.bucket.domain.Bucket;
import com.awn.bracket.bucket.domain.VisibilityType;
import com.awn.bracket.bucket.support.InMemoryBucketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FindBucketByIdHandlerTest {

    private InMemoryBucketRepository repository;
    private FindBucketByIdHandler handler;
    private UUID seedId;

    @BeforeEach
    void setUp() {
        seedId = UUID.randomUUID();
        repository = new InMemoryBucketRepository().seed(
                Bucket.restore(seedId, "media", VisibilityType.PRIVATE, "eu",
                        LocalDateTime.now().minusDays(1), LocalDateTime.now())
        );
        handler = new FindBucketByIdHandler(repository);
    }

    @Test
    @DisplayName("returns the matching bucket when the id exists")
    void returnsExistingBucket() {
        Optional<Bucket> found = handler.find(seedId);

        assertTrue(found.isPresent());
        assertEquals("media", found.get().name());
    }

    @Test
    @DisplayName("returns empty Optional for an unknown id (no exception)")
    void returnsEmptyForUnknownId() {
        assertFalse(handler.find(UUID.randomUUID()).isPresent());
    }

    @Test
    @DisplayName("returns empty Optional when id is null (guard against NPE)")
    void returnsEmptyForNullId() {
        assertFalse(handler.find(null).isPresent());
    }
}
