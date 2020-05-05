package io.harness.engine;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.waiter.OrchestrationNotifyEventListener.ORCHESTRATION;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.name.Named;

import io.harness.adviser.Advise;
import io.harness.adviser.Adviser;
import io.harness.adviser.AdviserObtainment;
import io.harness.adviser.AdvisingEvent;
import io.harness.ambiance.Ambiance;
import io.harness.ambiance.LevelExecution;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.ExcludeRedesign;
import io.harness.beans.EmbeddedUser;
import io.harness.delay.DelayEventHelper;
import io.harness.delegate.beans.ResponseData;
import io.harness.engine.advise.AdviseHandler;
import io.harness.engine.advise.AdviseHandlerFactory;
import io.harness.engine.executables.ExecutableInvoker;
import io.harness.engine.executables.ExecutableInvokerFactory;
import io.harness.engine.executables.InvokerPackage;
import io.harness.engine.resume.EngineResumeExecutor;
import io.harness.engine.resume.EngineWaitResumeCallback;
import io.harness.execution.NodeExecution;
import io.harness.execution.NodeExecution.NodeExecutionKeys;
import io.harness.execution.PlanExecution;
import io.harness.execution.status.ExecutionInstanceStatus;
import io.harness.execution.status.NodeExecutionStatus;
import io.harness.facilitator.Facilitator;
import io.harness.facilitator.FacilitatorObtainment;
import io.harness.facilitator.FacilitatorResponse;
import io.harness.facilitator.modes.ExecutionMode;
import io.harness.persistence.HPersistence;
import io.harness.plan.ExecutionNode;
import io.harness.plan.Plan;
import io.harness.registries.adviser.AdviserRegistry;
import io.harness.registries.facilitator.FacilitatorRegistry;
import io.harness.registries.level.LevelRegistry;
import io.harness.registries.state.StateRegistry;
import io.harness.state.State;
import io.harness.state.io.StateResponse;
import io.harness.state.io.StateTransput;
import io.harness.state.io.StatusNotifyResponseData;
import io.harness.waiter.WaitNotifyEngine;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.UpdateOperations;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Slf4j
@Redesign
@ExcludeRedesign
public class ExecutionEngine implements Engine {
  // For database needs
  @Inject @Named("enginePersistence") private HPersistence hPersistence;
  // For leveraging the wait notify engine
  @Inject private WaitNotifyEngine waitNotifyEngine;
  // Guice Injector
  @Inject private Injector injector;
  // ExecutorService for the engine
  @Inject @Named("EngineExecutorService") private ExecutorService executorService;
  // Registries
  @Inject private StateRegistry stateRegistry;
  @Inject private LevelRegistry levelRegistry;
  @Inject private AdviserRegistry adviserRegistry;
  @Inject private FacilitatorRegistry facilitatorRegistry;

  // For obtaining ambiance related information
  @Inject private AmbianceHelper ambianceHelper;

  // Obtain concrete entities from obtainments
  // States | Advisers | Facilitators | Inputs
  @Inject private EngineObtainmentHelper engineObtainmentHelper;

  // Helper methods for status updates and related methods
  @Inject private EngineStatusHelper engineStatusHelper;

  // Obtain appropriate invoker
  @Inject private ExecutableInvokerFactory executableInvokerFactory;

  // Obtain appropriate advise Handler
  @Inject private AdviseHandlerFactory adviseHandlerFactory;

  @Inject private DelayEventHelper delayEventHelper;

  public PlanExecution startExecution(@Valid Plan plan, EmbeddedUser createdBy) {
    PlanExecution instance = PlanExecution.builder()
                                 .uuid(generateUuid())
                                 .plan(plan)
                                 .status(ExecutionInstanceStatus.RUNNING)
                                 .createdBy(createdBy)
                                 .startTs(System.currentTimeMillis())
                                 .build();
    hPersistence.save(instance);
    ExecutionNode executionNode = plan.fetchStartingNode();
    if (executionNode == null) {
      logger.warn("Cannot Start Execution for empty plan");
      return null;
    }
    Ambiance ambiance = Ambiance.builder()
                            .setupAbstractions(plan.getSetupAbstractions())
                            .executionInstanceId(instance.getUuid())
                            .build();
    triggerExecution(ambiance, plan.fetchStartingNode());
    return instance;
  }

