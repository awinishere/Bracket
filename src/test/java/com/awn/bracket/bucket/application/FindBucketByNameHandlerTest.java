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

class FindBucketByNameHandlerTest {

    private InMemoryBucketRepository repository;
    private FindBucketByNameHandler handler;

    @BeforeEach
    void setUp() {
        repository = new InMemoryBucketRepository().seed(
                Bucket.restore(UUID.randomUUID(), "archive-2024",
                        VisibilityType.PRIVATE, "ap-southeast-1",
                        LocalDateTime.now().minusDays(2), LocalDateTime.now())
        );
        handler = new FindBucketByNameHandler(repository);
    }

    @Test
    @DisplayName("returns the bucket whose name matches exactly")
    void returnsExistingBucket() {
        Optional<Bucket> found = handler.find("archive-2024");

        assertTrue(found.isPresent());
        assertEquals(VisibilityType.PRIVATE, found.get().visibility());
    }

    @Test
    @DisplayName("returns empty for an unknown name")
    void returnsEmptyForUnknownName() {
        assertFalse(handler.find("does-not-exist").isPresent());
    }

    @Test
    @DisplayName("returns empty for null (guard against NPE)")
    void returnsEmptyForNull() {
        assertFalse(handler.find(null).isPresent());
    }

    @Test
    @DisplayName("returns empty for blank string (guard against useless query)")
    void returnsEmptyForBlank() {
        assertFalse(handler.find("   ").isPresent());
    }

    @Test
    @DisplayName("name lookup is case-sensitive")
    void caseSensitive() {
        assertFalse(handler.find("ARCHIVE-2024").isPresent(),
                "postgres default collation is case-sensitive; handler must not silently lowercase");
    }
}
