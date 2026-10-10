package com.awn.bracket.object.domain;

import java.io.InputStream;

public record BlobContent(
        InputStream stream,
        long contentLength,
        String contentType
) {
}
