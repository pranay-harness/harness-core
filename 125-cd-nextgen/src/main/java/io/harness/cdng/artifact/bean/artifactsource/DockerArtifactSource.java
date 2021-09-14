/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cdng.artifact.bean.artifactsource;

import io.harness.delegate.task.artifacts.ArtifactSourceType;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;

@Value
@FieldNameConstants(innerTypeName = "DockerArtifactSourceKeys")
@EqualsAndHashCode(callSuper = true)
public class DockerArtifactSource extends ArtifactSource {
  /** Docker hub registry connector identifier. */
  @NotEmpty String connectorRef;
  /** Images in repos need to be referenced via a path */
  @NotEmpty String imagePath;

  @Builder
  public DockerArtifactSource(String uuid, String accountId, ArtifactSourceType sourceType, String uniqueHash,
      long createdAt, long lastUpdatedAt, Long version, String connectorRef, String imagePath) {
    super(uuid, accountId, sourceType, uniqueHash, createdAt, lastUpdatedAt, version);
    this.connectorRef = connectorRef;
    this.imagePath = imagePath;
  }
}
