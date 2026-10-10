package com.awn.bracket.object.ports.in;

import com.awn.bracket.object.domain.Object;

import java.io.InputStream;

public interface UploadObjectUseCase {
    Object upload(
            String bucketName,
            String name,
            String contentType,
            long contentLength,
            InputStream content,
            String checksum
    );
}
