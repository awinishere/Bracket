package com.awn.bracket.bucket.application;

import com.awn.bracket.bucket.domain.Bucket;
import com.awn.bracket.bucket.domain.VisibilityType;
import com.awn.bracket.bucket.support.InMemoryBucketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreateBucketHandlerTest {

    private InMemoryBucketRepository repository;
    private CreateBucketHandler handler;

    @BeforeEach
    void setUp() {
        repository = new InMemoryBucketRepository();

        handler = new CreateBucketHandler();
        handler.repository = repository;
    }

    @Test
    @DisplayName("persists the new bucket and returns it with an assigned id + timestamps")
    void persistsAndReturnsId() {
        Bucket saved = handler.create("media-eu", VisibilityType.PRIVATE, "eu-central-1");

        assertNotNull(saved.id(), "handler must return a Bucket carrying a persisted id");
        assertNotNull(saved.createdAt());
        assertNotNull(saved.updatedAt());
        assertEquals("media-eu", saved.name());
        assertEquals(VisibilityType.PRIVATE, saved.visibility());
        assertEquals("eu-central-1", saved.region());
        assertEquals(1, repository.size());
    }

    @Test
    @DisplayName("rejects a duplicate name with IllegalStateException")
    void rejectsDuplicateName() {
        handler.create("dup", VisibilityType.PUBLIC, "us-east-1");

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> handler.create("dup", VisibilityType.PRIVATE, "eu-west-1"));
        assertTrue(ex.getMessage().contains("dup"),
                "message should mention the offending name; was: " + ex.getMessage());
        assertEquals(1, repository.size(), "no extra row should be written on conflict");
    }

    @Test
    @DisplayName("propagates IllegalArgumentException from the domain when the name is blank")
    void propagatesDomainValidationForBlankName() {
        assertThrows(IllegalArgumentException.class,
                () -> handler.create("   ", VisibilityType.PUBLIC, "us-east-1"));
        assertEquals(0, repository.size(), "domain must reject BEFORE persistence");
    }

    @Test
    @DisplayName("propagates IllegalArgumentException from the domain when visibility is null")
    void propagatesDomainValidationForNullVisibility() {
        assertThrows(IllegalArgumentException.class,
                () -> handler.create("ok-name", null, "us-east-1"));
        assertEquals(0, repository.size());
    }

    @Test
    @DisplayName("propagates IllegalArgumentException from the domain when the region is blank")
    void propagatesDomainValidationForBlankRegion() {
        assertThrows(IllegalArgumentException.class,
                () -> handler.create("ok-name", VisibilityType.PUBLIC, ""));
        assertEquals(0, repository.size());
    }
}
