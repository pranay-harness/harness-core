/**
 *
 */

package software.wings.sm;

import static software.wings.beans.ErrorCode.INVALID_ARGUMENT;
import static software.wings.beans.ErrorCode.PAUSE_ALL_ALREADY;
import static software.wings.beans.ErrorCode.RESUME_ALL_ALREADY;
import static software.wings.beans.ErrorCode.ROLLBACK_ALREADY;
import static software.wings.beans.ErrorCode.STATE_NOT_FOR_ABORT;
import static software.wings.beans.ErrorCode.STATE_NOT_FOR_PAUSE;
import static software.wings.beans.ErrorCode.STATE_NOT_FOR_RESUME;
import static software.wings.beans.ErrorCode.STATE_NOT_FOR_RETRY;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.SearchFilter.Operator.GT;
import static software.wings.beans.alert.AlertType.ManualInterventionNeeded;
import static software.wings.dl.PageRequest.Builder.aPageRequest;
import static software.wings.sm.ExecutionInterruptType.ABORT;
import static software.wings.sm.ExecutionInterruptType.ABORT_ALL;
import static software.wings.sm.ExecutionInterruptType.IGNORE;
import static software.wings.sm.ExecutionInterruptType.PAUSE;
import static software.wings.sm.ExecutionInterruptType.PAUSE_ALL;
import static software.wings.sm.ExecutionInterruptType.RESUME;
import static software.wings.sm.ExecutionInterruptType.ROLLBACK;
import static software.wings.sm.ExecutionStatus.PAUSED;
import static software.wings.sm.ExecutionStatus.RUNNING;
import static software.wings.sm.ExecutionStatus.STARTING;
import static software.wings.sm.ExecutionStatus.WAITING;
import static software.wings.sm.ExecutionStatusData.Builder.anExecutionStatusData;
import static software.wings.utils.Switch.noop;
import static software.wings.utils.Switch.unhandled;

import com.google.inject.Injector;

import org.slf4j.LoggerFactory;
import software.wings.beans.ReadPref;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.SortOrder.OrderType;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.alert.ManualInterventionNeededAlert;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.impl.WorkflowNotificationHelper;
import software.wings.service.intfc.AlertService;
import software.wings.waitnotify.WaitNotifyEngine;

