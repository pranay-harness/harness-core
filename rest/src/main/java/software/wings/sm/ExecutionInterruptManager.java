/**
 *
 */

package software.wings.sm;

import software.wings.beans.ErrorCode;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.SortOrder.OrderType;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.waitnotify.WaitNotifyEngine;

import javax.inject.Inject;

/**
 * The interface State machine event manager.
 *
 * @author Rishi
 */
public class ExecutionInterruptManager {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private StateMachineExecutor stateMachineExecutor;
  @Inject private WaitNotifyEngine waitNotifyEngine;

  /**
   * Register execution event execution event.
   *
   * @param executionInterrupt the execution event
   * @return the execution event
   */
  public ExecutionInterrupt registerExecutionInterrupt(ExecutionInterrupt executionInterrupt) {
    boolean inlineHandle = false;
    if (executionInterrupt.getExecutionInterruptType() == ExecutionInterruptType.PAUSE
        || executionInterrupt.getExecutionInterruptType() == ExecutionInterruptType.RESUME
        || executionInterrupt.getExecutionInterruptType() == ExecutionInterruptType.RETRY
        || executionInterrupt.getExecutionInterruptType() == ExecutionInterruptType.ABORT) {
      inlineHandle = true;
      if (executionInterrupt.getStateExecutionInstanceId() == null) {
        throw new WingsException(ErrorCode.INVALID_ARGUMENT, "args", "null stateExecutionInstanceId");
      }

      StateExecutionInstance stateExecutionInstance = wingsPersistence.get(StateExecutionInstance.class,
          executionInterrupt.getAppId(), executionInterrupt.getStateExecutionInstanceId());
      if (stateExecutionInstance == null) {
        throw new WingsException(ErrorCode.INVALID_ARGUMENT, "args",
            "invalid stateExecutionInstanceId: " + executionInterrupt.getStateExecutionInstanceId());
      }

      if (executionInterrupt.getExecutionInterruptType() == ExecutionInterruptType.RESUME
          && stateExecutionInstance.getStatus() != ExecutionStatus.PAUSED
          && stateExecutionInstance.getStatus() != ExecutionStatus.PAUSED_ON_ERROR) {
        throw new WingsException(ErrorCode.STATE_NOT_FOR_RESUME, "stateName", stateExecutionInstance.getStateName());
      }

      if (executionInterrupt.getExecutionInterruptType() == ExecutionInterruptType.RETRY
          && stateExecutionInstance.getStatus() != ExecutionStatus.PAUSED_ON_ERROR
          && stateExecutionInstance.getStatus() != ExecutionStatus.ERROR) {
        throw new WingsException(ErrorCode.STATE_NOT_FOR_RETRY, "stateName", stateExecutionInstance.getStateName());
      }

      if (executionInterrupt.getExecutionInterruptType() == ExecutionInterruptType.ABORT
          && stateExecutionInstance.getStatus() != ExecutionStatus.NEW
          && stateExecutionInstance.getStatus() != ExecutionStatus.STARTING
          && stateExecutionInstance.getStatus() != ExecutionStatus.RUNNING
          && stateExecutionInstance.getStatus() != ExecutionStatus.PAUSED
          && stateExecutionInstance.getStatus() != ExecutionStatus.PAUSED_ON_ERROR) {
        throw new WingsException(ErrorCode.STATE_NOT_FOR_ABORT, "stateName", stateExecutionInstance.getStateName());
      }
      if (executionInterrupt.getExecutionInterruptType() == ExecutionInterruptType.PAUSE
          && stateExecutionInstance.getStatus() != ExecutionStatus.NEW
          && stateExecutionInstance.getStatus() != ExecutionStatus.STARTING
          && stateExecutionInstance.getStatus() != ExecutionStatus.RUNNING) {
        throw new WingsException(ErrorCode.STATE_NOT_FOR_PAUSE, "stateName", stateExecutionInstance.getStateName());
      }
    }

    PageResponse<ExecutionInterrupt> res = listExecutionInterrupts(executionInterrupt);

    if (isPresent(res, ExecutionInterruptType.ABORT_ALL)) {
      throw new WingsException(ErrorCode.ABORT_ALL_ALREADY);
    }

    if (executionInterrupt.getExecutionInterruptType() == ExecutionInterruptType.PAUSE_ALL) {
      if (isPresent(res, ExecutionInterruptType.PAUSE_ALL)) {
        throw new WingsException(ErrorCode.PAUSE_ALL_ALREADY);
      }
      ExecutionInterrupt resumeAll = getExecutionInterrupt(res, ExecutionInterruptType.RESUME_ALL);
      if (resumeAll != null) {
        makeInactive(resumeAll);
      }
    }

    if (executionInterrupt.getExecutionInterruptType() == ExecutionInterruptType.RESUME_ALL) {
      ExecutionInterrupt pauseAll = getExecutionInterrupt(res, ExecutionInterruptType.PAUSE_ALL);
      if (pauseAll == null || isPresent(res, ExecutionInterruptType.RESUME_ALL)) {
        throw new WingsException(ErrorCode.RESUME_ALL_ALREADY);
      }
      makeInactive(pauseAll);
      waitNotifyEngine.notify(pauseAll.getUuid(),
          ExecutionStatusData.Builder.anExecutionStatusData().withExecutionStatus(ExecutionStatus.SUCCESS).build());
    }

    if (executionInterrupt.getExecutionInterruptType() == ExecutionInterruptType.ABORT_ALL) {
      inlineHandle = true;
    }

    executionInterrupt = wingsPersistence.saveAndGet(ExecutionInterrupt.class, executionInterrupt);

    if (inlineHandle) {
      stateMachineExecutor.handleInterrupt(executionInterrupt);
    }

    return executionInterrupt;
  }

