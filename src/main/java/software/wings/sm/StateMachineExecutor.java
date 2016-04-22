package software.wings.sm;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import javax.inject.Inject;

import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Singleton;

import software.wings.beans.ErrorConstants;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.waitnotify.NotifyCallback;
import software.wings.waitnotify.WaitNotifyEngine;

/**
 * Class responsible for executing state machine.
 * @author Rishi
 */
@Singleton
public class StateMachineExecutor {
  private static Logger logger = LoggerFactory.getLogger(StateMachineExecutor.class);
  @Inject private ExecutorService executorService;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private WaitNotifyEngine waitNotifyEngine;

  public void execute(StateMachine sm) {
    execute(sm, new HashMap<String, Serializable>());
  }

  /**
   * Starts execution of a state machine with given arguments.
   * @param sm StateMachine to execute.
   * @param arguments context arguments.
   */
  public void execute(StateMachine sm, Map<String, Serializable> arguments) {
    if (sm == null) {
      logger.error("StateMachine passed for execution is null");
      throw new WingsException(ErrorConstants.INVALID_ARGUMENT);
    }

    ExecutionContext context = new ExecutionContext();
    if (arguments == null) {
      arguments = new HashMap<>();
    }
    context.setParams(arguments);
    context.setStateMachineId(sm.getUuid());

    execute(sm, sm.getInitialState().getName(), context);
  }

  public void execute(StateMachine sm, String stateName, ExecutionContext context) {
    execute(sm, stateName, context, null, null);
  }

  /**
   * Executes a given state for a state machine
   * @param sm StateMachine to execute.
   * @param stateName state name to execute.
   * @param context context for the execution.
   * @param parentInstanceId parent instance for this execution.
   * @param notifyId id to notify on.
   */
  public void execute(
      StateMachine sm, String stateName, ExecutionContext context, String parentInstanceId, String notifyId) {
    SmInstance smInstance = new SmInstance();
    smInstance.setContext(context);
    smInstance.setStateName(stateName);
    smInstance.setParentInstanceId(parentInstanceId);
    smInstance.setStateMachineId(context.getStateMachineId());
    smInstance.setNotifyId(notifyId);
    smInstance = wingsPersistence.saveAndGet(SmInstance.class, smInstance);

    executorService.submit(new SmExecutionDispatcher(sm, this, smInstance));
  }

  public void execute(String smId, Map<String, Serializable> arguments) {
    execute(wingsPersistence.get(StateMachine.class, smId), arguments);
  }

  public void execute(
      String smId, String stateName, ExecutionContext context, String parentInstanceId, String notifyId) {
    execute(wingsPersistence.get(StateMachine.class, smId), stateName, context, parentInstanceId, notifyId);
  }

  public void execute(SmInstance smInstance) {
    StateMachine sm = wingsPersistence.get(StateMachine.class, smInstance.getStateMachineId());
    execute(sm, smInstance);
  }

  public void execute(StateMachine sm, SmInstance smInstance) {
    NotifyCallback callback = new SmAsynchResumeCallback(smInstance.getUuid());
    execute(sm, smInstance, waitNotifyEngine, callback);
  }

  /**
   * Executes a state machine instance for a state machine.
   * @param sm StateMachine to execute.
   * @param smInstance stateMachine instance to execute.
   * @param waitNotifyEngine waitNotify instance module.
   * @param callback callback to execute on notify.
   */
  public void execute(
      StateMachine sm, SmInstance smInstance, WaitNotifyEngine waitNotifyEngine, NotifyCallback callback) {
    updateStatus(smInstance, ExecutionStatus.RUNNING, "startTs");

    State currentState = null;
    try {
      currentState = sm.getState(smInstance.getStateName());
      ExecutionResponse executionResponse = currentState.execute(smInstance.getContext());
      handleExecuteResponse(sm, smInstance, waitNotifyEngine, callback, currentState, executionResponse);
    } catch (Exception exeception) {
      handleExecuteResponseException(sm, smInstance, waitNotifyEngine, currentState, exeception);
    }
  }

  /**
   * Resumes execution of a StateMachineInstance.
   * @param smInstanceId stateMachineInstance to resume.
   * @param response map of responses from state machine instances this state was waiting on.
   */
  public void resume(String smInstanceId, Map<String, ? extends Serializable> response) {
    SmInstance smInstance = wingsPersistence.get(SmInstance.class, smInstanceId);
    StateMachine sm = wingsPersistence.get(StateMachine.class, smInstance.getStateMachineId());
    State currentState = sm.getState(smInstance.getStateName());
    try {
      ExecutionResponse executionResponse = currentState.handleAsynchResponse(smInstance.getContext(), response);
      NotifyCallback callback = new SmAsynchResumeCallback(smInstance.getUuid());
      handleExecuteResponse(sm, smInstance, waitNotifyEngine, callback, currentState, executionResponse);
    } catch (Exception execution) {
      handleExecuteResponseException(sm, smInstance, waitNotifyEngine, currentState, execution);
    }
  }

