package software.wings.sm;

import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.ElementExecutionSummary.ElementExecutionSummaryBuilder.anElementExecutionSummary;
import static software.wings.beans.InstanceStatusSummary.InstanceStatusSummaryBuilder.anInstanceStatusSummary;
import static software.wings.dl.PageRequest.Builder.aPageRequest;
import static software.wings.sm.ElementNotifyResponseData.Builder.anElementNotifyResponseData;

import com.google.common.collect.Lists;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.InstanceElement;
import software.wings.beans.ElementExecutionSummary;
import software.wings.beans.ErrorCodes;
import software.wings.beans.ErrorStrategy;
import software.wings.beans.InstanceStatusSummary;
import software.wings.beans.SearchFilter.Operator;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsDeque;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.sm.ExecutionEvent.ExecutionEventBuilder;
import software.wings.sm.states.ElementStateExecutionData;
import software.wings.utils.JsonUtils;
import software.wings.utils.Misc;
import software.wings.waitnotify.NotifyCallback;
import software.wings.waitnotify.NotifyResponseData;
import software.wings.waitnotify.WaitNotifyEngine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import javax.inject.Inject;

/**
 * Class responsible for executing state machine.
 *
 * @author Rishi
 */
@Singleton
public class StateMachineExecutor {
  private final Logger logger = LoggerFactory.getLogger(getClass());
  @Inject private ExecutorService executorService;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject private Injector injector;
  @Inject private ExecutionInterruptManager executionInterruptManager;

  /**
   * Execute.
   *
   * @param appId         the app id
   * @param smId          the sm id
   * @param executionUuid the execution uuid
   * @param executionName the execution name
   * @param contextParams the context params
   * @param callback      the callback
   * @return the state execution instance
   */
  public StateExecutionInstance execute(String appId, String smId, String executionUuid, String executionName,
      List<ContextElement> contextParams, StateMachineExecutionCallback callback) {
    return execute(
        wingsPersistence.get(StateMachine.class, appId, smId), executionUuid, executionName, contextParams, callback);
  }

  /**
   * Execute.
   *
   * @param sm            the sm
   * @param executionUuid the execution uuid
   * @param executionName the execution name
   * @param contextParams the context params
   * @param callback      the callback
   * @return the state execution instance
   */
  public StateExecutionInstance execute(StateMachine sm, String executionUuid, String executionName,
      List<ContextElement> contextParams, StateMachineExecutionCallback callback) {
    if (sm == null) {
      logger.error("StateMachine passed for execution is null");
      throw new WingsException(ErrorCodes.INVALID_ARGUMENT);
    }

    StateExecutionInstance stateExecutionInstance = new StateExecutionInstance();
    stateExecutionInstance.setExecutionName(executionName);
    stateExecutionInstance.setExecutionUuid(executionUuid);

    WingsDeque<ContextElement> contextElements = new WingsDeque<>();
    if (contextParams != null) {
      contextElements.addAll(contextParams);
    }
    stateExecutionInstance.setContextElements(contextElements);

    stateExecutionInstance.setCallback(callback);

    if (stateExecutionInstance.getStateName() == null) {
      stateExecutionInstance.setStateName(sm.getInitialStateName());
    }
    return triggerExecution(sm, stateExecutionInstance);
  }

  /**
   * Execute.
   *
   * @param stateMachine           the state machine
   * @param stateExecutionInstance the state execution instance
   * @return the state execution instance
   */
  public StateExecutionInstance execute(StateMachine stateMachine, StateExecutionInstance stateExecutionInstance) {
    if (stateExecutionInstance == null) {
      throw new WingsException(ErrorCodes.INVALID_ARGUMENT, ErrorCodes.ARGS_NAME, "stateExecutionInstance");
    }
    if (stateMachine == null) {
      throw new WingsException(ErrorCodes.INVALID_ARGUMENT, ErrorCodes.ARGS_NAME, "rootStateMachine");
    }
    if (stateExecutionInstance.getChildStateMachineId() != null
        && !stateExecutionInstance.getChildStateMachineId().equals(stateMachine.getUuid())
        && stateMachine.getChildStateMachines().get(stateExecutionInstance.getChildStateMachineId()) == null) {
      throw new WingsException(ErrorCodes.INVALID_ARGUMENT, ErrorCodes.ARGS_NAME, "stateMachine");
    }
    StateMachine sm;
    if (stateExecutionInstance.getChildStateMachineId() == null) {
      sm = stateMachine;
    } else {
      sm = stateMachine.getChildStateMachines().get(stateExecutionInstance.getChildStateMachineId());
    }
    if (stateExecutionInstance.getStateName() == null) {
      stateExecutionInstance.setStateName(sm.getInitialStateName());
    }

    return triggerExecution(stateMachine, stateExecutionInstance);
  }

  /**
   * Trigger execution state execution instance.
   *
   * @param stateMachine           the state machine
   * @param stateExecutionInstance the state execution instance
   * @return the state execution instance
   */
  StateExecutionInstance triggerExecution(StateMachine stateMachine, StateExecutionInstance stateExecutionInstance) {
    if (stateExecutionInstance.getStateName() == null) {
      throw new WingsException(ErrorCodes.INVALID_ARGUMENT, ErrorCodes.ARGS_NAME, "stateName");
    }

    stateExecutionInstance.setAppId(stateMachine.getAppId());
    stateExecutionInstance.setStateMachineId(stateMachine.getUuid());
    stateExecutionInstance.setStateType(
        stateMachine.getState(stateExecutionInstance.getChildStateMachineId(), stateExecutionInstance.getStateName())
            .getStateType());
    if (stateExecutionInstance.getUuid() != null) {
      throw new WingsException(ErrorCodes.INVALID_REQUEST, "message", "StateExecutionInstance was already created");
    }

    stateExecutionInstance = wingsPersistence.saveAndGet(StateExecutionInstance.class, stateExecutionInstance);

    ExecutionContextImpl context = new ExecutionContextImpl(stateExecutionInstance, stateMachine, injector);
    injector.injectMembers(context);
    executorService.execute(new SmExecutionDispatcher(context, this));
    return stateExecutionInstance;
  }

