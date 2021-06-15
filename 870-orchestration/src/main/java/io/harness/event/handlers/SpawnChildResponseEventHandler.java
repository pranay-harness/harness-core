package io.harness.event.handlers;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.pms.contracts.execution.Status.QUEUED;

import io.harness.OrchestrationPublisherName;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.ExecutionEngineDispatcher;
import io.harness.engine.OrchestrationEngine;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.executions.plan.PlanService;
import io.harness.engine.resume.EngineResumeCallback;
import io.harness.exception.InvalidRequestException;
import io.harness.execution.NodeExecution;
import io.harness.execution.NodeExecution.NodeExecutionKeys;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.ExecutableResponse;
import io.harness.pms.contracts.execution.events.SdkResponseEventProto;
import io.harness.pms.contracts.execution.events.SpawnChildRequest;
import io.harness.pms.contracts.plan.PlanNodeProto;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.execution.utils.LevelUtils;
import io.harness.waiter.OldNotifyCallback;
import io.harness.waiter.WaitNotifyEngine;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.concurrent.ExecutorService;

@Singleton
@OwnedBy(HarnessTeam.PIPELINE)
public class SpawnChildResponseEventHandler implements SdkResponseEventHandler {
  @Inject private PlanService planService;
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private OrchestrationEngine engine;
  @Inject @Named("EngineExecutorService") private ExecutorService executorService;
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject @Named(OrchestrationPublisherName.PUBLISHER_NAME) private String publisherName;

  @Override
  public void handleEvent(SdkResponseEventProto event) {
    SpawnChildRequest request = event.getSdkResponseEventRequest().getSpawnChildRequest();

    NodeExecution childNodeExecution = buildChildNodeExecution(request);

    // Attach a Callback to the parent for the child
    OldNotifyCallback callback = EngineResumeCallback.builder().nodeExecutionId(request.getNodeExecutionId()).build();
    waitNotifyEngine.waitForAllOn(publisherName, callback, childNodeExecution.getUuid());

    // Update the parent with executable response
    nodeExecutionService.update(request.getNodeExecutionId(),
        ops -> ops.addToSet(NodeExecutionKeys.executableResponses, buildExecutableResponse(request)));

    executorService.submit(ExecutionEngineDispatcher.builder()
                               .ambiance(childNodeExecution.getAmbiance())
                               .orchestrationEngine(engine)
                               .build());
  }

  private NodeExecution buildChildNodeExecution(SpawnChildRequest spawnChildRequest) {
    NodeExecution nodeExecution = nodeExecutionService.get(spawnChildRequest.getNodeExecutionId());

    String childNodeId = extractChildNodeId(spawnChildRequest);
    PlanNodeProto node = planService.fetchNode(nodeExecution.getAmbiance().getPlanId(), childNodeId);

    String childInstanceId = generateUuid();
    Ambiance clonedAmbiance = AmbianceUtils.cloneForChild(
        nodeExecution.getAmbiance(), LevelUtils.buildLevelFromPlanNode(childInstanceId, node));
    return nodeExecutionService.save(NodeExecution.builder()
                                         .uuid(childInstanceId)
                                         .node(node)
                                         .ambiance(clonedAmbiance)
                                         .status(QUEUED)
                                         .notifyId(childInstanceId)
                                         .parentId(nodeExecution.getUuid())
                                         .build());
  }

  private String extractChildNodeId(SpawnChildRequest spawnChildRequest) {
    switch (spawnChildRequest.getSpawnableExecutableResponseCase()) {
      case CHILD:
        return spawnChildRequest.getChild().getChildNodeId();
      case CHILDCHAIN:
        return spawnChildRequest.getChildChain().getNextChildId();
      default:
        throw new InvalidRequestException("CHILD or CHILD_CHAIN response should be set");
    }
  }

  private ExecutableResponse buildExecutableResponse(SpawnChildRequest spawnChildRequest) {
    switch (spawnChildRequest.getSpawnableExecutableResponseCase()) {
      case CHILD:
        return ExecutableResponse.newBuilder().setChild(spawnChildRequest.getChild()).build();
      case CHILDCHAIN:
        return ExecutableResponse.newBuilder().setChildChain(spawnChildRequest.getChildChain()).build();
      default:
        throw new InvalidRequestException("CHILD or CHILD_CHAIN response should be set");
    }
  }
}