  private void handleExecuteResponseException(StateMachine sm, SmInstance smInstance, WaitNotifyEngine waitNotifyEngine,
      State currentState, Exception exception) {
    logger.info("Error seen in the state execution  - currentState : {}, smInstanceId: {}", currentState,
        smInstance.getUuid(), exception);
    try {
      updateContext(smInstance);
      failedTransition(waitNotifyEngine, sm, smInstance);
    } catch (Exception e2) {
      logger.error("Error in transitioning to failure state", e2);
    }
  }

  private void handleExecuteResponse(StateMachine sm, SmInstance smInstance, WaitNotifyEngine waitNotifyEngine,
      NotifyCallback callback, State currentState, ExecutionResponse executionResponse) {
    updateContext(smInstance);
    if (executionResponse.isAsynch()) {
      if (executionResponse.getCorrelationIds() == null || executionResponse.getCorrelationIds().size() == 0) {
        logger.error("executionResponse is null, but no correlationId - currentState : " + currentState.getName()
            + ", smInstanceId: " + smInstance.getUuid());
        updateStatus(smInstance, ExecutionStatus.ERROR, "endTs");
      } else {
        waitNotifyEngine.waitForAll(callback,
            executionResponse.getCorrelationIds().toArray(new String[executionResponse.getCorrelationIds().size()]));
      }
    } else {
      if (executionResponse.getExecutionStatus() == ExecutionStatus.SUCCESS) {
        successTransition(waitNotifyEngine, sm, smInstance);
      } else {
        failedTransition(waitNotifyEngine, sm, smInstance);
      }
    }
  }

  private void successTransition(WaitNotifyEngine waitNotifyEngine, StateMachine sm, SmInstance smInstance) {
    updateStatus(smInstance, ExecutionStatus.SUCCESS, "endTs");

    State nextState = sm.getSuccessTransition(smInstance.getStateName());
    if (nextState == null) {
      logger.info("nextSuccessState is null.. ending execution  - currentState : " + smInstance.getStateName()
          + ", smInstanceId: " + smInstance.getUuid());
      if (smInstance.getNotifyId() != null) {
        waitNotifyEngine.notify(smInstance.getNotifyId(), ExecutionStatus.SUCCESS);
      }
    } else {
      execute(
          sm, nextState.getName(), smInstance.getContext(), smInstance.getParentInstanceId(), smInstance.getNotifyId());
    }
  }

  private void failedTransition(WaitNotifyEngine waitNotifyEngine, StateMachine sm, SmInstance smInstance) {
    updateStatus(smInstance, ExecutionStatus.FAILED, "endTs");

    State nextState = sm.getFailureTransition(smInstance.getStateName());
    if (nextState == null) {
      logger.info("nextFailureState is null.. ending execution  - currentState : " + smInstance.getStateName()
          + ", smInstanceId: " + smInstance.getUuid());
      if (smInstance.getNotifyId() != null) {
        waitNotifyEngine.notify(smInstance.getNotifyId(), ExecutionStatus.FAILED);
      }
    } else {
      execute(
          sm, nextState.getName(), smInstance.getContext(), smInstance.getParentInstanceId(), smInstance.getNotifyId());
    }
  }

  private void updateStatus(SmInstance smInstance, ExecutionStatus status, String tsField) {
    UpdateOperations<SmInstance> ops = wingsPersistence.createUpdateOperations(SmInstance.class);
    ops.set("status", status);
    ops.set(tsField, System.currentTimeMillis());

    wingsPersistence.update(smInstance, ops);
  }

  private void updateContext(SmInstance smInstance) {
    ExecutionContext context = smInstance.getContext();
    if (!context.isDirty()) {
      return;
    }
    UpdateOperations<SmInstance> ops = wingsPersistence.createUpdateOperations(SmInstance.class);
    ops.set("context", smInstance.getContext());
    wingsPersistence.update(smInstance, ops);
    context.setDirty(false);
  }

  static class SmExecutionDispatcher implements Runnable {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private SmInstance smInstance;
    private StateMachine sm;
    private StateMachineExecutor stateMachineExecutor;

    /**
     * Creates a new SmExecutionDispatcher.
     * @param sm stateMachine for dispatcher.
     * @param smInstance stateMachineInstance to dispatch.
     */
    public SmExecutionDispatcher(StateMachine sm, StateMachineExecutor stateMachineExecutor, SmInstance smInstance) {
      this.sm = sm;
      this.stateMachineExecutor = stateMachineExecutor;
      this.smInstance = smInstance;
    }

    /* (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
      stateMachineExecutor.execute(sm, smInstance);
    }
  }
}