  /**
   * Start execution.
   *
   * @param appId                    the app id
   * @param stateExecutionInstanceId the state execution instance id
   */
  void startExecution(String appId, String stateExecutionInstanceId) {
    StateExecutionInstance stateExecutionInstance =
        wingsPersistence.get(StateExecutionInstance.class, appId, stateExecutionInstanceId);
    StateMachine sm = wingsPersistence.get(StateMachine.class, appId, stateExecutionInstance.getStateMachineId());
    ExecutionContextImpl context = new ExecutionContextImpl(stateExecutionInstance, sm, injector);
    injector.injectMembers(context);
    startExecution(context);
  }

  /**
   * Start execution.
   *
   * @param context the context
   */
  void startExecution(ExecutionContextImpl context) {
    StateExecutionInstance stateExecutionInstance = context.getStateExecutionInstance();

    ExecutionInterrupt executionInterrupt = executionInterruptManager.checkForExecutionInterrupt(
        stateExecutionInstance.getAppId(), stateExecutionInstance.getExecutionUuid());
    if (executionInterrupt != null
        && executionInterrupt.getExecutionInterruptType() == ExecutionInterruptType.PAUSE_ALL) {
      updateStatus(stateExecutionInstance, ExecutionStatus.PAUSED, Lists.newArrayList(ExecutionStatus.NEW));
      waitNotifyEngine.waitForAll(
          new ExecutionResumeAllCallback(stateExecutionInstance.getAppId(), stateExecutionInstance.getUuid()),
          executionInterrupt.getUuid());
      return;
    }
    StateMachine stateMachine = context.getStateMachine();

    boolean updated = updateStartStatus(stateExecutionInstance, ExecutionStatus.STARTING,
        Lists.newArrayList(ExecutionStatus.NEW, ExecutionStatus.PAUSED, ExecutionStatus.PAUSED_ON_ERROR));
    if (!updated) {
      WingsException ex =
          new WingsException("stateExecutionInstance: " + stateExecutionInstance.getUuid() + " could not be started");
      logger.error(ex.getMessage(), ex);
      throw ex;
    }

    State currentState = null;
    ExecutionResponse executionResponse = null;
    Exception ex = null;
    try {
      currentState =
          stateMachine.getState(stateExecutionInstance.getChildStateMachineId(), stateExecutionInstance.getStateName());
      injector.injectMembers(currentState);
      invokedvisors(context, currentState);
      executionResponse = currentState.execute(context);
    } catch (Exception exception) {
      logger.warn("Error in " + stateExecutionInstance.getStateName() + " execution", exception);
      ex = exception;
    }

    if (ex == null) {
      handleExecuteResponse(context, executionResponse);
    } else {
      handleExecuteResponseException(context, ex);
    }
  }

  private ExecutionEventAdvice invokedvisors(ExecutionContextImpl context, State state) {
    List<ExecutionEventAdvisor> advisors = context.getStateExecutionInstance().getExecutionEventAdvisors();
    if (advisors == null || advisors.isEmpty()) {
      return null;
    }

    ExecutionEventAdvice executionEventAdvice = null;
    for (ExecutionEventAdvisor advisor : advisors) {
      executionEventAdvice = advisor.onExecutionEvent(
          ExecutionEventBuilder.anExecutionEvent().withContext(context).withState(state).build());
    }
    return executionEventAdvice;
  }

  /**
   * Handle execute response state execution instance.
   *
   * @param context           the context
   * @param executionResponse the execution response
   * @return the state execution instance
   */
  StateExecutionInstance handleExecuteResponse(ExecutionContextImpl context, ExecutionResponse executionResponse) {
    StateExecutionInstance stateExecutionInstance = context.getStateExecutionInstance();
    StateMachine sm = context.getStateMachine();
    State currentState =
        sm.getState(stateExecutionInstance.getChildStateMachineId(), stateExecutionInstance.getStateName());

    ExecutionStatus status = executionResponse.getExecutionStatus();

    if (executionResponse.isAsync()) {
      if (executionResponse.getCorrelationIds() == null || executionResponse.getCorrelationIds().size() == 0) {
        logger.error("executionResponse is null, but no correlationId - currentState : " + currentState.getName()
            + ", stateExecutionInstanceId: " + stateExecutionInstance.getUuid());
        status = ExecutionStatus.ERROR;
      } else {
        NotifyCallback callback =
            new StateMachineResumeCallback(stateExecutionInstance.getAppId(), stateExecutionInstance.getUuid());
        waitNotifyEngine.waitForAll(callback,
            executionResponse.getCorrelationIds().toArray(new String[executionResponse.getCorrelationIds().size()]));
      }

      boolean updated = updateStateExecutionData(stateExecutionInstance, executionResponse.getStateExecutionData(),
          ExecutionStatus.RUNNING, null, executionResponse.getElements());
      if (!updated) {
        throw new WingsException("updateStateExecutionData failed");
      }
      invokedvisors(context, currentState);
      handleSpawningStateExecutionInstances(sm, stateExecutionInstance, executionResponse);

    } else {
      boolean updated = updateStateExecutionData(stateExecutionInstance, executionResponse.getStateExecutionData(),
          status, executionResponse.getErrorMessage(), executionResponse.getElements());
      if (!updated) {
        throw new WingsException("updateStateExecutionData failed");
      }
      ExecutionEventAdvice executionEventAdvice = invokedvisors(context, currentState);
      if (executionEventAdvice != null
          && (executionEventAdvice.getNextChildStateMachineId() != null
                 || executionEventAdvice.getNextStateName() != null)) {
        executionEventAdviceTransition(context, executionEventAdvice);
      } else if (status == ExecutionStatus.SUCCESS) {
        return successTransition(context);
      } else if (status == ExecutionStatus.FAILED || status == ExecutionStatus.ERROR) {
        return failedTransition(context, null);
      } else if (status == ExecutionStatus.ABORTED) {
        endTransition(context, stateExecutionInstance, ExecutionStatus.ABORTED, null);
      }
    }

    return stateExecutionInstance;
  }

