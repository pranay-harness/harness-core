/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.task.artifacts.ecr;

import io.harness.delegate.task.artifacts.ArtifactSourceType;
import io.harness.delegate.task.artifacts.response.ArtifactBuildDetailsNG;
import io.harness.delegate.task.artifacts.response.ArtifactDelegateResponse;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@EqualsAndHashCode(callSuper = false)
public class EcrArtifactDelegateResponse extends ArtifactDelegateResponse {
  /** Images in repos need to be referenced via a path */
  String imagePath;
  /** Tag refers to exact tag number */
  String tag;
  /** imageUrl refers to full path to image */
  String imageUrl;
  String authToken;

  @Builder
  public EcrArtifactDelegateResponse(ArtifactBuildDetailsNG buildDetails, ArtifactSourceType sourceType,
      String imagePath, String tag, String imageUrl, String authToken) {
    super(buildDetails, sourceType);
    this.imagePath = imagePath;
    this.tag = tag;
    this.imageUrl = imageUrl;
    this.authToken = authToken;
  }
}
