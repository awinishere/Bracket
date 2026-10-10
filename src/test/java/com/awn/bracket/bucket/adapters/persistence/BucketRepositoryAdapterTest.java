package com.awn.bracket.bucket.adapters.persistence;

import com.awn.bracket.bucket.domain.Bucket;
import com.awn.bracket.bucket.domain.VisibilityType;
import com.awn.bracket.bucket.ports.out.BucketRepository;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@DisplayName("BucketRepositoryAdapter - real Postgres queries")
class BucketRepositoryAdapterTest {

    @Inject
    BucketRepository repository;

    @Test
    @DisplayName("save() inserts a row; Postgres-backed id + @CreationTimestamp/@UpdateTimestamp are populated")
    void saveInsertsAndReadsBackFromDatabase() {
        String name = "it-save-" + System.nanoTime();
        Bucket saved = repository.save(Bucket.newBucket(name, VisibilityType.PRIVATE, "eu-west-1"));

        assertNotNull(saved.id(), "uuid must be assigned by persist/flush");
        assertNotNull(saved.createdAt());
        assertNotNull(saved.updatedAt());

        Optional<Bucket> found = repository.findById(saved.id());
        assertTrue(found.isPresent(), "row must be visible to a subsequent query");
        assertEquals(name, found.get().name());
        assertEquals(VisibilityType.PRIVATE, found.get().visibility());
        assertEquals("eu-west-1", found.get().region());
    }

    @Test
    @DisplayName("save() with an existing id performs an UPDATE (merge), not a second INSERT")
    void saveWithIdUpdatesExistingRow() {
        String name = "it-merge-" + System.nanoTime();
        Bucket created = repository.save(Bucket.newBucket(name, VisibilityType.PUBLIC, "us-east-1"));

        Bucket updated = Bucket.restore(created.id(), name, created.visibility(),
                "ap-southeast-1", created.createdAt(), created.updatedAt());
        Bucket result = repository.save(updated);

        assertEquals("ap-southeast-1", result.region());
        List<Bucket> all = repository.findAll();
        assertEquals(1, all.stream().filter(b -> b.name().equals(name)).count(),
                "merge must not create a duplicate row");
    }

    @Test
    @DisplayName("findByName() matches exactly and is case-sensitive (Postgres default collation)")
    void findByNameIsCaseSensitive() {
        String name = "IT-Case-" + System.nanoTime();
        repository.save(Bucket.newBucket(name, VisibilityType.PUBLIC, "eu"));

        assertTrue(repository.findByName(name).isPresent());
        assertFalse(repository.findByName(name.toLowerCase()).isPresent(),
                "plain equality query must not lowercase-match");
    }

    @Test
    @DisplayName("guards: null/blank lookups short-circuit to empty without hitting the DB")
    void lookupGuards() {
        assertFalse(repository.findById(null).isPresent());
        assertFalse(repository.findByName(null).isPresent());
        assertFalse(repository.findByName("   ").isPresent());
        assertFalse(repository.findById(UUID.randomUUID()).isPresent());
    }

    @Test
    @DisplayName("findAll() returns every persisted row")
    void findAllReturnsPersistedRows() {
        String n1 = "it-all-a-" + System.nanoTime();
        String n2 = "it-all-b-" + System.nanoTime();
        repository.save(Bucket.newBucket(n1, VisibilityType.PUBLIC, "eu"));
        repository.save(Bucket.newBucket(n2, VisibilityType.PRIVATE, "us"));

        List<Bucket> all = repository.findAll();
        assertTrue(all.stream().anyMatch(b -> b.name().equals(n1)));
        assertTrue(all.stream().anyMatch(b -> b.name().equals(n2)));
    }

    @Test
    @DisplayName("delete() removes the row; second call and unknown ids return false")
    void deleteRemovesRowAndIsIdempotent() {
        Bucket saved = repository.save(Bucket.newBucket("it-delete-" + System.nanoTime(),
                VisibilityType.PUBLIC, "eu"));

        assertTrue(repository.delete(saved.id()));
        assertFalse(repository.findById(saved.id()).isPresent(), "row must be gone from the DB");
        assertFalse(repository.delete(saved.id()), "second delete of the same id must be a no-op");
        assertFalse(repository.delete(UUID.randomUUID()));
        assertFalse(repository.delete(null));
    }

    @Test
    @DisplayName("unique index on name rejects duplicates at the DATABASE level")
    void uniqueNameConstraintIsEnforcedByPostgres() {
        String name = "it-unique-" + System.nanoTime();
        repository.save(Bucket.newBucket(name, VisibilityType.PUBLIC, "eu"));

        PersistenceException ex = assertThrows(PersistenceException.class,
                () -> repository.save(Bucket.newBucket(name, VisibilityType.PRIVATE, "us")));
        assertTrue(rootCauseMessage(ex).toLowerCase().contains("unique")
                        || rootCauseMessage(ex).contains("23505"),
                "expected a unique-constraint violation but got: " + rootCauseMessage(ex));

        assertEquals(1, repository.findAll().stream().filter(b -> b.name().equals(name)).count());
    }

    private static String rootCauseMessage(Throwable t) {
        Throwable c = t;
        while (c.getCause() != null) {
            c = c.getCause();
        }
        return String.valueOf(c.getMessage());
    }
}
