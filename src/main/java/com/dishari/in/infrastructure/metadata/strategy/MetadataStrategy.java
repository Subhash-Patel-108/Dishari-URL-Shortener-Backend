package com.dishari.in.infrastructure.metadata.strategy;

import com.dishari.in.infrastructure.metadata.RawMetadata;

import java.util.Optional;

public interface MetadataStrategy {
    Optional<RawMetadata> extract(String url, String html);
    String name();
}