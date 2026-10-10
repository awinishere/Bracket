package com.awn.bracket.object.adapters.persistence;

import com.awn.bracket.object.domain.Object;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "objects", schema = "object")
public class ObjectEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    public UUID id;

    @Column(name = "bucket_name", nullable = false)
    public String bucketName;

    @Column(name = "name", nullable = false)
    public String name;

    @Column(name = "content_type")
    public String contentType;

    @Column(name = "size_bytes", nullable = false)
    public BigInteger sizeBytes;

    @Column(name = "checksum", nullable = false)
    public String checksum;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    public LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    public LocalDateTime updatedAt;

    public static ObjectEntity from(Object o) {
        ObjectEntity e = new ObjectEntity();
        e.id = o.id();
        e.bucketName = o.bucketName();
        e.name = o.name();
        e.contentType = o.contentType();
        e.sizeBytes = o.sizeBytes();
        e.checksum = o.checksum();
        return e;
    }

    public Object toDomain() {
        return Object.restore(id, bucketName, name, contentType, sizeBytes, checksum, createdAt, updatedAt);
    }
}
