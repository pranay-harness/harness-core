/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonTypeName("ARTIFACT")
public class ArtifactStreamAllowedValueYaml implements AllowedValueYaml {
  private String artifactServerName;
  private String artifactStreamName;
  private String artifactStreamType;
  private String type;

  @Builder
  public ArtifactStreamAllowedValueYaml(
      String artifactServerName, String artifactStreamName, String artifactStreamType, String type) {
    this.artifactServerName = artifactServerName;
    this.artifactStreamName = artifactStreamName;
    this.artifactStreamType = artifactStreamType;
    this.type = type;
  }

  @Data
  @NoArgsConstructor
  public static final class Yaml {
    private String artifactServerName;
    private String artifactStreamName;
    private String artifactStreamType;
    private String type;

    @Builder
    public Yaml(String artifactServerName, String artifactStreamName, String artifactStreamType, String type) {
      this.artifactServerName = artifactServerName;
      this.artifactStreamName = artifactStreamName;
      this.artifactStreamType = artifactStreamType;
      this.type = type;
    }
  }
}
