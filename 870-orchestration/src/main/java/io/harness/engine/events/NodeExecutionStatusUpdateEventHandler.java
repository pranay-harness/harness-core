package io.harness.engine.events;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.interrupts.statusupdate.StepStatusUpdate;
import io.harness.engine.interrupts.statusupdate.StepStatusUpdateInfo;
import io.harness.execution.NodeExecution;
import io.harness.observer.AsyncInformObserver;
import io.harness.timeout.TimeoutEngine;
import io.harness.timeout.trackers.events.StatusUpdateTimeoutEvent;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.List;
import java.util.concurrent.ExecutorService;

@OwnedBy(PIPELINE)
public class NodeExecutionStatusUpdateEventHandler implements AsyncInformObserver, StepStatusUpdate {
  @Inject @Named("PipelineExecutorService") ExecutorService executorService;
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private TimeoutEngine timeoutEngine;

  @Override
  public void onStepStatusUpdate(StepStatusUpdateInfo stepStatusUpdateInfo) {
    String nodeExecutionId = stepStatusUpdateInfo.getNodeExecutionId();
    if (nodeExecutionId == null) {
      return;
    }

    NodeExecution nodeExecution = nodeExecutionService.get(nodeExecutionId);
    if (nodeExecution == null) {
      return;
    }

    List<String> timeoutInstanceIds = nodeExecution.getTimeoutInstanceIds();
    if (EmptyPredicate.isNotEmpty(timeoutInstanceIds)) {
      timeoutEngine.onEvent(timeoutInstanceIds, new StatusUpdateTimeoutEvent(nodeExecution.getStatus()));
    }
  }

  @Override
  public ExecutorService getInformExecutorService() {
    return executorService;
  }
}
