package io.harness.batch.processing.service.intfc;

public interface AwsS3SyncService { void syncBuckets(String src, String srcRegion, String dest); }
