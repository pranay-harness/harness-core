/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.beans;

import lombok.Builder;
import lombok.Data;
import lombok.Value;

@Data
@Value
@Builder
public class GCPArtifactUploadInfo implements ArtifactUploadInfo {
  private ArtifactUploadInfo.Type type = Type.GCP;

  GCPArtifactUploadInfo() {}

  @Override
  public ArtifactUploadInfo.Type getType() {
    return type;
  }
}
