/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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
