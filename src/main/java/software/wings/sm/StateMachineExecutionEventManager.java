/**
 *
 */

package software.wings.sm;

import software.wings.beans.ErrorCodes;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.SortOrder.OrderType;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.sm.ExecutionStatus.ExecutionStatusData;
import software.wings.waitnotify.WaitNotifyEngine;

import javax.inject.Inject;

/**
 * The interface State machine event manager.
 *
 * @author Rishi
 */
public class StateMachineExecutionEventManager {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private StateMachineExecutor stateMachineExecutor;
  @Inject private WaitNotifyEngine waitNotifyEngine;

  public ExecutionEvent registerExecutionEvent(ExecutionEvent executionEvent) {
    boolean inlineHandle = false;
    if (executionEvent.getExecutionEventType() == ExecutionEventType.PAUSE
        || executionEvent.getExecutionEventType() == ExecutionEventType.RESUME
        || executionEvent.getExecutionEventType() == ExecutionEventType.CONTINUE
        || executionEvent.getExecutionEventType() == ExecutionEventType.RETRY
        || executionEvent.getExecutionEventType() == ExecutionEventType.ABORT) {
      inlineHandle = true;
      if (executionEvent.getStateExecutionInstanceId() == null) {
        throw new WingsException(ErrorCodes.INVALID_ARGUMENT, "args",
            "no stateExecutionInstance for id: " + executionEvent.getStateExecutionInstanceId());
      }

      StateExecutionInstance stateExecutionInstance = wingsPersistence.get(
          StateExecutionInstance.class, executionEvent.getAppId(), executionEvent.getStateExecutionInstanceId());
      if (stateExecutionInstance == null) {
        throw new WingsException(ErrorCodes.INVALID_ARGUMENT, "args",
            "no stateExecutionInstance for id: " + executionEvent.getStateExecutionInstanceId());
      }

      if (executionEvent.getExecutionEventType() == ExecutionEventType.CONTINUE
          && stateExecutionInstance.getStatus() != ExecutionStatus.PAUSED) {
        throw new WingsException(ErrorCodes.STATE_NOT_FOR_RESUME, "stateName", stateExecutionInstance.getStateName());
      }

      if (executionEvent.getExecutionEventType() == ExecutionEventType.RESUME
          && stateExecutionInstance.getStatus() != ExecutionStatus.PAUSED) {
        throw new WingsException(ErrorCodes.STATE_NOT_FOR_RESUME, "stateName", stateExecutionInstance.getStateName());
      }

      if (executionEvent.getExecutionEventType() == ExecutionEventType.RETRY
          && stateExecutionInstance.getStatus() != ExecutionStatus.FAILED
          && stateExecutionInstance.getStatus() != ExecutionStatus.ERROR) {
        throw new WingsException(ErrorCodes.STATE_NOT_FOR_RETRY, "stateName", stateExecutionInstance.getStateName());
      }

      if (executionEvent.getExecutionEventType() == ExecutionEventType.ABORT
          && stateExecutionInstance.getStatus() != ExecutionStatus.NEW
          && stateExecutionInstance.getStatus() != ExecutionStatus.STARTING
          && stateExecutionInstance.getStatus() != ExecutionStatus.RUNNING
          && stateExecutionInstance.getStatus() != ExecutionStatus.PAUSED) {
        throw new WingsException(ErrorCodes.STATE_NOT_FOR_ABORT, "stateName", stateExecutionInstance.getStateName());
      }
      if (executionEvent.getExecutionEventType() == ExecutionEventType.PAUSE
          && stateExecutionInstance.getStatus() != ExecutionStatus.NEW
          && stateExecutionInstance.getStatus() != ExecutionStatus.STARTING
          && stateExecutionInstance.getStatus() != ExecutionStatus.RUNNING) {
        throw new WingsException(ErrorCodes.STATE_NOT_FOR_PAUSE, "stateName", stateExecutionInstance.getStateName());
      }
    }

    PageResponse<ExecutionEvent> res = listExecutionEvents(executionEvent);

    if ((executionEvent.getExecutionEventType() == ExecutionEventType.PAUSE_ALL
            || executionEvent.getExecutionEventType() == ExecutionEventType.RESUME_ALL
            || executionEvent.getExecutionEventType() == ExecutionEventType.ABORT_ALL)
        && isPresent(res, ExecutionEventType.ABORT_ALL)) {
      throw new WingsException(ErrorCodes.ABORT_ALL_ALREADY);
    }

    if (executionEvent.getExecutionEventType() == ExecutionEventType.PAUSE_ALL) {
      if (isPresent(res, ExecutionEventType.PAUSE_ALL)) {
        throw new WingsException(ErrorCodes.PAUSE_ALL_ALREADY);
      }
      ExecutionEvent resumeAll = getExecutionEvent(res, ExecutionEventType.RESUME_ALL);
      if (resumeAll != null) {
        makeInactive(resumeAll);
      }
    }

    if (executionEvent.getExecutionEventType() == ExecutionEventType.RESUME_ALL) {
      ExecutionEvent pauseAll = getExecutionEvent(res, ExecutionEventType.PAUSE_ALL);
      if (pauseAll == null || isPresent(res, ExecutionEventType.RESUME_ALL)) {
        throw new WingsException(ErrorCodes.RESUME_ALL_ALREADY);
      }
      makeInactive(pauseAll);
      waitNotifyEngine.notify(pauseAll.getUuid(),
          ExecutionStatusData.Builder.anExecutionStatusData().withExecutionStatus(ExecutionStatus.SUCCESS).build());
    }

    if (executionEvent.getExecutionEventType() == ExecutionEventType.ABORT_ALL) {
      inlineHandle = true;
    }

    executionEvent = wingsPersistence.saveAndGet(ExecutionEvent.class, executionEvent);

    if (inlineHandle) {
      stateMachineExecutor.handleEvent(executionEvent);
    }

    return executionEvent;
  }

