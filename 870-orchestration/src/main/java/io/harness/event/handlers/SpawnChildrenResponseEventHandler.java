package io.harness.event.handlers;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import io.harness.OrchestrationPublisherName;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.ExecutionEngineDispatcher;
import io.harness.engine.OrchestrationEngine;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.executions.plan.PlanService;
import io.harness.engine.resume.EngineResumeCallback;
import io.harness.execution.NodeExecution;
import io.harness.execution.NodeExecution.NodeExecutionKeys;
import io.harness.plan.Plan;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.ChildrenExecutableResponse.Child;
import io.harness.pms.contracts.execution.ExecutableResponse;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.events.SdkResponseEventProto;
import io.harness.pms.contracts.execution.events.SpawnChildrenRequest;
import io.harness.pms.contracts.plan.PlanNodeProto;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.execution.utils.LevelUtils;
import io.harness.waiter.OldNotifyCallback;
import io.harness.waiter.WaitNotifyEngine;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

@Singleton
@OwnedBy(HarnessTeam.PIPELINE)
public class SpawnChildrenResponseEventHandler implements SdkResponseEventHandler {
  @Inject private PlanService planService;
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private OrchestrationEngine engine;
  @Inject @Named("EngineExecutorService") private ExecutorService executorService;
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject @Named(OrchestrationPublisherName.PUBLISHER_NAME) private String publisherName;

  @Override
  public void handleEvent(SdkResponseEventProto event) {
    SpawnChildrenRequest request = event.getSdkResponseEventRequest().getSpawnChildrenRequest();
    NodeExecution nodeExecution = nodeExecutionService.get(request.getNodeExecutionId());
    Ambiance ambiance = nodeExecution.getAmbiance();
    Plan plan = planService.fetchPlan(nodeExecution.getAmbiance().getPlanId());
    List<String> callbackIds = new ArrayList<>();
    for (Child child : request.getChildren().getChildrenList()) {
      String uuid = generateUuid();
      callbackIds.add(uuid);
      PlanNodeProto node = plan.fetchNode(child.getChildNodeId());
      Ambiance clonedAmbiance = AmbianceUtils.cloneForChild(ambiance, LevelUtils.buildLevelFromPlanNode(uuid, node));
      NodeExecution childNodeExecution = NodeExecution.builder()
                                             .uuid(uuid)
                                             .node(node)
                                             .ambiance(clonedAmbiance)
                                             .status(Status.QUEUED)
                                             .notifyId(uuid)
                                             .parentId(nodeExecution.getUuid())
                                             .build();
      nodeExecutionService.save(childNodeExecution);
      executorService.submit(
          ExecutionEngineDispatcher.builder().ambiance(clonedAmbiance).orchestrationEngine(engine).build());
    }

    // Attach a Callback to the parent for the child
    OldNotifyCallback callback = EngineResumeCallback.builder().nodeExecutionId(request.getNodeExecutionId()).build();
    waitNotifyEngine.waitForAllOn(publisherName, callback, callbackIds.toArray(new String[0]));

    // Update the parent with executable response
    nodeExecutionService.update(nodeExecution.getUuid(),
        ops
        -> ops.addToSet(NodeExecutionKeys.executableResponses,
            ExecutableResponse.newBuilder().setChildren(request.getChildren()).build()));
  }
}
