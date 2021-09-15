/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.engine.interrupts.statusupdate;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.observers.NodeStatusUpdateHandler;
import io.harness.engine.observers.NodeUpdateInfo;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@OwnedBy(PIPELINE)
@Singleton
public class NodeStatusUpdateHandlerFactory {
  @Inject ApprovalStepStatusUpdate approvalStepStatusUpdate;
  @Inject InterventionWaitStepStatusUpdate interventionWaitStepStatusUpdate;
  @Inject PausedStepStatusUpdate pausedStepStatusUpdate;
  @Inject ResumeStepStatusUpdate resumeStepStatusUpdate;
  @Inject TerminalStepStatusUpdate terminalStepStatusUpdate;
  @Inject AbortAndRunningStepStatusUpdate abortAndRunningStepStatusUpdate;

  public NodeStatusUpdateHandler obtainStepStatusUpdate(NodeUpdateInfo nodeStatusUpdateInfo) {
    switch (nodeStatusUpdateInfo.getStatus()) {
      case APPROVAL_WAITING:
        return approvalStepStatusUpdate;
      case INTERVENTION_WAITING:
        return interventionWaitStepStatusUpdate;
      case PAUSED:
        return pausedStepStatusUpdate;
      case QUEUED:
        return resumeStepStatusUpdate;
      case ABORTED:
        return abortAndRunningStepStatusUpdate;
      default:
        // Do not do this for other statuses as there multiple queries
        // Till Now only handling these will figure out a better way to do this
        return null;
    }
  }
}
