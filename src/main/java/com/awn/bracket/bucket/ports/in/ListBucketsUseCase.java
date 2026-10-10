package com.awn.bracket.bucket.ports.in;

import com.awn.bracket.bucket.domain.Bucket;

import java.util.List;

public interface ListBucketsUseCase {

    List<Bucket> list();
}