  public void startNodeExecution(Ambiance ambiance) {
    // Update to Running Status
    NodeExecution nodeExecution =
        Preconditions.checkNotNull(hPersistence.createQuery(NodeExecution.class)
                                       .filter(NodeExecutionKeys.uuid, ambiance.obtainCurrentRuntimeId())
                                       .get());
    ExecutionNode node = nodeExecution.getNode();
    // Facilitate and execute
    List<StateTransput> inputs =
        engineObtainmentHelper.obtainInputs(ambiance, node.getRefObjects(), nodeExecution.getAdditionalInputs());
    facilitateExecution(ambiance, node, inputs);
  }

  public void triggerExecution(Ambiance ambiance, ExecutionNode node) {
    Ambiance cloned = ambiance.obtainCurrentRuntimeId() == null
        ? ambiance
        : ambiance.cloneForFinish(levelRegistry.obtain(node.getLevelType()));
    String uuid = generateUuid();
    NodeExecution previousNodeExecution = null;
    if (ambiance.obtainCurrentRuntimeId() != null) {
      previousNodeExecution = engineStatusHelper.updateNodeInstance(
          ambiance.obtainCurrentRuntimeId(), ops -> ops.set(NodeExecutionKeys.nextId, uuid));
    }
    cloned.addLevelExecution(LevelExecution.builder()
                                 .setupId(node.getUuid())
                                 .runtimeId(uuid)
                                 .level(levelRegistry.obtain(node.getLevelType()))
                                 .build());

    NodeExecution nodeExecution =
        NodeExecution.builder()
            .uuid(uuid)
            .ambiance(cloned)
            .node(node)
            .startTs(System.currentTimeMillis())
            .status(NodeExecutionStatus.QUEUED)
            .notifyId(previousNodeExecution == null ? null : previousNodeExecution.getNotifyId())
            .parentId(previousNodeExecution == null ? null : previousNodeExecution.getParentId())
            .previousId(previousNodeExecution == null ? null : previousNodeExecution.getUuid())
            .build();
    hPersistence.save(nodeExecution);
    executorService.submit(ExecutionEngineDispatcher.builder().ambiance(cloned).executionEngine(this).build());
  }

  private void facilitateExecution(Ambiance ambiance, ExecutionNode node, List<StateTransput> inputs) {
    FacilitatorResponse facilitatorResponse = null;
    for (FacilitatorObtainment obtainment : node.getFacilitatorObtainments()) {
      Facilitator facilitator = facilitatorRegistry.obtain(obtainment.getType());
      facilitatorResponse = facilitator.facilitate(ambiance, obtainment.getParameters(), inputs);
      if (facilitatorResponse != null) {
        break;
      }
    }
    Preconditions.checkNotNull(facilitatorResponse,
        "No execution mode detected for State. Name: " + node.getName() + "Type : " + node.getStateType());
    ExecutionMode mode = facilitatorResponse.getExecutionMode();
    Consumer<UpdateOperations<NodeExecution>> ops = op -> op.set(NodeExecutionKeys.mode, mode);
    if (facilitatorResponse.getInitialWait() != null && facilitatorResponse.getInitialWait().getSeconds() != 0) {
      FacilitatorResponse finalFacilitatorResponse = facilitatorResponse;
      Preconditions.checkNotNull(engineStatusHelper.updateNodeInstance(ambiance.obtainCurrentRuntimeId(),
          ops.andThen(op
              -> op.set(NodeExecutionKeys.status, NodeExecutionStatus.TIMED_WAITING)
                     .set(NodeExecutionKeys.initialWaitDuration, finalFacilitatorResponse.getInitialWait()))));
      String resumeId =
          delayEventHelper.delay(finalFacilitatorResponse.getInitialWait().getSeconds(), Collections.emptyMap());
      waitNotifyEngine.waitForAllOn(ORCHESTRATION,
          EngineWaitResumeCallback.builder().ambiance(ambiance).facilitatorResponse(finalFacilitatorResponse).build(),
          resumeId);
    } else {
      Preconditions.checkNotNull(engineStatusHelper.updateNodeInstance(ambiance.obtainCurrentRuntimeId(),
          ops.andThen(op -> op.set(NodeExecutionKeys.status, NodeExecutionStatus.RUNNING))));
      invokeState(ambiance, facilitatorResponse);
    }
  }

