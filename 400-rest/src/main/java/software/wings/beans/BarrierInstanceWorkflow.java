/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
