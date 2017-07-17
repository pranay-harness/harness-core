/**
 *
 */

package software.wings.sm;

import static software.wings.dl.PageRequest.Builder.aPageRequest;

import com.google.inject.Injector;

import org.slf4j.LoggerFactory;
import software.wings.beans.ErrorCode;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.SortOrder.OrderType;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.impl.WorkflowNotificationHelper;
import software.wings.waitnotify.WaitNotifyEngine;

import javax.inject.Inject;

/**
 * The interface State machine event manager.
 *
 * @author Rishi
 */
public class ExecutionInterruptManager {
  private final org.slf4j.Logger logger = LoggerFactory.getLogger(getClass());

  @Inject private WingsPersistence wingsPersistence;
  @Inject private StateMachineExecutor stateMachineExecutor;
  @Inject private WaitNotifyEngine waitNotifyEngine;

  @Inject private Injector injector;
  @Inject private WorkflowNotificationHelper workflowNotificationHelper;

  /**
   * Register execution event execution event.
   *
   * @param executionInterrupt the execution event
   * @return the execution event
   */
  public ExecutionInterrupt registerExecutionInterrupt(ExecutionInterrupt executionInterrupt) {
    if (executionInterrupt.getExecutionInterruptType() == ExecutionInterruptType.PAUSE
        || executionInterrupt.getExecutionInterruptType() == ExecutionInterruptType.RESUME
        || executionInterrupt.getExecutionInterruptType() == ExecutionInterruptType.RETRY
        || executionInterrupt.getExecutionInterruptType() == ExecutionInterruptType.ABORT) {
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
          && stateExecutionInstance.getStatus() != ExecutionStatus.WAITING) {
        throw new WingsException(ErrorCode.STATE_NOT_FOR_RESUME, "stateName", stateExecutionInstance.getStateName());
      }

      if (executionInterrupt.getExecutionInterruptType() == ExecutionInterruptType.RETRY
          && stateExecutionInstance.getStatus() != ExecutionStatus.WAITING
          && stateExecutionInstance.getStatus() != ExecutionStatus.ERROR) {
        throw new WingsException(ErrorCode.STATE_NOT_FOR_RETRY, "stateName", stateExecutionInstance.getStateName());
      }

      if (executionInterrupt.getExecutionInterruptType() == ExecutionInterruptType.ABORT
          && stateExecutionInstance.getStatus() != ExecutionStatus.NEW
          && stateExecutionInstance.getStatus() != ExecutionStatus.STARTING
          && stateExecutionInstance.getStatus() != ExecutionStatus.RUNNING
          && stateExecutionInstance.getStatus() != ExecutionStatus.PAUSED
          && stateExecutionInstance.getStatus() != ExecutionStatus.WAITING) {
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

    executionInterrupt = wingsPersistence.saveAndGet(ExecutionInterrupt.class, executionInterrupt);
    stateMachineExecutor.handleInterrupt(executionInterrupt);

    sendNotificationIfRequired(executionInterrupt);

    return executionInterrupt;
  }

  private void sendNotificationIfRequired(ExecutionInterrupt executionInterrupt) {
    switch (executionInterrupt.getExecutionInterruptType()) {
      case PAUSE_ALL: {
        sendNotification(executionInterrupt, ExecutionStatus.PAUSED);
        break;
      }
      case RESUME_ALL: {
        sendNotification(executionInterrupt, ExecutionStatus.RESUMED);
        break;
      }
      case ABORT_ALL: {
        sendNotification(executionInterrupt, ExecutionStatus.ABORTED);
        break;
      }
    }
  }

  private void sendNotification(ExecutionInterrupt executionInterrupt, ExecutionStatus status) {
    PageRequest<StateExecutionInstance> pageRequest =
        aPageRequest()
            .withLimit("1")
            .addFilter("appId", Operator.EQ, executionInterrupt.getAppId())
            .addFilter("executionUuid", Operator.EQ, executionInterrupt.getExecutionUuid())
            .addOrder("createdAt", OrderType.DESC)
            .build();

    PageResponse<StateExecutionInstance> pageResponse =
        wingsPersistence.query(StateExecutionInstance.class, pageRequest);
    if (pageResponse == null || pageResponse.isEmpty()) {
      logger.error("No StateExecutionInstance found for sendNotification");
      return;
    }
    StateMachine sm = wingsPersistence.get(
        StateMachine.class, executionInterrupt.getAppId(), pageResponse.get(0).getStateMachineId());
    ExecutionContextImpl context = new ExecutionContextImpl(pageResponse.get(0), sm, injector);
    injector.injectMembers(context);

    workflowNotificationHelper.sendWorkflowStatusChangeNotification(context, status);
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
