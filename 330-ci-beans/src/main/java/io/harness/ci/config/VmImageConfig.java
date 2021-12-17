package io.harness.ci.config;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class VmImageConfig {
  String gitClone;
  String buildAndPushDockerRegistry;
  String buildAndPushECR;
  String buildAndPushGCR;
  String gcsUpload;
  String s3Upload;
  String artifactoryUpload;
  String cacheGCS;
  String cacheS3;
}
