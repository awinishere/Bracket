package com.awn.bracket.bucket.application;

import com.awn.bracket.bucket.domain.Bucket;
import com.awn.bracket.bucket.domain.VisibilityType;
import com.awn.bracket.bucket.support.InMemoryBucketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeleteBucketHandlerTest {

    private InMemoryBucketRepository repository;
    private DeleteBucketHandler handler;
    private UUID existingId;

    @BeforeEach
    void setUp() {
        existingId = UUID.randomUUID();
        repository = new InMemoryBucketRepository().seed(
                Bucket.restore(existingId, "old-media", VisibilityType.PRIVATE, "eu",
                        LocalDateTime.now().minusDays(5), LocalDateTime.now())
        );
        handler = new DeleteBucketHandler();
        handler.repository = repository;
    }

    @Test
    @DisplayName("returns true and removes the row when the id exists")
    void removesExistingBucket() {
        boolean removed = handler.delete(existingId);

        assertTrue(removed);
        assertEquals(0, repository.size());
        assertFalse(repository.findById(existingId).isPresent());
    }

    @Test
    @DisplayName("returns false when the id is unknown (idempotent delete)")
    void returnsFalseForUnknownId() {
        assertFalse(handler.delete(UUID.randomUUID()));
        assertEquals(1, repository.size(), "unknown delete must not touch the store");
    }

    @Test
    @DisplayName("returns false when the id is null (guard against NPE)")
    void returnsFalseForNullId() {
        assertFalse(handler.delete(null));
        assertEquals(1, repository.size());
    }

    @Test
    @DisplayName("second delete of the same id returns false")
    void secondDeleteIsFalse() {
        assertTrue(handler.delete(existingId));
        assertFalse(handler.delete(existingId), "delete must be idempotent: second call returns false");
    }
}
