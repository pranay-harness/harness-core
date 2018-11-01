package software.wings.sm;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.eraro.ErrorCode.INVALID_ARGUMENT;
import static io.harness.eraro.ErrorCode.STATE_NOT_FOR_TYPE;
import static io.harness.exception.WingsException.ExecutionContext.MANAGER;
import static io.harness.govern.Switch.unhandled;
import static io.harness.persistence.ReadPref.CRITICAL;
import static io.harness.persistence.ReadPref.NORMAL;
import static io.harness.threading.Morpheus.quietSleep;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.ExecutionScope.WORKFLOW;
import static software.wings.beans.InformationNotification.Builder.anInformationNotification;
import static software.wings.beans.alert.AlertType.ManualInterventionNeeded;
import static software.wings.common.NotificationMessageResolver.NotificationMessageType.MANUAL_INTERVENTION_NEEDED_NOTIFICATION;
import static software.wings.sm.ElementNotifyResponseData.Builder.anElementNotifyResponseData;
import static software.wings.sm.ExecutionInterrupt.ExecutionInterruptBuilder.anExecutionInterrupt;
import static software.wings.sm.ExecutionInterruptType.PAUSE_ALL;
import static software.wings.sm.ExecutionInterruptType.RESUME_ALL;
import static software.wings.sm.ExecutionInterruptType.RETRY;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;
import static software.wings.sm.ExecutionStatus.ABORTED;
import static software.wings.sm.ExecutionStatus.DISCONTINUING;
import static software.wings.sm.ExecutionStatus.ERROR;
import static software.wings.sm.ExecutionStatus.EXPIRED;
import static software.wings.sm.ExecutionStatus.FAILED;
import static software.wings.sm.ExecutionStatus.NEW;
import static software.wings.sm.ExecutionStatus.PAUSED;
import static software.wings.sm.ExecutionStatus.PAUSING;
import static software.wings.sm.ExecutionStatus.QUEUED;
import static software.wings.sm.ExecutionStatus.RUNNING;
import static software.wings.sm.ExecutionStatus.STARTING;
import static software.wings.sm.ExecutionStatus.SUCCESS;
import static software.wings.sm.ExecutionStatus.WAITING;
import static software.wings.sm.ExecutionStatus.activeStatuses;
import static software.wings.sm.ExecutionStatus.brokeStatuses;
import static software.wings.sm.ExecutionStatus.isBrokeStatus;
import static software.wings.sm.ExecutionStatus.isFinalStatus;
import static software.wings.sm.ExecutionStatus.isPositiveStatus;
import static software.wings.sm.StateExecutionData.StateExecutionDataBuilder.aStateExecutionData;
import static software.wings.utils.Validator.notNullCheck;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.harness.beans.PageResponse;
import io.harness.delegate.task.protocol.ResponseData;
import io.harness.eraro.ErrorCode;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.observer.Subject;
import io.harness.scheduler.PersistentScheduler;
import lombok.Getter;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Account;
import software.wings.beans.Application;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.Environment;
import software.wings.beans.ErrorStrategy;
import software.wings.beans.NotificationRule;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.alert.AlertType;
import software.wings.beans.alert.ManualInterventionNeededAlert;
import software.wings.common.Constants;
import software.wings.common.NotificationMessageResolver;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsExceptionMapper;
import software.wings.service.impl.DelayEventHelper;
import software.wings.service.impl.ExecutionLogContext;
import software.wings.service.impl.workflow.WorkflowNotificationHelper;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.NotificationService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.ExecutionEvent.ExecutionEventBuilder;
import software.wings.sm.states.BarrierState;
import software.wings.sm.states.EnvState;
import software.wings.sm.states.PhaseStepSubWorkflow;
import software.wings.sm.states.PhaseSubWorkflow;
import software.wings.utils.KryoUtils;
import software.wings.utils.MapperUtils;
import software.wings.utils.Misc;
import software.wings.waitnotify.ErrorNotifyResponseData;
import software.wings.waitnotify.NotifyCallback;
import software.wings.waitnotify.WaitNotifyEngine;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import javax.validation.constraints.NotNull;

/**
 * Class responsible for executing state machine.
 *
 * @author Rishi
 */
@Singleton
public class StateMachineExecutor {
  private static final Logger logger = LoggerFactory.getLogger(StateMachineExecutor.class);

  @Getter private Subject<StateStatusUpdate> statusUpdateSubject = new Subject<>();

  @Inject private AppService appService;
  @Inject private ExecutorService executorService;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject private Injector injector;
  @Inject private WorkflowExecutionService workflowExecutionService;
  @Inject private ExecutionInterruptManager executionInterruptManager;
  @Inject @Named("BackgroundJobScheduler") private PersistentScheduler jobScheduler;
  @Inject private DelegateService delegateService;
  @Inject private AlertService alertService;
  @Inject private WorkflowService workflowService;
  @Inject private NotificationService notificationService;
  @Inject private NotificationMessageResolver notificationMessageResolver;
  @Inject private WorkflowNotificationHelper workflowNotificationHelper;
  @Inject private DelayEventHelper delayEventHelper;
  @Inject private FeatureFlagService featureFlagService;

  /**
   * Execute.
   *
   * @param appId         the app id
   * @param smId          the sm id
   * @param executionUuid the execution uuid
   * @param executionName the execution name
   * @param contextParams the context params
   * @param callback      the callback
   * @return the state execution instance
   */
  public StateExecutionInstance execute(String appId, String smId, String executionUuid, String executionName,
      List<ContextElement> contextParams, StateMachineExecutionCallback callback) {
    return execute(wingsPersistence.get(StateMachine.class, appId, smId, CRITICAL), executionUuid, executionName,
        contextParams, callback, null);
  }

  /**
   * Execute.
   *
   * @param appId         the app id
   * @param smId          the sm id
   * @param executionUuid the execution uuid
   * @param executionName the execution name
   * @param contextParams the context params
   * @param callback      the callback
   * @param executionEventAdvisor      the executionEventAdvisor
   * @return the state execution instance
   */
  public StateExecutionInstance execute(String appId, String smId, String executionUuid, String executionName,
      List<ContextElement> contextParams, StateMachineExecutionCallback callback,
      ExecutionEventAdvisor executionEventAdvisor) {
    return execute(wingsPersistence.get(StateMachine.class, appId, smId, CRITICAL), executionUuid, executionName,
        contextParams, callback, executionEventAdvisor);
  }

