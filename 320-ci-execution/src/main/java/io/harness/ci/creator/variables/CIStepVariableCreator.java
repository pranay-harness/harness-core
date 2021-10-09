package io.harness.ci.creator.variables;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.sdk.core.pipeline.variables.GenericStepVariableCreator;

import com.google.common.collect.Sets;
import java.util.Set;

@OwnedBy(HarnessTeam.CI)
public class CIStepVariableCreator extends GenericStepVariableCreator {
  @Override
  public Set<String> getSupportedStepTypes() {
    return Sets.newHashSet("SaveCacheS3", "BuildAndPushGCR", "BuildAndPushECR", "BuildAndPushDockerRegistry", "Plugin",
        "RestoreCacheGCS", "RestoreCacheS3", "SaveCacheGCS", "S3Upload", "GCSUpload", "ArtifactoryUpload");
  }
}