import java.util.List;
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
  @Inject private AlertService alertService;

  /**
   * Register execution event execution event.
   *
   * @param executionInterrupt the execution event
   * @return the execution event
   */
  public ExecutionInterrupt registerExecutionInterrupt(ExecutionInterrupt executionInterrupt) {
    StateExecutionInstance stateExecutionInstance = null;
    ExecutionInterruptType executionInterruptType = executionInterrupt.getExecutionInterruptType();
    if (executionInterruptType == PAUSE || executionInterruptType == IGNORE
        || executionInterruptType == ExecutionInterruptType.RETRY || executionInterruptType == ABORT
        || executionInterruptType == RESUME) {
      if (executionInterrupt.getStateExecutionInstanceId() == null) {
        throw new WingsException(INVALID_ARGUMENT, "args", "null stateExecutionInstanceId");
      }

      stateExecutionInstance = wingsPersistence.get(StateExecutionInstance.class, executionInterrupt.getAppId(),
          executionInterrupt.getStateExecutionInstanceId());
      if (stateExecutionInstance == null) {
        throw new WingsException(INVALID_ARGUMENT, "args",
            "invalid stateExecutionInstanceId: " + executionInterrupt.getStateExecutionInstanceId());
      }

      if (executionInterruptType == RESUME && stateExecutionInstance.getStatus() != PAUSED) {
        throw new WingsException(STATE_NOT_FOR_RESUME, "stateName", stateExecutionInstance.getStateName());
      }

      if (executionInterruptType == IGNORE && stateExecutionInstance.getStatus() != PAUSED
          && stateExecutionInstance.getStatus() != WAITING) {
        throw new WingsException(STATE_NOT_FOR_RESUME, "stateName", stateExecutionInstance.getStateName());
      }

      if (executionInterruptType == ExecutionInterruptType.RETRY && stateExecutionInstance.getStatus() != WAITING
          && stateExecutionInstance.getStatus() != ExecutionStatus.ERROR) {
        throw new WingsException(STATE_NOT_FOR_RETRY, "stateName", stateExecutionInstance.getStateName());
      }

      if (executionInterruptType == ABORT && stateExecutionInstance.getStatus() != ExecutionStatus.NEW
          && stateExecutionInstance.getStatus() != STARTING && stateExecutionInstance.getStatus() != RUNNING
          && stateExecutionInstance.getStatus() != PAUSED && stateExecutionInstance.getStatus() != WAITING) {
        throw new WingsException(STATE_NOT_FOR_ABORT, "stateName", stateExecutionInstance.getStateName());
      }
      if (executionInterruptType == PAUSE && stateExecutionInstance.getStatus() != ExecutionStatus.NEW
          && stateExecutionInstance.getStatus() != STARTING && stateExecutionInstance.getStatus() != RUNNING) {
        throw new WingsException(STATE_NOT_FOR_PAUSE, "stateName", stateExecutionInstance.getStateName());
      }
    }

    PageResponse<ExecutionInterrupt> res = listExecutionInterrupts(executionInterrupt);

    //    if (isPresent(res, ABORT_ALL)) {
    //      throw new WingsException(ABORT_ALL_ALREADY);
    //    }

    if (executionInterruptType == ROLLBACK) {
      if (isPresent(res, ROLLBACK)) {
        throw new WingsException(ROLLBACK_ALREADY);
      }
    }

    if (executionInterruptType == PAUSE_ALL) {
      if (isPresent(res, PAUSE_ALL)) {
        throw new WingsException(PAUSE_ALL_ALREADY);
      }
      ExecutionInterrupt resumeAll = getExecutionInterrupt(res, ExecutionInterruptType.RESUME_ALL);
      if (resumeAll != null) {
        makeInactive(resumeAll);
      }
    }

    if (executionInterruptType == ExecutionInterruptType.RESUME_ALL) {
      ExecutionInterrupt pauseAll = getExecutionInterrupt(res, PAUSE_ALL);
      if (pauseAll == null || isPresent(res, ExecutionInterruptType.RESUME_ALL)) {
        throw new WingsException(RESUME_ALL_ALREADY);
      }
      makeInactive(pauseAll);
      waitNotifyEngine.notify(
          pauseAll.getUuid(), anExecutionStatusData().withExecutionStatus(ExecutionStatus.SUCCESS).build());
    }

    executionInterrupt = wingsPersistence.saveAndGet(ExecutionInterrupt.class, executionInterrupt);
    stateMachineExecutor.handleInterrupt(executionInterrupt);

    sendNotificationIfRequired(executionInterrupt);
    closeAlertsIfOpened(stateExecutionInstance, executionInterrupt);
    return executionInterrupt;
  }

  private void sendNotificationIfRequired(ExecutionInterrupt executionInterrupt) {
    final ExecutionInterruptType executionInterruptType = executionInterrupt.getExecutionInterruptType();
    switch (executionInterruptType) {
      case PAUSE_ALL: {
        sendNotification(executionInterrupt, PAUSED);
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
      default:
        unhandled(executionInterruptType);
    }
  }

  /**
   * Closes alerts if any opened
   * @param stateExecutionInstance
   * @param executionInterrupt
   */
  private void closeAlertsIfOpened(
      StateExecutionInstance stateExecutionInstance, ExecutionInterrupt executionInterrupt) {
    String stateExecutionInstanceId = stateExecutionInstance != null ? stateExecutionInstance.getUuid()
                                                                     : executionInterrupt.getStateExecutionInstanceId();
    String executionId = stateExecutionInstance != null ? stateExecutionInstance.getExecutionUuid()
                                                        : executionInterrupt.getExecutionUuid();
    String appId = stateExecutionInstance != null ? stateExecutionInstance.getAppId() : executionInterrupt.getAppId();
    try {
      final ExecutionInterruptType executionInterruptType = executionInterrupt.getExecutionInterruptType();
      switch (executionInterruptType) {
        case RESUME:
        case RETRY:
        case IGNORE:
        case ROLLBACK:
        case ABORT:
        case RESUME_ALL:
        case MARK_SUCCESS:
        case MARK_FAILED: {
          // Close ManualIntervention alert
          ManualInterventionNeededAlert manualInterventionNeededAlert =
              ManualInterventionNeededAlert.builder()
                  .executionId(executionId)
                  .stateExecutionInstanceId(stateExecutionInstanceId)
                  .build();
          alertService.closeAlert(null, appId, ManualInterventionNeeded, manualInterventionNeededAlert);
          break;
        }
        case ABORT_ALL:
        case PAUSE:
        case END_EXECUTION:
        case ROLLBACK_DONE:
          noop();
          break;

        default:
          unhandled(executionInterruptType);
      }
    } catch (Exception e) {
      logger.error("Failed to close the ManualNeededAlert/ ApprovalNeededAlert  for appId, executionId  ", appId,
          executionId, e);
    }
  }

  private void sendNotification(ExecutionInterrupt executionInterrupt, ExecutionStatus status) {
    WorkflowExecution workflowExecution = wingsPersistence.get(
        WorkflowExecution.class, executionInterrupt.getAppId(), executionInterrupt.getExecutionUuid());
    PageRequest<StateExecutionInstance> pageRequest =
        aPageRequest()
            .withLimit("1")
            .addFilter("appId", EQ, executionInterrupt.getAppId())
            .addFilter("executionUuid", EQ, executionInterrupt.getExecutionUuid())
            .addFilter("createdAt", GT, workflowExecution.getCreatedAt())
            .addOrder("createdAt", OrderType.DESC)
            .withReadPref(ReadPref.CRITICAL)
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
    PageRequest<ExecutionInterrupt> req = aPageRequest()
                                              .withReadPref(ReadPref.CRITICAL)
                                              .addFilter("appId", EQ, executionInterrupt.getAppId())
                                              .addFilter("executionUuid", EQ, executionInterrupt.getExecutionUuid())
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
  public List<ExecutionInterrupt> checkForExecutionInterrupt(String appId, String executionUuid) {
    PageRequest<ExecutionInterrupt> req =
        aPageRequest()
            .withReadPref(ReadPref.CRITICAL)
            .addFilter("appId", EQ, appId)
            .addFilter("executionUuid", EQ, executionUuid)
            .addFilter("executionInterruptType", Operator.IN, ABORT_ALL, PAUSE_ALL, ROLLBACK)
            .addOrder("createdAt", OrderType.DESC)
            .build();
    PageResponse<ExecutionInterrupt> res = wingsPersistence.query(ExecutionInterrupt.class, req);
    if (res == null) {
      return null;
    }
    return res.getResponse();
  }
}
