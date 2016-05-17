package software.wings.sm;

import com.google.inject.Singleton;

import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.ErrorConstants;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.waitnotify.NotifyCallback;
import software.wings.waitnotify.WaitNotifyEngine;

import java.io.Serializable;
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

  public StateExecutionInstance execute(String smId, ExecutionStandardParams stdParams) {
    return execute(wingsPersistence.get(StateMachine.class, smId), stdParams);
  }

  public StateExecutionInstance execute(StateMachine sm, ExecutionStandardParams stdParams) {
    return execute(sm, stdParams, null);
  }

  public StateExecutionInstance execute(
      StateMachine sm, ExecutionStandardParams stdParams, List<Repeatable> contextParams) {
    if (sm == null) {
      logger.error("StateMachine passed for execution is null");
      throw new WingsException(ErrorConstants.INVALID_ARGUMENT);
    }

    if (stdParams == null) {
      logger.error("stdParams passed for execution is null");
      throw new WingsException(ErrorConstants.INVALID_ARGUMENT);
    }

    ExecutionContextImpl context = new ExecutionContextImpl();
    if (stdParams.getStartTs() == null) {
      stdParams.setStartTs(System.currentTimeMillis());
    }
    context.setStandardParams(stdParams);

    if (contextParams != null) {
      context.getContextElements().addAll(contextParams);
    }
    context.setStateMachineId(sm.getUuid());

    return execute(sm, sm.getInitialState().getName(), context);
  }

  public StateExecutionInstance execute(StateMachine sm, String stateName, ExecutionContextImpl context) {
    return execute(sm, stateName, context, null, null);
  }

  /**
   * Executes a given state for a state machine
   *
   * @param sm               StateMachine to execute.
   * @param stateName        state name to execute.
   * @param context          context for the execution.
   * @param parentInstanceId parent instance for this execution.
   * @param notifyId         id to notify on.
   */
  public StateExecutionInstance execute(
      StateMachine sm, String stateName, ExecutionContextImpl context, String parentInstanceId, String notifyId) {
    return execute(sm, stateName, context, parentInstanceId, notifyId, null);
  }

  public StateExecutionInstance execute(StateMachine sm, String stateName, ExecutionContextImpl context,
      String parentInstanceId, String notifyId, String prevInstanceId) {
    StateExecutionInstance stateExecutionInstance = new StateExecutionInstance();
    stateExecutionInstance.setAppId(context.getStandardParams().getAppId());
    stateExecutionInstance.setContext(context);
    stateExecutionInstance.setStateName(stateName);
    stateExecutionInstance.setParentInstanceId(parentInstanceId);
    stateExecutionInstance.setStateMachineId(context.getStateMachineId());
    stateExecutionInstance.setNotifyId(notifyId);
    stateExecutionInstance.setPrevInstanceId(prevInstanceId);
    stateExecutionInstance.setWorkflowExecutionId(context.getStandardParams().getWorkflowExecutionId());
    stateExecutionInstance = wingsPersistence.saveAndGet(StateExecutionInstance.class, stateExecutionInstance);

    executorService.submit(new SmExecutionDispatcher(sm, this, stateExecutionInstance));
    return stateExecutionInstance;
  }

  public StateExecutionInstance execute(
      String smId, String stateName, ExecutionContextImpl context, String parentInstanceId, String notifyId) {
    return execute(wingsPersistence.get(StateMachine.class, smId), stateName, context, parentInstanceId, notifyId);
  }

  public StateExecutionInstance execute(String smId, String stateName, ExecutionContextImpl context,
      String parentInstanceId, String notifyId, String prevInstanceId) {
    return execute(
        wingsPersistence.get(StateMachine.class, smId), stateName, context, parentInstanceId, notifyId, prevInstanceId);
  }

  public StateExecutionInstance execute(StateExecutionInstance stateExecutionInstance) {
    StateMachine sm = wingsPersistence.get(StateMachine.class, stateExecutionInstance.getStateMachineId());
    return execute(sm, stateExecutionInstance);
  }

  public StateExecutionInstance execute(StateMachine sm, StateExecutionInstance stateExecutionInstance) {
    NotifyCallback callback = new StateMachineResumeCallback(stateExecutionInstance.getUuid());
    return execute(sm, stateExecutionInstance, waitNotifyEngine, callback);
  }

  /**
   * Executes a state machine instance for a state machine.
   *
   * @param sm               StateMachine to execute.
   * @param stateExecutionInstance       stateMachine instance to execute.
   * @param waitNotifyEngine waitNotify instance module.
   * @param callback         callback to execute on notify.
   */
  public StateExecutionInstance execute(StateMachine sm, StateExecutionInstance stateExecutionInstance,
      WaitNotifyEngine waitNotifyEngine, NotifyCallback callback) {
    updateStatus(stateExecutionInstance, ExecutionStatus.RUNNING, "startTs");

    State currentState = null;
    try {
      currentState = sm.getState(stateExecutionInstance.getStateName());
      ExecutionResponse executionResponse = currentState.execute(stateExecutionInstance.getContext());
      return handleExecuteResponse(
          sm, stateExecutionInstance, waitNotifyEngine, callback, currentState, executionResponse);
    } catch (Exception exeception) {
      return handleExecuteResponseException(sm, stateExecutionInstance, waitNotifyEngine, currentState, exeception);
    }
  }

  /**
   * Resumes execution of a StateMachineInstance.
   *
   * @param stateExecutionInstanceId stateMachineInstance to resume.
   * @param response     map of responses from state machine instances this state was waiting on.
   */
  public void resume(String stateExecutionInstanceId, Map<String, ? extends Serializable> response) {
    StateExecutionInstance stateExecutionInstance =
        wingsPersistence.get(StateExecutionInstance.class, stateExecutionInstanceId);
    StateMachine sm = wingsPersistence.get(StateMachine.class, stateExecutionInstance.getStateMachineId());
    State currentState = sm.getState(stateExecutionInstance.getStateName());
    try {
      ExecutionResponse executionResponse =
          currentState.handleAsynchResponse(stateExecutionInstance.getContext(), response);
      NotifyCallback callback = new StateMachineResumeCallback(stateExecutionInstance.getUuid());
      handleExecuteResponse(sm, stateExecutionInstance, waitNotifyEngine, callback, currentState, executionResponse);
    } catch (Exception execution) {
      handleExecuteResponseException(sm, stateExecutionInstance, waitNotifyEngine, currentState, execution);
    }
  }

  private StateExecutionInstance handleExecuteResponseException(StateMachine sm,
      StateExecutionInstance stateExecutionInstance, WaitNotifyEngine waitNotifyEngine, State currentState,
      Exception exception) {
    logger.info("Error seen in the state execution  - currentState : {}, stateExecutionInstanceId: {}", currentState,
        stateExecutionInstance.getUuid(), exception);
    try {
      updateContext(stateExecutionInstance, null);
      return failedTransition(waitNotifyEngine, sm, stateExecutionInstance, exception);
    } catch (Exception e2) {
      logger.error("Error in transitioning to failure state", e2);
    }
    return null;
  }

  private StateExecutionInstance handleExecuteResponse(StateMachine sm, StateExecutionInstance stateExecutionInstance,
      WaitNotifyEngine waitNotifyEngine, NotifyCallback callback, State currentState,
      ExecutionResponse executionResponse) {
    updateContext(stateExecutionInstance, executionResponse);
    if (executionResponse.isAsynch()) {
      if (executionResponse.getCorrelationIds() == null || executionResponse.getCorrelationIds().size() == 0) {
        logger.error("executionResponse is null, but no correlationId - currentState : " + currentState.getName()
            + ", stateExecutionInstanceId: " + stateExecutionInstance.getUuid());
        updateStatus(stateExecutionInstance, ExecutionStatus.ERROR, "endTs");
      } else {
        waitNotifyEngine.waitForAll(callback,
            executionResponse.getCorrelationIds().toArray(new String[executionResponse.getCorrelationIds().size()]));
      }
    } else {
      if (executionResponse.getExecutionStatus() == ExecutionStatus.SUCCESS) {
        return successTransition(waitNotifyEngine, sm, stateExecutionInstance);
      } else {
        return failedTransition(waitNotifyEngine, sm, stateExecutionInstance, null);
      }
    }
    return null;
  }

  private StateExecutionInstance successTransition(
      WaitNotifyEngine waitNotifyEngine, StateMachine sm, StateExecutionInstance stateExecutionInstance) {
    updateStatus(stateExecutionInstance, ExecutionStatus.SUCCESS, "endTs");

    State nextState = sm.getSuccessTransition(stateExecutionInstance.getStateName());
    if (nextState == null) {
      logger.info("nextSuccessState is null.. ending execution  - currentState : "
          + stateExecutionInstance.getStateName() + ", stateExecutionInstanceId: " + stateExecutionInstance.getUuid());
      if (stateExecutionInstance.getNotifyId() == null) {
        logger.info("State Machine execution ended for the {}", sm.getName());
        ExecutionStandardParams stdParams = stateExecutionInstance.getContext().getStandardParams();
        if (stdParams != null && stdParams.getCallback() != null) {
          stdParams.getCallback().callback(stateExecutionInstance.getContext(), ExecutionStatus.SUCCESS, null);
        }
      } else {
        waitNotifyEngine.notify(stateExecutionInstance.getNotifyId(), ExecutionStatus.SUCCESS);
      }
    } else {
      return execute(sm, nextState.getName(), stateExecutionInstance.getContext(),
          stateExecutionInstance.getParentInstanceId(), stateExecutionInstance.getNotifyId(),
          stateExecutionInstance.getUuid());
    }

    return null;
  }

  private StateExecutionInstance failedTransition(WaitNotifyEngine waitNotifyEngine, StateMachine sm,
      StateExecutionInstance stateExecutionInstance, Exception exception) {
    updateStatus(stateExecutionInstance, ExecutionStatus.FAILED, "endTs");

    State nextState = sm.getFailureTransition(stateExecutionInstance.getStateName());
    if (nextState == null) {
      logger.info("nextFailureState is null.. ending execution  - currentState : "
          + stateExecutionInstance.getStateName() + ", stateExecutionInstanceId: " + stateExecutionInstance.getUuid());
      if (stateExecutionInstance.getNotifyId() == null) {
        logger.info("State Machine execution failed for the {}", sm.getName());
        ExecutionStandardParams stdParams = stateExecutionInstance.getContext().getStandardParams();
        if (stdParams != null && stdParams.getCallback() != null) {
          stdParams.getCallback().callback(stateExecutionInstance.getContext(), ExecutionStatus.FAILED, exception);
        }
      } else {
        waitNotifyEngine.notify(stateExecutionInstance.getNotifyId(), ExecutionStatus.FAILED);
      }
    } else {
      return execute(sm, nextState.getName(), stateExecutionInstance.getContext(),
          stateExecutionInstance.getParentInstanceId(), stateExecutionInstance.getNotifyId(),
          stateExecutionInstance.getUuid());
    }
    return null;
  }

  private void updateStatus(StateExecutionInstance stateExecutionInstance, ExecutionStatus status, String tsField) {
    UpdateOperations<StateExecutionInstance> ops =
        wingsPersistence.createUpdateOperations(StateExecutionInstance.class);
    ops.set("status", status);
    ops.set(tsField, System.currentTimeMillis());

    wingsPersistence.update(stateExecutionInstance, ops);
  }

  private void updateContext(StateExecutionInstance stateExecutionInstance, ExecutionResponse executionResponse) {
    ExecutionContextImpl context = stateExecutionInstance.getContext();
    if (!context.isDirty() && executionResponse.getStateExecutionData() == null) {
      return;
    }

    if (executionResponse != null && executionResponse.getStateExecutionData() != null) {
      context.getStateExecutionMap().put(
          stateExecutionInstance.getStateName(), executionResponse.getStateExecutionData());
    }
    UpdateOperations<StateExecutionInstance> ops =
        wingsPersistence.createUpdateOperations(StateExecutionInstance.class);
    ops.set("context", stateExecutionInstance.getContext());
    wingsPersistence.update(stateExecutionInstance, ops);
    context.setDirty(false);
  }

  static class SmExecutionDispatcher implements Runnable {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private StateExecutionInstance stateExecutionInstance;
    private StateMachine sm;
    private StateMachineExecutor stateMachineExecutor;

    /**
     * Creates a new SmExecutionDispatcher.
     *
     * @param sm         stateMachine for dispatcher.
     * @param stateExecutionInstance stateMachineInstance to dispatch.
     */
    public SmExecutionDispatcher(
        StateMachine sm, StateMachineExecutor stateMachineExecutor, StateExecutionInstance stateExecutionInstance) {
      this.sm = sm;
      this.stateMachineExecutor = stateMachineExecutor;
      this.stateExecutionInstance = stateExecutionInstance;
    }

    /* (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
      stateMachineExecutor.execute(sm, stateExecutionInstance);
    }
  }
}
