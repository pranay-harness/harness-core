/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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
}