  private StateExecutionInstance executionEventAdviceTransition(
      ExecutionContextImpl context, ExecutionEventAdvice executionEventAdvice) {
    StateMachine sm = context.getStateMachine();
    State nextState =
        sm.getState(executionEventAdvice.getNextChildStateMachineId(), executionEventAdvice.getNextStateName());
    StateExecutionInstance cloned =
        clone(context.getStateExecutionInstance(), executionEventAdvice.getNextChildStateMachineId(), nextState);
    return triggerExecution(sm, cloned);
  }

  /**
   * Handle execute response exception state execution instance.
   *
   * @param context   the context
   * @param exception the exception
   * @return the state execution instance
   */
  StateExecutionInstance handleExecuteResponseException(ExecutionContextImpl context, Exception exception) {
    StateExecutionInstance stateExecutionInstance = context.getStateExecutionInstance();
    StateMachine sm = context.getStateMachine();
    State currentState =
        sm.getState(stateExecutionInstance.getChildStateMachineId(), stateExecutionInstance.getStateName());
    logger.info("Error seen in the state execution  - currentState : {}, stateExecutionInstanceId: {}", currentState,
        stateExecutionInstance.getUuid(), exception);

    updateStateExecutionData(stateExecutionInstance, null, ExecutionStatus.FAILED, exception.getMessage(), null);

    try {
      return failedTransition(context, exception);
    } catch (Exception e2) {
      logger.error("Error in transitioning to failure state", e2);
    }
    return null;
  }

  private StateExecutionInstance successTransition(ExecutionContextImpl context) {
    StateExecutionInstance stateExecutionInstance = context.getStateExecutionInstance();
    StateMachine sm = context.getStateMachine();

    State nextState =
        sm.getSuccessTransition(stateExecutionInstance.getChildStateMachineId(), stateExecutionInstance.getStateName());
    if (nextState == null) {
      logger.info("nextSuccessState is null.. ending execution  - currentState : "
          + stateExecutionInstance.getStateName() + ", stateExecutionInstanceId: " + stateExecutionInstance.getUuid());

      logger.info("State Machine execution ended for the stateMachine: {}, executionUuid: {}", sm.getName(),
          stateExecutionInstance.getExecutionUuid());

      endTransition(context, stateExecutionInstance, ExecutionStatus.SUCCESS, null);
    } else {
      StateExecutionInstance cloned = clone(stateExecutionInstance, nextState);
      return triggerExecution(sm, cloned);
    }

    return null;
  }

  private StateExecutionInstance failedTransition(ExecutionContextImpl context, Exception exception) {
    StateExecutionInstance stateExecutionInstance = context.getStateExecutionInstance();
    StateMachine sm = context.getStateMachine();

    State nextState =
        sm.getFailureTransition(stateExecutionInstance.getChildStateMachineId(), stateExecutionInstance.getStateName());
    if (nextState == null) {
      logger.info("nextFailureState is null.. for the currentState : {}, stateExecutionInstanceId: {}",
          stateExecutionInstance.getStateName(), stateExecutionInstance.getUuid());

      ErrorStrategy errorStrategy = context.getErrorStrategy();
      if (errorStrategy == null || errorStrategy == ErrorStrategy.FAIL) {
        logger.info("Ending execution  - currentState : {}, stateExecutionInstanceId: {}",
            stateExecutionInstance.getStateName(), stateExecutionInstance.getUuid());
        endTransition(context, stateExecutionInstance, ExecutionStatus.FAILED, exception);
      } else if (errorStrategy == ErrorStrategy.PAUSE) {
        logger.info("Pausing execution  - currentState : {}, stateExecutionInstanceId: {}",
            stateExecutionInstance.getStateName(), stateExecutionInstance.getUuid());
        updateStatus(
            stateExecutionInstance, ExecutionStatus.PAUSED_ON_ERROR, Lists.newArrayList(ExecutionStatus.FAILED));
      } else {
        // TODO: handle more strategy
        logger.info("Unhandled error strategy for the state: {}, stateExecutionInstanceId: {}, errorStrategy: {}"
                + stateExecutionInstance.getStateName(),
            stateExecutionInstance.getUuid(), errorStrategy);
      }
    } else {
      StateExecutionInstance cloned = clone(stateExecutionInstance, nextState);
      return triggerExecution(sm, cloned);
    }
    return null;
  }

  private void endTransition(ExecutionContextImpl context, StateExecutionInstance stateExecutionInstance,
      ExecutionStatus status, Exception exception) {
    StateMachineExecutionCallback callback = stateExecutionInstance.getCallback();
    if (stateExecutionInstance.getNotifyId() == null) {
      if (stateExecutionInstance.getCallback() != null) {
        injector.injectMembers(callback);
        callback.callback(context, status, exception);
      } else {
        logger.info("No callback for the stateMachine: {}, executionUuid: {}", context.getStateMachine().getName(),
            stateExecutionInstance.getExecutionUuid());
      }
    } else {
      notify(stateExecutionInstance, status);
    }
  }

