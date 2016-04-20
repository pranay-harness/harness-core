/**
 *
 */
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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import javax.inject.Inject;

/**
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

  public void execute(
      StateMachine sm, String stateName, ExecutionContext context, String parentInstanceId, String notifyId) {
    SMInstance smInstance = new SMInstance();
    smInstance.setContext(context);
    smInstance.setStateName(stateName);
    smInstance.setParentInstanceId(parentInstanceId);
    smInstance.setStateMachineId(context.getStateMachineId());
    smInstance.setNotifyId(notifyId);
    smInstance = wingsPersistence.saveAndGet(SMInstance.class, smInstance);

    executorService.submit(new SMExecutionDispatcher(sm, this, smInstance));
  }

  public void execute(String smId, Map<String, Serializable> arguments) {
    execute(wingsPersistence.get(StateMachine.class, smId), arguments);
  }

  public void execute(
      String smId, String stateName, ExecutionContext context, String parentInstanceId, String notifyId) {
    execute(wingsPersistence.get(StateMachine.class, smId), stateName, context, parentInstanceId, notifyId);
  }

  public void execute(SMInstance smInstance) {
    StateMachine sm = wingsPersistence.get(StateMachine.class, smInstance.getStateMachineId());
    execute(sm, smInstance);
  }

  public void execute(StateMachine sm, SMInstance smInstance) {
    NotifyCallback callback = new SMAsynchResumeCallback(smInstance.getUuid());
    execute(sm, smInstance, waitNotifyEngine, callback);
  }

  public void execute(
      StateMachine sm, SMInstance smInstance, WaitNotifyEngine waitNotifyEngine, NotifyCallback callback) {
    updateStatus(smInstance, ExecutionStatus.RUNNING, "startTs");

    State currentState = null;
    try {
      currentState = sm.getState(smInstance.getStateName());
      ExecutionResponse executionResponse = currentState.execute(smInstance.getContext());
      handleExecuteResponse(sm, smInstance, waitNotifyEngine, callback, currentState, executionResponse);
    } catch (Exception e) {
      handleExecuteResponseException(sm, smInstance, waitNotifyEngine, currentState, e);
    }
  }

  /**
   * @param sm
   * @param smInstance
   * @param waitNotifyEngine
   * @param currentState
   * @param e
   */
  private void handleExecuteResponseException(
      StateMachine sm, SMInstance smInstance, WaitNotifyEngine waitNotifyEngine, State currentState, Exception e) {
    logger.info("Error seen in the state execution  - currentState : " + currentState.getName()
            + ", smInstanceId: " + smInstance.getUuid(),
        e);
    try {
      updateContext(smInstance);
      failedTransition(waitNotifyEngine, sm, smInstance);
    } catch (Exception e2) {
      logger.error("Error in transitioning to failure state", e2);
    }
  }

  /**
   * @param sm
   * @param smInstance
   * @param waitNotifyEngine
   * @param callback
   * @param currentState
   * @param executionResponse
   */
  private void handleExecuteResponse(StateMachine sm, SMInstance smInstance, WaitNotifyEngine waitNotifyEngine,
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

  public void resume(String smInstanceId, Map<String, ? extends Serializable> response) {
    SMInstance smInstance = wingsPersistence.get(SMInstance.class, smInstanceId);
    StateMachine sm = wingsPersistence.get(StateMachine.class, smInstance.getStateMachineId());
    State currentState = sm.getState(smInstance.getStateName());
    try {
      ExecutionResponse executionResponse = currentState.handleAsynchResponse(smInstance.getContext(), response);
      NotifyCallback callback = new SMAsynchResumeCallback(smInstance.getUuid());
      handleExecuteResponse(sm, smInstance, waitNotifyEngine, callback, currentState, executionResponse);
    } catch (Exception e) {
      handleExecuteResponseException(sm, smInstance, waitNotifyEngine, currentState, e);
    }
  }

  /**
   * @param waitNotifyEngine
   * @param sm
   * @param smInstance
   */
  private void successTransition(WaitNotifyEngine waitNotifyEngine, StateMachine sm, SMInstance smInstance) {
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

  /**
   * @param waitNotifyEngine
   * @param sm
   * @param smInstance
   */
  private void failedTransition(WaitNotifyEngine waitNotifyEngine, StateMachine sm, SMInstance smInstance) {
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

  private void updateStatus(SMInstance smInstance, ExecutionStatus status, String tsField) {
    UpdateOperations<SMInstance> ops = wingsPersistence.createUpdateOperations(SMInstance.class);
    ops.set("status", status);
    ops.set(tsField, System.currentTimeMillis());

    wingsPersistence.update(smInstance, ops);
  }

  private void updateContext(SMInstance smInstance) {
    ExecutionContext context = smInstance.getContext();
    if (!context.isDirty()) {
      return;
    }
    UpdateOperations<SMInstance> ops = wingsPersistence.createUpdateOperations(SMInstance.class);
    ops.set("context", smInstance.getContext());
    wingsPersistence.update(smInstance, ops);
    context.setDirty(false);
  }

  static class SMExecutionDispatcher implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(SMExecutionDispatcher.class);
    private SMInstance smInstance;
    private StateMachine sm;
    private StateMachineExecutor stateMachineExecutor;

    /**
     * @param sm
     * @param smInstance
     */
    public SMExecutionDispatcher(StateMachine sm, StateMachineExecutor stateMachineExecutor, SMInstance smInstance) {
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
