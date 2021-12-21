package io.harness.ci.config;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CIStepConfig {
  StepImageConfig gitCloneConfig;
  StepImageConfig buildAndPushDockerRegistryConfig;
  StepImageConfig buildAndPushECRConfig;
  StepImageConfig buildAndPushGCRConfig;
  StepImageConfig gcsUploadConfig;
  StepImageConfig s3UploadConfig;
  StepImageConfig artifactoryUploadConfig;
  StepImageConfig cacheGCSConfig;
  StepImageConfig cacheS3Config;
  VmImageConfig vmImageConfig;
}
