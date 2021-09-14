/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.task.artifacts.mappers;

import io.harness.artifacts.beans.BuildDetailsInternal;
import io.harness.artifacts.gcr.beans.GcrInternalConfig;
import io.harness.delegate.task.artifacts.ArtifactSourceType;
import io.harness.delegate.task.artifacts.gcr.GcrArtifactDelegateRequest;
import io.harness.delegate.task.artifacts.gcr.GcrArtifactDelegateResponse;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.experimental.UtilityClass;

@UtilityClass
public class GcrRequestResponseMapper {
  public GcrInternalConfig toGcrInternalConfig(GcrArtifactDelegateRequest request, String basicAuthHeader) {
    return GcrInternalConfig.builder()
        .registryHostname(request.getRegistryHostname())
        .basicAuthHeader(basicAuthHeader)
        .build();
  }

  public GcrArtifactDelegateResponse toGcrResponse(
      BuildDetailsInternal buildDetailsInternal, GcrArtifactDelegateRequest request) {
    return GcrArtifactDelegateResponse.builder()
        .buildDetails(ArtifactBuildDetailsMapper.toBuildDetailsNG(buildDetailsInternal))
        .imagePath(request.getImagePath())
        .tag(buildDetailsInternal.getNumber())
        .sourceType(ArtifactSourceType.GCR)
        .build();
  }

  public List<GcrArtifactDelegateResponse> toGcrResponse(
      List<Map<String, String>> labelsList, GcrArtifactDelegateRequest request) {
    return IntStream.range(0, request.getTagsList().size())
        .mapToObj(i
            -> GcrArtifactDelegateResponse.builder()
                   .buildDetails(
                       ArtifactBuildDetailsMapper.toBuildDetailsNG(labelsList.get(i), request.getTagsList().get(i)))
                   .imagePath(request.getImagePath())
                   .sourceType(ArtifactSourceType.GCR)
                   .build())
        .collect(Collectors.toList());
  }
}
