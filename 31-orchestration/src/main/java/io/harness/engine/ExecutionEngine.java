package io.harness.engine;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.state.execution.status.NodeExecutionStatus.TASK_WAITING;
import static io.harness.waiter.OrchestrationNotifyEventListener.ORCHESTRATION;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.name.Named;

import io.harness.adviser.Advise;
import io.harness.adviser.Adviser;
import io.harness.adviser.AdvisingEvent;
import io.harness.delegate.beans.ResponseData;
import io.harness.engine.resume.EngineResumeCallback;
import io.harness.engine.resume.EngineResumeExecutor;
import io.harness.exception.InvalidRequestException;
import io.harness.facilitate.Facilitator;
import io.harness.facilitate.FacilitatorResponse;
import io.harness.facilitate.modes.async.AsyncExecutable;
import io.harness.facilitate.modes.async.AsyncExecutableResponse;
import io.harness.facilitate.modes.sync.SyncExecutable;
import io.harness.persistence.HPersistence;
import io.harness.plan.ExecutionNode;
import io.harness.plan.ExecutionPlan;
import io.harness.registries.adviser.AdviserRegistry;
import io.harness.registries.facilitator.FacilitatorRegistry;
import io.harness.registries.resolver.ResolverRegistry;
import io.harness.registries.state.StateRegistry;
import io.harness.state.State;
import io.harness.state.execution.ExecutionNodeInstance;
import io.harness.state.execution.ExecutionNodeInstance.ExecutionNodeInstanceKeys;
import io.harness.state.execution.status.NodeExecutionStatus;
import io.harness.state.io.StateExecutionPackage;
import io.harness.state.io.StateResponse;
import io.harness.state.io.StateTransput;
import io.harness.state.io.ambiance.Ambiance;
import io.harness.waiter.NotifyCallback;
import io.harness.waiter.WaitNotifyEngine;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import javax.validation.Valid;

@Slf4j
public class ExecutionEngine implements Engine {
  // For database needs
  @Inject private HPersistence hPersistence;

  // For leveraging the wait notify engine
  @Inject private WaitNotifyEngine waitNotifyEngine;

  // Guice Injector
  @Inject private Injector injector;

  // ExecutorService for the engine
  @Inject @Named("EngineExecutorService") private ExecutorService executorService;

  // Registries
  @Inject private AdviserRegistry adviserRegistry;
  @Inject private StateRegistry stateRegistry;
  @Inject private FacilitatorRegistry facilitatorRegistry;
  @Inject private ResolverRegistry resolverRegistry;

  public void startExecution(@Valid ExecutionPlan executionPlan) {
    ExecutionNode executionNode = executionPlan.fetchStartingNode();
    if (executionNode == null) {
      logger.warn("Cannot Start Execution for empty plan");
      return;
    }
    Ambiance ambiance = Ambiance.builder().setupAbstractions(executionPlan.getSetupAbstractions()).build();
    startNodeExecution(ambiance, executionNode);
  }

  private void startNodeExecution(Ambiance ambiance, ExecutionNode executionNode) {
    // Create ExecutionNodeInstance save to the database
    ExecutionNodeInstance nodeInstance = ExecutionNodeInstance.builder()
                                             .uuid(generateUuid())
                                             .node(executionNode)
                                             .status(NodeExecutionStatus.QUEUED)
                                             .build();
    hPersistence.save(nodeInstance);
    handleNewNodeInstance(ambiance, nodeInstance.getUuid());
  }

  public void handleNewNodeInstance(Ambiance ambiance, String nodeInstanceId) {
    // Update to Running Status
    ExecutionNodeInstance nodeInstance = hPersistence.createQuery(ExecutionNodeInstance.class)
                                             .filter(ExecutionNodeInstanceKeys.uuid, nodeInstanceId)
                                             .get();

    ExecutionNode node = nodeInstance.getNode();
    // Audit and execute
    List<StateTransput> inputs = EngineHelper.obtainInputs(node.getRefObjects());
    List<Facilitator> facilitators = EngineHelper.obtainFacilitators(node.getFacilitatorObtainments());
    FacilitatorResponse facilitatorResponse = null;
    for (Facilitator facilitator : facilitators) {
      facilitatorResponse = facilitator.facilitate(ambiance, inputs);
      if (facilitatorResponse != null) {
        break;
      }
    }
    if (facilitatorResponse == null) {
      throw new InvalidRequestException(
          "No execution mode detected for State. Name: " + node.getName() + "Type : " + node.getStateType());
    }
    ExecutionNodeInstance updatedNodeInstance = updateNodeInstanceStatus(nodeInstanceId, NodeExecutionStatus.RUNNING);
    if (updatedNodeInstance == null) {
      throw new InvalidRequestException(
          "Cannot set the Node Execution instance in running state id: " + nodeInstanceId);
    }
    invokeState(ambiance, facilitatorResponse, updatedNodeInstance);
  }

