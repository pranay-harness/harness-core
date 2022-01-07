/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans.alert;

import io.harness.alert.AlertData;
import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.WorkflowType;

import lombok.Builder;
import lombok.Data;

/**
 * Created by sgurubelli on 10/18/17.
 */
@Data
@Builder
@TargetModule(HarnessModule._957_CG_BEANS)
public class ApprovalNeededAlert implements AlertData {
  private String executionId;
  private String approvalId;
  private String name;
  private String envId;
  private WorkflowType workflowType;
  private String workflowExecutionId;
  private String pipelineExecutionId;

  @Override
  public boolean matches(AlertData alertData) {
    return approvalId.equals(((ApprovalNeededAlert) alertData).getApprovalId());
  }

  @Override
  public String buildTitle() {
    return name + " needs approval";
  }
}
