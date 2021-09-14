/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.azure.model.blueprint.assignment.operation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AssignmentDeploymentJob {
  private String action;
  private String jobId;
  private String jobState;
  private String kind;
  private String requestUri;
  private AssignmentDeploymentJobResult[] history;
  private AssignmentDeploymentJobResult result;
}
