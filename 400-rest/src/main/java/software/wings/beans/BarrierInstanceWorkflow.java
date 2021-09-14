/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

@Data
@Builder
@FieldNameConstants(innerTypeName = "BarrierInstanceWorkflowKeys")
public class BarrierInstanceWorkflow {
  private String uuid;

  private String pipelineStageId;
  private String pipelineStageExecutionId;

  private String workflowExecutionId;

  private String phaseUuid;
  private String phaseExecutionId;

  private String stepUuid;
  private String stepExecutionId;

  public String getUniqueWorkflowKeyInPipeline() {
    return pipelineStageId + uuid;
  }
}
