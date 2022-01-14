package io.harness.beans.steps;

import static io.harness.annotations.dev.HarnessTeam.CI;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CI)
public interface StepSpecTypeConstants {
  String RUN = "Run";
  String RUN_TEST = "RunTests";
  String PLUGIN = "Plugin";
  String RESTORE_CACHE_GCS = "RestoreCacheGCS";
  String RESTORE_CACHE_S3 = "RestoreCacheS3";
  String SAVE_CACHE_GCS = "SaveCacheGCS";
  String SAVE_CACHE_S3 = "SaveCacheS3";
  String ARTIFACTORY_UPLOAD = "ArtifactoryUpload";
  String GCS_UPLOAD = "GCSUpload";
  String S3_UPLOAD = "S3Upload";

  String BUILD_AND_PUSH_GCR = "BuildAndPushGCR";
  String BUILD_AND_PUSH_ECR = "BuildAndPushECR";
  String BUILD_AND_PUSH_DOCKER_REGISTRY = "BuildAndPushDockerRegistry";
}
