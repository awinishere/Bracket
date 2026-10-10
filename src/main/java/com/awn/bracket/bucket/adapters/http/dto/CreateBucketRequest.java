package com.awn.bracket.bucket.adapters.in.http.dto;

import com.awn.bracket.bucket.domain.VisibilityType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateBucketRequest(
        @NotBlank(message = "bucket name must not be blank")
        String name,

        @NotNull(message = "bucket visibility must not be null")
        VisibilityType visibility,

        @NotBlank(message = "bucket region must not be blank")
        String region
) {
}
