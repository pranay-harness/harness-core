package io.harness.ci.plan.creator.step;

import io.harness.plancreator.steps.GenericStepPMSPlanCreator;

import com.google.common.collect.Sets;
import java.util.Set;

public class CIPMSStepPlanCreator extends GenericStepPMSPlanCreator {
  @Override
  public Set<String> getSupportedStepTypes() {
    return Sets.newHashSet("SaveCacheS3", "Test", "TestIntelligence", "SaveCache", "liteEngineTask", "GitClone",
        "BuildAndPushGCR", "BuildAndPushECR", "BuildAndPushDockerHub", "Cleanup", "Plugin", "PublishArtifacts",
        "RestoreCacheGCS", "RestoreCacheS3", "RestoreCache", "SaveCacheGCS", "Run", "S3Upload", "GCSUpload",
        "ArtifactoryUpload");
  }
}