package com.awn.bracket.bucket.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BucketTest {

    @Nested
    @DisplayName("newBucket factory")
    class NewBucketFactory {

        @Test
        @DisplayName("leaves id and timestamps null so the DB can assign them")
        void leavesIdAndTimestampsNull() {
            Bucket b = Bucket.newBucket("media-eu", VisibilityType.PRIVATE, "eu-central-1");

            assertNull(b.id(), "id must be null before persistence");
            assertNull(b.createdAt(), "createdAt must be null before persistence");
            assertNull(b.updatedAt(), "updatedAt must be null before persistence");
            assertEquals("media-eu", b.name());
            assertEquals(VisibilityType.PRIVATE, b.visibility());
            assertEquals("eu-central-1", b.region());
        }

        @Test
        @DisplayName("rejects blank name")
        void rejectsBlankName() {
            var ex = assertThrows(IllegalArgumentException.class,
                    () -> Bucket.newBucket("   ", VisibilityType.PUBLIC, "us-east-1"));
            assertTrue(ex.getMessage().toLowerCase().contains("name"),
                    "error message should mention the invalid field but was: " + ex.getMessage());
        }

        @Test
        @DisplayName("rejects null name")
        void rejectsNullName() {
            assertThrows(IllegalArgumentException.class,
                    () -> Bucket.newBucket(null, VisibilityType.PUBLIC, "us-east-1"));
        }

        @Test
        @DisplayName("rejects null visibility")
        void rejectsNullVisibility() {
            assertThrows(IllegalArgumentException.class,
                    () -> Bucket.newBucket("ok-name", null, "us-east-1"));
        }

        @Test
        @DisplayName("rejects blank region")
        void rejectsBlankRegion() {
            assertThrows(IllegalArgumentException.class,
                    () -> Bucket.newBucket("ok-name", VisibilityType.PUBLIC, "  "));
        }

        @Test
        @DisplayName("rejects null region")
        void rejectsNullRegion() {
            assertThrows(IllegalArgumentException.class,
                    () -> Bucket.newBucket("ok-name", VisibilityType.PUBLIC, null));
        }
    }

    @Nested
    @DisplayName("restore factory")
    class RestoreFactory {

        @Test
        @DisplayName("preserves every field including id and timestamps")
        void preservesAllFields() {
            UUID id = UUID.randomUUID();
            LocalDateTime created = LocalDateTime.now().minusDays(3);
            LocalDateTime updated = created.plusHours(2);

            Bucket b = Bucket.restore(id, "archive", VisibilityType.PUBLIC, "ap-southeast-1", created, updated);

            assertEquals(id, b.id());
            assertEquals("archive", b.name());
            assertEquals(VisibilityType.PUBLIC, b.visibility());
            assertEquals("ap-southeast-1", b.region());
            assertEquals(created, b.createdAt());
            assertEquals(updated, b.updatedAt());
        }

        @Test
        @DisplayName("still enforces invariants (blank name is rejected)")
        void stillEnforcesInvariants() {
            assertThrows(IllegalArgumentException.class,
                    () -> Bucket.restore(UUID.randomUUID(), "",
                            VisibilityType.PUBLIC, "x", LocalDateTime.now(), LocalDateTime.now()));
        }
    }

    @Nested
    @DisplayName("with* copy methods")
    class CopyMethods {

        @Test
        @DisplayName("withId returns a new instance carrying the given id, everything else unchanged")
        void withIdReplacesIdOnly() {
            Bucket original = Bucket.newBucket("media", VisibilityType.PRIVATE, "eu");
            UUID fresh = UUID.randomUUID();

            Bucket updated = original.withId(fresh);

            assertNotSameButEqual(original, updated);
            assertNull(original.id(), "original must stay untouched");
            assertEquals(fresh, updated.id());
            assertEquals("media", updated.name());
            assertEquals(VisibilityType.PRIVATE, updated.visibility());
            assertEquals("eu", updated.region());
        }

        @Test
        @DisplayName("withTimestamps returns a new instance carrying the given timestamps")
        void withTimestampsReplacesTimestampsOnly() {
            Bucket original = Bucket.newBucket("media", VisibilityType.PRIVATE, "eu");
            LocalDateTime created = LocalDateTime.now().minusDays(1);
            LocalDateTime updated = created.plusMinutes(5);

            Bucket stamped = original.withTimestamps(created, updated);

            assertNull(original.createdAt());
            assertNull(original.updatedAt());
            assertEquals(created, stamped.createdAt());
            assertEquals(updated, stamped.updatedAt());
            assertEquals("media", stamped.name());
        }

        private void assertNotSameButEqual(Bucket a, Bucket b) {
            assertNotNull(a);
            assertNotNull(b);

            assertTrue(!a.equals(b), "withId must return a new value, not mutate");
        }
    }
}
