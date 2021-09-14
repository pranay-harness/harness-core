/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.azure.model.blueprint.artifact;

import io.harness.azure.model.blueprint.ParameterValue;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Artifact {
  private String id;
  private String name;
  private String kind;
  private String type;
  private Properties properties;

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Properties {
    // PolicyAssignmentArtifact
    private String policyDefinitionId;
    // RoleAssignmentArtifact
    private String[] principalIds;
    private String roleDefinitionId;
    // TemplateArtifact
    private Map<String, Object> template;

    // PolicyAssignmentArtifact & TemplateArtifact
    private Map<String, ParameterValue> parameters;

    // common for all artifacts kind
    private String[] dependsOn;
    private String description;
    private String displayName;
    private String resourceGroup;
  }
}