  private void abortExecution(ExecutionContextImpl context) {
    StateExecutionInstance stateExecutionInstance = context.getStateExecutionInstance();
    boolean updated = updateStatus(stateExecutionInstance, ExecutionStatus.ABORTING,
        Lists.newArrayList(
            ExecutionStatus.NEW, ExecutionStatus.STARTING, ExecutionStatus.RUNNING, ExecutionStatus.PAUSED));
    if (!updated) {
      throw new WingsException(ErrorCodes.STATE_NOT_FOR_ABORT, "stateName", stateExecutionInstance.getStateName());
    }

    abortMarkedInstance(context, stateExecutionInstance);
  }

  private void abortMarkedInstance(ExecutionContextImpl context, StateExecutionInstance stateExecutionInstance) {
    boolean updated = false;
    StateMachine sm = context.getStateMachine();
    try {
      updated = false;
      State currentState =
          sm.getState(stateExecutionInstance.getChildStateMachineId(), stateExecutionInstance.getStateName());
      injector.injectMembers(currentState);
      currentState.handleAbortEvent(context);
      updated = updateStateExecutionData(stateExecutionInstance, null, ExecutionStatus.ABORTED, null,
          Lists.newArrayList(ExecutionStatus.ABORTING), null);
      invokedvisors(context, currentState);

      endTransition(context, stateExecutionInstance, ExecutionStatus.ABORTED, null);
    } catch (Exception e) {
      logger.error("Error in aborting", e);
    }
    if (!updated) {
      throw new WingsException(ErrorCodes.STATE_ABORT_FAILED, "stateName", stateExecutionInstance.getStateName());
    }
  }

  private void notify(StateExecutionInstance stateExecutionInstance, ExecutionStatus status) {
    ElementNotifyResponseData notifyResponseData = anElementNotifyResponseData().withExecutionStatus(status).build();
    if (stateExecutionInstance.getNotifyElements() != null && !stateExecutionInstance.getNotifyElements().isEmpty()) {
      notifyResponseData.setContextElements(stateExecutionInstance.getNotifyElements());
    }
    waitNotifyEngine.notify(stateExecutionInstance.getNotifyId(), notifyResponseData);
  }

  private void handleSpawningStateExecutionInstances(
      StateMachine sm, StateExecutionInstance stateExecutionInstance, ExecutionResponse executionResponse) {
    if (executionResponse instanceof SpawningExecutionResponse) {
      SpawningExecutionResponse spawningExecutionResponse = (SpawningExecutionResponse) executionResponse;
      if (spawningExecutionResponse.getStateExecutionInstanceList() != null
          && spawningExecutionResponse.getStateExecutionInstanceList().size() > 0) {
        for (StateExecutionInstance childStateExecutionInstance :
            spawningExecutionResponse.getStateExecutionInstanceList()) {
          childStateExecutionInstance.setUuid(null);
          childStateExecutionInstance.setParentInstanceId(stateExecutionInstance.getUuid());
          childStateExecutionInstance.setAppId(stateExecutionInstance.getAppId());
          childStateExecutionInstance.setNotifyElements(null);
          if (childStateExecutionInstance.getStateName() == null
              && childStateExecutionInstance.getChildStateMachineId() != null) {
            if (sm.getChildStateMachines().get(childStateExecutionInstance.getChildStateMachineId()) == null) {
              notify(childStateExecutionInstance, ExecutionStatus.SUCCESS);
              return;
            }
            childStateExecutionInstance.setStateName(sm.getChildStateMachines()
                                                         .get(childStateExecutionInstance.getChildStateMachineId())
                                                         .getInitialStateName());
          }
          triggerExecution(sm, childStateExecutionInstance);
        }
      }
    }
  }

  /**
   * @param stateExecutionInstance
   * @param childStateMachineId
   *@param nextState  @return
   */
  private StateExecutionInstance clone(
      StateExecutionInstance stateExecutionInstance, String childStateMachineId, State nextState) {
    StateExecutionInstance cloned = clone(stateExecutionInstance, nextState);
    cloned.setChildStateMachineId(childStateMachineId);
    return cloned;
  }

  /**
   * @param stateExecutionInstance
   *@param nextState  @return
   */
  private StateExecutionInstance clone(StateExecutionInstance stateExecutionInstance, State nextState) {
    StateExecutionInstance cloned = JsonUtils.clone(stateExecutionInstance, StateExecutionInstance.class);
    cloned.setUuid(null);
    cloned.setStateName(nextState.getName());
    cloned.setPrevInstanceId(stateExecutionInstance.getUuid());
    cloned.setContextTransition(false);
    cloned.setStatus(ExecutionStatus.NEW);
    cloned.setStartTs(null);
    cloned.setEndTs(null);
    return cloned;
  }

  private boolean updateStartStatus(StateExecutionInstance stateExecutionInstance, ExecutionStatus status,
      List<ExecutionStatus> existingExecutionStatus) {
    stateExecutionInstance.setStartTs(System.currentTimeMillis());
    return updateStatus(
        stateExecutionInstance, "startTs", stateExecutionInstance.getStartTs(), status, existingExecutionStatus);
  }

  private boolean updateEndStatus(StateExecutionInstance stateExecutionInstance, ExecutionStatus status,
      List<ExecutionStatus> existingExecutionStatus) {
    stateExecutionInstance.setEndTs(System.currentTimeMillis());
    return updateStatus(
        stateExecutionInstance, "endTs", stateExecutionInstance.getStartTs(), status, existingExecutionStatus);
  }

