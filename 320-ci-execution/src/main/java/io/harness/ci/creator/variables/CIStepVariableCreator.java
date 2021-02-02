package io.harness.ci.creator.variables;

import io.harness.pms.sdk.core.pipeline.variables.GenericStepVariableCreator;

import com.google.common.collect.Sets;
import java.util.Set;

public class CIStepVariableCreator extends GenericStepVariableCreator {
  @Override
  public Set<String> getSupportedStepTypes() {
    return Sets.newHashSet("SaveCacheS3", "BuildAndPushGCR", "BuildAndPushECR", "BuildAndPushDockerRegistry", "Plugin",
        "RestoreCacheGCS", "RestoreCacheS3", "SaveCacheGCS", "Run", "S3Upload", "GCSUpload", "ArtifactoryUpload");
  }
}
