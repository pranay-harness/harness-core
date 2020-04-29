package io.harness.engine;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;

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
import io.harness.beans.EmbeddedUser;
import io.harness.delegate.beans.ResponseData;
import io.harness.engine.advise.AdviseHandler;
import io.harness.engine.advise.AdviseHandlerFactory;
import io.harness.engine.executables.ExecutableInvoker;
import io.harness.engine.executables.ExecutableInvokerFactory;
import io.harness.engine.executables.InvokerPackage;
import io.harness.engine.resume.EngineResumeExecutor;
import io.harness.facilitate.Facilitator;
import io.harness.facilitate.FacilitatorObtainment;
import io.harness.facilitate.FacilitatorResponse;
import io.harness.facilitate.modes.ExecutionMode;
import io.harness.persistence.HPersistence;
import io.harness.plan.ExecutionNode;
import io.harness.plan.ExecutionPlan;
import io.harness.registries.adviser.AdviserRegistry;
import io.harness.registries.facilitator.FacilitatorRegistry;
import io.harness.registries.level.LevelRegistry;
import io.harness.registries.state.StateRegistry;
import io.harness.state.State;
import io.harness.state.execution.ExecutionInstance;
import io.harness.state.execution.ExecutionNodeInstance;
import io.harness.state.execution.ExecutionNodeInstance.ExecutionNodeInstanceKeys;
import io.harness.state.execution.status.ExecutionInstanceStatus;
import io.harness.state.execution.status.NodeExecutionStatus;
import io.harness.state.io.StateResponse;
import io.harness.state.io.StateTransput;
import io.harness.state.io.StatusNotifyResponseData;
import io.harness.waiter.WaitNotifyEngine;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Slf4j
@Redesign
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

  // Helpers

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

  public ExecutionInstance startExecution(@Valid ExecutionPlan executionPlan, EmbeddedUser createdBy) {
    ExecutionInstance instance = ExecutionInstance.builder()
                                     .uuid(generateUuid())
                                     .executionPlan(executionPlan)
                                     .status(ExecutionInstanceStatus.RUNNING)
                                     .createdBy(createdBy)
                                     .startTs(System.currentTimeMillis())
                                     .build();
    hPersistence.save(instance);
    ExecutionNode executionNode = executionPlan.fetchStartingNode();
    if (executionNode == null) {
      logger.warn("Cannot Start Execution for empty plan");
      return null;
    }
    Ambiance ambiance = Ambiance.builder()
                            .setupAbstractions(executionPlan.getSetupAbstractions())
                            .executionInstanceId(instance.getUuid())
                            .build();
    triggerExecution(ambiance, executionPlan.fetchStartingNode());
    return instance;
  }

  public void startNodeExecution(Ambiance ambiance) {
    startNodeInstance(ambiance, ambiance.obtainCurrentRuntimeId());
  }

  public void triggerExecution(Ambiance ambiance, ExecutionNode node) {
    Ambiance cloned = ambiance.obtainCurrentRuntimeId() == null
        ? ambiance
        : ambiance.cloneForFinish(levelRegistry.obtain(node.getLevelType()));
    String uuid = generateUuid();
    ExecutionNodeInstance previousInstance = null;
    if (ambiance.obtainCurrentRuntimeId() != null) {
      previousInstance = engineStatusHelper.updateNodeInstance(
          ambiance.obtainCurrentRuntimeId(), ops -> ops.set(ExecutionNodeInstanceKeys.nextId, uuid));
    }
    cloned.addLevelExecution(LevelExecution.builder()
                                 .setupId(node.getUuid())
                                 .runtimeId(uuid)
                                 .level(levelRegistry.obtain(node.getLevelType()))
                                 .build());

    ExecutionNodeInstance nodeInstance = ExecutionNodeInstance.builder()
                                             .uuid(uuid)
                                             .ambiance(cloned)
                                             .node(node)
                                             .startTs(System.currentTimeMillis())
                                             .status(NodeExecutionStatus.QUEUED)
                                             .notifyId(previousInstance == null ? null : previousInstance.getNotifyId())
                                             .parentId(previousInstance == null ? null : previousInstance.getParentId())
                                             .previousId(previousInstance == null ? null : previousInstance.getUuid())
                                             .build();
    hPersistence.save(nodeInstance);
    executorService.submit(ExecutionEngineDispatcher.builder().ambiance(cloned).executionEngine(this).build());
  }

  public void startNodeInstance(Ambiance ambiance, @NotNull String nodeInstanceId) {
    // Update to Running Status
    ExecutionNodeInstance nodeInstance =
        Preconditions.checkNotNull(hPersistence.createQuery(ExecutionNodeInstance.class)
                                       .filter(ExecutionNodeInstanceKeys.uuid, nodeInstanceId)
                                       .get());

    ExecutionNode node = nodeInstance.getNode();
    // Audit and execute
    List<StateTransput> inputs =
        engineObtainmentHelper.obtainInputs(ambiance, node.getRefObjects(), nodeInstance.getAdditionalInputs());
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

    ExecutionNodeInstance updatedNodeInstance =
        Preconditions.checkNotNull(engineStatusHelper.updateNodeInstance(nodeInstanceId,
            ops
            -> ops.set(ExecutionNodeInstanceKeys.mode, mode)
                   .set(ExecutionNodeInstanceKeys.status, NodeExecutionStatus.RUNNING)));

    invokeState(ambiance, facilitatorResponse, updatedNodeInstance);
  }

  private void invokeState(
      Ambiance ambiance, FacilitatorResponse facilitatorResponse, ExecutionNodeInstance nodeInstance) {
    ExecutionNode node = nodeInstance.getNode();
    State currentState = Preconditions.checkNotNull(
        stateRegistry.obtain(node.getStateType()), "Cannot find state for state type: " + node.getStateType());
    injector.injectMembers(currentState);
    List<StateTransput> inputs =
        engineObtainmentHelper.obtainInputs(ambiance, node.getRefObjects(), nodeInstance.getAdditionalInputs());
    ExecutableInvoker invoker = executableInvokerFactory.obtainInvoker(facilitatorResponse.getExecutionMode());
    invoker.invokeExecutable(InvokerPackage.builder()
                                 .state(currentState)
                                 .ambiance(ambiance)
                                 .inputs(inputs)
                                 .parameters(node.getStateParameters())
                                 .passThroughData(facilitatorResponse.getPassThroughData())
                                 .build());
  }

  public void handleStateResponse(@NotNull String nodeInstanceId, StateResponse stateResponse) {
    ExecutionNodeInstance nodeInstance = engineStatusHelper.updateNodeInstance(nodeInstanceId,
        ops
        -> ops.set(ExecutionNodeInstanceKeys.status, stateResponse.getStatus())
               .set(ExecutionNodeInstanceKeys.endTs, System.currentTimeMillis()));

    // TODO handle Failure
    ExecutionNode node = nodeInstance.getNode();
    if (isEmpty(node.getAdviserObtainments())) {
      endTransition(nodeInstance);
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
      endTransition(nodeInstance);
      return;
    }
    handleAdvise(nodeInstance, advise);
  }

  private void endTransition(ExecutionNodeInstance nodeInstance) {
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

  private void handleAdvise(@NotNull ExecutionNodeInstance nodeInstance, @NotNull Advise advise) {
    Ambiance ambiance = nodeInstance.getAmbiance();
    AdviseHandler adviseHandler = adviseHandlerFactory.obtainHandler(advise.getType());
    adviseHandler.handleAdvise(ambiance, advise);
  }

  public void resume(String nodeInstanceId, Map<String, ResponseData> response, boolean asyncError) {
    ExecutionNodeInstance nodeInstance = engineStatusHelper.updateNodeInstance(
        nodeInstanceId, ops -> ops.set(ExecutionNodeInstanceKeys.status, NodeExecutionStatus.RUNNING));

    ExecutionNode node = nodeInstance.getNode();
    State currentState = stateRegistry.obtain(node.getStateType());
    injector.injectMembers(currentState);
    if (nodeInstance.getStatus() != NodeExecutionStatus.RUNNING) {
      logger.warn(
          "nodeInstance: {} status {} is no longer in RUNNING state", nodeInstance.getUuid(), nodeInstance.getStatus());
      return;
    }
    executorService.execute(EngineResumeExecutor.builder()
                                .executionNodeInstance(nodeInstance)
                                .response(response)
                                .asyncError(asyncError)
                                .executionEngine(this)
                                .stateRegistry(stateRegistry)
                                .injector(injector)
                                .build());
  }
}