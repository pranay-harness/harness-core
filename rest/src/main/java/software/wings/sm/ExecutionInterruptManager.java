/**
 *
 */

package software.wings.sm;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.eraro.ErrorCode.INVALID_ARGUMENT;
import static io.harness.eraro.ErrorCode.PAUSE_ALL_ALREADY;
import static io.harness.eraro.ErrorCode.RESUME_ALL_ALREADY;
import static io.harness.eraro.ErrorCode.ROLLBACK_ALREADY;
import static io.harness.eraro.ErrorCode.STATE_NOT_FOR_TYPE;
import static io.harness.govern.Switch.noop;
import static io.harness.govern.Switch.unhandled;
import static java.util.Arrays.asList;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.SearchFilter.Operator.GE;
import static software.wings.beans.SearchFilter.Operator.IN;
import static software.wings.beans.alert.AlertType.ManualInterventionNeeded;
import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;
import static software.wings.exception.WingsException.ExecutionContext.MANAGER;
import static software.wings.exception.WingsException.USER;
import static software.wings.sm.ExecutionInterruptType.ABORT;
import static software.wings.sm.ExecutionInterruptType.ABORT_ALL;
import static software.wings.sm.ExecutionInterruptType.IGNORE;
import static software.wings.sm.ExecutionInterruptType.PAUSE;
import static software.wings.sm.ExecutionInterruptType.PAUSE_ALL;
import static software.wings.sm.ExecutionInterruptType.RESUME;
import static software.wings.sm.ExecutionInterruptType.RESUME_ALL;
import static software.wings.sm.ExecutionInterruptType.RETRY;
import static software.wings.sm.ExecutionInterruptType.ROLLBACK;
import static software.wings.sm.ExecutionStatus.ERROR;
import static software.wings.sm.ExecutionStatus.FAILED;
import static software.wings.sm.ExecutionStatus.NEW;
import static software.wings.sm.ExecutionStatus.PAUSED;
import static software.wings.sm.ExecutionStatus.RESUMED;
import static software.wings.sm.ExecutionStatus.RUNNING;
import static software.wings.sm.ExecutionStatus.STARTING;
import static software.wings.sm.ExecutionStatus.SUCCESS;
import static software.wings.sm.ExecutionStatus.WAITING;
import static software.wings.sm.ExecutionStatusData.Builder.anExecutionStatusData;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Injector;

import io.harness.persistence.ReadPref;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.SortOrder.OrderType;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.alert.ManualInterventionNeededAlert;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.exception.WingsExceptionMapper;
import software.wings.service.impl.workflow.WorkflowNotificationHelper;
import software.wings.service.intfc.AlertService;
import software.wings.waitnotify.WaitNotifyEngine;

import java.util.List;
import java.util.Map;

/**
 * The interface State machine event manager.
 *
 * @author Rishi
 */
public class ExecutionInterruptManager {
  private static final Logger logger = LoggerFactory.getLogger(ExecutionInterruptManager.class);

  @Inject private WingsPersistence wingsPersistence;
  @Inject private StateMachineExecutor stateMachineExecutor;
  @Inject private WaitNotifyEngine waitNotifyEngine;

  @Inject private Injector injector;
  @Inject private WorkflowNotificationHelper workflowNotificationHelper;
  @Inject private AlertService alertService;

  Map<ExecutionInterruptType, List<ExecutionStatus>> acceptableIndividualStatusList =
      ImmutableMap.<ExecutionInterruptType, List<ExecutionStatus>>builder()
          .put(RESUME, asList(PAUSED))
          .put(IGNORE, asList(PAUSED, WAITING))
          .put(RETRY, asList(WAITING, FAILED, ERROR))
          .put(ABORT, asList(NEW, STARTING, RUNNING, PAUSED, WAITING))
          .put(PAUSE, asList(NEW, STARTING, RUNNING))
          .build();