  public void invokeState(Ambiance ambiance, FacilitatorResponse facilitatorResponse) {
    NodeExecution nodeExecution = Preconditions.checkNotNull(ambianceHelper.obtainNodeExecution(ambiance));
    ExecutionNode node = nodeExecution.getNode();
    State currentState = stateRegistry.obtain(node.getStateType());
    injector.injectMembers(currentState);
    List<StateTransput> inputs =
        engineObtainmentHelper.obtainInputs(ambiance, node.getRefObjects(), nodeExecution.getAdditionalInputs());
    ExecutableInvoker invoker = executableInvokerFactory.obtainInvoker(facilitatorResponse.getExecutionMode());
    invoker.invokeExecutable(InvokerPackage.builder()
                                 .state(currentState)
                                 .ambiance(ambiance)
                                 .inputs(inputs)
                                 .parameters(node.getStateParameters())
                                 .passThroughData(facilitatorResponse.getPassThroughData())
                                 .build());
  }

  public void handleStateResponse(@NotNull String nodeExecutionId, StateResponse stateResponse) {
    NodeExecution nodeExecution = engineStatusHelper.updateNodeInstance(nodeExecutionId,
        ops
        -> ops.set(NodeExecutionKeys.status, stateResponse.getStatus())
               .set(NodeExecutionKeys.outcomes, stateResponse.getOutcomes())
               .set(NodeExecutionKeys.endTs, System.currentTimeMillis()));

    // TODO handle Failure
    ExecutionNode node = nodeExecution.getNode();
    if (isEmpty(node.getAdviserObtainments())) {
      endTransition(nodeExecution);
      return;
    }
    Advise advise = null;
    for (AdviserObtainment obtainment : node.getAdviserObtainments()) {
      Adviser adviser = adviserRegistry.obtain(obtainment.getType());
      advise = adviser.onAdviseEvent(
          AdvisingEvent.builder().stateResponse(stateResponse).adviserParameters(obtainment.getParameters()).build());
      if (advise != null) {
        break;
      }
    }
    if (advise == null) {
      endTransition(nodeExecution);
      return;
    }
    handleAdvise(nodeExecution, advise);
  }

  private void endTransition(NodeExecution nodeInstance) {
    if (isNotEmpty(nodeInstance.getNotifyId())) {
      StatusNotifyResponseData responseData =
          StatusNotifyResponseData.builder().status(NodeExecutionStatus.SUCCEEDED).build();
      waitNotifyEngine.doneWith(nodeInstance.getNotifyId(), responseData);
    } else {
      logger.info("End Execution");
      engineStatusHelper.updateExecutionInstanceStatus(
          nodeInstance.getAmbiance().getExecutionInstanceId(), ExecutionInstanceStatus.SUCCEEDED);
    }
  }

  private void handleAdvise(@NotNull NodeExecution nodeExecution, @NotNull Advise advise) {
    Ambiance ambiance = nodeExecution.getAmbiance();
    AdviseHandler adviseHandler = adviseHandlerFactory.obtainHandler(advise.getType());
    adviseHandler.handleAdvise(ambiance, advise);
  }

  public void resume(String nodeInstanceId, Map<String, ResponseData> response, boolean asyncError) {
    NodeExecution nodeExecution = engineStatusHelper.updateNodeInstance(
        nodeInstanceId, ops -> ops.set(NodeExecutionKeys.status, NodeExecutionStatus.RUNNING));

    ExecutionNode node = nodeExecution.getNode();
    State currentState = stateRegistry.obtain(node.getStateType());
    injector.injectMembers(currentState);
    if (nodeExecution.getStatus() != NodeExecutionStatus.RUNNING) {
      logger.warn("nodeInstance: {} status {} is no longer in RUNNING state", nodeExecution.getUuid(),
          nodeExecution.getStatus());
      return;
    }
    executorService.execute(EngineResumeExecutor.builder()
                                .nodeExecution(nodeExecution)
                                .response(response)
                                .asyncError(asyncError)
                                .executionEngine(this)
                                .stateRegistry(stateRegistry)
                                .injector(injector)
                                .build());
  }
}