  private boolean updateStatus(StateExecutionInstance stateExecutionInstance, ExecutionStatus status,
      List<ExecutionStatus> existingExecutionStatus) {
    return updateStatus(stateExecutionInstance, null, null, status, existingExecutionStatus);
  }

  private boolean updateStatus(StateExecutionInstance stateExecutionInstance, String tsField, Long tsValue,
      ExecutionStatus status, List<ExecutionStatus> existingExecutionStatus) {
    stateExecutionInstance.setStatus(status);

    UpdateOperations<StateExecutionInstance> ops =
        wingsPersistence.createUpdateOperations(StateExecutionInstance.class);
    ops.set("status", stateExecutionInstance.getStatus());
    if (tsField != null) {
      ops.set(tsField, tsValue);
    }

    Query<StateExecutionInstance> query = wingsPersistence.createQuery(StateExecutionInstance.class)
                                              .field("appId")
                                              .equal(stateExecutionInstance.getAppId())
                                              .field(ID_KEY)
                                              .equal(stateExecutionInstance.getUuid())
                                              .field("status")
                                              .in(existingExecutionStatus);
    UpdateResults updateResult = wingsPersistence.update(query, ops);
    if (updateResult == null || updateResult.getWriteResult() == null || updateResult.getWriteResult().getN() != 1) {
      logger.warn(
          "StateExecutionInstance status could not be updated- stateExecutionInstance: {}, tsField: {}, tsValue: {}, status: {}, existingExecutionStatus: {}, ",
          stateExecutionInstance.getUuid(), tsField, tsValue, status, existingExecutionStatus);
      return false;
    } else {
      return true;
    }
  }

  private boolean updateStateExecutionData(StateExecutionInstance stateExecutionInstance,
      StateExecutionData stateExecutionData, ExecutionStatus status, String errorMsg, List<ContextElement> elements) {
    return updateStateExecutionData(stateExecutionInstance, stateExecutionData, status, errorMsg, null, elements);
  }

  private boolean updateStateExecutionData(StateExecutionInstance stateExecutionInstance,
      StateExecutionData stateExecutionData, ExecutionStatus status, String errorMsg,
      List<ExecutionStatus> runningStatusLists, List<ContextElement> elements) {
    Map<String, StateExecutionData> stateExecutionMap = stateExecutionInstance.getStateExecutionMap();
    if (stateExecutionMap == null) {
      stateExecutionMap = new HashMap<>();
      stateExecutionInstance.setStateExecutionMap(stateExecutionMap);
    }

    if (stateExecutionData == null) {
      stateExecutionData = stateExecutionMap.get(stateExecutionInstance.getStateName());
      if (stateExecutionData == null) {
        stateExecutionData = new StateExecutionData();
      }
    }

    stateExecutionData.setStateName(stateExecutionInstance.getStateName());
    stateExecutionMap.put(stateExecutionInstance.getStateName(), stateExecutionData);

    UpdateOperations<StateExecutionInstance> ops =
        wingsPersistence.createUpdateOperations(StateExecutionInstance.class);

    stateExecutionInstance.setStatus(status);
    ops.set("status", stateExecutionInstance.getStatus());

    if (status == ExecutionStatus.SUCCESS || status == ExecutionStatus.FAILED || status == ExecutionStatus.ERROR
        || status == ExecutionStatus.ABORTED) {
      stateExecutionInstance.setEndTs(System.currentTimeMillis());
      ops.set("endTs", stateExecutionInstance.getEndTs());
    }

    if (elements != null && !elements.isEmpty()) {
      List<ContextElement> notifyElements = stateExecutionInstance.getNotifyElements();
      if (notifyElements == null) {
        notifyElements = new ArrayList<>();
      }
      for (ContextElement e : elements) {
        if (e != null) {
          stateExecutionInstance.getContextElements().push(e);
          notifyElements.add(e);
        }
      }

      ops.set("contextElements", stateExecutionInstance.getContextElements());

      stateExecutionInstance.setNotifyElements(notifyElements);
      ops.set("notifyElements", notifyElements);
    }
    stateExecutionData.setStartTs(stateExecutionInstance.getStartTs());
    if (stateExecutionInstance.getEndTs() != null) {
      stateExecutionData.setEndTs(stateExecutionInstance.getEndTs());
    }
    stateExecutionData.setStatus(stateExecutionInstance.getStatus());
    if (errorMsg != null) {
      stateExecutionData.setErrorMsg(errorMsg);
    }

    if (runningStatusLists == null || runningStatusLists.isEmpty()) {
      runningStatusLists = Lists.newArrayList(ExecutionStatus.NEW, ExecutionStatus.STARTING, ExecutionStatus.RUNNING,
          ExecutionStatus.PAUSED, ExecutionStatus.PAUSED_ON_ERROR, ExecutionStatus.ABORTING);
    }

    Query<StateExecutionInstance> query = wingsPersistence.createQuery(StateExecutionInstance.class)
                                              .field("appId")
                                              .equal(stateExecutionInstance.getAppId())
                                              .field(ID_KEY)
                                              .equal(stateExecutionInstance.getUuid())
                                              .field("status")
                                              .in(runningStatusLists);

    ops.set("stateExecutionMap", stateExecutionInstance.getStateExecutionMap());
    UpdateResults updateResult = wingsPersistence.update(query, ops);
    boolean updated = true;
    if (updateResult == null || updateResult.getWriteResult() == null || updateResult.getWriteResult().getN() != 1) {
      logger.warn(
          "StateExecutionInstance status could not be updated- stateExecutionInstance: {}, stateExecutionData: {}, status: {}, errorMsg: {}, ",
          stateExecutionInstance.getUuid(), stateExecutionData, status, errorMsg);

      updated = false;
    }

    if ((status == ExecutionStatus.SUCCESS || status == ExecutionStatus.FAILED || status == ExecutionStatus.ERROR)
        && (stateExecutionInstance.getStateType().equals(StateType.REPEAT.name())
               || stateExecutionInstance.getStateType().equals(StateType.FORK.name())
               || stateExecutionInstance.getStateType().equals(StateType.PHASE_STEP.name())
               || stateExecutionInstance.getStateType().equals(StateType.PHASE.name()))) {
      refreshSummary(stateExecutionInstance);
    }
    return updated;
  }

