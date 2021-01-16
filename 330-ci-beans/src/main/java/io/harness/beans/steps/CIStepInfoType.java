package io.harness.beans.steps;

import static io.harness.beans.steps.CIStepInfoType.CIStepExecEnvironment.CI_LITE_ENGINE;
import static io.harness.beans.steps.CIStepInfoType.CIStepExecEnvironment.CI_MANAGER;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

public enum CIStepInfoType {
  BUILD(CI_LITE_ENGINE, "Build"),
  TEST(CI_LITE_ENGINE, "Test"),
  SETUP_ENV(CI_MANAGER, "SetupEnv"),
  CLEANUP(CI_MANAGER, "Cleanup"),
  PUBLISH(CI_LITE_ENGINE, "Publish"),
  RUN(CI_LITE_ENGINE, "Run"),
  PLUGIN(CI_LITE_ENGINE, "Plugin"),
  GIT_CLONE(CI_LITE_ENGINE, "GitClone"),
  LITE_ENGINE_TASK(CI_LITE_ENGINE, "liteEngineTask"),
  SAVE_CACHE(CI_LITE_ENGINE, "SaveCache"),
  TEST_INTELLIGENCE(CI_LITE_ENGINE, "TestIntelligence"),
  RESTORE_CACHE(CI_LITE_ENGINE, "restoreCacheStepInfo"),
  ECR(CI_LITE_ENGINE, "BuildAndPushECR"),
  GCR(CI_LITE_ENGINE, "BuildAndPushGCR"),
  DOCKER(CI_LITE_ENGINE, "BuildAndPushDockerHub"),
  UPLOAD_GCS(CI_LITE_ENGINE, "GCSUpload"),
  UPLOAD_S3(CI_LITE_ENGINE, "S3Upload"),
  SAVE_CACHE_GCS(CI_LITE_ENGINE, "SaveCacheGCS"),
  RESTORE_CACHE_GCS(CI_LITE_ENGINE, "RestoreCacheGCS"),
  SAVE_CACHE_S3(CI_LITE_ENGINE, "SaveCacheGCS"),
  RESTORE_CACHE_S3(CI_LITE_ENGINE, "RestoreCacheS3"),
  UPLOAD_ARTIFACTORY(CI_LITE_ENGINE, "ArtifactoryUpload");

  @Getter private CIStepExecEnvironment ciStepExecEnvironment;

  private String displayName;

  @JsonValue
  public String getDisplayName() {
    return displayName;
  }

  CIStepInfoType(CIStepExecEnvironment ciStepExecEnvironment, String displayName) {
    this.ciStepExecEnvironment = ciStepExecEnvironment;
    this.displayName = displayName;
  }
  public enum CIStepExecEnvironment { CI_MANAGER, CI_LITE_ENGINE }
}
