/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.beans.steps;

import io.harness.beans.plugin.compatible.PluginCompatibleStep;
import io.harness.ci.config.CIExecutionServiceConfig;
import io.harness.ci.config.StepImageConfig;

import java.util.List;

public class CIStepInfoUtils {
  public static String getPluginCustomStepImage(
      PluginCompatibleStep step, CIExecutionServiceConfig ciExecutionServiceConfig) {
    return getPluginCustomStepImageConfig(step, ciExecutionServiceConfig).getImage();
  }

  public static List<String> getPluginCustomStepEntrypoint(
      PluginCompatibleStep step, CIExecutionServiceConfig ciExecutionServiceConfig) {
    return getPluginCustomStepImageConfig(step, ciExecutionServiceConfig).getEntrypoint();
  }

  private static StepImageConfig getPluginCustomStepImageConfig(
      PluginCompatibleStep step, CIExecutionServiceConfig ciExecutionServiceConfig) {
    switch (step.getStepType().getType()) {
      case "BuildAndPushDockerRegistry":
        return ciExecutionServiceConfig.getStepConfig().getBuildAndPushDockerRegistryConfig();
      case "BuildAndPushGCR":
        return ciExecutionServiceConfig.getStepConfig().getBuildAndPushGCRConfig();
      case "BuildAndPushECR":
        return ciExecutionServiceConfig.getStepConfig().getBuildAndPushECRConfig();
      case "RestoreCacheS3":
      case "SaveCacheS3":
        return ciExecutionServiceConfig.getStepConfig().getCacheS3Config();
      case "S3Upload":
        return ciExecutionServiceConfig.getStepConfig().getS3UploadConfig();
      case "GCSUpload":
        return ciExecutionServiceConfig.getStepConfig().getGcsUploadConfig();
      case "SaveCacheGCS":
      case "RestoreCacheGCS":
        return ciExecutionServiceConfig.getStepConfig().getCacheGCSConfig();
      case "ArtifactoryUpload":
        return ciExecutionServiceConfig.getStepConfig().getArtifactoryUploadConfig();
      default:
        throw new IllegalStateException("Unexpected value: " + step.getStepType().getType());
    }
  }
}
