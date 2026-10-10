package com.awn.bracket.bucket.ports.in;

import com.awn.bracket.bucket.domain.Bucket;
import com.awn.bracket.bucket.domain.VisibilityType;

public interface CreateBucketUseCase {

    Bucket create(String name, VisibilityType visibility, String region);
}
