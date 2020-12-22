package io.harness.ci.plan.creator.step;

import io.harness.plancreator.steps.GenericStepPMSPlanCreator;

import com.google.common.collect.Sets;
import java.util.Set;

public class CIPMSStepPlanCreator extends GenericStepPMSPlanCreator {
  @Override
  public Set<String> getSupportedStepTypes() {
    return Sets.newHashSet("saveCacheS3", "test", "testIntelligence", "saveCache", "liteEngineTask", "gitClone",
        "buildAndPushGCR", "buildAndPushECR", "buildAndPushDockerHub", "cleanup", "plugin", "publishArtifacts",
        "restoreCacheGCS", "restoreCacheS3", "restoreCache", "saveCacheGCS", "run", "publishArtifacts");
  }
}