  /**
   * Resumes execution of a StateMachineInstance.
   *
   * @param appId                    the app id
   * @param stateExecutionInstanceId stateMachineInstance to resume.
   * @param response                 map of responses from state machine instances this state was waiting on.
   */
  public void resume(String appId, String stateExecutionInstanceId, Map<String, NotifyResponseData> response) {
    StateExecutionInstance stateExecutionInstance =
        wingsPersistence.get(StateExecutionInstance.class, appId, stateExecutionInstanceId);
    StateMachine sm = wingsPersistence.get(StateMachine.class, appId, stateExecutionInstance.getStateMachineId());
    State currentState =
        sm.getState(stateExecutionInstance.getChildStateMachineId(), stateExecutionInstance.getStateName());
    injector.injectMembers(currentState);

    while (stateExecutionInstance.getStatus() == ExecutionStatus.NEW
        || stateExecutionInstance.getStatus() == ExecutionStatus.STARTING) {
      logger.warn("stateExecutionInstance: {} status is not in RUNNING state yet", stateExecutionInstance.getUuid());
      // TODO - more elegant way
      Misc.quietSleep(500);
      stateExecutionInstance = wingsPersistence.get(StateExecutionInstance.class, appId, stateExecutionInstanceId);
    }
    if (stateExecutionInstance.getStatus() != ExecutionStatus.RUNNING) {
      WingsException ex = new WingsException(
          "stateExecutionInstance: " + stateExecutionInstance.getUuid() + " status is no longer in RUNNING state");
      logger.error(ex.getMessage(), ex);
      throw ex;
    }
    ExecutionContextImpl context = new ExecutionContextImpl(stateExecutionInstance, sm, injector);
    injector.injectMembers(context);
    executorService.execute(new SmExecutionAsyncResumer(context, currentState, response, this));
  }

  /**
   * Handle event.
   *
   * @param workflowExecutionInterrupt the workflow execution event
   */
  public void handleEvent(ExecutionInterrupt workflowExecutionInterrupt) {
    switch (workflowExecutionInterrupt.getExecutionInterruptType()) {
      case RESUME: {
        StateExecutionInstance stateExecutionInstance = wingsPersistence.get(StateExecutionInstance.class,
            workflowExecutionInterrupt.getAppId(), workflowExecutionInterrupt.getStateExecutionInstanceId());

        StateMachine sm = wingsPersistence.get(
            StateMachine.class, workflowExecutionInterrupt.getAppId(), stateExecutionInstance.getStateMachineId());

        State currentState =
            sm.getState(stateExecutionInstance.getChildStateMachineId(), stateExecutionInstance.getStateName());
        injector.injectMembers(currentState);

        ExecutionContextImpl context = new ExecutionContextImpl(stateExecutionInstance, sm, injector);
        injector.injectMembers(context);
        executorService.execute(new SmExecutionResumer(context, this));
        break;
      }

      case RETRY: {
        StateExecutionInstance stateExecutionInstance = wingsPersistence.get(StateExecutionInstance.class,
            workflowExecutionInterrupt.getAppId(), workflowExecutionInterrupt.getStateExecutionInstanceId());

        clearStateExecutionData(stateExecutionInstance);
        StateMachine sm = wingsPersistence.get(
            StateMachine.class, workflowExecutionInterrupt.getAppId(), stateExecutionInstance.getStateMachineId());

        State currentState =
            sm.getState(stateExecutionInstance.getChildStateMachineId(), stateExecutionInstance.getStateName());
        injector.injectMembers(currentState);

        ExecutionContextImpl context = new ExecutionContextImpl(stateExecutionInstance, sm, injector);
        injector.injectMembers(context);
        executorService.execute(new SmExecutionDispatcher(context, this));
        break;
      }

      case ABORT: {
        StateExecutionInstance stateExecutionInstance = wingsPersistence.get(StateExecutionInstance.class,
            workflowExecutionInterrupt.getAppId(), workflowExecutionInterrupt.getStateExecutionInstanceId());

        StateMachine sm = wingsPersistence.get(
            StateMachine.class, workflowExecutionInterrupt.getAppId(), stateExecutionInstance.getStateMachineId());
        ExecutionContextImpl context = new ExecutionContextImpl(stateExecutionInstance, sm, injector);
        injector.injectMembers(context);
        abortExecution(context);
        break;
      }
      case ABORT_ALL: {
        boolean updated = markAbortingState(workflowExecutionInterrupt);
        PageRequest<StateExecutionInstance> pageRequest =
            aPageRequest()
                .withLimit(PageRequest.UNLIMITED)
                .addFilter("appId", Operator.EQ, workflowExecutionInterrupt.getAppId())
                .addFilter("executionUuid", Operator.EQ, workflowExecutionInterrupt.getExecutionUuid())
                .addFilter("status", Operator.IN, ExecutionStatus.ABORTING)
                .addFilter("stateType", Operator.NOT_IN, StateType.REPEAT.name(), StateType.FORK.name())
                .build();

        PageResponse<StateExecutionInstance> stateExecutionInstances =
            wingsPersistence.query(StateExecutionInstance.class, pageRequest);
        if (stateExecutionInstances == null || stateExecutionInstances.isEmpty()) {
          logger.warn(
              "ABORT_ALL workflowExecutionInterrupt: {} being ignored as no running instance found for executionUuid: {}",
              workflowExecutionInterrupt.getUuid(), workflowExecutionInterrupt.getExecutionUuid());
        }

        StateMachine sm = wingsPersistence.get(StateMachine.class, workflowExecutionInterrupt.getAppId(),
            stateExecutionInstances.get(0).getStateMachineId());

        for (StateExecutionInstance stateExecutionInstance : stateExecutionInstances) {
          ExecutionContextImpl context = new ExecutionContextImpl(stateExecutionInstance, sm, injector);
          injector.injectMembers(context);
          abortMarkedInstance(context, stateExecutionInstance);
        }
        break;
      }
    }
    // TODO - more cases
  }

