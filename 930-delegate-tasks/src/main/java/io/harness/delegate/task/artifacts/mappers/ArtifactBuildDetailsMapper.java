/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.task.artifacts.mappers;

import io.harness.artifacts.beans.BuildDetailsInternal;
import io.harness.delegate.task.artifacts.response.ArtifactBuildDetailsNG;

import java.util.Map;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ArtifactBuildDetailsMapper {
  public ArtifactBuildDetailsNG toBuildDetailsNG(BuildDetailsInternal buildDetailsInternal) {
    return ArtifactBuildDetailsNG.builder()
        .buildUrl(buildDetailsInternal.getBuildUrl())
        .metadata(buildDetailsInternal.getMetadata())
        .number(buildDetailsInternal.getNumber())
        .uiDisplayName(buildDetailsInternal.getUiDisplayName())
        .build();
  }

  public ArtifactBuildDetailsNG toBuildDetailsNG(Map<String, String> labelsMap, String tag) {
    return ArtifactBuildDetailsNG.builder().number(tag).labelsMap(labelsMap).build();
  }
}
