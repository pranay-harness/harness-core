package software.wings.sm;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.ErrorCode.INVALID_ARGUMENT;
import static software.wings.beans.ReadPref.CRITICAL;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.SearchFilter.Operator.GT;
import static software.wings.beans.alert.AlertType.ManualInterventionNeeded;
import static software.wings.dl.PageRequest.Builder.aPageRequest;
import static software.wings.sm.ElementNotifyResponseData.Builder.anElementNotifyResponseData;
import static software.wings.sm.ExecutionInterruptType.PAUSE_ALL;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;
import static software.wings.sm.ExecutionStatus.ABORTED;
import static software.wings.sm.ExecutionStatus.ABORTING;
import static software.wings.sm.ExecutionStatus.ERROR;
import static software.wings.sm.ExecutionStatus.FAILED;
import static software.wings.sm.ExecutionStatus.NEW;
import static software.wings.sm.ExecutionStatus.PAUSED;
import static software.wings.sm.ExecutionStatus.PAUSING;
import static software.wings.sm.ExecutionStatus.QUEUED;
import static software.wings.sm.ExecutionStatus.RUNNING;
import static software.wings.sm.ExecutionStatus.STARTING;
import static software.wings.sm.ExecutionStatus.SUCCESS;
import static software.wings.sm.ExecutionStatus.WAITING;
import static software.wings.sm.StateExecutionData.StateExecutionDataBuilder.aStateExecutionData;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import org.apache.commons.collections.CollectionUtils;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateResults;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Application;
import software.wings.beans.ErrorCode;
import software.wings.beans.ErrorStrategy;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.alert.ManualInterventionNeededAlert;
import software.wings.common.Constants;
import software.wings.common.UUIDGenerator;
import software.wings.common.cache.ResponseCodeCache;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsDeque;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.scheduler.NotifyJob;
import software.wings.scheduler.QuartzScheduler;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.DelegateService;
import software.wings.sm.ExecutionEvent.ExecutionEventBuilder;
import software.wings.utils.KryoUtils;
import software.wings.utils.MapperUtils;
import software.wings.utils.Misc;
import software.wings.waitnotify.ErrorNotifyResponseData;
import software.wings.waitnotify.NotifyCallback;
import software.wings.waitnotify.NotifyResponseData;
import software.wings.waitnotify.WaitNotifyEngine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

/**
 * Class responsible for executing state machine.
 *
 * @author Rishi
 */