  private void clearStateExecutionData(StateExecutionInstance stateExecutionInstance) {
    Map<String, StateExecutionData> stateExecutionMap = stateExecutionInstance.getStateExecutionMap();
    if (stateExecutionMap == null) {
      return;
    }

    UpdateOperations<StateExecutionInstance> ops =
        wingsPersistence.createUpdateOperations(StateExecutionInstance.class);

    StateExecutionData stateExecutionData = stateExecutionMap.get(stateExecutionInstance.getStateName());
    ops.add("stateExecutionDataHistory", stateExecutionData);
    stateExecutionInstance.getStateExecutionDataHistory().add(stateExecutionData);

    stateExecutionMap.remove(stateExecutionInstance.getStateName());
    ops.set("stateExecutionMap", stateExecutionInstance.getStateExecutionMap());

    if (stateExecutionInstance.getEndTs() != null) {
      stateExecutionInstance.setEndTs(null);
      ops.unset("endTs");
    }

    Query<StateExecutionInstance> query = wingsPersistence.createQuery(StateExecutionInstance.class)
                                              .field("appId")
                                              .equal(stateExecutionInstance.getAppId())
                                              .field(ID_KEY)
                                              .equal(stateExecutionInstance.getUuid())
                                              .field("status")
                                              .equal(ExecutionStatus.PAUSED_ON_ERROR);

    UpdateResults updateResult = wingsPersistence.update(query, ops);
    if (updateResult == null || updateResult.getWriteResult() == null || updateResult.getWriteResult().getN() != 1) {
      logger.warn("clearStateExecutionData could not be completed for the stateExecutionInstance: {}",
          stateExecutionInstance.getUuid());

      throw new WingsException(ErrorCodes.RETRY_FAILED, "stateName", stateExecutionInstance.getStateName());
    }
  }

  private void refreshSummary(StateExecutionInstance parentStateExecutionInstance) {
    StateExecutionData stateExecutionData = parentStateExecutionInstance.getStateExecutionData();
    if (!(stateExecutionData instanceof ElementStateExecutionData)) {
      logger.error(
          "refreshSummary could not be done for parentStateExecutionInstance: {} as stateExecutionData is not of ElementStateExecutionData type",
          parentStateExecutionInstance.getUuid());
      return;
    }

    PageRequest<StateExecutionInstance> pageRequest =
        aPageRequest()
            .withLimit(PageRequest.UNLIMITED)
            .addFilter("appId", Operator.EQ, parentStateExecutionInstance.getAppId())
            .addFilter("executionUuid", Operator.EQ, parentStateExecutionInstance.getExecutionUuid())
            .addFilter("parentInstanceId", Operator.IN, parentStateExecutionInstance.getUuid())
            .build();

    PageResponse<StateExecutionInstance> pageResponse =
        wingsPersistence.query(StateExecutionInstance.class, pageRequest);
    if (pageResponse == null || pageResponse.isEmpty()) {
      return;
    }

    List<StateExecutionInstance> contextTransitionInstances = new ArrayList<>();
    Map<String, StateExecutionInstance> prevInstanceIdMap = new HashMap<>();

    mapChildInstances(pageResponse.getResponse(), prevInstanceIdMap, contextTransitionInstances);

    ElementStateExecutionData elementStateExecutionData = (ElementStateExecutionData) stateExecutionData;
    elementStateExecutionData.setInstanceStatusSummary(new ArrayList<>());
    for (StateExecutionInstance stateExecutionInstance : contextTransitionInstances) {
      if (stateExecutionInstance.getContextElement() == null) {
        logger.error(
            "refreshSummary - no contextElement for stateExecutionInstance: {}", stateExecutionInstance.getUuid());
        continue;
      }
      ContextElement contextElement = stateExecutionInstance.getContextElement();
      ElementExecutionSummary elementExecutionSummary = anElementExecutionSummary()
                                                            .withContextElement(contextElement)
                                                            .withStartTs(stateExecutionInstance.getStartTs())
                                                            .build();
      elementStateExecutionData.getElementStatusSummary().add(elementExecutionSummary);

      List<InstanceStatusSummary> instanceStatusSummary = new ArrayList<>();
      StateExecutionInstance last = stateExecutionInstance;
      StateExecutionInstance next = stateExecutionInstance;
      while (next != null) {
        if ((next.getStateType().equals(StateType.REPEAT.name()) || next.getStateType().equals(StateType.FORK.name())
                || next.getStateType().equals(StateType.PHASE.name())
                || next.getStateType().equals(StateType.PHASE_STEP.name()))
            && (next.getStateExecutionData() instanceof ElementStateExecutionData)) {
          ElementStateExecutionData childStateExecutionData = (ElementStateExecutionData) next.getStateExecutionData();
          if (childStateExecutionData.getInstanceStatusSummary() != null) {
            instanceStatusSummary.addAll(childStateExecutionData.getInstanceStatusSummary());
          }
        }
        last = next;
        next = prevInstanceIdMap.get(next.getUuid());
      }

      if (elementExecutionSummary.getEndTs() == null || elementExecutionSummary.getEndTs() < last.getEndTs()) {
        elementExecutionSummary.setEndTs(last.getEndTs());
        elementExecutionSummary.setStatus(last.getStatus());
      }
      elementExecutionSummary.setStatus(last.getStatus());
      if (last.getContextElement() != null
          && last.getContextElement().getElementType() == ContextElementType.INSTANCE) {
        instanceStatusSummary.add(anInstanceStatusSummary()
                                      .withStatus(last.getStatus())
                                      .withInstanceElement((InstanceElement) last.getContextElement())
                                      .build());
      }
      elementExecutionSummary.setInstancesCount(instanceStatusSummary.size());
      elementStateExecutionData.getInstanceStatusSummary().addAll(instanceStatusSummary);
    }
    wingsPersistence.updateField(StateExecutionInstance.class, parentStateExecutionInstance.getUuid(),
        "stateExecutionMap", parentStateExecutionInstance.getStateExecutionMap());
  }

