package com.awn.bracket.bucket.ports.in;

import java.util.UUID;

public interface DeleteBucketUseCase {

    boolean delete(UUID id);
}
