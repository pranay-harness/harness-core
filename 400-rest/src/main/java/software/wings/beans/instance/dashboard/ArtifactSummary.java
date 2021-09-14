/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans.instance.dashboard;

import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Artifact information
 * @author rktummala on 08/13/17
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ArtifactSummary extends AbstractEntitySummary {
  private String artifactSourceName;
  private String buildNo;
  private Map<String, Object> artifactParameters;

  @Builder
  public ArtifactSummary(String id, String name, String type, String artifactSourceName, String buildNo,
      Map<String, Object> artifactParameters) {
    super(id, name, type);
    this.artifactSourceName = artifactSourceName;
    this.buildNo = buildNo;
    this.artifactParameters = artifactParameters;
  }
}