  private void mapChildInstances(List<StateExecutionInstance> childInstances,
      Map<String, StateExecutionInstance> prevInstanceIdMap, List<StateExecutionInstance> contextTransitionInstances) {
    childInstances.forEach(stateExecutionInstance -> {
      String prevInstanceId = stateExecutionInstance.getPrevInstanceId();
      if (prevInstanceId != null) {
        prevInstanceIdMap.put(prevInstanceId, stateExecutionInstance);
      }
      if (stateExecutionInstance.isContextTransition()) {
        contextTransitionInstances.add(stateExecutionInstance);
      }
    });
  }

  private boolean markAbortingState(ExecutionInterrupt workflowExecutionInterrupt) {
    UpdateOperations<StateExecutionInstance> ops =
        wingsPersistence.createUpdateOperations(StateExecutionInstance.class);
    ops.set("status", ExecutionStatus.ABORTING);

    Query<StateExecutionInstance> query =
        wingsPersistence.createQuery(StateExecutionInstance.class)
            .field("appId")
            .equal(workflowExecutionInterrupt.getAppId())
            .field("executionUuid")
            .equal(workflowExecutionInterrupt.getExecutionUuid())
            .field("status")
            .in(Lists.newArrayList(ExecutionStatus.NEW, ExecutionStatus.RUNNING, ExecutionStatus.STARTING,
                ExecutionStatus.PAUSED, ExecutionStatus.PAUSED_ON_ERROR))
            .field("stateType")
            .notIn(Lists.newArrayList(StateType.REPEAT.name(), StateType.FORK.name(), StateType.PHASE.name(),
                StateType.PHASE_STEP.name(), StateType.SUB_WORKFLOW.name()));
    UpdateResults updateResult = wingsPersistence.update(query, ops);
    if (updateResult == null || updateResult.getWriteResult() == null || updateResult.getWriteResult().getN() == 0) {
      logger.warn("No stateExecutionInstance could be marked as ABORTING - appId: {}, executionUuid: {}",
          workflowExecutionInterrupt.getAppId(), workflowExecutionInterrupt.getExecutionUuid());
      return false;
    } else {
      return true;
    }
  }

  private static class SmExecutionDispatcher implements Runnable {
    private ExecutionContextImpl context;
    private StateMachineExecutor stateMachineExecutor;

    /**
     * Instantiates a new Sm execution dispatcher.
     *
     * @param context              the context
     * @param stateMachineExecutor the state machine executor
     */
    public SmExecutionDispatcher(ExecutionContextImpl context, StateMachineExecutor stateMachineExecutor) {
      this.context = context;
      this.stateMachineExecutor = stateMachineExecutor;
    }

    /* (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
      stateMachineExecutor.startExecution(context);
    }
  }

  private static class SmExecutionResumer implements Runnable {
    private ExecutionContextImpl context;
    private StateMachineExecutor stateMachineExecutor;

    /**
     * Instantiates a new Sm execution dispatcher.
     *
     * @param context              the context
     * @param stateMachineExecutor the state machine executor
     */
    public SmExecutionResumer(ExecutionContextImpl context, StateMachineExecutor stateMachineExecutor) {
      this.context = context;
      this.stateMachineExecutor = stateMachineExecutor;
    }

    /* (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
      try {
        stateMachineExecutor.handleExecuteResponse(context, new ExecutionResponse());
      } catch (Exception ex) {
        stateMachineExecutor.handleExecuteResponseException(context, ex);
      }
    }
  }

  private static class SmExecutionAsyncResumer implements Runnable {
    private ExecutionContextImpl context;
    private StateMachineExecutor stateMachineExecutor;
    private State state;
    private Map<String, NotifyResponseData> response;

    /**
     * Instantiates a new Sm execution dispatcher.
     *
     * @param context              the context
     * @param state                the state
     * @param response             the response
     * @param stateMachineExecutor the state machine executor
     */
    public SmExecutionAsyncResumer(ExecutionContextImpl context, State state, Map<String, NotifyResponseData> response,
        StateMachineExecutor stateMachineExecutor) {
      this.context = context;
      this.state = state;
      this.response = response;
      this.stateMachineExecutor = stateMachineExecutor;
    }

    /* (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
      try {
        ExecutionResponse executionResponse = state.handleAsyncResponse(context, response);
        stateMachineExecutor.handleExecuteResponse(context, executionResponse);
      } catch (Exception ex) {
        stateMachineExecutor.handleExecuteResponseException(context, ex);
      }
    }
  }
}
