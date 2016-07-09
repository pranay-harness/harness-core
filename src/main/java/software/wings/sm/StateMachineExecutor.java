package software.wings.sm;

import static software.wings.sm.ExecutionStatus.ExecutionStatusData.Builder.anExecutionStatusData;

import com.google.inject.Injector;
import com.google.inject.Singleton;

import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.ErrorCodes;
import software.wings.beans.WorkflowExecutionEvent;
import software.wings.dl.WingsDeque;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.utils.JsonUtils;
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
  void startExecution(ExecutionContextImpl context) {
    StateExecutionInstance stateExecutionInstance = context.getStateExecutionInstance();
    StateMachine stateMachine = context.getStateMachine();

    stateExecutionInstance.setStatus(ExecutionStatus.RUNNING);
    stateExecutionInstance.setStartTs(System.currentTimeMillis());
    updateStatus(stateExecutionInstance, "startTs", stateExecutionInstance.getStartTs());

    State currentState = null;
    try {
      currentState = stateMachine.getState(stateExecutionInstance.getStateName());
      injector.injectMembers(currentState);
      ExecutionResponse executionResponse = currentState.execute(context);
      handleExecuteResponse(context, executionResponse);
    } catch (Exception exeception) {
      stateExecutionInstance.setStatus(ExecutionStatus.FAILED);
      stateExecutionInstance.setEndTs(System.currentTimeMillis());
      handleExecuteResponseException(context, exeception);
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
    ExecutionContextImpl context = new ExecutionContextImpl(stateExecutionInstance, sm, injector);
    injector.injectMembers(context);
    try {
      ExecutionResponse executionResponse = currentState.handleAsyncResponse(context, response);
      handleExecuteResponse(context, executionResponse);
    } catch (Exception ex) {
      handleExecuteResponseException(context, ex);
    }
  }

  public void handleEvent(WorkflowExecutionEvent workflowExecutionEvent) {
    StateExecutionInstance stateExecutionInstance = wingsPersistence.get(StateExecutionInstance.class,
        workflowExecutionEvent.getAppId(), workflowExecutionEvent.getStateExecutionInstanceId());
    StateMachine sm = wingsPersistence.get(
        StateMachine.class, workflowExecutionEvent.getAppId(), stateExecutionInstance.getStateMachineId());
    State currentState = sm.getState(stateExecutionInstance.getStateName());
    injector.injectMembers(currentState);
    ExecutionContextImpl context = new ExecutionContextImpl(stateExecutionInstance, sm, injector);
    injector.injectMembers(context);
    try {
      currentState.handleEvent(context, workflowExecutionEvent);
    } catch (Exception ex) {
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
        stateExecutionInstance.setStatus(status);
        stateExecutionInstance.setEndTs(System.currentTimeMillis());
        updateStatus(stateExecutionInstance, "endTs", stateExecutionInstance.getEndTs());
      } else {
        NotifyCallback callback =
            new StateMachineResumeCallback(stateExecutionInstance.getAppId(), stateExecutionInstance.getUuid());
        waitNotifyEngine.waitForAll(callback,
            executionResponse.getCorrelationIds().toArray(new String[executionResponse.getCorrelationIds().size()]));
      }

      updateStateExecutionData(stateExecutionInstance, executionResponse.getStateExecutionData(), status, null);
      handleSpawningStateExecutionInstances(sm, stateExecutionInstance, executionResponse);

    } else {
      stateExecutionInstance.setStatus(status);
      stateExecutionInstance.setEndTs(System.currentTimeMillis());
      updateStateExecutionData(stateExecutionInstance, executionResponse.getStateExecutionData(), status,
          executionResponse.getErrorMessage());

      if (status == ExecutionStatus.SUCCESS) {
        return successTransition(context);
      } else {
        return failedTransition(context, null);
      }
    }
    return null;
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

  private StateExecutionInstance successTransition(ExecutionContextImpl context) {
    StateExecutionInstance stateExecutionInstance = context.getStateExecutionInstance();
    StateMachine sm = context.getStateMachine();

    updateStatus(stateExecutionInstance, "endTs", stateExecutionInstance.getEndTs());

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

    updateStatus(stateExecutionInstance, "endTs", stateExecutionInstance.getEndTs());

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
    return cloned;
  }

  private void updateStatus(StateExecutionInstance stateExecutionInstance, String tsField, Long tsValue) {
    UpdateOperations<StateExecutionInstance> ops =
        wingsPersistence.createUpdateOperations(StateExecutionInstance.class);
    ops.set("status", stateExecutionInstance.getStatus());
    ops.set(tsField, tsValue);

    wingsPersistence.update(stateExecutionInstance, ops);
  }

  private void updateStateExecutionData(StateExecutionInstance stateExecutionInstance,
      StateExecutionData stateExecutionData, ExecutionStatus status, String errorMsg) {
    Map<String, StateExecutionData> stateExecutionMap = stateExecutionInstance.getStateExecutionMap();
    if (stateExecutionMap == null) {
      stateExecutionMap = new HashMap<>();
    }

    if (stateExecutionData == null) {
      stateExecutionData = stateExecutionMap.get(stateExecutionInstance.getStateName());
      if (stateExecutionData == null) {
        stateExecutionData = new StateExecutionData();
      }
    }
    stateExecutionData.setStartTs(stateExecutionInstance.getStartTs());
    if (stateExecutionInstance.getEndTs() != null) {
      stateExecutionData.setEndTs(stateExecutionInstance.getEndTs());
    }
    stateExecutionData.setStatus(stateExecutionInstance.getStatus());
    if (errorMsg != null) {
      stateExecutionData.setErrorMsg(errorMsg);
    }
    stateExecutionMap.put(stateExecutionInstance.getStateName(), stateExecutionData);

    UpdateOperations<StateExecutionInstance> ops =
        wingsPersistence.createUpdateOperations(StateExecutionInstance.class);
    ops.set("stateExecutionMap", stateExecutionMap);
    wingsPersistence.update(stateExecutionInstance, ops);
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
}
