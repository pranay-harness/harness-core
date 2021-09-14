/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl.analysis;

import software.wings.sm.StateExecutionInstance;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CVCertifiedDetailsForWorkflowState {
  String workflowName;
  String workflowId;
  String workflowExecutionId;
  String pipelineId;
  String pipelineName;
  String pipelineExecutionId;
  String phaseName;
  String stateExecutionId;
  StateExecutionInstance executionDetails;
}