  private void makeInactive(ExecutionEvent executionEvent) {
    wingsPersistence.delete(executionEvent);
  }

  private boolean isPresent(PageResponse<ExecutionEvent> res, ExecutionEventType eventType) {
    return getExecutionEvent(res, eventType) != null;
  }

  private ExecutionEvent getExecutionEvent(PageResponse<ExecutionEvent> res, ExecutionEventType eventType) {
    if (res == null || res.size() == 0) {
      return null;
    }
    for (ExecutionEvent evt : res) {
      if (evt.getExecutionEventType() == eventType) {
        return evt;
      }
    }
    return null;
  }

  private PageResponse<ExecutionEvent> listExecutionEvents(ExecutionEvent executionEvent) {
    PageRequest<ExecutionEvent> req = PageRequest.Builder.aPageRequest()
                                          .addFilter("appId", Operator.EQ, executionEvent.getAppId())
                                          .addFilter("envId", Operator.EQ, executionEvent.getEnvId())
                                          .addFilter("executionUuid", Operator.EQ, executionEvent.getExecutionUuid())
                                          .addOrder("createdAt", OrderType.DESC)
                                          .build();
    return wingsPersistence.query(ExecutionEvent.class, req);
  }
  /**
   * Check for event workflow execution event.
   *
   * @return the workflow execution event
   */
  public ExecutionEvent checkForExecutionEvent(String appId, String executionUuid) {
    PageRequest<ExecutionEvent> req =
        PageRequest.Builder.aPageRequest()
            .addFilter("appId", Operator.EQ, appId)
            .addFilter("executionUuid", Operator.EQ, executionUuid)
            .addFilter("executionEventType", Operator.IN, ExecutionEventType.ABORT_ALL, ExecutionEventType.PAUSE_ALL)
            .addOrder("createdAt", OrderType.DESC)
            .build();
    return wingsPersistence.get(ExecutionEvent.class, req);
  }
}
