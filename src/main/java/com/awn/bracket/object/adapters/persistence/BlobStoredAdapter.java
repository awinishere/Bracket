package com.awn.bracket.object.adapters.persistence;

import com.awn.bracket.object.domain.BlobContent;
import com.awn.bracket.object.extensions.BlobNotFoundException;
import com.awn.bracket.object.ports.out.BlobStorage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.InputStream;

@ApplicationScoped
public class BlobStoredAdapter implements BlobStorage {

    private static final Logger LOG = Logger.getLogger(BlobStoredAdapter.class);

    private static final String FALLBACK_CONTENT_TYPE = "application/octet-stream";

    @Inject
    S3Client s3;

    @ConfigProperty(name = "bracket.blob.bucket")
    String bucket;

    @Override
    public void put(String key, InputStream content, long contentLength, String contentType) {
        requireKey(key);
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType != null ? contentType : FALLBACK_CONTENT_TYPE)
                .build();
        s3.putObject(request, RequestBody.fromInputStream(content, contentLength));
        LOG.debugf("stored blob '%s' (%d bytes) in bucket '%s'", key, contentLength, bucket);
    }

    @Override
    public BlobContent get(String key) {
        requireKey(key);
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();
        try {
            ResponseInputStream<GetObjectResponse> response = s3.getObject(request);
            GetObjectResponse meta = response.response();
            String contentType = meta.contentType() != null ? meta.contentType() : FALLBACK_CONTENT_TYPE;
            return new BlobContent(response, meta.contentLength(), contentType);
        } catch (NoSuchKeyException e) {
            throw new BlobNotFoundException("blob '" + key + "' not found");
        }
    }

    @Override
    public void delete(String key) {
        requireKey(key);
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();
        s3.deleteObject(request);
        LOG.debugf("deleted blob '%s' from bucket '%s'", key, bucket);
    }

    private void requireKey(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("blob key must not be blank");
        }
    }
}