@Singleton
public class StateMachineExecutor {
  private static final Logger logger = LoggerFactory.getLogger(StateMachineExecutor.class);
  @Inject private ExecutorService executorService;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject private Injector injector;
  @Inject private ExecutionInterruptManager executionInterruptManager;
  @Inject @Named("JobScheduler") private QuartzScheduler jobScheduler;
  @Inject private DelegateService delegateService;
  @Inject private AlertService alertService;

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
      throw new WingsException(INVALID_ARGUMENT);
    }

    StateExecutionInstance stateExecutionInstance = new StateExecutionInstance();
    stateExecutionInstance.setExecutionName(executionName);
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
  public StateExecutionInstance queue(StateMachine stateMachine, StateExecutionInstance stateExecutionInstance) {
    if (stateExecutionInstance == null) {
      throw new WingsException(INVALID_ARGUMENT).addParam(ErrorCode.ARGS_NAME, "stateExecutionInstance");
    }
    if (stateMachine == null) {
      throw new WingsException(INVALID_ARGUMENT).addParam(ErrorCode.ARGS_NAME, "rootStateMachine");
    }
    if (stateExecutionInstance.getChildStateMachineId() != null
        && !stateExecutionInstance.getChildStateMachineId().equals(stateMachine.getUuid())
        && stateMachine.getChildStateMachines().get(stateExecutionInstance.getChildStateMachineId()) == null) {
      throw new WingsException(INVALID_ARGUMENT).addParam(ErrorCode.ARGS_NAME, "stateMachine");
    }
    StateMachine sm;
    if (stateExecutionInstance.getChildStateMachineId() == null) {
      sm = stateMachine;
    } else {
      sm = stateMachine.getChildStateMachines().get(stateExecutionInstance.getChildStateMachineId());
    }
    if (stateExecutionInstance.getStateName() == null) {
      stateExecutionInstance.setStateName(sm.getInitialStateName());
    }
    return saveStateExecutionInstance(stateMachine, stateExecutionInstance);
  }

  private StateExecutionInstance saveStateExecutionInstance(
      StateMachine stateMachine, StateExecutionInstance stateExecutionInstance) {
    if (stateExecutionInstance.getStateName() == null) {
      throw new WingsException(INVALID_ARGUMENT).addParam(ErrorCode.ARGS_NAME, "stateName");
    }

    stateExecutionInstance.setAppId(stateMachine.getAppId());
    stateExecutionInstance.setStateMachineId(stateMachine.getUuid());
    State state =
        stateMachine.getState(stateExecutionInstance.getChildStateMachineId(), stateExecutionInstance.getStateName());
    stateExecutionInstance.setRollback(state.isRollback());
    stateExecutionInstance.setStateType(state.getStateType());
    if (stateExecutionInstance.getUuid() != null) {
      throw new WingsException(ErrorCode.INVALID_REQUEST)
          .addParam("message", "StateExecutionInstance was already created");
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

    List<ExecutionInterrupt> executionInterrupts = executionInterruptManager.checkForExecutionInterrupt(
        stateExecutionInstance.getAppId(), stateExecutionInstance.getExecutionUuid());
    if (executionInterrupts != null) {
      Optional<ExecutionInterrupt> pauseAll =
          executionInterrupts.stream()
              .filter(ei -> ei != null && ei.getExecutionInterruptType() == PAUSE_ALL)
              .findFirst();
      if (pauseAll.isPresent()) {
        updateStatus(stateExecutionInstance, PAUSED, Lists.newArrayList(NEW, QUEUED));
        waitNotifyEngine.waitForAll(new ExecutionResumeAllCallback(stateExecutionInstance.getAppId(),
                                        stateExecutionInstance.getExecutionUuid(), stateExecutionInstance.getUuid()),
            pauseAll.get().getUuid());
        return;
      }
    }

    StateMachine stateMachine = context.getStateMachine();
    boolean updated =
        updateStartStatus(stateExecutionInstance, STARTING, Lists.newArrayList(NEW, QUEUED, PAUSED, WAITING));
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
      String resumeId = scheduleWaitNotify(currentState.getWaitInterval());
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
    Exception ex = null;
    try {
      StateMachine stateMachine = context.getStateMachine();
      State currentState =
          stateMachine.getState(stateExecutionInstance.getChildStateMachineId(), stateExecutionInstance.getStateName());
      if (stateExecutionInstance.getStateParams() != null) {
        MapperUtils.mapObject(stateExecutionInstance.getStateParams(), currentState);
      }
      injector.injectMembers(currentState);
      invokeAdvisors(context, currentState);
      executionResponse = currentState.execute(context);
    } catch (Exception exception) {
      logger.warn(
          "Error in {} execution: {}", stateExecutionInstance.getStateName(), Misc.getMessage(exception), exception);
      ex = exception;
    }

    if (ex == null) {
      handleExecuteResponse(context, executionResponse);
    } else {
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
        waitNotifyEngine.waitForAll(callback,
            executionResponse.getCorrelationIds().toArray(new String[executionResponse.getCorrelationIds().size()]));
      }

      boolean updated = updateStateExecutionData(stateExecutionInstance, executionResponse.getStateExecutionData(),
          status, null, executionResponse.getContextElements(), executionResponse.getNotifyElements(),
          executionResponse.getDelegateTaskId());
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
      } else if (status == SUCCESS) {
        return successTransition(context);
      } else if (status == FAILED || status == ERROR) {
        return failedTransition(context, null);
      } else if (status == ABORTED) {
        endTransition(context, stateExecutionInstance, ABORTED, null);
      }
    }
    return stateExecutionInstance;
  }

  private StateExecutionInstance reloadStateExecutionInstanceAndCheckStatus(
      StateExecutionInstance stateExecutionInstance) {
    String stateExecutionInstanceId = stateExecutionInstance.getUuid();
    stateExecutionInstance = getStateExecutionInstance(
        stateExecutionInstance.getAppId(), stateExecutionInstance.getExecutionUuid(), stateExecutionInstanceId);
    if (stateExecutionInstance.getStatus().isFinalStatus()) {
      logger.debug("StateExecutionInstance already reached the final status. Skipping the update for "
          + stateExecutionInstanceId);
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
        UpdateOperations<StateExecutionInstance> ops =
            wingsPersistence.createUpdateOperations(StateExecutionInstance.class);
        ops.set("status", WAITING);

        if (executionEventAdvice.getStateParams() != null) {
          ops.set("stateParams", executionEventAdvice.getStateParams());
        }

        List<ExecutionStatus> existingExecutionStatus = asList(FAILED);
        Query<StateExecutionInstance> query = wingsPersistence.createQuery(StateExecutionInstance.class)
                                                  .field("appId")
                                                  .equal(stateExecutionInstance.getAppId())
                                                  .field(ID_KEY)
                                                  .equal(stateExecutionInstance.getUuid())
                                                  .field("status")
                                                  .in(existingExecutionStatus);
        UpdateResults updateResult = wingsPersistence.update(query, ops);
        if (updateResult == null || updateResult.getWriteResult() == null
            || updateResult.getWriteResult().getN() != 1) {
          logger.error(
              "StateExecutionInstance status could not be updated- stateExecutionInstance: {}, status: {}, existingExecutionStatus: {}, stateParams: {}",
              stateExecutionInstance.getUuid(), status, existingExecutionStatus, executionEventAdvice.getStateParams());
        }
        // Open an alert
        openAnAlert(context, stateExecutionInstance);

        break;
      }
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
          String resumeId = scheduleWaitNotify(executionEventAdvice.getWaitInterval());
          waitNotifyEngine.waitForAll(new ExecutionWaitRetryCallback(stateExecutionInstance.getAppId(),
                                          stateExecutionInstance.getExecutionUuid(), stateExecutionInstance.getUuid()),
              resumeId);
        } else {
          logger.info("No Retry Wait Interval found");
          retryStateExecutionInstance(stateExecutionInstance, null);
        }
        break;
      }
      case END_EXECUTION: {
        if (!status.isFinalStatus()) {
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
      logger.warn("Failed to open ManualInterventionNeeded alarm for  executionId {} and name ",
          context.getWorkflowExecutionId(), context.getWorkflowExecutionName(), e);
    }
  }

  private String scheduleWaitNotify(int waitInterval) {
    String resumeId = UUIDGenerator.getUuid();
    long wakeupTs = System.currentTimeMillis() + (waitInterval * 1000);
    JobDetail job = JobBuilder.newJob(NotifyJob.class)
                        .withIdentity(resumeId, Constants.WAIT_RESUME_GROUP)
                        .usingJobData("correlationId", resumeId)
                        .usingJobData("executionStatus", SUCCESS.name())
                        .build();
    Trigger trigger =
        TriggerBuilder.newTrigger().withIdentity(resumeId).startAt(new Date(wakeupTs)).forJob(job).build();
    jobScheduler.scheduleJob(job, trigger);

    logger.info("ExecutionWaitCallback job scheduled - waitInterval: {}", waitInterval);
    return resumeId;
  }

  private StateExecutionInstance executionEventAdviceTransition(
      ExecutionContextImpl context, ExecutionEventAdvice executionEventAdvice) {
    StateMachine sm = context.getStateMachine();
    State nextState =
        sm.getState(executionEventAdvice.getNextChildStateMachineId(), executionEventAdvice.getNextStateName());
    StateExecutionInstance cloned =
        clone(context.getStateExecutionInstance(), executionEventAdvice.getNextChildStateMachineId(), nextState);
    return triggerExecution(sm, cloned);
  }

  /**
   * Handle execute response exception state execution instance.
   *
   * @param context   the context
   * @param e the exception
   * @return the state execution instance
   */
  StateExecutionInstance handleExecuteResponseException(ExecutionContextImpl context, Exception e) {
    StateExecutionInstance stateExecutionInstance = context.getStateExecutionInstance();
    StateMachine sm = context.getStateMachine();
    State currentState =
        sm.getState(stateExecutionInstance.getChildStateMachineId(), stateExecutionInstance.getStateName());
    logger.warn("Error seen in the state execution - currentState : {}, stateExecutionInstanceId: {} : {}",
        currentState, stateExecutionInstance.getUuid(), Misc.getMessage(e), e);

    String errorMessage;
    if (e instanceof WingsException) {
      WingsException ex = (WingsException) e;
      errorMessage = Joiner.on(",").join(
          ex.getResponseMessageList()
              .stream()
              .map(responseMessage -> ResponseCodeCache.getInstance().rebuildMessage(responseMessage, ex.getParams()))
              .collect(toList()));
    } else {
      errorMessage = e.getMessage();
    }
    updateStateExecutionData(stateExecutionInstance, null, FAILED, errorMessage, null, null);

    try {
      return failedTransition(context, e);
    } catch (Exception e2) {
      logger.error("Error in transitioning to failure state", e2);
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
        updateStatus(stateExecutionInstance, WAITING, Lists.newArrayList(FAILED));
      } else {
        // TODO: handle more strategy
        logger.info("Unhandled error strategy for the state: {}, stateExecutionInstanceId: {}, errorStrategy: {}"
                + stateExecutionInstance.getStateName(),
            stateExecutionInstance.getUuid(), errorStrategy);
      }
    } else {
      StateExecutionInstance cloned = clone(stateExecutionInstance, nextState);
      return triggerExecution(sm, cloned);
    }
    return null;
  }

  private void endTransition(ExecutionContextImpl context, StateExecutionInstance stateExecutionInstance,
      ExecutionStatus status, Exception exception) {
    StateMachineExecutionCallback callback = stateExecutionInstance.getCallback();
    if (stateExecutionInstance.getNotifyId() == null) {
      if (stateExecutionInstance.getCallback() != null) {
        injector.injectMembers(callback);
        callback.callback(context, status, exception);
      } else {
        logger.info("No callback for the stateMachine: {}, executionUuid: {}", context.getStateMachine().getName(),
            stateExecutionInstance.getExecutionUuid());
      }
    } else {
      notify(stateExecutionInstance, status);
    }
  }

  private void abortExecution(ExecutionContextImpl context) {
    StateExecutionInstance stateExecutionInstance = context.getStateExecutionInstance();
    boolean updated = updateStatus(
        stateExecutionInstance, ABORTING, Lists.newArrayList(NEW, QUEUED, STARTING, RUNNING, PAUSED, WAITING));
    if (!updated) {
      throw new WingsException(ErrorCode.STATE_NOT_FOR_ABORT)
          .addParam("stateName", stateExecutionInstance.getStateName());
    }

    abortMarkedInstance(context, stateExecutionInstance);
  }

  private void abortMarkedInstance(ExecutionContextImpl context, StateExecutionInstance stateExecutionInstance) {
    boolean updated = false;
    StateMachine sm = context.getStateMachine();
    try {
      updated = false;
      State currentState =
          sm.getState(stateExecutionInstance.getChildStateMachineId(), stateExecutionInstance.getStateName());
      injector.injectMembers(currentState);
      if (isNotBlank(stateExecutionInstance.getDelegateTaskId())) {
        delegateService.abortTask(context.getApp().getAccountId(), stateExecutionInstance.getDelegateTaskId());
      }
      if (stateExecutionInstance.getStateParams() != null) {
        MapperUtils.mapObject(stateExecutionInstance.getStateParams(), currentState);
      }
      currentState.handleAbortEvent(context);
      updated =
          updateStateExecutionData(stateExecutionInstance, null, ABORTED, null, asList(ABORTING), null, null, null);
      invokeAdvisors(context, currentState);

      endTransition(context, stateExecutionInstance, ABORTED, null);
    } catch (Exception e) {
      logger.error("Error in aborting", e);
    }
    if (!updated) {
      throw new WingsException(ErrorCode.STATE_ABORT_FAILED)
          .addParam("stateName", stateExecutionInstance.getStateName());
    }
  }

  private void notify(StateExecutionInstance stateExecutionInstance, ExecutionStatus status) {
    ElementNotifyResponseData notifyResponseData = anElementNotifyResponseData().withExecutionStatus(status).build();
    if (stateExecutionInstance.getNotifyElements() != null && !stateExecutionInstance.getNotifyElements().isEmpty()) {
      notifyResponseData.setContextElements(stateExecutionInstance.getNotifyElements());
    }
    waitNotifyEngine.notify(stateExecutionInstance.getNotifyId(), notifyResponseData);
  }

  private void handleSpawningStateExecutionInstances(
      StateMachine sm, StateExecutionInstance stateExecutionInstance, ExecutionResponse executionResponse) {
    if (executionResponse instanceof SpawningExecutionResponse) {
      SpawningExecutionResponse spawningExecutionResponse = (SpawningExecutionResponse) executionResponse;
      if (CollectionUtils.isNotEmpty(spawningExecutionResponse.getStateExecutionInstanceList())) {
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
            childStateExecutionInstance.setStateName(sm.getChildStateMachines()
                                                         .get(childStateExecutionInstance.getChildStateMachineId())
                                                         .getInitialStateName());
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
    cloned.setUuid(null);
    cloned.setStateParams(null);
    cloned.setStateName(nextState.getName());
    cloned.setPrevInstanceId(stateExecutionInstance.getUuid());
    cloned.setContextTransition(false);
    cloned.setStatus(NEW);
    cloned.setStartTs(null);
    cloned.setEndTs(null);
    return cloned;
  }

  private boolean updateStartStatus(StateExecutionInstance stateExecutionInstance, ExecutionStatus status,
      List<ExecutionStatus> existingExecutionStatus) {
    stateExecutionInstance.setStartTs(System.currentTimeMillis());
    return updateStatus(
        stateExecutionInstance, "startTs", stateExecutionInstance.getStartTs(), status, existingExecutionStatus);
  }

  private boolean updateEndStatus(StateExecutionInstance stateExecutionInstance, ExecutionStatus status,
      List<ExecutionStatus> existingExecutionStatus) {
    stateExecutionInstance.setEndTs(System.currentTimeMillis());
    return updateStatus(
        stateExecutionInstance, "endTs", stateExecutionInstance.getStartTs(), status, existingExecutionStatus);
  }

  private boolean updateStatus(StateExecutionInstance stateExecutionInstance, ExecutionStatus status,
      List<ExecutionStatus> existingExecutionStatus) {
    return updateStatus(stateExecutionInstance, null, null, status, existingExecutionStatus);
  }

  private boolean updateStatus(StateExecutionInstance stateExecutionInstance, String tsField, Long tsValue,
      ExecutionStatus status, List<ExecutionStatus> existingExecutionStatus) {
    stateExecutionInstance.setStatus(status);

    UpdateOperations<StateExecutionInstance> ops =
        wingsPersistence.createUpdateOperations(StateExecutionInstance.class);
    ops.set("status", stateExecutionInstance.getStatus());
    if (tsField != null) {
      ops.set(tsField, tsValue);
    }

    Query<StateExecutionInstance> query = wingsPersistence.createQuery(StateExecutionInstance.class)
                                              .field("appId")
                                              .equal(stateExecutionInstance.getAppId())
                                              .field(ID_KEY)
                                              .equal(stateExecutionInstance.getUuid())
                                              .field("status")
                                              .in(existingExecutionStatus);
    UpdateResults updateResult = wingsPersistence.update(query, ops);
    if (updateResult == null || updateResult.getWriteResult() == null || updateResult.getWriteResult().getN() != 1) {
      logger.warn(
          "StateExecutionInstance status could not be updated- stateExecutionInstance: {}, tsField: {}, tsValue: {}, status: {}, existingExecutionStatus: {}, ",
          stateExecutionInstance.getUuid(), tsField, tsValue, status, existingExecutionStatus);
      return false;
    } else {
      return true;
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
      List<ExecutionStatus> runningStatusLists, List<ContextElement> contextElements,
      List<ContextElement> notifyElements, String delegateTaskId) {
    Map<String, StateExecutionData> stateExecutionMap = stateExecutionInstance.getStateExecutionMap();
    if (stateExecutionMap == null) {
      stateExecutionMap = new HashMap<>();
      stateExecutionInstance.setStateExecutionMap(stateExecutionMap);
    }

    if (stateExecutionData == null) {
      stateExecutionData = stateExecutionMap.get(stateExecutionInstance.getStateName());
      if (stateExecutionData == null) {
        stateExecutionData = new StateExecutionData();
      }
    }

    stateExecutionData.setStateName(stateExecutionInstance.getStateName());
    stateExecutionData.setStateType(stateExecutionInstance.getStateType());
    stateExecutionMap.put(stateExecutionInstance.getStateName(), stateExecutionData);

    UpdateOperations<StateExecutionInstance> ops =
        wingsPersistence.createUpdateOperations(StateExecutionInstance.class);

    stateExecutionInstance.setStatus(status);
    ops.set("status", stateExecutionInstance.getStatus());

    if (status == SUCCESS || status == FAILED || status == ERROR || status == ABORTED) {
      stateExecutionInstance.setEndTs(System.currentTimeMillis());
      ops.set("endTs", stateExecutionInstance.getEndTs());
    }

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
      runningStatusLists = asList(NEW, QUEUED, STARTING, RUNNING, PAUSED, WAITING, ABORTING);
    }

    if (isNotBlank(delegateTaskId)) {
      ops.set("delegateTaskId", delegateTaskId);
    }

    Query<StateExecutionInstance> query = wingsPersistence.createQuery(StateExecutionInstance.class)
                                              .field("appId")
                                              .equal(stateExecutionInstance.getAppId())
                                              .field(ID_KEY)
                                              .equal(stateExecutionInstance.getUuid())
                                              .field("status")
                                              .in(runningStatusLists);

    ops.set("stateExecutionMap", stateExecutionInstance.getStateExecutionMap());

    UpdateResults updateResult = wingsPersistence.update(query, ops);
    boolean updated = true;
    if (updateResult == null || updateResult.getWriteResult() == null || updateResult.getWriteResult().getN() != 1) {
      logger.warn(
          "StateExecutionInstance status could not be updated- stateExecutionInstance: {}, stateExecutionData: {}, status: {}, errorMsg: {}, ",
          stateExecutionInstance.getUuid(), stateExecutionData, status, errorMsg);

      updated = false;
    }

    return updated;
  }

  /**
   * Resumes execution of a StateMachineInstance.
   *
   * @param appId                    the app id
   * @param stateExecutionInstanceId stateMachineInstance to resume.
   * @param response                 map of responses from state machine instances this state was waiting on.
   */
  public void resume(String appId, String executionUuid, String stateExecutionInstanceId,
      Map<String, NotifyResponseData> response, boolean asyncError) {
    StateExecutionInstance stateExecutionInstance =
        getStateExecutionInstance(appId, executionUuid, stateExecutionInstanceId);
    StateMachine sm =
        wingsPersistence.get(StateMachine.class, appId, stateExecutionInstance.getStateMachineId(), CRITICAL);
    State currentState =
        sm.getState(stateExecutionInstance.getChildStateMachineId(), stateExecutionInstance.getStateName());
    injector.injectMembers(currentState);

    while (stateExecutionInstance.getStatus() == NEW || stateExecutionInstance.getStatus() == QUEUED
        || stateExecutionInstance.getStatus() == STARTING) {
      logger.warn("stateExecutionInstance: {} status is not in RUNNING state yet", stateExecutionInstance.getUuid());
      // TODO - more elegant way
      Misc.quietSleep(500);
      stateExecutionInstance = getStateExecutionInstance(appId, executionUuid, stateExecutionInstanceId);
    }
    if (stateExecutionInstance.getStatus() != RUNNING && stateExecutionInstance.getStatus() != PAUSED
        && stateExecutionInstance.getStatus() != ABORTING) {
      logger.warn("stateExecutionInstance: " + stateExecutionInstance.getUuid()
          + " status is no longer in RUNNING/PAUSED/ABORTING state");
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

    switch (workflowExecutionInterrupt.getExecutionInterruptType()) {
      case IGNORE: {
        StateExecutionInstance stateExecutionInstance = getStateExecutionInstance(workflowExecutionInterrupt.getAppId(),
            workflowExecutionInterrupt.getExecutionUuid(), workflowExecutionInterrupt.getStateExecutionInstanceId());

        updateStatus(stateExecutionInstance, FAILED, Lists.newArrayList(WAITING));

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
        StateExecutionInstance stateExecutionInstance = getStateExecutionInstance(workflowExecutionInterrupt.getAppId(),
            workflowExecutionInterrupt.getExecutionUuid(), workflowExecutionInterrupt.getStateExecutionInstanceId());

        StateMachine sm = wingsPersistence.get(StateMachine.class, workflowExecutionInterrupt.getAppId(),
            stateExecutionInstance.getStateMachineId(), CRITICAL);

        State currentState =
            sm.getState(stateExecutionInstance.getChildStateMachineId(), stateExecutionInstance.getStateName());
        injector.injectMembers(currentState);

        ExecutionContextImpl context = new ExecutionContextImpl(stateExecutionInstance, sm, injector);
        injector.injectMembers(context);
        executorService.execute(new SmExecutionResumer(context, this, SUCCESS));
        break;
      }

      case RETRY: {
        StateExecutionInstance stateExecutionInstance = getStateExecutionInstance(workflowExecutionInterrupt.getAppId(),
            workflowExecutionInterrupt.getExecutionUuid(), workflowExecutionInterrupt.getStateExecutionInstanceId());

        retryStateExecutionInstance(stateExecutionInstance, workflowExecutionInterrupt.getProperties());

        break;
      }

      case ABORT: {
        StateExecutionInstance stateExecutionInstance = getStateExecutionInstance(workflowExecutionInterrupt.getAppId(),
            workflowExecutionInterrupt.getExecutionUuid(), workflowExecutionInterrupt.getStateExecutionInstanceId());

        StateMachine sm = wingsPersistence.get(StateMachine.class, workflowExecutionInterrupt.getAppId(),
            stateExecutionInstance.getStateMachineId(), CRITICAL);
        ExecutionContextImpl context = new ExecutionContextImpl(stateExecutionInstance, sm, injector);
        injector.injectMembers(context);
        abortExecution(context);
        break;
      }
      case ABORT_ALL: {
        abortInstancesByStatus(
            workflowExecutionInterrupt, workflowExecution, NEW, RUNNING, STARTING, PAUSED, PAUSING, WAITING);
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
      default: {}
    }
    // TODO - more cases
  }

  private void endExecution(ExecutionInterrupt workflowExecutionInterrupt, WorkflowExecution workflowExecution) {
    abortInstancesByStatus(
        workflowExecutionInterrupt, workflowExecution, NEW, QUEUED, RUNNING, STARTING, PAUSED, PAUSING);
    PageRequest<StateExecutionInstance> pageRequest =
        aPageRequest()
            .withReadPref(CRITICAL)
            .withLimit(PageRequest.UNLIMITED)
            .addFilter("appId", EQ, workflowExecutionInterrupt.getAppId())
            .addFilter("executionUuid", EQ, workflowExecutionInterrupt.getExecutionUuid())
            .addFilter("status", Operator.IN, WAITING)
            .addFilter("createdAt", GT, workflowExecution.getCreatedAt())
            .build();

    List<StateExecutionInstance> allStateExecutionInstances = getAllStateExecutionInstances(pageRequest);
    for (StateExecutionInstance stateExecutionInstance : allStateExecutionInstances) {
      StateMachine sm = wingsPersistence.get(StateMachine.class, workflowExecutionInterrupt.getAppId(),
          stateExecutionInstance.getStateMachineId(), CRITICAL);
      ExecutionContextImpl context = new ExecutionContextImpl(stateExecutionInstance, sm, injector);
      injector.injectMembers(context);
      updateStateExecutionData(stateExecutionInstance, null, FAILED, null, asList(WAITING), null, null, null);
      endTransition(context, stateExecutionInstance, FAILED, null);
    }
  }

  private void abortInstancesByStatus(
      ExecutionInterrupt workflowExecutionInterrupt, WorkflowExecution workflowExecution, ExecutionStatus... statuses) {
    boolean updated = markAbortingState(workflowExecutionInterrupt, workflowExecution, statuses);
    if (updated) {
      PageRequest<StateExecutionInstance> pageRequest =
          aPageRequest()
              .withReadPref(CRITICAL)
              .withLimit(PageRequest.UNLIMITED)
              .addFilter("appId", EQ, workflowExecutionInterrupt.getAppId())
              .addFilter("executionUuid", EQ, workflowExecutionInterrupt.getExecutionUuid())
              .addFilter("status", Operator.IN, ABORTING)
              .addFilter("createdAt", GT, workflowExecution.getCreatedAt())
              .build();

      List<StateExecutionInstance> allStateExecutionInstances = getAllStateExecutionInstances(pageRequest);
      if (isEmpty(allStateExecutionInstances)) {
        logger.warn(
            "ABORT_ALL workflowExecutionInterrupt: {} being ignored as no running instance found for executionUuid: {}",
            workflowExecutionInterrupt.getUuid(), workflowExecutionInterrupt.getExecutionUuid());
      } else {
        for (StateExecutionInstance stateExecutionInstance : allStateExecutionInstances) {
          StateMachine sm = wingsPersistence.get(StateMachine.class, workflowExecutionInterrupt.getAppId(),
              stateExecutionInstance.getStateMachineId(), CRITICAL);
          ExecutionContextImpl context = new ExecutionContextImpl(stateExecutionInstance, sm, injector);
          injector.injectMembers(context);
          abortMarkedInstance(context, stateExecutionInstance);
          endTransition(context, stateExecutionInstance, ABORTED, null);
        }
      }
    }
  }

  void retryStateExecutionInstance(
      String appId, String executionUuid, String stateExecutionInstanceId, Map<String, Object> stateParams) {
    StateExecutionInstance stateExecutionInstance =
        getStateExecutionInstance(appId, executionUuid, stateExecutionInstanceId);
    retryStateExecutionInstance(stateExecutionInstance, stateParams);
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

  private void clearStateExecutionData(StateExecutionInstance stateExecutionInstance, Map<String, Object> stateParams) {
    Map<String, StateExecutionData> stateExecutionMap = stateExecutionInstance.getStateExecutionMap();
    if (stateExecutionMap == null) {
      return;
    }

    UpdateOperations<StateExecutionInstance> ops =
        wingsPersistence.createUpdateOperations(StateExecutionInstance.class);

    StateExecutionData stateExecutionData = stateExecutionMap.get(stateExecutionInstance.getStateName());
    ops.addToSet("stateExecutionDataHistory", stateExecutionData);
    stateExecutionInstance.getStateExecutionDataHistory().add(stateExecutionData);

    stateExecutionMap.remove(stateExecutionInstance.getStateName());
    ops.set("stateExecutionMap", stateExecutionInstance.getStateExecutionMap());

    if (stateParams != null) {
      ops.set("stateParams", stateParams);
    }

    if (stateExecutionInstance.getEndTs() != null) {
      stateExecutionInstance.setEndTs(null);
      ops.unset("endTs");
    }
    ops.set("status", NEW);

    Query<StateExecutionInstance> query = wingsPersistence.createQuery(StateExecutionInstance.class)
                                              .field("appId")
                                              .equal(stateExecutionInstance.getAppId())
                                              .field(ID_KEY)
                                              .equal(stateExecutionInstance.getUuid())
                                              .field("status")
                                              .in(asList(WAITING, FAILED, ERROR));

    UpdateResults updateResult = wingsPersistence.update(query, ops);
    if (updateResult == null || updateResult.getWriteResult() == null || updateResult.getWriteResult().getN() != 1) {
      logger.warn("clearStateExecutionData could not be completed for the stateExecutionInstance: {}",
          stateExecutionInstance.getUuid());

      throw new WingsException(ErrorCode.RETRY_FAILED).addParam("stateName", stateExecutionInstance.getStateName());
    }
  }

  private List<StateExecutionInstance> getAllStateExecutionInstances(PageRequest<StateExecutionInstance> req) {
    return wingsPersistence.queryAll(StateExecutionInstance.class, req);
  }

  private boolean markAbortingState(
      ExecutionInterrupt workflowExecutionInterrupt, WorkflowExecution workflowExecution, ExecutionStatus... statuses) {
    // Get all that are eligible for aborting
    PageRequest<StateExecutionInstance> pageRequest =
        aPageRequest()
            .withReadPref(CRITICAL)
            .withLimit(PageRequest.UNLIMITED)
            .addFilter("appId", EQ, workflowExecutionInterrupt.getAppId())
            .addFilter("executionUuid", EQ, workflowExecutionInterrupt.getExecutionUuid())
            .addFilter("status", Operator.IN, Arrays.copyOf(statuses, statuses.length, Object[].class))
            .addFilter("createdAt", GT, workflowExecution.getCreatedAt())
            .addFieldsIncluded("uuid", "stateType")
            .build();

    List<StateExecutionInstance> allStateExecutionInstances = getAllStateExecutionInstances(pageRequest);
    if (isEmpty(allStateExecutionInstances)) {
      logger.warn("No stateExecutionInstance could be marked as ABORTING - appId: {}, executionUuid: {}",
          workflowExecutionInterrupt.getAppId(), workflowExecutionInterrupt.getExecutionUuid());
      return false;
    }
    List<String> leafInstanceIds =
        getAllLeafInstanceIds(workflowExecutionInterrupt, workflowExecution, allStateExecutionInstances);

    UpdateOperations<StateExecutionInstance> ops =
        wingsPersistence.createUpdateOperations(StateExecutionInstance.class);
    ops.set("status", ABORTING);

    Query<StateExecutionInstance> query = wingsPersistence.createQuery(StateExecutionInstance.class)
                                              .field("appId")
                                              .equal(workflowExecutionInterrupt.getAppId())
                                              .field("executionUuid")
                                              .equal(workflowExecutionInterrupt.getExecutionUuid())
                                              .field("uuid")
                                              .in(leafInstanceIds);
    // Set the status to ABORTING
    UpdateResults updateResult = wingsPersistence.update(query, ops);
    if (updateResult == null || updateResult.getWriteResult() == null || updateResult.getWriteResult().getN() == 0) {
      logger.warn("No stateExecutionInstance could be marked as ABORTING - appId: {}, executionUuid: {}",
          workflowExecutionInterrupt.getAppId(), workflowExecutionInterrupt.getExecutionUuid());
      return false;
    }
    return true;
  }

  private List<String> getAllLeafInstanceIds(ExecutionInterrupt workflowExecutionInterrupt,
      WorkflowExecution workflowExecution, List<StateExecutionInstance> stateExecutionInstances) {
    List<String> allInstanceIds =
        stateExecutionInstances.stream().map(StateExecutionInstance::getUuid).collect(Collectors.toList());

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
            .collect(Collectors.toList());

    if (isEmpty(parentInstanceIds)) {
      return allInstanceIds;
    }

    // Query children
    PageRequest<StateExecutionInstance> pageRequest =
        aPageRequest()
            .withReadPref(CRITICAL)
            .withLimit(PageRequest.UNLIMITED)
            .addFilter("appId", EQ, workflowExecutionInterrupt.getAppId())
            .addFilter("executionUuid", EQ, workflowExecutionInterrupt.getExecutionUuid())
            .addFilter("parentInstanceId", Operator.IN, parentInstanceIds.toArray())
            .addFilter("status", Operator.IN, NEW, QUEUED, STARTING, RUNNING, PAUSED, PAUSING, WAITING)
            .addFilter("createdAt", GT, workflowExecution.getCreatedAt())
            .build();

    List<StateExecutionInstance> childInstances = getAllStateExecutionInstances(pageRequest);

    // get distinct parent Ids
    List<String> parentIdsHavingChildren = childInstances.stream()
                                               .map(StateExecutionInstance::getParentInstanceId)
                                               .distinct()
                                               .collect(Collectors.toList());

    // parent with no children
    allInstanceIds.removeAll(parentIdsHavingChildren);

    // Mark aborting
    return allInstanceIds;
  }

  private StateExecutionInstance getStateExecutionInstance(
      String appId, String executionUuid, String stateExecutionInstanceId) {
    // TODO: convert this not to use Query directly after default createdAt sorting is taken off
    Query<StateExecutionInstance> query = wingsPersistence.createQuery(StateExecutionInstance.class, CRITICAL)
                                              .field(ID_KEY)
                                              .equal(stateExecutionInstanceId)
                                              .field("appId")
                                              .equal(appId);
    if (executionUuid != null) {
      query.field("executionUuid").equal(executionUuid);
    }
    return query.get();
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
      stateMachineExecutor.startExecution(context);
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
      } catch (Exception ex) {
        stateMachineExecutor.handleExecuteResponseException(context, ex);
      }
    }
  }

  private static class SmExecutionAsyncResumer implements Runnable {
    private ExecutionContextImpl context;
    private StateMachineExecutor stateMachineExecutor;
    private State state;
    private Map<String, NotifyResponseData> response;
    private boolean asyncError;

    /**
     * Instantiates a new Sm execution dispatcher.
     *
     * @param context              the context
     * @param state                the state
     * @param response             the response
     * @param stateMachineExecutor the state machine executor
     */
    SmExecutionAsyncResumer(ExecutionContextImpl context, State state, Map<String, NotifyResponseData> response,
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
      try {
        if (!asyncError) {
          StateExecutionInstance stateExecutionInstance = context.getStateExecutionInstance();
          if (stateExecutionInstance.getStateParams() != null) {
            MapperUtils.mapObject(stateExecutionInstance.getStateParams(), state);
          }
          ExecutionResponse executionResponse = state.handleAsyncResponse(context, response);
          stateMachineExecutor.handleExecuteResponse(context, executionResponse);
        } else {
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
        }
      } catch (Exception ex) {
        stateMachineExecutor.handleExecuteResponseException(context, ex);
      }
    }
  }
}
