/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.EmbeddedUser;
import io.harness.beans.ExecutionStatus;
import io.harness.state.inspection.StateInspection;

import software.wings.sm.StateExecutionData;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Created by anubhaw on 10/26/16.
 */

@OwnedBy(CDC)
@Getter
@Setter
@Builder
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
public class PipelineStageExecution {
  private String pipelineStageElementId;
  private String stateUuid;
  private String stateName;
  private String stateType;
  private ExecutionStatus status;
  private Long startTs;
  private Long expiryTs;
  private Long endTs;
  private Long estimatedTime;
  @Builder.Default private List<WorkflowExecution> workflowExecutions = new ArrayList<>();
  private StateExecutionData stateExecutionData;
  private String message;
  private boolean looped;
  private boolean waitingForInputs;
  private ParallelInfo parallelInfo;
  private EmbeddedUser triggeredBy;
  private StateInspection disableAssertionInspection;
  private String skipCondition;
  private boolean needsInputButNotReceivedYet;

  public List<WorkflowExecution> getWorkflowExecutions() {
    return Objects.isNull(workflowExecutions) ? new ArrayList<>() : workflowExecutions;
  }
}
