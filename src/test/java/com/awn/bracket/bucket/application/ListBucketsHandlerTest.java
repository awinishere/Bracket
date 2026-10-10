package com.awn.bracket.bucket.application;

import com.awn.bracket.bucket.domain.Bucket;
import com.awn.bracket.bucket.domain.VisibilityType;
import com.awn.bracket.bucket.support.InMemoryBucketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ListBucketsHandlerTest {

    private InMemoryBucketRepository repository;
    private ListBucketsHandler handler;

    @BeforeEach
    void setUp() {
        repository = new InMemoryBucketRepository();
        handler = new ListBucketsHandler(repository);
    }

    @Test
    @DisplayName("returns an empty list when nothing has been persisted")
    void emptyStoreReturnsEmptyList() {
        List<Bucket> result = handler.list();

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("returns every persisted bucket exactly once")
    void returnsEveryBucket() {
        seed("a", VisibilityType.PUBLIC, "us-east-1");
        seed("b", VisibilityType.PRIVATE, "eu-west-1");
        seed("c", VisibilityType.PUBLIC, "ap-southeast-1");

        List<Bucket> result = handler.list();

        assertEquals(3, result.size());
        assertTrue(result.stream().anyMatch(b -> b.name().equals("a")));
        assertTrue(result.stream().anyMatch(b -> b.name().equals("b")));
        assertTrue(result.stream().anyMatch(b -> b.name().equals("c")));
    }

    @Test
    @DisplayName("reflects deletions on the next call")
    void reflectsDeletions() {
        Bucket kept = seed("keep", VisibilityType.PUBLIC, "us");
        Bucket doomed = seed("drop", VisibilityType.PUBLIC, "us");

        repository.delete(doomed.id());

        List<Bucket> result = handler.list();
        assertEquals(1, result.size());
        assertEquals("keep", result.get(0).name());
        assertEquals(kept.id(), result.get(0).id());
    }

    private Bucket seed(String name, VisibilityType visibility, String region) {
        return repository.save(Bucket.restore(
                UUID.randomUUID(), name, visibility, region,
                LocalDateTime.now().minusDays(1), LocalDateTime.now()));
    }
}
