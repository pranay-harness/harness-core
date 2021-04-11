package io.harness.event.handlers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.ExecutionEngineDispatcher;
import io.harness.engine.OrchestrationEngine;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.execution.NodeExecution;
import io.harness.execution.NodeExecutionMapper;
import io.harness.pms.contracts.execution.NodeExecutionProto;
import io.harness.pms.execution.SdkResponseEventInternal;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.concurrent.ExecutorService;

@Singleton
@OwnedBy(HarnessTeam.PIPELINE)
public class QueueNodeExecutionEventHandler implements SdkResponseEventHandler {
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private OrchestrationEngine engine;
  @Inject @Named("EngineExecutorService") private ExecutorService executorService;

  @Override
  public void handleEvent(SdkResponseEventInternal event) {
    NodeExecutionProto nodeExecutionProto =
        event.getSdkResponseEventRequest().getQueueNodeExecutionRequest().getNodeExecution();
    NodeExecution nodeExecution = NodeExecutionMapper.fromNodeExecutionProto(nodeExecutionProto);
    nodeExecutionService.save(nodeExecution);
    executorService.submit(
        ExecutionEngineDispatcher.builder().ambiance(nodeExecution.getAmbiance()).orchestrationEngine(engine).build());
  }
}
