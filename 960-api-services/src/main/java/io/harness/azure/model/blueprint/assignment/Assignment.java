/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.azure.model.blueprint.assignment;

import io.harness.azure.model.blueprint.ParameterValue;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.microsoft.azure.management.network.ManagedServiceIdentity;
import java.util.Map;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Assignment {
  private String id;
  private String location;
  private String name;
  private String type;
  private ManagedServiceIdentity identity;
  private Properties properties;

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Properties {
    private String blueprintId;
    private String description;
    private String displayName;
    private String scope;
    private AssignmentLockSettings locks;
    private Map<String, ParameterValue> parameters;
    private AssignmentProvisioningState provisioningState;
    private Map<String, ResourceGroupValue> resourceGroups;
    private AssignmentStatus status;
  }
}