  private void invokeState(
      Ambiance ambiance, FacilitatorResponse facilitatorResponse, ExecutionNodeInstance nodeInstance) {
    ExecutionNode node = nodeInstance.getNode();
    State currentState = stateRegistry.obtain(node.getStateType());
    injector.injectMembers(currentState);
    StateExecutionPackage stateExecutionPackage = StateExecutionPackage.builder()
                                                      .ambiance(ambiance)
                                                      .inputs(EngineHelper.obtainInputs(node.getRefObjects()))
                                                      .stateParameter(node.getStateParameters())
                                                      .build();
    switch (facilitatorResponse.getExecutionMode()) {
      case SYNC:
        SyncExecutable syncExecutable = (SyncExecutable) currentState;
        StateResponse stateResponse =
            syncExecutable.executeSync(stateExecutionPackage, facilitatorResponse.getPassThroughData());
        handleStateResponse(nodeInstance.getUuid(), stateResponse);
        break;
      case ASYNC:
        AsyncExecutable asyncExecutable = (AsyncExecutable) currentState;
        AsyncExecutableResponse asyncExecutableResponse = asyncExecutable.executeAsync(stateExecutionPackage);
        handleAsyncExecutableResponse(nodeInstance, asyncExecutableResponse);
        break;
      default:
        logger.info("Add More Handlers, Throw Exception for now");
        throw new InvalidRequestException(
            "No Handling present for execution mode type :" + facilitatorResponse.getExecutionMode());
    }
  }

  public void handleStateResponse(String nodeInstanceId, StateResponse stateResponse) {
    ExecutionNodeInstance nodeInstance = updateNodeInstanceStatus(nodeInstanceId, stateResponse.getExecutionStatus());
    // TODO handle Failure
    ExecutionNode nodeDefinition = nodeInstance.getNode();
    List<Adviser> advisers = EngineHelper.obtainAdvisers(nodeDefinition.getAdviserObtainments());
    if (isEmpty(advisers)) {
      logger.info("No advisers present should end execution");
      return;
    }
    Advise advise = null;
    for (Adviser adviser : advisers) {
      advise = adviser.onAdviseEvent(AdvisingEvent.builder().stateResponse(stateResponse).build());
      if (advise != null) {
        break;
      }
    }
    if (advise == null) {
      logger.info("End Execution");
      return;
    }
    handleAdvise(advise);
  }

  private ExecutionNodeInstance updateNodeInstanceStatus(String nodeInstanceId, NodeExecutionStatus status) {
    Query<ExecutionNodeInstance> findQuery =
        hPersistence.createQuery(ExecutionNodeInstance.class).filter(ExecutionNodeInstanceKeys.uuid, nodeInstanceId);
    UpdateOperations<ExecutionNodeInstance> operations =
        hPersistence.createUpdateOperations(ExecutionNodeInstance.class).set(ExecutionNodeInstanceKeys.status, status);
    return hPersistence.findAndModify(findQuery, operations, HPersistence.upsertReturnNewOptions);
  }

  private void handleAdvise(Advise advise) {
    if (advise != null) {
      logger.info("Advise Received with nextNodeId: {}", advise);
    }
  }

  private void handleAsyncExecutableResponse(
      ExecutionNodeInstance nodeInstance, AsyncExecutableResponse asyncExecutableResponse) {
    ExecutionNode nodeDefinition = nodeInstance.getNode();
    if (isEmpty(asyncExecutableResponse.getCallbackIds())) {
      logger.error("executionResponse is null, but no correlationId - currentState : " + nodeDefinition.getName()
          + ", stateExecutionInstanceId: " + nodeInstance.getUuid());
      throw new InvalidRequestException("Callback Ids cannot be empty for Async Executable Response");
    }
    NotifyCallback callback = EngineResumeCallback.builder().nodeInstanceId(nodeInstance.getUuid()).build();
    waitNotifyEngine.waitForAllOn(
        ORCHESTRATION, callback, asyncExecutableResponse.getCallbackIds().toArray(new String[0]));

    // Update Execution Node Instance state to TASK_WAITING
    updateNodeInstanceStatus(nodeInstance.getUuid(), TASK_WAITING);
  }

  public void resume(String nodeInstanceId, Map<String, ResponseData> response, boolean asyncError) {
    ExecutionNodeInstance nodeInstance = updateNodeInstanceStatus(nodeInstanceId, NodeExecutionStatus.RUNNING);
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
                                .build());
  }
}