  private void makeInactive(ExecutionInterrupt executionInterrupt) {
    wingsPersistence.delete(executionInterrupt);
  }

  private boolean isPresent(PageResponse<ExecutionInterrupt> res, ExecutionInterruptType eventType) {
    return getExecutionInterrupt(res, eventType) != null;
  }

  private ExecutionInterrupt getExecutionInterrupt(
      PageResponse<ExecutionInterrupt> res, ExecutionInterruptType eventType) {
    if (res == null || res.size() == 0) {
      return null;
    }
    for (ExecutionInterrupt evt : res) {
      if (evt.getExecutionInterruptType() == eventType) {
        return evt;
      }
    }
    return null;
  }

  private PageResponse<ExecutionInterrupt> listExecutionInterrupts(ExecutionInterrupt executionInterrupt) {
    PageRequest<ExecutionInterrupt> req =
        PageRequest.Builder.aPageRequest()
            .addFilter("appId", Operator.EQ, executionInterrupt.getAppId())
            .addFilter("envId", Operator.EQ, executionInterrupt.getEnvId())
            .addFilter("executionUuid", Operator.EQ, executionInterrupt.getExecutionUuid())
            .addOrder("createdAt", OrderType.DESC)
            .build();
    return wingsPersistence.query(ExecutionInterrupt.class, req);
  }

  /**
   * Check for event workflow execution event.
   *
   * @param appId         the app id
   * @param executionUuid the execution uuid
   * @return the workflow execution event
   */
  public ExecutionInterrupt checkForExecutionInterrupt(String appId, String executionUuid) {
    PageRequest<ExecutionInterrupt> req = PageRequest.Builder.aPageRequest()
                                              .addFilter("appId", Operator.EQ, appId)
                                              .addFilter("executionUuid", Operator.EQ, executionUuid)
                                              .addFilter("executionInterruptType", Operator.IN,
                                                  ExecutionInterruptType.ABORT_ALL, ExecutionInterruptType.PAUSE_ALL)
                                              .addOrder("createdAt", OrderType.DESC)
                                              .build();
    return wingsPersistence.get(ExecutionInterrupt.class, req);
  }
}