  /**
   * Register execution event execution event.
   *
   * @param executionInterrupt the execution event
   * @return the execution event
   */
  public ExecutionInterrupt registerExecutionInterrupt(ExecutionInterrupt executionInterrupt) {
    StateExecutionInstance stateExecutionInstance = null;
    ExecutionInterruptType executionInterruptType = executionInterrupt.getExecutionInterruptType();
    if (acceptableIndividualStatusList.containsKey(executionInterruptType)) {
      if (executionInterrupt.getStateExecutionInstanceId() == null) {
        throw new WingsException(INVALID_ARGUMENT).addParam("args", "null stateExecutionInstanceId");
      }

      stateExecutionInstance = wingsPersistence.get(StateExecutionInstance.class, executionInterrupt.getAppId(),
          executionInterrupt.getStateExecutionInstanceId());
      if (stateExecutionInstance == null) {
        throw new WingsException(INVALID_ARGUMENT)
            .addParam("args", "invalid stateExecutionInstanceId: " + executionInterrupt.getStateExecutionInstanceId());
      }

      final List<ExecutionStatus> statuses = acceptableIndividualStatusList.get(executionInterruptType);
      if (!statuses.contains(stateExecutionInstance.getStatus())) {
        throw new WingsException(STATE_NOT_FOR_TYPE)
            .addParam("displayName", stateExecutionInstance.getDisplayName())
            .addParam("type", executionInterruptType.name())
            .addParam("status", stateExecutionInstance.getStatus().name())
            .addParam("statuses", statuses);
      }
    }

    PageResponse<ExecutionInterrupt> res = listActiveExecutionInterrupts(executionInterrupt);

    if (executionInterruptType == ROLLBACK) {
      if (isPresent(res, ROLLBACK)) {
        throw new WingsException(ROLLBACK_ALREADY, USER);
      }
    }

    if (executionInterruptType == PAUSE_ALL) {
      if (isPresent(res, PAUSE_ALL)) {
        throw new WingsException(PAUSE_ALL_ALREADY, USER);
      }
      ExecutionInterrupt resumeAll = getExecutionInterrupt(res, ExecutionInterruptType.RESUME_ALL);
      if (resumeAll != null) {
        seize(resumeAll);
      }
    }

    if (executionInterruptType == ExecutionInterruptType.RESUME_ALL) {
      ExecutionInterrupt pauseAll = getExecutionInterrupt(res, PAUSE_ALL);
      if (pauseAll == null || isPresent(res, ExecutionInterruptType.RESUME_ALL)) {
        throw new WingsException(RESUME_ALL_ALREADY, USER);
      }
      seize(pauseAll);
      waitNotifyEngine.notify(pauseAll.getUuid(), anExecutionStatusData().withExecutionStatus(SUCCESS).build());
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
      case PAUSE_ALL:
        sendNotification(executionInterrupt, PAUSED);
        break;
      case RESUME_ALL:
        sendNotification(executionInterrupt, RESUMED);
        break;
      case ABORT_ALL:
      case ABORT:
      case MARK_EXPIRED:
      case PAUSE:
      case RESUME:
      case RETRY:
      case IGNORE:
      case MARK_FAILED:
      case MARK_SUCCESS:
      case ROLLBACK:
      case END_EXECUTION:
      case ROLLBACK_DONE:
        noop();
        break;
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
        case MARK_EXPIRED:
        case RESUME_ALL:
        case MARK_SUCCESS:
        case MARK_FAILED:
        case END_EXECUTION:
        case ABORT_ALL: {
          // Close ManualIntervention alert
          ManualInterventionNeededAlert manualInterventionNeededAlert =
              ManualInterventionNeededAlert.builder()
                  .executionId(executionId)
                  .stateExecutionInstanceId(stateExecutionInstanceId)
                  .build();
          alertService.closeAlert(null, appId, ManualInterventionNeeded, manualInterventionNeededAlert);
          break;
        }
        case PAUSE:
        case PAUSE_ALL:
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
    try {
      WorkflowExecution workflowExecution = wingsPersistence.get(
          WorkflowExecution.class, executionInterrupt.getAppId(), executionInterrupt.getExecutionUuid());
      PageRequest<StateExecutionInstance> pageRequest =
          aPageRequest()
              .withLimit("1")
              .addFilter("appId", EQ, executionInterrupt.getAppId())
              .addFilter("executionUuid", EQ, executionInterrupt.getExecutionUuid())
              .addFilter("createdAt", GE, workflowExecution.getCreatedAt())
              .addOrder("createdAt", OrderType.DESC)
              .withReadPref(ReadPref.CRITICAL)
              .build();

      PageResponse<StateExecutionInstance> pageResponse =
          wingsPersistence.query(StateExecutionInstance.class, pageRequest);
      if (isEmpty(pageResponse)) {
        logger.error("No StateExecutionInstance found for sendNotification");
        return;
      }
      StateMachine sm = wingsPersistence.get(
          StateMachine.class, executionInterrupt.getAppId(), pageResponse.get(0).getStateMachineId());
      ExecutionContextImpl context = new ExecutionContextImpl(pageResponse.get(0), sm, injector);
      injector.injectMembers(context);

      workflowNotificationHelper.sendWorkflowStatusChangeNotification(context, status);
    } catch (WingsException exception) {
      WingsExceptionMapper.logProcessedMessages(exception, MANAGER, logger);
    } catch (RuntimeException exception) {
      logger.error("Unknown runtime exception: ", exception);
    }
  }

  private void seize(ExecutionInterrupt executionInterrupt) {
    UpdateOperations<ExecutionInterrupt> updateOps =
        wingsPersistence.createUpdateOperations(ExecutionInterrupt.class).set("seized", true);
    wingsPersistence.update(executionInterrupt, updateOps);
  }

  private boolean isPresent(PageResponse<ExecutionInterrupt> res, ExecutionInterruptType eventType) {
    return getExecutionInterrupt(res, eventType) != null;
  }

  private ExecutionInterrupt getExecutionInterrupt(
      PageResponse<ExecutionInterrupt> res, ExecutionInterruptType eventType) {
    if (isEmpty(res)) {
      return null;
    }
    for (ExecutionInterrupt evt : res) {
      if (evt.getExecutionInterruptType() == eventType) {
        return evt;
      }
    }
    return null;
  }

  private PageResponse<ExecutionInterrupt> listActiveExecutionInterrupts(ExecutionInterrupt executionInterrupt) {
    PageRequest<ExecutionInterrupt> req = aPageRequest()
                                              .withReadPref(ReadPref.CRITICAL)
                                              .addFilter("appId", EQ, executionInterrupt.getAppId())
                                              .addFilter("executionUuid", EQ, executionInterrupt.getExecutionUuid())
                                              .addFilter("seized", EQ, false)
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
            .addFilter("executionInterruptType", IN, ABORT_ALL, PAUSE_ALL, RESUME_ALL, ROLLBACK)
            .addFilter("seized", EQ, false)
            .addOrder("createdAt", OrderType.DESC)
            .build();
    PageResponse<ExecutionInterrupt> res = wingsPersistence.query(ExecutionInterrupt.class, req);
    if (res == null) {
      return null;
    }
    return res.getResponse();
  }
}
