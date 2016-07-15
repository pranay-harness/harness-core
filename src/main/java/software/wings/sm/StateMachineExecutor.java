package software.wings.sm;

import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.sm.ExecutionStatus.ExecutionStatusData.Builder.anExecutionStatusData;

import com.google.common.collect.Lists;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.ErrorCodes;
import software.wings.dl.WingsDeque;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.utils.JsonUtils;
import software.wings.utils.Misc;
import software.wings.waitnotify.NotifyCallback;
import software.wings.waitnotify.NotifyResponseData;
import software.wings.waitnotify.WaitNotifyEngine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import javax.inject.Inject;

// TODO: Auto-generated Javadoc

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
  @Inject private StateMachineExecutionEventManager stateMachineExecutionEventManager;

  /**
   * Execute.
   *
   * @param appId         the app id
   * @param smId          the sm id
   * @param executionUuid the execution uuid
   * @return the state execution instance
   */
  public StateExecutionInstance execute(String appId, String smId, String executionUuid) {
    return execute(appId, smId, executionUuid, null);
  }

  /**
   * Execute.
   *
   * @param appId         the app id
   * @param smId          the sm id
   * @param executionUuid the execution uuid
   * @param contextParams the context params
   * @return the state execution instance
   */
  public StateExecutionInstance execute(
      String appId, String smId, String executionUuid, List<ContextElement> contextParams) {
    return execute(wingsPersistence.get(StateMachine.class, appId, smId), executionUuid, contextParams, null);
  }

  /**
   * Execute.
   *
   * @param appId         the app id
   * @param smId          the sm id
   * @param executionUuid the execution uuid
   * @param contextParams the context params
   * @param callback      the callback
   * @return the state execution instance
   */
  public StateExecutionInstance execute(String appId, String smId, String executionUuid,
      List<ContextElement> contextParams, StateMachineExecutionCallback callback) {
    return execute(wingsPersistence.get(StateMachine.class, appId, smId), executionUuid, contextParams, callback);
  }

  /**
   * Execute.
   *
   * @param sm            the sm
   * @param executionUuid the execution uuid
   * @param contextParams the context params
   * @param callback      the callback
   * @return the state execution instance
   */
  public StateExecutionInstance execute(StateMachine sm, String executionUuid, List<ContextElement> contextParams,
      StateMachineExecutionCallback callback) {
    if (sm == null) {
      logger.error("StateMachine passed for execution is null");
      throw new WingsException(ErrorCodes.INVALID_ARGUMENT);
    }

    StateExecutionInstance stateExecutionInstance = new StateExecutionInstance();
    stateExecutionInstance.setAppId(sm.getAppId());
    stateExecutionInstance.setStateMachineId(sm.getUuid());
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
      throw new WingsException(ErrorCodes.INVALID_ARGUMENT, ErrorCodes.ARGS_NAME, "stateMachine");
    }
    if (stateExecutionInstance.getStateName() == null) {
      stateExecutionInstance.setStateName(stateMachine.getInitialStateName());
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

    stateExecutionInstance.setStateMachineId(stateMachine.getUuid());
    stateExecutionInstance.setStateType(stateMachine.getState(stateExecutionInstance.getStateName()).getStateType());

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
   * @param context the context
   */
  void startExecution(String appId, String stateExecutionInstanceId) {
    StateExecutionInstance stateExecutionInstance =
        wingsPersistence.get(StateExecutionInstance.class, appId, stateExecutionInstanceId);
    StateMachine sm = wingsPersistence.get(StateMachine.class, appId, stateExecutionInstance.getStateMachineId());
    State currentState = sm.getState(stateExecutionInstance.getStateName());
    injector.injectMembers(currentState);
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

    ExecutionEvent executionEvent = stateMachineExecutionEventManager.checkForExecutionEvent(
        stateExecutionInstance.getAppId(), stateExecutionInstance.getExecutionUuid());
    if (executionEvent != null && executionEvent.getExecutionEventType() == ExecutionEventType.PAUSE_ALL) {
      updateStatus(stateExecutionInstance, ExecutionStatus.PAUSED, Lists.newArrayList(ExecutionStatus.NEW));
      waitNotifyEngine.waitForAll(
          new ExecutionResumeAllCallback(stateExecutionInstance.getAppId(), stateExecutionInstance.getUuid()),
          executionEvent.getUuid());
      return;
    }
    StateMachine stateMachine = context.getStateMachine();

    boolean updated = updateStartStatus(stateExecutionInstance, ExecutionStatus.STARTING,
        Lists.newArrayList(ExecutionStatus.NEW, ExecutionStatus.PAUSED));
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
      currentState = stateMachine.getState(stateExecutionInstance.getStateName());
      injector.injectMembers(currentState);
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

  private StateExecutionInstance handleExecuteResponse(
      ExecutionContextImpl context, ExecutionResponse executionResponse) {
    StateExecutionInstance stateExecutionInstance = context.getStateExecutionInstance();
    StateMachine sm = context.getStateMachine();
    State currentState = sm.getState(stateExecutionInstance.getStateName());

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

      boolean updated = updateStateExecutionData(
          stateExecutionInstance, executionResponse.getStateExecutionData(), ExecutionStatus.RUNNING, null);
      if (!updated) {
        throw new WingsException("updateStateExecutionData failed");
      }
      handleSpawningStateExecutionInstances(sm, stateExecutionInstance, executionResponse);

    } else {
      boolean updated = updateStateExecutionData(stateExecutionInstance, executionResponse.getStateExecutionData(),
          status, executionResponse.getErrorMessage());
      if (!updated) {
        throw new WingsException("updateStateExecutionData failed");
      }
      if (status == ExecutionStatus.SUCCESS) {
        return successTransition(context);
      } else if (status == ExecutionStatus.FAILED || status == ExecutionStatus.ERROR) {
        return failedTransition(context, null);
      }
    }

    return stateExecutionInstance;
  }

  private StateExecutionInstance handleExecuteResponseException(ExecutionContextImpl context, Exception exception) {
    StateExecutionInstance stateExecutionInstance = context.getStateExecutionInstance();
    StateMachine sm = context.getStateMachine();
    State currentState = sm.getState(stateExecutionInstance.getStateName());
    logger.info("Error seen in the state execution  - currentState : {}, stateExecutionInstanceId: {}", currentState,
        stateExecutionInstance.getUuid(), exception);

    updateStateExecutionData(stateExecutionInstance, null, ExecutionStatus.FAILED, exception.getMessage());

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

    State nextState = sm.getSuccessTransition(stateExecutionInstance.getStateName());
    if (nextState == null) {
      logger.info("nextSuccessState is null.. ending execution  - currentState : "
          + stateExecutionInstance.getStateName() + ", stateExecutionInstanceId: " + stateExecutionInstance.getUuid());
      if (stateExecutionInstance.getNotifyId() == null) {
        logger.info("State Machine execution ended for the stateMachine: {}, executionUuid: {}", sm.getName(),
            stateExecutionInstance.getExecutionUuid());
        if (stateExecutionInstance.getCallback() != null) {
          injector.injectMembers(stateExecutionInstance.getCallback());
          stateExecutionInstance.getCallback().callback(context, ExecutionStatus.SUCCESS, null);
        } else {
          logger.info("No callback for the stateMachine: {}, executionUuid: {}", sm.getName(),
              stateExecutionInstance.getExecutionUuid());
        }
      } else {
        waitNotifyEngine.notify(stateExecutionInstance.getNotifyId(),
            anExecutionStatusData().withExecutionStatus(ExecutionStatus.SUCCESS).build());
      }
    } else {
      StateExecutionInstance cloned = clone(stateExecutionInstance, nextState);
      return triggerExecution(sm, cloned);
    }

    return null;
  }

  private StateExecutionInstance failedTransition(ExecutionContextImpl context, Exception exception) {
    StateExecutionInstance stateExecutionInstance = context.getStateExecutionInstance();
    StateMachine sm = context.getStateMachine();

    State nextState = sm.getFailureTransition(stateExecutionInstance.getStateName());
    if (nextState == null) {
      logger.info("nextFailureState is null.. ending execution  - currentState : "
          + stateExecutionInstance.getStateName() + ", stateExecutionInstanceId: " + stateExecutionInstance.getUuid());
      if (stateExecutionInstance.getNotifyId() == null) {
        logger.info("State Machine execution failed for the stateMachine: {}, executionUuid: {}", sm.getName(),
            stateExecutionInstance.getExecutionUuid());
        if (stateExecutionInstance.getCallback() != null) {
          injector.injectMembers(stateExecutionInstance.getCallback());
          stateExecutionInstance.getCallback().callback(context, ExecutionStatus.FAILED, exception);
        } else {
          logger.info("No callback for the stateMachine: {}, executionUuid: {}", sm.getName(),
              stateExecutionInstance.getExecutionUuid());
        }
      } else {
        waitNotifyEngine.notify(stateExecutionInstance.getNotifyId(),
            anExecutionStatusData().withExecutionStatus(ExecutionStatus.FAILED).build());
      }
    } else {
      StateExecutionInstance cloned = clone(stateExecutionInstance, nextState);
      return triggerExecution(sm, cloned);
    }
    return null;
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
          triggerExecution(sm, childStateExecutionInstance);
        }
      }
    }
  }

  /**
   * @param stateExecutionInstance
   * @param nextState
   * @return
   */
  private StateExecutionInstance clone(StateExecutionInstance stateExecutionInstance, State nextState) {
    StateExecutionInstance cloned = JsonUtils.clone(stateExecutionInstance, StateExecutionInstance.class);
    cloned.setUuid(null);
    cloned.setStateName(nextState.getName());
    cloned.setPrevInstanceId(stateExecutionInstance.getUuid());
    cloned.setContextElementName(null);
    cloned.setContextElementType(null);
    cloned.setContextTransition(false);
    cloned.setStatus(ExecutionStatus.NEW);
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
      StateExecutionData stateExecutionData, ExecutionStatus status, String errorMsg) {
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

    stateExecutionMap.put(stateExecutionInstance.getStateName(), stateExecutionData);

    UpdateOperations<StateExecutionInstance> ops =
        wingsPersistence.createUpdateOperations(StateExecutionInstance.class);

    stateExecutionInstance.setStatus(status);
    ops.set("status", stateExecutionInstance.getStatus());

    if (status == ExecutionStatus.SUCCESS || status == ExecutionStatus.FAILED || status == ExecutionStatus.ERROR) {
      stateExecutionInstance.setEndTs(System.currentTimeMillis());
      ops.set("endTs", stateExecutionInstance.getEndTs());
    }

    stateExecutionData.setStartTs(stateExecutionInstance.getStartTs());
    if (stateExecutionInstance.getEndTs() != null) {
      stateExecutionData.setEndTs(stateExecutionInstance.getEndTs());
    }
    stateExecutionData.setStatus(stateExecutionInstance.getStatus());
    if (errorMsg != null) {
      stateExecutionData.setErrorMsg(errorMsg);
    }

    Query<StateExecutionInstance> query = wingsPersistence.createQuery(StateExecutionInstance.class)
                                              .field("appId")
                                              .equal(stateExecutionInstance.getAppId())
                                              .field(ID_KEY)
                                              .equal(stateExecutionInstance.getUuid())
                                              .field("status")
                                              .in(Lists.newArrayList(ExecutionStatus.NEW, ExecutionStatus.STARTING,
                                                  ExecutionStatus.RUNNING, ExecutionStatus.PAUSED));

    ops.set("stateExecutionMap", stateExecutionInstance.getStateExecutionMap());
    UpdateResults updateResult = wingsPersistence.update(query, ops);
    if (updateResult == null || updateResult.getWriteResult() == null || updateResult.getWriteResult().getN() != 1) {
      logger.warn(
          "StateExecutionInstance status could not be updated- stateExecutionInstance: {}, stateExecutionData: {}, status: {}, errorMsg: {}, ",
          stateExecutionInstance.getUuid(), stateExecutionData, status, errorMsg);
      return false;
    } else {
      return true;
    }
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
    State currentState = sm.getState(stateExecutionInstance.getStateName());
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
   * @param workflowExecutionEvent the workflow execution event
   */
  public void handleEvent(ExecutionEvent workflowExecutionEvent) {
    switch (workflowExecutionEvent.getExecutionEventType()) {
      case RESUME: {
        StateExecutionInstance stateExecutionInstance = wingsPersistence.get(StateExecutionInstance.class,
            workflowExecutionEvent.getAppId(), workflowExecutionEvent.getStateExecutionInstanceId());

        StateMachine sm = wingsPersistence.get(
            StateMachine.class, workflowExecutionEvent.getAppId(), stateExecutionInstance.getStateMachineId());

        State currentState = sm.getState(stateExecutionInstance.getStateName());
        injector.injectMembers(currentState);

        ExecutionContextImpl context = new ExecutionContextImpl(stateExecutionInstance, sm, injector);
        injector.injectMembers(context);
        executorService.execute(new SmExecutionResumer(context, this));
        break;
      }

      case ABORT: {
        StateExecutionInstance stateExecutionInstance = wingsPersistence.get(StateExecutionInstance.class,
            workflowExecutionEvent.getAppId(), workflowExecutionEvent.getStateExecutionInstanceId());

        boolean updated = updateEndStatus(stateExecutionInstance, ExecutionStatus.ABORTING,
            Lists.newArrayList(
                ExecutionStatus.NEW, ExecutionStatus.STARTING, ExecutionStatus.RUNNING, ExecutionStatus.PAUSED));
        if (!updated) {
          throw new WingsException(ErrorCodes.STATE_NOT_FOR_ABORT, "stateName", stateExecutionInstance.getStateName());
        }
        StateMachine sm = wingsPersistence.get(
            StateMachine.class, workflowExecutionEvent.getAppId(), stateExecutionInstance.getStateMachineId());
        State currentState = sm.getState(stateExecutionInstance.getStateName());
        injector.injectMembers(currentState);
        ExecutionContextImpl context = new ExecutionContextImpl(stateExecutionInstance, sm, injector);
        injector.injectMembers(context);
        try {
          updated = false;
          currentState.handleAbortEvent(context);
          updated = updateEndStatus(
              stateExecutionInstance, ExecutionStatus.ABORTED, Lists.newArrayList(ExecutionStatus.ABORTING));
        } catch (Exception e) {
          logger.error("Error in aborting", e);
        }
        if (!updated) {
          throw new WingsException(ErrorCodes.STATE_ABORT_FAILED, "stateName", stateExecutionInstance.getStateName());
        }
        break;
      }
    }
    // TODO - more cases
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