  /**
   * Execute.
   *
   * @param sm            the sm
   * @param executionUuid the execution uuid
   * @param executionName the execution name
   * @param contextParams the context params
   * @param callback      the callback
   * @return the state execution instance
   */
  public StateExecutionInstance execute(StateMachine sm, String executionUuid, String executionName,
      List<ContextElement> contextParams, StateMachineExecutionCallback callback,
      ExecutionEventAdvisor executionEventAdvisor) {
    if (sm == null) {
      logger.error("StateMachine passed for execution is null");
      throw new WingsException(INVALID_ARGUMENT).addParam("args", "State machine is null");
    }

    StateExecutionInstance stateExecutionInstance = new StateExecutionInstance();
    stateExecutionInstance.setExecutionName(executionName);
    stateExecutionInstance.setExecutionUuid(executionUuid);

    LinkedList<ContextElement> contextElements = new LinkedList<>();
    if (contextParams != null) {
      contextElements.addAll(contextParams);
    }
    stateExecutionInstance.setContextElements(contextElements);

    stateExecutionInstance.setCallback(callback);

    if (stateExecutionInstance.getDisplayName() == null) {
      stateExecutionInstance.setStateName(sm.getInitialStateName());
      stateExecutionInstance.setDisplayName(sm.getInitialStateName());
    }
    if (executionEventAdvisor != null) {
      stateExecutionInstance.setExecutionEventAdvisors(asList(executionEventAdvisor));
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
    stateExecutionInstance = queue(stateMachine, stateExecutionInstance);
    return triggerExecution(stateMachine, stateExecutionInstance);
  }

  /**
   * Execute.
   *
   * @param stateMachine           the state machine
   * @param stateExecutionInstance the state execution instance
   * @return the state execution instance
   */
  public StateExecutionInstance queue(
      @NotNull StateMachine stateMachine, @NotNull StateExecutionInstance stateExecutionInstance) {
    if (stateExecutionInstance.getChildStateMachineId() != null
        && !stateExecutionInstance.getChildStateMachineId().equals(stateMachine.getUuid())
        && stateMachine.getChildStateMachines().get(stateExecutionInstance.getChildStateMachineId()) == null) {
      throw new InvalidRequestException(format("State instance %s child machine does not exist in the state machine %s",
          stateExecutionInstance.getUuid(), stateMachine.getUuid()));
    }
    StateMachine sm;
    if (stateExecutionInstance.getChildStateMachineId() == null) {
      sm = stateMachine;
    } else {
      sm = stateMachine.getChildStateMachines().get(stateExecutionInstance.getChildStateMachineId());
    }
    if (stateExecutionInstance.getDisplayName() == null) {
      stateExecutionInstance.setStateName(sm.getInitialStateName());
      stateExecutionInstance.setDisplayName(sm.getInitialStateName());
    }
    return saveStateExecutionInstance(stateMachine, stateExecutionInstance);
  }

  private StateExecutionInstance saveStateExecutionInstance(
      StateMachine stateMachine, StateExecutionInstance stateExecutionInstance) {
    notNullCheck("displayName", stateExecutionInstance.getDisplayName());
    notNullCheck("stateName", stateExecutionInstance.getStateName());

    stateExecutionInstance.setAppId(stateMachine.getAppId());
    stateExecutionInstance.setStateMachineId(stateMachine.getUuid());
    State state =
        stateMachine.getState(stateExecutionInstance.getChildStateMachineId(), stateExecutionInstance.getStateName());
    stateExecutionInstance.setRollback(state.isRollback());
    stateExecutionInstance.setStateType(state.getStateType());

    if (state instanceof EnvState) {
      stateExecutionInstance.setPipelineStateElementId(((EnvState) state).getPipelineStateElementId());
    }

    if (state instanceof PhaseStepSubWorkflow) {
      stateExecutionInstance.setPhaseSubWorkflowId(((PhaseStepSubWorkflow) state).getSubWorkflowId());
    } else if (state instanceof PhaseSubWorkflow) {
      stateExecutionInstance.setPhaseSubWorkflowId(((PhaseSubWorkflow) state).getSubWorkflowId());
    } else {
      stateExecutionInstance.setPhaseSubWorkflowId(null);
    }

    stateExecutionInstance.setStepId(state instanceof BarrierState ? state.getId() : null);

    if (stateExecutionInstance.getUuid() != null) {
      throw new InvalidRequestException("StateExecutionInstance was already created");
    }

    Integer timeout = state.getTimeoutMillis();
    if (timeout == null) {
      timeout = Constants.DEFAULT_STATE_TIMEOUT_MILLIS;
    }
    if (state.getWaitInterval() != null) {
      timeout += state.getWaitInterval() * 1000;
    }
    stateExecutionInstance.setExpiryTs(System.currentTimeMillis() + timeout);
    return wingsPersistence.saveAndGet(StateExecutionInstance.class, stateExecutionInstance);
  }

  /**
   * Trigger execution state execution instance.
   *
   * @param stateMachine           the state machine
   * @param stateExecutionInstance the state execution instance
   * @return the state execution instance
   */
  StateExecutionInstance triggerExecution(StateMachine stateMachine, StateExecutionInstance stateExecutionInstance) {
    stateExecutionInstance = saveStateExecutionInstance(stateMachine, stateExecutionInstance);
    ExecutionContextImpl context = new ExecutionContextImpl(stateExecutionInstance, stateMachine, injector);
    injector.injectMembers(context);
    executorService.execute(new SmExecutionDispatcher(context, this));
    return stateExecutionInstance;
  }

  /**
   * Start execution.
   *
   * @param appId                    the app id
   * @param executionUuid the executionUuid
   */
  public boolean startQueuedExecution(String appId, String executionUuid) {
    PageResponse<StateExecutionInstance> pageResponse = wingsPersistence.query(StateExecutionInstance.class,
        aPageRequest()
            .withReadPref(CRITICAL)
            .addFilter("appId", EQ, appId)
            .addFilter("executionUuid", EQ, executionUuid)
            .build());

    if (pageResponse == null || pageResponse.size() != 1 || pageResponse.get(0) == null
        || !(pageResponse.get(0).getStatus() == QUEUED || pageResponse.get(0).getStatus() == NEW)) {
      return false;
    }
    StateExecutionInstance stateExecutionInstance = pageResponse.get(0);
    StateMachine stateMachine =
        wingsPersistence.get(StateMachine.class, stateExecutionInstance.getStateMachineId(), CRITICAL);
    logger.info("Starting execution of StateMachine {} with initialState {}", stateMachine.getName(),
        stateMachine.getInitialStateName());
    startExecution(stateMachine, stateExecutionInstance);
    return true;
  }

  /**
   * Start execution.
   *
   * @param appId                    the app id
   * @param executionUuid the execution Uuid
   * @param stateExecutionInstanceId the state execution instance id
   */
  void startExecution(String appId, String executionUuid, String stateExecutionInstanceId) {
    StateExecutionInstance stateExecutionInstance =
        getStateExecutionInstance(appId, executionUuid, stateExecutionInstanceId);
    StateMachine sm =
        wingsPersistence.get(StateMachine.class, appId, stateExecutionInstance.getStateMachineId(), CRITICAL);
    startExecution(sm, stateExecutionInstance);
  }

  /**
   * Start execution.
   *
   * @param stateMachine                    the stateMachine
   * @param stateExecutionInstance the stateExecutionInstance
   */
  public void startExecution(StateMachine stateMachine, StateExecutionInstance stateExecutionInstance) {
    ExecutionContextImpl context = new ExecutionContextImpl(stateExecutionInstance, stateMachine, injector);
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

    ExecutionInterrupt reason = null;
    List<ExecutionInterrupt> executionInterrupts = executionInterruptManager.checkForExecutionInterrupt(
        stateExecutionInstance.getAppId(), stateExecutionInstance.getExecutionUuid());
    if (executionInterrupts != null) {
      Optional<ExecutionInterrupt> pauseAll =
          executionInterrupts.stream()
              .filter(ei -> ei != null && ei.getExecutionInterruptType() == PAUSE_ALL)
              .findFirst();
      if (pauseAll.isPresent()) {
        updateStatus(stateExecutionInstance, PAUSED, Lists.newArrayList(NEW, QUEUED), pauseAll.get());
        waitNotifyEngine.waitForAll(new ExecutionResumeAllCallback(stateExecutionInstance.getAppId(),
                                        stateExecutionInstance.getExecutionUuid(), stateExecutionInstance.getUuid()),
            pauseAll.get().getUuid());
        return;
      }

      if (stateExecutionInstance.getStatus() == PAUSED) {
        Optional<ExecutionInterrupt> resumeAll =
            executionInterrupts.stream()
                .filter(ei -> ei != null && ei.getExecutionInterruptType() == RESUME_ALL)
                .findFirst();

        if (resumeAll.isPresent()) {
          reason = resumeAll.get();
        }
      }
    }

    StateMachine stateMachine = context.getStateMachine();
    boolean updated =
        updateStartStatus(stateExecutionInstance, STARTING, Lists.newArrayList(NEW, QUEUED, PAUSED, WAITING), reason);
    if (!updated) {
      throw new WingsException("stateExecutionInstance: " + stateExecutionInstance.getUuid() + " could not be started");
    }
    State currentState =
        stateMachine.getState(stateExecutionInstance.getChildStateMachineId(), stateExecutionInstance.getStateName());
    if (currentState.getWaitInterval() != null && currentState.getWaitInterval() > 0) {
      StateExecutionData stateExecutionData =
          aStateExecutionData()
              .withWaitInterval(currentState.getWaitInterval())
              .withErrorMsg("Waiting " + currentState.getWaitInterval() + " seconds before execution")
              .build();
      updated = updateStateExecutionData(stateExecutionInstance, stateExecutionData, RUNNING, null, null, null);
      if (!updated) {
        throw new WingsException("updateStateExecutionData failed");
      }
      String resumeId = delayEventHelper.delay(currentState.getWaitInterval(), Collections.emptyMap());
      waitNotifyEngine.waitForAll(new ExecutionWaitCallback(stateExecutionInstance.getAppId(),
                                      stateExecutionInstance.getExecutionUuid(), stateExecutionInstance.getUuid()),
          resumeId);
      return;
    }

    startStateExecution(context, stateExecutionInstance);
  }

  void startStateExecution(String appId, String executionUuid, String stateExecutionInstanceId) {
    logger.info("startStateExecution called after wait");

    StateExecutionInstance stateExecutionInstance =
        getStateExecutionInstance(appId, executionUuid, stateExecutionInstanceId);
    StateMachine sm =
        wingsPersistence.get(StateMachine.class, appId, stateExecutionInstance.getStateMachineId(), CRITICAL);
    ExecutionContextImpl context = new ExecutionContextImpl(stateExecutionInstance, sm, injector);
    injector.injectMembers(context);
    startStateExecution(context, stateExecutionInstance);
  }

  private void startStateExecution(ExecutionContextImpl context, StateExecutionInstance stateExecutionInstance) {
    ExecutionResponse executionResponse = null;
    WingsException ex = null;
    try {
      StateMachine stateMachine = context.getStateMachine();
      State currentState =
          stateMachine.getState(stateExecutionInstance.getChildStateMachineId(), stateExecutionInstance.getStateName());

      logger.info("startStateExecution for State {} of type {}", currentState.getName(), currentState.getStateType());

      if (stateExecutionInstance.getStateParams() != null) {
        MapperUtils.mapObject(stateExecutionInstance.getStateParams(), currentState);
      }
      injector.injectMembers(currentState);
      invokeAdvisors(context, currentState);
      executionResponse = currentState.execute(context);

      handleExecuteResponse(context, executionResponse);
    } catch (WingsException exception) {
      ex = exception;
    } catch (Exception exception) {
      ex = new WingsException(exception);
    }

    if (ex != null) {
      handleExecuteResponseException(context, ex);
    }
  }

  private ExecutionEventAdvice invokeAdvisors(ExecutionContextImpl context, State state) {
    List<ExecutionEventAdvisor> advisors = context.getStateExecutionInstance().getExecutionEventAdvisors();
    if (isEmpty(advisors)) {
      return null;
    }

    ExecutionEventAdvice executionEventAdvice = null;
    for (ExecutionEventAdvisor advisor : advisors) {
      executionEventAdvice = advisor.onExecutionEvent(
          ExecutionEventBuilder.anExecutionEvent().withContext(context).withState(state).build());
    }
    return executionEventAdvice;
  }

  /**
   * Handle execute response state execution instance.
   *
   * @param context           the context
   * @param executionResponse the execution response
   * @return the state execution instance
   */
  StateExecutionInstance handleExecuteResponse(ExecutionContextImpl context, ExecutionResponse executionResponse) {
    StateExecutionInstance stateExecutionInstance = context.getStateExecutionInstance();
    StateMachine sm = context.getStateMachine();
    State currentState =
        sm.getState(stateExecutionInstance.getChildStateMachineId(), stateExecutionInstance.getStateName());

    ExecutionStatus status = executionResponse.getExecutionStatus();
    if (executionResponse.isAsync()) {
      if (isEmpty(executionResponse.getCorrelationIds())) {
        logger.error("executionResponse is null, but no correlationId - currentState : " + currentState.getName()
            + ", stateExecutionInstanceId: " + stateExecutionInstance.getUuid());
        status = ERROR;
      } else {
        if (status != PAUSED) {
          status = RUNNING;
        }
        NotifyCallback callback = new StateMachineResumeCallback(stateExecutionInstance.getAppId(),
            stateExecutionInstance.getExecutionUuid(), stateExecutionInstance.getUuid());
        waitNotifyEngine.waitForAll(callback, executionResponse.getCorrelationIds().toArray(new String[0]));
      }

      boolean updated = updateStateExecutionData(stateExecutionInstance, executionResponse.getStateExecutionData(),
          status, executionResponse.getErrorMessage(), executionResponse.getContextElements(),
          executionResponse.getNotifyElements(), executionResponse.getDelegateTaskId());
      if (!updated) {
        throw new WingsException("updateStateExecutionData failed");
      }
      invokeAdvisors(context, currentState);
      if (status == RUNNING) {
        handleSpawningStateExecutionInstances(sm, stateExecutionInstance, executionResponse);
      }

    } else {
      boolean updated = updateStateExecutionData(stateExecutionInstance, executionResponse.getStateExecutionData(),
          status, executionResponse.getErrorMessage(), executionResponse.getContextElements(),
          executionResponse.getNotifyElements());
      if (!updated) {
        return reloadStateExecutionInstanceAndCheckStatus(stateExecutionInstance);
      }

      ExecutionEventAdvice executionEventAdvice = invokeAdvisors(context, currentState);
      if (executionEventAdvice != null) {
        return handleExecutionEventAdvice(context, stateExecutionInstance, status, executionEventAdvice);
      } else if (isPositiveStatus(status)) {
        return successTransition(context);
      } else if (isBrokeStatus(status)) {
        return failedTransition(context, null);
      } else if (ExecutionStatus.isDiscontinueStatus(status)) {
        endTransition(context, stateExecutionInstance, status, null);
      }
    }
    return stateExecutionInstance;
  }

  private StateExecutionInstance reloadStateExecutionInstanceAndCheckStatus(
      StateExecutionInstance stateExecutionInstance) {
    String stateExecutionInstanceId = stateExecutionInstance.getUuid();
    stateExecutionInstance = getStateExecutionInstance(
        stateExecutionInstance.getAppId(), stateExecutionInstance.getExecutionUuid(), stateExecutionInstanceId);
    if (isFinalStatus(stateExecutionInstance.getStatus())) {
      if (logger.isDebugEnabled()) {
        logger.debug("StateExecutionInstance already reached the final status. Skipping the update for "
            + stateExecutionInstanceId);
      }
      return stateExecutionInstance;
    } else {
      throw new WingsException("updateStateExecutionData failed");
    }
  }

  private StateExecutionInstance handleExecutionEventAdvice(ExecutionContextImpl context,
      StateExecutionInstance stateExecutionInstance, ExecutionStatus status,
      ExecutionEventAdvice executionEventAdvice) {
    switch (executionEventAdvice.getExecutionInterruptType()) {
      case MARK_FAILED: {
        return failedTransition(context, null);
      }
      case MARK_SUCCESS: {
        if (status != SUCCESS) {
          updateEndStatus(stateExecutionInstance, SUCCESS, asList(status));
        }
        return successTransition(context);
      }
      case IGNORE: {
        return successTransition(context);
      }
      case ABORT: {
        endTransition(context, stateExecutionInstance, ABORTED, null);
        break;
      }
      case PAUSE: {
        updateStatus(stateExecutionInstance, WAITING, brokeStatuses(), null, ops -> {
          if (executionEventAdvice.getStateParams() != null) {
            ops.set("stateParams", executionEventAdvice.getStateParams());
          }
        });

        // Open an alert
        openAnAlert(context, stateExecutionInstance);
        sendManualInterventionNeededNotification(context);
        break;
      }
      case NEXT_STEP:
      case ROLLBACK: {
        if (executionEventAdvice.getNextChildStateMachineId() != null
            || executionEventAdvice.getNextStateName() != null) {
          executionEventAdviceTransition(context, executionEventAdvice);
          break;
        }
        throw new WingsException(INVALID_ARGUMENT).addParam("args", "rollbackStateMachineId or rollbackStateName");
      }
      case ROLLBACK_DONE: {
        endTransition(context, stateExecutionInstance, FAILED, null);
        break;
      }
      case RETRY: {
        if (executionEventAdvice.getWaitInterval() != null && executionEventAdvice.getWaitInterval() > 0) {
          logger.info("Retry Wait Interval : {}", executionEventAdvice.getWaitInterval());
          String resumeId = delayEventHelper.delay(executionEventAdvice.getWaitInterval(), Collections.emptyMap());
          waitNotifyEngine.waitForAll(new ExecutionWaitRetryCallback(stateExecutionInstance.getAppId(),
                                          stateExecutionInstance.getExecutionUuid(), stateExecutionInstance.getUuid()),
              resumeId);
        } else {
          executionInterruptManager.registerExecutionInterrupt(
              anExecutionInterrupt()
                  .withAppId(stateExecutionInstance.getAppId())
                  .withExecutionUuid(stateExecutionInstance.getExecutionUuid())
                  .withStateExecutionInstanceId(stateExecutionInstance.getUuid())
                  .withExecutionInterruptType(RETRY)
                  .build());
        }
        break;
      }
      case END_EXECUTION: {
        if (!isFinalStatus(status)) {
          status = ABORTED;
        }
        endTransition(context, stateExecutionInstance, status, null);
        break;
      }
      default: {
        throw new WingsException(INVALID_ARGUMENT)
            .addParam("args",
                "executionEventAdvice.getExecutionInterruptType: " + executionEventAdvice.getExecutionInterruptType());
      }
    }

    return stateExecutionInstance;
  }

  private Map<String, String> getManualInterventionPlaceholderValues(ExecutionContextImpl context) {
    WorkflowExecution workflowExecution = workflowExecutionService.getExecutionDetails(
        context.getApp().getUuid(), context.getWorkflowExecutionId(), false, emptySet());
    String artifactsMessage =
        workflowNotificationHelper.getArtifactsMessage(context, workflowExecution, WORKFLOW, null);

    return notificationMessageResolver.getPlaceholderValues(context, workflowExecution.getTriggeredBy().getName(),
        workflowExecution.getStartTs(), System.currentTimeMillis(), "", "", artifactsMessage, ExecutionStatus.PAUSED,
        AlertType.ManualInterventionNeeded);
  }

  protected void sendManualInterventionNeededNotification(ExecutionContextImpl context) {
    Application app = context.getApp();

    Workflow workflow = workflowService.readWorkflow(app.getAppId(), context.getWorkflowId());
    CanaryOrchestrationWorkflow orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow();
    List<NotificationRule> notificationRules = orchestrationWorkflow.getNotificationRules();

    Map<String, String> placeholderValues = getManualInterventionPlaceholderValues(context);
    notificationService.sendNotificationAsync(
        anInformationNotification()
            .withAppId(app.getName())
            .withAccountId(app.getAccountId())
            .withNotificationTemplateId(MANUAL_INTERVENTION_NEEDED_NOTIFICATION.name())
            .withNotificationTemplateVariables(placeholderValues)
            .build(),
        notificationRules);
  }

  private void openAnAlert(ExecutionContextImpl context, StateExecutionInstance stateExecutionInstance) {
    try {
      Application app = context.getApp();

      ManualInterventionNeededAlert manualInterventionNeededAlert =
          ManualInterventionNeededAlert.builder()
              .envId(context.getEnv().getUuid())
              .stateExecutionInstanceId(stateExecutionInstance.getUuid())
              .executionId(context.getWorkflowExecutionId())
              .name(context.getWorkflowExecutionName())
              .build();
      alertService.openAlert(
          app.getAccountId(), app.getUuid(), ManualInterventionNeeded, manualInterventionNeededAlert);
    } catch (Exception e) {
      logger.warn("Failed to open ManualInterventionNeeded alarm for executionId {} and name {}",
          context.getWorkflowExecutionId(), context.getWorkflowExecutionName(), e);
    }
  }

  private StateExecutionInstance executionEventAdviceTransition(
      ExecutionContextImpl context, ExecutionEventAdvice executionEventAdvice) {
    StateMachine sm = context.getStateMachine();
    State nextState =
        sm.getState(executionEventAdvice.getNextChildStateMachineId(), executionEventAdvice.getNextStateName());
    StateExecutionInstance cloned =
        clone(context.getStateExecutionInstance(), executionEventAdvice.getNextChildStateMachineId(), nextState);
    if (executionEventAdvice.getNextStateDisplayName() != null) {
      cloned.setDisplayName(executionEventAdvice.getNextStateDisplayName());
    }
    if (executionEventAdvice.getRollbackPhaseName() != null) {
      cloned.setRollbackPhaseName(executionEventAdvice.getRollbackPhaseName());
    }
    return triggerExecution(sm, cloned);
  }

  /**
   * Handle execute response exception state execution instance.
   *
   * @param context   the context
   * @param exception the exception
   * @return the state execution instance
   */
  @SuppressFBWarnings("NP_NULL_PARAM_DEREF")
  StateExecutionInstance handleExecuteResponseException(ExecutionContextImpl context, WingsException exception) {
    StateExecutionInstance stateExecutionInstance = null;
    State currentState = null;
    try {
      stateExecutionInstance = context.getStateExecutionInstance();
      StateMachine sm = context.getStateMachine();
      currentState =
          sm.getState(stateExecutionInstance.getChildStateMachineId(), stateExecutionInstance.getStateName());
      addContext(context, exception);
      WingsExceptionMapper.logProcessedMessages(exception, MANAGER, logger);
    } catch (RuntimeException ex) {
      logger.error("Error when processing exception", ex);
    }

    updateStateExecutionData(stateExecutionInstance, null, FAILED, Misc.getMessage(exception), null, null);

    try {
      ExecutionEventAdvice executionEventAdvice = invokeAdvisors(context, currentState);
      if (executionEventAdvice != null) {
        return handleExecutionEventAdvice(context, stateExecutionInstance, FAILED, executionEventAdvice);
      }
    } catch (RuntimeException ex) {
      logger.error("Error when trying to obtain the advice ", ex);
    }

    try {
      return failedTransition(context, exception);
    } catch (RuntimeException ex) {
      logger.error("Error in transitioning to failure state", ex);
    }
    return null;
  }

  private StateExecutionInstance successTransition(ExecutionContextImpl context) {
    StateExecutionInstance stateExecutionInstance = context.getStateExecutionInstance();
    StateMachine sm = context.getStateMachine();

    State nextState =
        sm.getSuccessTransition(stateExecutionInstance.getChildStateMachineId(), stateExecutionInstance.getStateName());
    if (nextState == null) {
      logger.info("nextSuccessState is null.. ending execution  - currentState : "
          + stateExecutionInstance.getStateName() + ", stateExecutionInstanceId: " + stateExecutionInstance.getUuid());

      logger.info("State Machine execution ended for the stateMachine: {}, executionUuid: {}", sm.getName(),
          stateExecutionInstance.getExecutionUuid());

      endTransition(context, stateExecutionInstance, SUCCESS, null);
    } else {
      StateExecutionInstance cloned = clone(stateExecutionInstance, nextState);
      return triggerExecution(sm, cloned);
    }

    return null;
  }

  private StateExecutionInstance failedTransition(ExecutionContextImpl context, Exception exception) {
    StateExecutionInstance stateExecutionInstance = context.getStateExecutionInstance();
    StateMachine sm = context.getStateMachine();

    State nextState =
        sm.getFailureTransition(stateExecutionInstance.getChildStateMachineId(), stateExecutionInstance.getStateName());
    if (nextState == null) {
      logger.info("nextFailureState is null.. for the currentState : {}, stateExecutionInstanceId: {}",
          stateExecutionInstance.getStateName(), stateExecutionInstance.getUuid());

      ErrorStrategy errorStrategy = context.getErrorStrategy();
      if (errorStrategy == null || errorStrategy == ErrorStrategy.FAIL) {
        logger.info("Ending execution  - currentState : {}, stateExecutionInstanceId: {}",
            stateExecutionInstance.getStateName(), stateExecutionInstance.getUuid());
        endTransition(context, stateExecutionInstance, FAILED, exception);
      } else if (errorStrategy == ErrorStrategy.PAUSE) {
        logger.info("Pausing execution  - currentState : {}, stateExecutionInstanceId: {}",
            stateExecutionInstance.getStateName(), stateExecutionInstance.getUuid());
        updateStatus(stateExecutionInstance, WAITING, Lists.newArrayList(FAILED), null);
      } else {
        // TODO: handle more strategy
        logger.info("Unhandled error strategy for the state: {}, stateExecutionInstanceId: {}, errorStrategy: {}",
            stateExecutionInstance.getStateName(), stateExecutionInstance.getUuid(), errorStrategy);
      }
    } else {
      StateExecutionInstance cloned = clone(stateExecutionInstance, nextState);
      return triggerExecution(sm, cloned);
    }
    return null;
  }

  private void endTransition(ExecutionContextImpl context, StateExecutionInstance stateExecutionInstance,
      ExecutionStatus status, Exception exception) {
    if (stateExecutionInstance.getNotifyId() != null) {
      notify(stateExecutionInstance, status);
    } else {
      executeCallback(context, stateExecutionInstance, status, exception);
    }
  }

  public void executeCallback(ExecutionContextImpl context, StateExecutionInstance stateExecutionInstance,
      ExecutionStatus status, Exception exception) {
    StateMachineExecutionCallback callback = stateExecutionInstance.getCallback();
    if (callback == null) {
      return;
    }

    injector.injectMembers(callback);
    callback.callback(context, status, exception);
  }

  private void discontinueExecution(ExecutionContextImpl context, ExecutionInterruptType interruptType) {
    StateExecutionInstance stateExecutionInstance = context.getStateExecutionInstance();

    final List<ExecutionStatus> executionStatuses = asList(NEW, QUEUED, STARTING, RUNNING, PAUSED, WAITING);

    boolean updated = updateStatus(stateExecutionInstance, DISCONTINUING, executionStatuses, null);
    if (!updated) {
      throw new WingsException(STATE_NOT_FOR_TYPE)
          .addParam("displayName", stateExecutionInstance.getDisplayName())
          .addParam("type", DISCONTINUING.name())
          .addParam(StateExecutionInstance.STATUS_KEY, stateExecutionInstance.getStatus().name())
          .addParam("statuses", executionStatuses);
    }

    ExecutionStatus finalStatus = getFinalStatus(interruptType);
    discontinueMarkedInstance(context, stateExecutionInstance, finalStatus);
  }

  private ExecutionStatus getFinalStatus(ExecutionInterruptType interruptType) {
    switch (interruptType) {
      case MARK_EXPIRED:
        return EXPIRED;
      case ABORT:
        return ABORTED;
      default:
        unhandled(interruptType);
    }
    return ERROR;
  }

  private void discontinueMarkedInstance(
      ExecutionContextImpl context, StateExecutionInstance stateExecutionInstance, ExecutionStatus finalStatus) {
    boolean updated = false;
    StateMachine sm = context.getStateMachine();
    try {
      State currentState =
          sm.getState(stateExecutionInstance.getChildStateMachineId(), stateExecutionInstance.getStateName());
      injector.injectMembers(currentState);
      if (isNotBlank(stateExecutionInstance.getDelegateTaskId())) {
        if (finalStatus == ABORTED) {
          delegateService.abortTask(context.getApp().getAccountId(), stateExecutionInstance.getDelegateTaskId());
        } else {
          delegateService.expireTask(context.getApp().getAccountId(), stateExecutionInstance.getDelegateTaskId());
        }
      }
      if (stateExecutionInstance.getStateParams() != null) {
        MapperUtils.mapObject(stateExecutionInstance.getStateParams(), currentState);
      }
      currentState.handleAbortEvent(context);
      updated = updateStateExecutionData(
          stateExecutionInstance, null, finalStatus, null, singletonList(DISCONTINUING), null, null, null);
      invokeAdvisors(context, currentState);

      endTransition(context, stateExecutionInstance, finalStatus, null);
    } catch (Exception e) {
      logger.error("Error in discontinuing", e);
    }
    if (!updated) {
      throw new WingsException(ErrorCode.STATE_DISCONTINUE_FAILED)
          .addParam("displayName", stateExecutionInstance.getDisplayName());
    }
  }

  private void notify(StateExecutionInstance stateExecutionInstance, ExecutionStatus status) {
    ElementNotifyResponseData notifyResponseData = anElementNotifyResponseData().withExecutionStatus(status).build();
    if (isNotEmpty(stateExecutionInstance.getNotifyElements())) {
      notifyResponseData.setContextElements(stateExecutionInstance.getNotifyElements());
    }
    waitNotifyEngine.notify(stateExecutionInstance.getNotifyId(), notifyResponseData);
  }

  private void handleSpawningStateExecutionInstances(
      StateMachine sm, StateExecutionInstance stateExecutionInstance, ExecutionResponse executionResponse) {
    if (executionResponse instanceof SpawningExecutionResponse) {
      SpawningExecutionResponse spawningExecutionResponse = (SpawningExecutionResponse) executionResponse;
      if (isNotEmpty(spawningExecutionResponse.getStateExecutionInstanceList())) {
        for (StateExecutionInstance childStateExecutionInstance :
            spawningExecutionResponse.getStateExecutionInstanceList()) {
          childStateExecutionInstance.setUuid(null);
          childStateExecutionInstance.setStateParams(null);
          childStateExecutionInstance.setParentInstanceId(stateExecutionInstance.getUuid());
          childStateExecutionInstance.setAppId(stateExecutionInstance.getAppId());
          childStateExecutionInstance.setNotifyElements(null);
          if (childStateExecutionInstance.getStateName() == null
              && childStateExecutionInstance.getChildStateMachineId() != null) {
            if (sm.getChildStateMachines().get(childStateExecutionInstance.getChildStateMachineId()) == null) {
              notify(childStateExecutionInstance, SUCCESS);
              return;
            }
            String initialStateName = sm.getChildStateMachines()
                                          .get(childStateExecutionInstance.getChildStateMachineId())
                                          .getInitialStateName();
            childStateExecutionInstance.setDisplayName(initialStateName);
            childStateExecutionInstance.setStateName(initialStateName);
          }
          triggerExecution(sm, childStateExecutionInstance);
        }
      }
    }
  }

  /**
   * @param stateExecutionInstance
   * @param childStateMachineId
   * @param nextState              @return
   */
  private StateExecutionInstance clone(
      StateExecutionInstance stateExecutionInstance, String childStateMachineId, State nextState) {
    StateExecutionInstance cloned = clone(stateExecutionInstance, nextState);
    cloned.setChildStateMachineId(childStateMachineId);
    return cloned;
  }

  /**
   * @param stateExecutionInstance
   * @param nextState              @return
   */
  private StateExecutionInstance clone(StateExecutionInstance stateExecutionInstance, State nextState) {
    StateExecutionInstance cloned = KryoUtils.clone(stateExecutionInstance);
    cloned.setInterruptHistory(null);
    cloned.setStateExecutionDataHistory(null);
    cloned.setDedicatedInterruptCount(null);
    cloned.setUuid(null);
    cloned.setStateParams(null);
    cloned.setDisplayName(nextState.getName());
    cloned.setStateName(nextState.getName());
    cloned.setPrevInstanceId(stateExecutionInstance.getUuid());
    cloned.setDelegateTaskId(null);
    cloned.setContextTransition(false);
    cloned.setStatus(NEW);
    cloned.setStartTs(null);
    cloned.setEndTs(null);
    cloned.setCreatedAt(0);
    cloned.setLastUpdatedAt(0);
    return cloned;
  }

  private boolean updateStartStatus(StateExecutionInstance stateExecutionInstance, ExecutionStatus status,
      Collection<ExecutionStatus> existingExecutionStatus, ExecutionInterrupt reason) {
    stateExecutionInstance.setStartTs(System.currentTimeMillis());
    return updateStatus(stateExecutionInstance, status, existingExecutionStatus, reason,
        ops -> { ops.set("startTs", stateExecutionInstance.getStartTs()); });
  }

  private boolean updateEndStatus(StateExecutionInstance stateExecutionInstance, ExecutionStatus status,
      Collection<ExecutionStatus> existingExecutionStatus) {
    stateExecutionInstance.setEndTs(System.currentTimeMillis());
    return updateStatus(stateExecutionInstance, status, existingExecutionStatus, null,
        ops -> { ops.set("endTs", stateExecutionInstance.getEndTs()); });
  }

  private boolean updateStatus(StateExecutionInstance stateExecutionInstance, ExecutionStatus status,
      Collection<ExecutionStatus> existingExecutionStatus, ExecutionInterrupt reason) {
    return updateStatus(stateExecutionInstance, status, existingExecutionStatus, reason, null);
  }

  private boolean updateStatus(StateExecutionInstance stateExecutionInstance, ExecutionStatus status,
      Collection<ExecutionStatus> existingExecutionStatus, ExecutionInterrupt reason,
      Consumer<UpdateOperations<StateExecutionInstance>> more) {
    UpdateOperations<StateExecutionInstance> ops =
        wingsPersistence.createUpdateOperations(StateExecutionInstance.class);

    statusUpdateOperation(stateExecutionInstance, status, ops);

    if (more != null) {
      more.accept(ops);
    }

    if (reason != null) {
      if (stateExecutionInstance.getUuid().equals(reason.getStateExecutionInstanceId())) {
        logger.error(
            format("The reason execution interrupt with type %s is already assigned to this execution instance.",
                reason.getExecutionInterruptType().name()),
            new Exception(""));
      } else {
        ops.addToSet("interruptHistory",
            ExecutionInterruptEffect.builder().interruptId(reason.getUuid()).tookEffectAt(new Date()).build());
      }
    }

    Query<StateExecutionInstance> query =
        wingsPersistence.createQuery(StateExecutionInstance.class)
            .filter(StateExecutionInstance.APP_ID_KEY, stateExecutionInstance.getAppId())
            .filter(StateExecutionInstance.ID_KEY, stateExecutionInstance.getUuid())
            .field(StateExecutionInstance.STATUS_KEY)
            .in(existingExecutionStatus);
    UpdateResults updateResult = wingsPersistence.update(query, ops);
    if (updateResult == null || updateResult.getWriteResult() == null || updateResult.getWriteResult().getN() != 1) {
      logger.error("StateExecutionInstance status could not be updated - "
              + "stateExecutionInstance: {},  status: {}, existingExecutionStatus: {}, ",
          stateExecutionInstance.getUuid(), status, existingExecutionStatus);
      return false;
    }

    statusUpdateSubject.fireInform(StateStatusUpdate::stateExecutionStatusUpdated, stateExecutionInstance.getAppId(),
        stateExecutionInstance.getExecutionUuid(), stateExecutionInstance.getUuid(), status);
    return true;
  }

  private void statusUpdateOperation(StateExecutionInstance stateExecutionInstance, ExecutionStatus status,
      UpdateOperations<StateExecutionInstance> ops) {
    stateExecutionInstance.setStatus(status);
    ops.set(StateExecutionInstance.STATUS_KEY, stateExecutionInstance.getStatus());

    stateExecutionInstance.setDedicatedInterruptCount(
        workflowExecutionService.getExecutionInterruptCount(stateExecutionInstance.getUuid()));
    ops.set(StateExecutionInstance.DEDICATED_INTERRUPT_COUNT_KEY, stateExecutionInstance.getDedicatedInterruptCount());

    if (isFinalStatus(status)) {
      stateExecutionInstance.setEndTs(System.currentTimeMillis());
      ops.set("endTs", stateExecutionInstance.getEndTs());
    }
  }

  private boolean updateStateExecutionData(StateExecutionInstance stateExecutionInstance,
      StateExecutionData stateExecutionData, ExecutionStatus status, String errorMsg, List<ContextElement> elements,
      List<ContextElement> notifyElements) {
    return updateStateExecutionData(
        stateExecutionInstance, stateExecutionData, status, errorMsg, null, elements, notifyElements, null);
  }

  private boolean updateStateExecutionData(StateExecutionInstance stateExecutionInstance,
      StateExecutionData stateExecutionData, ExecutionStatus status, String errorMsg, List<ContextElement> elements,
      List<ContextElement> notifyElements, String delegateTaskId) {
    return updateStateExecutionData(
        stateExecutionInstance, stateExecutionData, status, errorMsg, null, elements, notifyElements, delegateTaskId);
  }

  private boolean updateStateExecutionData(StateExecutionInstance stateExecutionInstance,
      StateExecutionData stateExecutionData, ExecutionStatus status, String errorMsg,
      Collection<ExecutionStatus> runningStatusLists, List<ContextElement> contextElements,
      List<ContextElement> notifyElements, String delegateTaskId) {
    Map<String, StateExecutionData> stateExecutionMap = stateExecutionInstance.getStateExecutionMap();
    if (stateExecutionMap == null) {
      stateExecutionMap = new HashMap<>();
      stateExecutionInstance.setStateExecutionMap(stateExecutionMap);
    }

    if (stateExecutionData == null) {
      stateExecutionData = stateExecutionMap.get(stateExecutionInstance.getDisplayName());
      if (stateExecutionData == null) {
        stateExecutionData = new StateExecutionData();
      }
    }

    stateExecutionData.setStateName(stateExecutionInstance.getDisplayName());
    stateExecutionData.setStateType(stateExecutionInstance.getStateType());
    stateExecutionMap.put(stateExecutionInstance.getDisplayName(), stateExecutionData);

    UpdateOperations<StateExecutionInstance> ops =
        wingsPersistence.createUpdateOperations(StateExecutionInstance.class);

    statusUpdateOperation(stateExecutionInstance, status, ops);

    if (isNotEmpty(contextElements)) {
      contextElements.forEach(contextElement -> stateExecutionInstance.getContextElements().push(contextElement));
      ops.set("contextElements", stateExecutionInstance.getContextElements());
    }

    if (isNotEmpty(notifyElements)) {
      if (stateExecutionInstance.getNotifyElements() == null) {
        stateExecutionInstance.setNotifyElements(new ArrayList<>());
      }
      stateExecutionInstance.getNotifyElements().addAll(notifyElements);
      ops.set("notifyElements", stateExecutionInstance.getNotifyElements());
    }

    stateExecutionData.setElement(stateExecutionInstance.getContextElement());
    stateExecutionData.setStartTs(stateExecutionInstance.getStartTs());
    if (stateExecutionInstance.getEndTs() != null) {
      stateExecutionData.setEndTs(stateExecutionInstance.getEndTs());
    }
    stateExecutionData.setStatus(stateExecutionInstance.getStatus());
    if (errorMsg != null) {
      stateExecutionData.setErrorMsg(errorMsg);
    }
    stateExecutionData.setStateParams(stateExecutionInstance.getStateParams());

    if (isEmpty(runningStatusLists)) {
      runningStatusLists = activeStatuses();
    }

    if (isNotBlank(delegateTaskId)) {
      ops.set("delegateTaskId", delegateTaskId);
    }

    Query<StateExecutionInstance> query = wingsPersistence.createQuery(StateExecutionInstance.class)
                                              .filter("appId", stateExecutionInstance.getAppId())
                                              .filter(ID_KEY, stateExecutionInstance.getUuid())
                                              .field(StateExecutionInstance.STATUS_KEY)
                                              .in(runningStatusLists);

    ops.set("stateExecutionMap", stateExecutionInstance.getStateExecutionMap());

    UpdateResults updateResult = wingsPersistence.update(query, ops);
    if (updateResult == null || updateResult.getWriteResult() == null || updateResult.getWriteResult().getN() != 1) {
      logger.warn("StateExecutionInstance status could not be updated -"
              + " stateExecutionInstance: {}, stateExecutionData: {}, status: {}, errorMsg: {}, ",
          stateExecutionInstance.getUuid(), stateExecutionData, status, errorMsg);

      return false;
    }

    statusUpdateSubject.fireInform(StateStatusUpdate::stateExecutionStatusUpdated, stateExecutionInstance.getAppId(),
        stateExecutionInstance.getExecutionUuid(), stateExecutionInstance.getUuid(), status);
    return true;
  }

  /**
   * Resumes execution of a StateMachineInstance.
   *
   * @param appId                    the app id
   * @param stateExecutionInstanceId stateMachineInstance to resume.
   * @param response                 map of responses from state machine instances this state was waiting on.
   */
  public void resume(String appId, String executionUuid, String stateExecutionInstanceId,
      Map<String, ResponseData> response, boolean asyncError) {
    StateExecutionInstance stateExecutionInstance =
        getStateExecutionInstance(appId, executionUuid, stateExecutionInstanceId);
    StateMachine sm =
        wingsPersistence.get(StateMachine.class, appId, stateExecutionInstance.getStateMachineId(), CRITICAL);
    State currentState =
        sm.getState(stateExecutionInstance.getChildStateMachineId(), stateExecutionInstance.getStateName());
    injector.injectMembers(currentState);

    while (stateExecutionInstance.getStatus() == NEW || stateExecutionInstance.getStatus() == QUEUED
        || stateExecutionInstance.getStatus() == STARTING) {
      logger.warn("stateExecutionInstance: {} status {} is not in RUNNING state yet", stateExecutionInstance.getUuid(),
          stateExecutionInstance.getStatus());
      // TODO - more elegant way
      quietSleep(Duration.ofMillis(500));
      stateExecutionInstance = getStateExecutionInstance(appId, executionUuid, stateExecutionInstanceId);
    }
    if (stateExecutionInstance.getStatus() != RUNNING && stateExecutionInstance.getStatus() != PAUSED
        && stateExecutionInstance.getStatus() != DISCONTINUING) {
      logger.warn("stateExecutionInstance: {} status {} is no longer in RUNNING/PAUSED/DISCONTINUING state",
          stateExecutionInstance.getUuid(), stateExecutionInstance.getStatus());
      return;
    }
    ExecutionContextImpl context = new ExecutionContextImpl(stateExecutionInstance, sm, injector);
    injector.injectMembers(context);
    executorService.execute(new SmExecutionAsyncResumer(context, currentState, response, this, asyncError));
  }

  /**
   * Handle event.
   *
   * @param workflowExecutionInterrupt the workflow execution event
   */
  public void handleInterrupt(ExecutionInterrupt workflowExecutionInterrupt) {
    WorkflowExecution workflowExecution = wingsPersistence.get(WorkflowExecution.class,
        workflowExecutionInterrupt.getAppId(), workflowExecutionInterrupt.getExecutionUuid(), CRITICAL);

    final ExecutionInterruptType type = workflowExecutionInterrupt.getExecutionInterruptType();
    switch (type) {
      case IGNORE: {
        StateExecutionInstance stateExecutionInstance = getStateExecutionInstance(workflowExecutionInterrupt.getAppId(),
            workflowExecutionInterrupt.getExecutionUuid(), workflowExecutionInterrupt.getStateExecutionInstanceId());

        updateStatus(stateExecutionInstance, FAILED, Lists.newArrayList(WAITING), null);

        StateMachine sm = wingsPersistence.get(StateMachine.class, workflowExecutionInterrupt.getAppId(),
            stateExecutionInstance.getStateMachineId(), CRITICAL);

        State currentState =
            sm.getState(stateExecutionInstance.getChildStateMachineId(), stateExecutionInstance.getStateName());
        injector.injectMembers(currentState);

        ExecutionContextImpl context = new ExecutionContextImpl(stateExecutionInstance, sm, injector);
        injector.injectMembers(context);
        successTransition(context);
        break;
      }

      case RESUME:
      case MARK_SUCCESS: {
        ExecutionContextImpl context = getExecutionContext(workflowExecutionInterrupt);
        executorService.execute(new SmExecutionResumer(context, this, SUCCESS));
        break;
      }

      case RETRY: {
        StateExecutionInstance stateExecutionInstance = getStateExecutionInstance(workflowExecutionInterrupt.getAppId(),
            workflowExecutionInterrupt.getExecutionUuid(), workflowExecutionInterrupt.getStateExecutionInstanceId());

        retryStateExecutionInstance(stateExecutionInstance, workflowExecutionInterrupt.getProperties());
        break;
      }

      case MARK_EXPIRED:
      case ABORT: {
        ExecutionContextImpl context = getExecutionContext(workflowExecutionInterrupt);
        discontinueExecution(context, type);
        break;
      }
      case ABORT_ALL: {
        abortInstancesByStatus(workflowExecutionInterrupt, workflowExecution,
            EnumSet.<ExecutionStatus>of(NEW, QUEUED, RUNNING, STARTING, PAUSED, PAUSING, WAITING));
        break;
      }
      case END_EXECUTION:
      case ROLLBACK: {
        endExecution(workflowExecutionInterrupt, workflowExecution);
        break;
      }
      case PAUSE_ALL: {
        break;
      }
      case RESUME_ALL: {
        break;
      }
      default:
        unhandled(type);
    }
  }

  private ExecutionContextImpl getExecutionContext(ExecutionInterrupt workflowExecutionInterrupt) {
    return getExecutionContext(workflowExecutionInterrupt.getAppId(), workflowExecutionInterrupt.getExecutionUuid(),
        workflowExecutionInterrupt.getStateExecutionInstanceId());
  }

  public ExecutionContextImpl getExecutionContext(String appId, String executionUuid, String stateExecutionInstanceId) {
    StateExecutionInstance stateExecutionInstance =
        getStateExecutionInstance(appId, executionUuid, stateExecutionInstanceId);
    if (stateExecutionInstance == null) {
      logger.info("could not find state execution for app {}, workflow execution {}, uuid {}", appId, executionUuid,
          stateExecutionInstanceId);
      return null;
    }
    StateMachine sm =
        wingsPersistence.get(StateMachine.class, appId, stateExecutionInstance.getStateMachineId(), NORMAL);

    return new ExecutionContextImpl(stateExecutionInstance, sm, injector);
  }

  private void endExecution(ExecutionInterrupt workflowExecutionInterrupt, WorkflowExecution workflowExecution) {
    abortInstancesByStatus(workflowExecutionInterrupt, workflowExecution,
        EnumSet.<ExecutionStatus>of(NEW, QUEUED, RUNNING, STARTING, PAUSED, PAUSING));

    List<StateExecutionInstance> allStateExecutionInstances =
        wingsPersistence.createQuery(StateExecutionInstance.class)
            .filter(StateExecutionInstance.APP_ID_KEY, workflowExecutionInterrupt.getAppId())
            .filter(StateExecutionInstance.EXECUTION_UUID_KEY, workflowExecutionInterrupt.getExecutionUuid())
            .filter(StateExecutionInstance.STATUS_KEY, WAITING)
            .field(StateExecutionInstance.CREATED_AT_KEY)
            .greaterThanOrEq(workflowExecution.getCreatedAt())
            .asList();

    for (StateExecutionInstance stateExecutionInstance : allStateExecutionInstances) {
      StateMachine sm = wingsPersistence.get(StateMachine.class, workflowExecutionInterrupt.getAppId(),
          stateExecutionInstance.getStateMachineId(), CRITICAL);
      ExecutionContextImpl context = new ExecutionContextImpl(stateExecutionInstance, sm, injector);
      injector.injectMembers(context);
      updateStateExecutionData(stateExecutionInstance, null, FAILED, null, asList(WAITING), null, null, null);
      endTransition(context, stateExecutionInstance, FAILED, null);
    }
  }

  private void abortInstancesByStatus(ExecutionInterrupt workflowExecutionInterrupt,
      WorkflowExecution workflowExecution, Collection<ExecutionStatus> statuses) {
    if (!markAbortingState(workflowExecutionInterrupt, workflowExecution, statuses)) {
      return;
    }

    List<StateExecutionInstance> allStateExecutionInstances =
        wingsPersistence.createQuery(StateExecutionInstance.class)
            .filter(StateExecutionInstance.APP_ID_KEY, workflowExecutionInterrupt.getAppId())
            .filter(StateExecutionInstance.EXECUTION_UUID_KEY, workflowExecutionInterrupt.getExecutionUuid())
            .filter(StateExecutionInstance.STATUS_KEY, DISCONTINUING)
            .field(StateExecutionInstance.CREATED_AT_KEY)
            .greaterThanOrEq(workflowExecution.getCreatedAt())
            .asList();

    if (isEmpty(allStateExecutionInstances)) {
      logger.warn(
          "ABORT_ALL workflowExecutionInterrupt: {} being ignored as no running instance found for executionUuid: {}",
          workflowExecutionInterrupt.getUuid(), workflowExecutionInterrupt.getExecutionUuid());
      return;
    }

    for (StateExecutionInstance stateExecutionInstance : allStateExecutionInstances) {
      StateMachine sm = wingsPersistence.get(StateMachine.class, workflowExecutionInterrupt.getAppId(),
          stateExecutionInstance.getStateMachineId(), CRITICAL);
      ExecutionContextImpl context = new ExecutionContextImpl(stateExecutionInstance, sm, injector);
      injector.injectMembers(context);
      discontinueMarkedInstance(context, stateExecutionInstance, ABORTED);
    }
  }

  private void retryStateExecutionInstance(
      StateExecutionInstance stateExecutionInstance, Map<String, Object> stateParams) {
    clearStateExecutionData(stateExecutionInstance, stateParams);
    stateExecutionInstance = getStateExecutionInstance(
        stateExecutionInstance.getAppId(), stateExecutionInstance.getExecutionUuid(), stateExecutionInstance.getUuid());
    StateMachine sm = wingsPersistence.get(
        StateMachine.class, stateExecutionInstance.getAppId(), stateExecutionInstance.getStateMachineId(), CRITICAL);

    State currentState =
        sm.getState(stateExecutionInstance.getChildStateMachineId(), stateExecutionInstance.getStateName());
    injector.injectMembers(currentState);

    ExecutionContextImpl context = new ExecutionContextImpl(stateExecutionInstance, sm, injector);
    injector.injectMembers(context);
    executorService.execute(new SmExecutionDispatcher(context, this));
  }

  protected void clearStateExecutionData(
      StateExecutionInstance stateExecutionInstance, Map<String, Object> stateParams) {
    Map<String, StateExecutionData> stateExecutionMap = stateExecutionInstance.getStateExecutionMap();
    if (stateExecutionMap == null) {
      return;
    }

    UpdateOperations<StateExecutionInstance> ops =
        wingsPersistence.createUpdateOperations(StateExecutionInstance.class);

    StateExecutionData stateExecutionData = stateExecutionMap.get(stateExecutionInstance.getDisplayName());
    ops.addToSet("stateExecutionDataHistory", stateExecutionData);
    stateExecutionInstance.getStateExecutionDataHistory().add(stateExecutionData);

    stateExecutionMap.remove(stateExecutionInstance.getDisplayName());
    ops.set("stateExecutionMap", stateExecutionMap);

    List<ContextElement> notifyElements = new ArrayList<>();
    final String prevInstanceId = stateExecutionInstance.getPrevInstanceId();
    if (prevInstanceId != null) {
      final StateExecutionInstance prevStateExecutionInstance =
          wingsPersistence.get(StateExecutionInstance.class, prevInstanceId);
      Preconditions.checkNotNull(prevStateExecutionInstance);
      if (prevStateExecutionInstance.getNotifyElements() != null) {
        notifyElements = prevStateExecutionInstance.getNotifyElements();
      }
    }
    ops.set("notifyElements", notifyElements);

    if (stateParams != null) {
      ops.set("stateParams", stateParams);
    }

    if (stateExecutionInstance.getEndTs() != null) {
      stateExecutionInstance.setEndTs(null);
      ops.unset("endTs");
    }
    ops.set(StateExecutionInstance.STATUS_KEY, NEW);

    Query<StateExecutionInstance> query = wingsPersistence.createQuery(StateExecutionInstance.class)
                                              .filter("appId", stateExecutionInstance.getAppId())
                                              .filter(ID_KEY, stateExecutionInstance.getUuid())
                                              .field(StateExecutionInstance.STATUS_KEY)
                                              .in(asList(WAITING, FAILED, ERROR));

    UpdateResults updateResult = wingsPersistence.update(query, ops);
    if (updateResult == null || updateResult.getWriteResult() == null || updateResult.getWriteResult().getN() != 1) {
      throw new WingsException(ErrorCode.RETRY_FAILED).addParam("displayName", stateExecutionInstance.getDisplayName());
    }
    statusUpdateSubject.fireInform(StateStatusUpdate::stateExecutionStatusUpdated, stateExecutionInstance.getAppId(),
        stateExecutionInstance.getExecutionUuid(), stateExecutionInstance.getUuid(), NEW);
  }

  private boolean markAbortingState(@NotNull ExecutionInterrupt workflowExecutionInterrupt,
      WorkflowExecution workflowExecution, Collection<ExecutionStatus> statuses) {
    // Get all that are eligible for discontinuing

    List<StateExecutionInstance> allStateExecutionInstances =
        wingsPersistence.createQuery(StateExecutionInstance.class)
            .filter(StateExecutionInstance.APP_ID_KEY, workflowExecutionInterrupt.getAppId())
            .filter(StateExecutionInstance.EXECUTION_UUID_KEY, workflowExecutionInterrupt.getExecutionUuid())
            .field(StateExecutionInstance.STATUS_KEY)
            .in(statuses)
            .field(StateExecutionInstance.CREATED_AT_KEY)
            .greaterThanOrEq(workflowExecution.getCreatedAt())
            .project(StateExecutionInstance.ID_KEY, true)
            .project(StateExecutionInstance.STATE_TYPE_KEY, true)
            .asList();

    if (isEmpty(allStateExecutionInstances)) {
      logger.warn("No stateExecutionInstance could be marked as DISCONTINUING - appId: {}, executionUuid: {}",
          workflowExecutionInterrupt.getAppId(), workflowExecutionInterrupt.getExecutionUuid());
      return false;
    }
    List<String> leafInstanceIds =
        getAllLeafInstanceIds(workflowExecutionInterrupt, workflowExecution, allStateExecutionInstances);

    UpdateOperations<StateExecutionInstance> ops =
        wingsPersistence.createUpdateOperations(StateExecutionInstance.class);

    ops.set(StateExecutionInstance.STATUS_KEY, DISCONTINUING);

    ops.addToSet(StateExecutionInstance.INTERRUPT_HISTORY_KEY,
        ExecutionInterruptEffect.builder()
            .interruptId(workflowExecutionInterrupt.getUuid())
            .tookEffectAt(new Date())
            .build());

    Query<StateExecutionInstance> query =
        wingsPersistence.createQuery(StateExecutionInstance.class)
            .filter(StateExecutionInstance.APP_ID_KEY, workflowExecutionInterrupt.getAppId())
            .filter(StateExecutionInstance.EXECUTION_UUID_KEY, workflowExecutionInterrupt.getExecutionUuid())
            .field(StateExecutionInstance.ID_KEY)
            .in(leafInstanceIds);
    // Set the status to DISCONTINUING
    UpdateResults updateResult = wingsPersistence.update(query, ops);
    if (updateResult == null || updateResult.getWriteResult() == null || updateResult.getWriteResult().getN() == 0) {
      logger.warn("No stateExecutionInstance could be marked as DISCONTINUING - appId: {}, executionUuid: {}",
          workflowExecutionInterrupt.getAppId(), workflowExecutionInterrupt.getExecutionUuid());
      return false;
    }
    return true;
  }

  private List<String> getAllLeafInstanceIds(ExecutionInterrupt workflowExecutionInterrupt,
      WorkflowExecution workflowExecution, List<StateExecutionInstance> stateExecutionInstances) {
    List<String> allInstanceIds =
        stateExecutionInstances.stream().map(StateExecutionInstance::getUuid).collect(toList());

    // Get Parent Ids
    List<String> parentInstanceIds =
        stateExecutionInstances.stream()
            .filter(stateExecutionInstance
                -> stateExecutionInstance.getStateType().equals(StateType.REPEAT.name())
                    || stateExecutionInstance.getStateType().equals(StateType.FORK.name())
                    || stateExecutionInstance.getStateType().equals(StateType.PHASE_STEP.name())
                    || stateExecutionInstance.getStateType().equals(StateType.PHASE.name())
                    || stateExecutionInstance.getStateType().equals(StateType.SUB_WORKFLOW.name()))
            .map(StateExecutionInstance::getUuid)
            .collect(toList());

    if (isEmpty(parentInstanceIds)) {
      return allInstanceIds;
    }

    List<StateExecutionInstance> childInstances =
        wingsPersistence.createQuery(StateExecutionInstance.class)
            .filter(StateExecutionInstance.APP_ID_KEY, workflowExecutionInterrupt.getAppId())
            .filter(StateExecutionInstance.EXECUTION_UUID_KEY, workflowExecutionInterrupt.getExecutionUuid())
            .field(StateExecutionInstance.PARENT_INSTANCE_ID_KEY)
            .in(parentInstanceIds)
            .field(StateExecutionInstance.STATUS_KEY)
            .in(EnumSet.<ExecutionStatus>of(NEW, QUEUED, STARTING, RUNNING, PAUSED, PAUSING, WAITING))
            .field(StateExecutionInstance.CREATED_AT_KEY)
            .greaterThanOrEq(workflowExecution.getCreatedAt())
            .asList();

    // get distinct parent Ids
    List<String> parentIdsHavingChildren =
        childInstances.stream().map(StateExecutionInstance::getParentInstanceId).distinct().collect(toList());

    // parent with no children
    allInstanceIds.removeAll(parentIdsHavingChildren);

    // Mark aborting
    return allInstanceIds;
  }

  private StateExecutionInstance getStateExecutionInstance(
      String appId, String executionUuid, String stateExecutionInstanceId) {
    // TODO: convert this not to use Query directly after default createdAt sorting is taken off
    Query<StateExecutionInstance> query = wingsPersistence.createQuery(StateExecutionInstance.class, CRITICAL)
                                              .filter(ID_KEY, stateExecutionInstanceId)
                                              .filter(StateExecutionInstance.APP_ID_KEY, appId);
    if (executionUuid != null) {
      query.filter("executionUuid", executionUuid);
    }
    return query.get();
  }

  public void addContext(ExecutionContextImpl context, WingsException exception) {
    if (context == null) {
      return;
    }

    if (context.getAppId() != null) {
      final String accountId = appService.getAccountIdByAppId(context.getAppId());
      exception.addContext(Account.class, accountId);
      exception.addContext(Application.class, context.getAppId());
    }
    if (context.getEnv() != null) {
      exception.addContext(Environment.class, context.getEnv().getUuid());
    }
    exception.addContext(WorkflowExecution.class, context.getWorkflowExecutionId());

    if (context.getStateExecutionInstance() != null) {
      exception.addContext(StateExecutionInstance.class, context.getStateExecutionInstance().getUuid());
    }
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
    SmExecutionDispatcher(ExecutionContextImpl context, StateMachineExecutor stateMachineExecutor) {
      this.context = context;
      this.stateMachineExecutor = stateMachineExecutor;
    }

    /* (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
      try (ExecutionLogContext ctx = new ExecutionLogContext(context.getWorkflowExecutionId())) {
        stateMachineExecutor.startExecution(context);
      } catch (WingsException exception) {
        stateMachineExecutor.addContext(context, exception);
        WingsExceptionMapper.logProcessedMessages(exception, MANAGER, logger);
      } catch (Exception exception) {
        logger.error("Unhandled exception", exception);
      }
    }
  }

  private static class SmExecutionResumer implements Runnable {
    private ExecutionContextImpl context;
    private StateMachineExecutor stateMachineExecutor;
    private ExecutionStatus status;

    /**
     * Instantiates a new Sm execution dispatcher.
     *
     * @param context              the context
     * @param stateMachineExecutor the state machine executor
     */
    SmExecutionResumer(
        ExecutionContextImpl context, StateMachineExecutor stateMachineExecutor, ExecutionStatus status) {
      this.context = context;
      this.stateMachineExecutor = stateMachineExecutor;
      this.status = status;
    }

    /* (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
      try {
        stateMachineExecutor.handleExecuteResponse(context, anExecutionResponse().withExecutionStatus(status).build());
      } catch (WingsException ex) {
        stateMachineExecutor.handleExecuteResponseException(context, ex);
      } catch (Exception ex) {
        stateMachineExecutor.handleExecuteResponseException(context, new WingsException(ex));
      }
    }
  }

  private static class SmExecutionAsyncResumer implements Runnable {
    private ExecutionContextImpl context;
    private StateMachineExecutor stateMachineExecutor;
    private State state;
    private Map<String, ResponseData> response;
    private boolean asyncError;

    /**
     * Instantiates a new Sm execution dispatcher.
     *
     * @param context              the context
     * @param state                the state
     * @param response             the response
     * @param stateMachineExecutor the state machine executor
     */
    SmExecutionAsyncResumer(ExecutionContextImpl context, State state, Map<String, ResponseData> response,
        StateMachineExecutor stateMachineExecutor, boolean asyncError) {
      this.context = context;
      this.state = state;
      this.response = response;
      this.stateMachineExecutor = stateMachineExecutor;
      this.asyncError = asyncError;
    }

    /* (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
      try (ExecutionLogContext ctx = new ExecutionLogContext(context.getWorkflowExecutionId())) {
        if (asyncError) {
          StateExecutionData stateExecutionData = context.getStateExecutionInstance().getStateExecutionData();
          ErrorNotifyResponseData errorNotifyResponseData =
              (ErrorNotifyResponseData) response.values().iterator().next();
          stateExecutionData.setErrorMsg(errorNotifyResponseData.getErrorMessage());
          stateExecutionData.setStatus(ERROR);
          stateMachineExecutor.handleExecuteResponse(context,
              anExecutionResponse()
                  .withExecutionStatus(ERROR)
                  .withStateExecutionData(stateExecutionData)
                  .withErrorMessage(errorNotifyResponseData.getErrorMessage())
                  .build());
          return;
        }

        StateExecutionInstance stateExecutionInstance = context.getStateExecutionInstance();
        if (stateExecutionInstance.getStateParams() != null) {
          MapperUtils.mapObject(stateExecutionInstance.getStateParams(), state);
        }
        ExecutionResponse executionResponse = state.handleAsyncResponse(context, response);
        stateMachineExecutor.handleExecuteResponse(context, executionResponse);
      } catch (WingsException ex) {
        stateMachineExecutor.handleExecuteResponseException(context, ex);
      } catch (Exception ex) {
        stateMachineExecutor.handleExecuteResponseException(context, new WingsException(ex));
      }
    }
  }
}
