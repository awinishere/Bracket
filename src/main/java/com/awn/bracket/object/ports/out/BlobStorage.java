package com.awn.bracket.object.ports.out;

import com.awn.bracket.object.domain.BlobContent;

import java.io.InputStream;

public interface BlobStorage {
    void put(String key, InputStream content, long contentLength, String contentType);
    BlobContent get(String key);
    void delete(String key);
}
