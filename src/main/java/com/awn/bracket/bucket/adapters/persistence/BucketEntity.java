package com.awn.bracket.bucket.adapters.out.persistence;

import com.awn.bracket.bucket.domain.Bucket;
import com.awn.bracket.bucket.domain.VisibilityType;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "buckets", schema = "bucket")
public class BucketEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    public UUID id;

    @Column(name = "name", nullable = false, unique = true)
    public String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false)
    public VisibilityType visibility;

    @Column(name = "region", nullable = false)
    public String region;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    public LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    public LocalDateTime updatedAt;

    public static BucketEntity from(Bucket b) {
        BucketEntity e = new BucketEntity();
        e.id = b.id();
        e.name = b.name();
        e.visibility = b.visibility();
        e.region = b.region();

        return e;
    }

    public Bucket toDomain() {
        return Bucket.restore(id, name, visibility, region, createdAt, updatedAt);
    }
}
