package io.harness.event.handlers;

import io.harness.OrchestrationPublisherName;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.ExecutionEngineDispatcher;
import io.harness.engine.OrchestrationEngine;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.resume.EngineResumeCallback;
import io.harness.execution.NodeExecution;
import io.harness.execution.NodeExecution.NodeExecutionKeys;
import io.harness.execution.NodeExecutionMapper;
import io.harness.pms.contracts.execution.NodeExecutionProto;
import io.harness.pms.contracts.execution.events.SpawnChildRequest;
import io.harness.pms.execution.SdkResponseEventInternal;
import io.harness.waiter.OldNotifyCallback;
import io.harness.waiter.WaitNotifyEngine;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.concurrent.ExecutorService;

@Singleton
@OwnedBy(HarnessTeam.PIPELINE)
public class SpawnChildResponseEventHandler implements SdkResponseEventHandler {
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private OrchestrationEngine engine;
  @Inject @Named("EngineExecutorService") private ExecutorService executorService;
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject @Named(OrchestrationPublisherName.PUBLISHER_NAME) private String publisherName;

  @Override
  public void handleEvent(SdkResponseEventInternal event) {
    SpawnChildRequest request = event.getSdkResponseEventRequest().getSpawnChildRequest();
    NodeExecutionProto childNodeExecution = request.getChildNodeExecution();

    // Attach a Callback to the parent for the child
    OldNotifyCallback callback = EngineResumeCallback.builder().nodeExecutionId(request.getNodeExecutionId()).build();
    waitNotifyEngine.waitForAllOn(publisherName, callback, childNodeExecution.getUuid());

    // Update the parent with executable response
    nodeExecutionService.update(request.getNodeExecutionId(),
        ops -> ops.addToSet(NodeExecutionKeys.executableResponses, request.getExecutableResponse()));

    // Save the child node execution and Dispatch
    NodeExecution nodeExecution =
        nodeExecutionService.save(NodeExecutionMapper.fromNodeExecutionProto(childNodeExecution));
    executorService.submit(
        ExecutionEngineDispatcher.builder().ambiance(nodeExecution.getAmbiance()).orchestrationEngine(engine).build());
  }
}
