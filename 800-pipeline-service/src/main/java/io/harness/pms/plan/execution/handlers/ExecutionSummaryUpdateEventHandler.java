package io.harness.pms.plan.execution.handlers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.observers.NodeStatusUpdateObserver;
import io.harness.engine.observers.NodeUpdateInfo;
import io.harness.engine.observers.NodeUpdateObserver;
import io.harness.logging.AutoLogContext;
import io.harness.observer.AsyncInformObserver;
import io.harness.pms.plan.execution.service.PmsExecutionSummaryService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.concurrent.ExecutorService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@OwnedBy(HarnessTeam.PIPELINE)
public class ExecutionSummaryUpdateEventHandler
    implements NodeUpdateObserver, NodeStatusUpdateObserver, AsyncInformObserver {
  @Inject @Named("PipelineExecutorService") private ExecutorService executorService;
  @Inject private PmsExecutionSummaryService pmsExecutionSummaryService;

  @Override
  public ExecutorService getInformExecutorService() {
    return executorService;
  }

  @Override
  public void onNodeUpdate(NodeUpdateInfo nodeUpdateInfo) {
    // Only endTs should be updated as all other things should come with status updates
    pmsExecutionSummaryService.updateEndTs(nodeUpdateInfo.getPlanExecutionId(), nodeUpdateInfo.getNodeExecution());
  }

  @Override
  public void onNodeStatusUpdate(NodeUpdateInfo nodeUpdateInfo) {
    try (AutoLogContext ignore = nodeUpdateInfo.autoLogContext()) {
      log.info("ExecutionSummaryStatusUpdateEventHandler Starting to update PipelineExecutionSummaryEntity");
      pmsExecutionSummaryService.update(nodeUpdateInfo.getPlanExecutionId(), nodeUpdateInfo.getNodeExecution());
      log.info("ExecutionSummaryStatusUpdateEventHandler finished updating PipelineExecutionSummaryEntity");
    }
  }
}
