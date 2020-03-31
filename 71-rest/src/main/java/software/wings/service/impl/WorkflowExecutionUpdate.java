/**
 *
 */

package software.wings.service.impl;

import static io.harness.beans.ExecutionStatus.SUCCESS;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.ExecutionContext.MANAGER;
import static io.harness.mongo.MongoUtils.setUnset;
import static java.lang.String.format;
import static software.wings.sm.StateType.PHASE;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;

import io.fabric8.utils.Lists;
import io.harness.beans.EmbeddedUser;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.WorkflowType;
import io.harness.event.handler.impl.EventPublishHelper;
import io.harness.event.handler.impl.segment.SegmentHandler;
import io.harness.event.usagemetrics.UsageMetricsEventPublisher;
import io.harness.event.usagemetrics.UsageMetricsHelper;
import io.harness.exception.WingsException;
import io.harness.logging.ExceptionLogger;
import io.harness.queue.QueuePublisher;
import io.harness.waiter.WaitNotifyEngine;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.FindAndModifyOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.beans.Account;
import software.wings.beans.Application;
import software.wings.beans.EnvSummary;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.beans.FeatureName;
import software.wings.beans.HarnessTagLink;
import software.wings.beans.NameValuePair;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowExecution.WorkflowExecutionKeys;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.workflow.WorkflowNotificationHelper;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.BarrierService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.HarnessTagService;
import software.wings.service.intfc.ResourceConstraintService;
import software.wings.service.intfc.TriggerService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.trigger.DeploymentTriggerService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateExecutionInstance.StateExecutionInstanceKeys;
import software.wings.sm.StateMachineExecutionCallback;
import software.wings.sm.states.EnvState.EnvExecutionResponseData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The Class WorkflowExecutionUpdate.
 *
 * @author Rishi
 */
@Slf4j
public class WorkflowExecutionUpdate implements StateMachineExecutionCallback {
  private String appId;
  private String workflowExecutionId;
  private boolean needToNotifyPipeline;

  @Inject private WingsPersistence wingsPersistence;
  @Inject private WorkflowExecutionService workflowExecutionService;
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject private WorkflowNotificationHelper workflowNotificationHelper;
  @Inject private QueuePublisher<ExecutionEvent> executionEventQueue;
  @Inject private AlertService alertService;
  @Inject private TriggerService triggerService;
  @Inject private transient DeploymentTriggerService deploymentTriggerService;
  @Inject private transient AppService appService;
  @Inject private FeatureFlagService featureFlagService;
  @Inject private ResourceConstraintService resourceConstraintService;
  @Inject private BarrierService barrierService;
  @Inject private EventPublishHelper eventPublishHelper;
  @Inject private UsageMetricsEventPublisher usageMetricsEventPublisher;
  @Inject private AccountService accountService;
  @Inject private UsageMetricsHelper usageMetricsHelper;
  @Inject private SegmentHandler segmentHandler;
  @Inject private HarnessTagService harnessTagService;

  /**
   * Instantiates a new workflow execution update.
   */
  public WorkflowExecutionUpdate() {}

  /**
   * Instantiates a new workflow execution update.
   *
   * @param appId               the app id
   * @param workflowExecutionId the workflow execution id
   */
  public WorkflowExecutionUpdate(String appId, String workflowExecutionId) {
    this.appId = appId;
    this.workflowExecutionId = workflowExecutionId;
  }

  @UtilityClass
  public static final class Keys {
    public static final String SUCCESS = "Deployment Succeeded";
    public static final String REJECTED = "Deployment Rejected";
    public static final String EXPIRED = "Deployment Expired";
    public static final String ABORTED = "Deployment Aborted";
    public static final String FAILED = "Deployment Failed";
    public static final String MODULE = "module";
    public static final String DEPLOYMENT = "Deployment";
    public static final String PRODUCTION = "production";
  }

  /**
   * Gets app id.
   *
   * @return the app id
   */
  public String getAppId() {
    return appId;
  }

  /**
   * Sets app id.
   *
   * @param appId the app id
   */
  public void setAppId(String appId) {
    this.appId = appId;
  }

  /**
   * Gets workflow execution id.
   *
   * @return the workflow execution id
   */
  public String getWorkflowExecutionId() {
    return workflowExecutionId;
  }

  /**
   * Sets workflow execution id.
   *
   * @param workflowExecutionId the workflow execution id
   */
  public void setWorkflowExecutionId(String workflowExecutionId) {
    this.workflowExecutionId = workflowExecutionId;
  }

  private static final FindAndModifyOptions callbackFindAndModifyOptions = new FindAndModifyOptions().returnNew(false);

  public StateExecutionInstance getRollbackInstance(String workflowExecutionId) {
    return wingsPersistence.createQuery(StateExecutionInstance.class)
        .filter(StateExecutionInstanceKeys.executionUuid, workflowExecutionId)
        .filter(StateExecutionInstanceKeys.stateType, PHASE.name())
        .filter(StateExecutionInstanceKeys.rollback, true)
        .order(Sort.ascending(StateExecutionInstanceKeys.createdAt))
        .project(StateExecutionInstanceKeys.startTs, true)
        .get();
  }

  @Override
  public void callback(ExecutionContext context, ExecutionStatus status, Exception ex) {
    final WorkflowExecution execution = wingsPersistence.createQuery(WorkflowExecution.class)
                                            .filter(WorkflowExecutionKeys.appId, appId)
                                            .filter(WorkflowExecutionKeys.uuid, workflowExecutionId)
                                            .project(WorkflowExecutionKeys.startTs, true)
                                            .get();

    Long startTs = execution == null ? null : execution.getStartTs();
    Long endTs = startTs == null ? null : System.currentTimeMillis();
    Long duration = startTs == null ? null : endTs - startTs;

    final StateExecutionInstance rollbackInstance = getRollbackInstance(workflowExecutionId);
    Long rollbackStartTs = rollbackInstance == null ? null : rollbackInstance.getStartTs();
    Long rollbackDuration = rollbackStartTs == null ? null : endTs - rollbackStartTs;

    Query<WorkflowExecution> query = wingsPersistence.createQuery(WorkflowExecution.class)
                                         .filter(WorkflowExecutionKeys.appId, appId)
                                         .filter(WorkflowExecutionKeys.uuid, workflowExecutionId)
                                         .field(WorkflowExecutionKeys.status)
                                         .in(ExecutionStatus.activeStatuses());

    UpdateOperations<WorkflowExecution> updateOps =
        wingsPersistence.createUpdateOperations(WorkflowExecution.class).set(WorkflowExecutionKeys.status, status);
    setUnset(updateOps, WorkflowExecutionKeys.endTs, endTs);
    setUnset(updateOps, WorkflowExecutionKeys.duration, duration);
    setUnset(updateOps, WorkflowExecutionKeys.rollbackStartTs, rollbackStartTs);
    setUnset(updateOps, WorkflowExecutionKeys.rollbackDuration, rollbackDuration);

    wingsPersistence.findAndModify(query, updateOps, callbackFindAndModifyOptions);

    handlePostExecution(context);

    final String workflowId = context.getWorkflowId(); // this will be pipelineId in case of pipeline
    List<NameValuePair> resolvedTags = resolveDeploymentTags(context, workflowId);
    addTagsToWorkflowExecution(resolvedTags);

    if (WorkflowType.PIPELINE != context.getWorkflowType()) {
      try {
        workflowNotificationHelper.sendWorkflowStatusChangeNotification(context, status);
      } catch (Exception exception) {
        // Failing to send notification is not considered critical to interrupt the status update.
        logger.error("Failed to send notification.", exception);
      }
      if (needToNotifyPipeline) {
        try {
          waitNotifyEngine.doneWith(workflowExecutionId, new EnvExecutionResponseData(workflowExecutionId, status));
        } catch (WingsException exception) {
          ExceptionLogger.logProcessedMessages(exception, MANAGER, logger);
        }
      }
    } else {
      if (status == SUCCESS) {
        String accountId = appService.getAccountIdByAppId(appId);
        if (!featureFlagService.isEnabled(FeatureName.TRIGGER_REFACTOR, accountId)) {
          triggerService.triggerExecutionPostPipelineCompletionAsync(appId, workflowId);
        } else {
          deploymentTriggerService.triggerExecutionPostPipelineCompletionAsync(appId, workflowId);
        }
      }
    }
    if (ExecutionStatus.isFinalStatus(status)) {
      try {
        WorkflowExecution workflowExecution = workflowExecutionService.getWorkflowExecution(appId, workflowExecutionId);
        alertService.deploymentCompleted(appId, context.getWorkflowExecutionId());
        if (workflowExecution == null) {
          logger.warn("No workflowExecution for workflowExecution:[{}], appId:[{}],", workflowExecutionId, appId);
          return;
        }
        final Application applicationDataForReporting = usageMetricsHelper.getApplication(appId);
        String accountID = applicationDataForReporting.getAccountId();
        /**
         * PL-2326 : Workflow execution did not even start -> was in queued state. In
         * this case, startTS and endTS are not populated. Ignoring these events.
         */
        if (workflowExecution.getStartTs() != null && workflowExecution.getEndTs() != null) {
          updateDeploymentInformation(workflowExecution);
          workflowExecution = workflowExecutionService.getWorkflowExecution(appId, workflowExecutionId);
          /**
           * Had to do a double check on the finalStatus since workflowStatus is still not in finalStatus while
           * the callBack says it is finalStatus (Check with Srinivas)
           */
          if (ExecutionStatus.isFinalStatus(workflowExecution.getStatus())) {
            usageMetricsEventPublisher.publishDeploymentTimeSeriesEvent(accountID, workflowExecution);
          } else {
            logger.warn("Workflow [{}] has executionStatus:[{}], different status:[{}]", workflowExecutionId,
                workflowExecution.getStatus(), status);
          }
        }

        eventPublishHelper.handleDeploymentCompleted(workflowExecution);
        if (workflowExecution.getPipelineExecutionId() == null) {
          String applicationName = applicationDataForReporting.getName();
          Account account = accountService.getFromCache(accountID);
          // The null check is in case the account has been physical deleted.
          if (account == null) {
            logger.warn("Workflow execution in application {} is associated with deleted account {}", applicationName,
                accountID);
          }
        }
        if (WorkflowType.PIPELINE != context.getWorkflowType()) {
          if (workflowExecution.getPipelineExecutionId() != null) {
            workflowExecutionService.refreshCollectedArtifacts(
                appId, workflowExecution.getPipelineExecutionId(), workflowExecutionId);
          }
        }

        reportDeploymentEventToSegment(workflowExecution);
      } catch (Exception e) {
        logger.error(
            "Failed to generate events for workflowExecution:[{}], appId:[{}],", workflowExecutionId, appId, e);
      }
    }
  }

  @VisibleForTesting
  public List<NameValuePair> resolveDeploymentTags(ExecutionContext context, String workflowId) {
    String accountId = appService.getAccountIdByAppId(appId);
    List<HarnessTagLink> harnessTagLinks = harnessTagService.getTagLinksWithEntityId(accountId, workflowId);
    List<NameValuePair> resolvedTags = new ArrayList<>();
    if (isNotEmpty(harnessTagLinks)) {
      for (HarnessTagLink harnessTagLink : harnessTagLinks) {
        String tagKey = context.renderExpression(harnessTagLink.getKey());
        // checking string equals null as the jexl library seems to be returning the string "null" in some cases when
        // the expression can't be evaluated instead of returning the original expression
        // if key can't be evaluated, don't store it
        if (isEmpty(tagKey) || tagKey.equals("null")
            || (harnessTagLink.getKey().startsWith("${") && harnessTagLink.getKey().equals(tagKey))) {
          continue;
        }
        String tagValue = context.renderExpression(harnessTagLink.getValue());
        // if value can't be evaluated, set it to ""
        if (tagValue == null || tagValue.equals("null")
            || (harnessTagLink.getValue().startsWith("${") && harnessTagLink.getValue().equals(tagValue))) {
          tagValue = "";
        }
        resolvedTags.add(NameValuePair.builder().name(tagKey).value(tagValue).build());
      }
    }
    return resolvedTags;
  }

  @VisibleForTesting
  public void addTagsToWorkflowExecution(List<NameValuePair> resolvedTags) {
    if (isNotEmpty(resolvedTags)) {
      Query<WorkflowExecution> query = wingsPersistence.createQuery(WorkflowExecution.class)
                                           .filter(WorkflowExecutionKeys.appId, appId)
                                           .filter(WorkflowExecutionKeys.uuid, workflowExecutionId);
      UpdateOperations<WorkflowExecution> updateOps = wingsPersistence.createUpdateOperations(WorkflowExecution.class)
                                                          .addToSet(WorkflowExecutionKeys.tags, resolvedTags);
      wingsPersistence.findAndModify(query, updateOps, callbackFindAndModifyOptions);
      logger.info(format("[%d] tags added to workflow execution: [%s]", resolvedTags.size(), workflowExecutionId));
    }
  }

  public void updateDeploymentInformation(WorkflowExecution workflowExecution) {
    UpdateOperations<WorkflowExecution> updateOps;
    updateOps = wingsPersistence.createUpdateOperations(WorkflowExecution.class);
    boolean update = false;
    final List<String> deployedCloudProviders =
        workflowExecutionService.getCloudProviderIdsForExecution(workflowExecution);

    if (!Lists.isNullOrEmpty(deployedCloudProviders)) {
      update = true;
      setUnset(updateOps, WorkflowExecutionKeys.deployedCloudProviders, deployedCloudProviders);
    }
    final List<String> deployedServices = workflowExecutionService.getServiceIdsForExecution(workflowExecution);
    if (!Lists.isNullOrEmpty(deployedServices)) {
      update = true;
      setUnset(updateOps, WorkflowExecutionKeys.deployedServices, deployedServices);
    }
    final List<EnvSummary> deployedEnvironments =
        workflowExecutionService.getEnvironmentsForExecution(workflowExecution);

    if (!Lists.isNullOrEmpty(deployedEnvironments)) {
      update = true;
      setUnset(updateOps, WorkflowExecutionKeys.deployedEnvironments, deployedEnvironments);
    }

    if (update) {
      wingsPersistence.findAndModify(wingsPersistence.createQuery(WorkflowExecution.class)
                                         .filter(WorkflowExecutionKeys.uuid, workflowExecution.getUuid()),
          updateOps, callbackFindAndModifyOptions);
    }
  }

  private boolean isProdEnv(WorkflowExecution workflowExecution) {
    return EnvironmentType.PROD == workflowExecution.getEnvType();
  }

  @VisibleForTesting
  public void reportDeploymentEventToSegment(WorkflowExecution workflowExecution) {
    try {
      String accountId = workflowExecution.getAccountId();

      Map<String, String> properties = new HashMap<>();
      properties.put(SegmentHandler.Keys.GROUP_ID, accountId);
      properties.put(Keys.MODULE, Keys.DEPLOYMENT);
      properties.put(Keys.PRODUCTION, Boolean.toString(isProdEnv(workflowExecution)));

      Map<String, Boolean> integrations = new HashMap<>();
      integrations.put(SegmentHandler.Keys.NATERO, true);
      integrations.put(SegmentHandler.Keys.SALESFORCE, false);
      Account account = accountService.getFromCacheWithFallback(accountId);
      EmbeddedUser triggeredBy = workflowExecution.getTriggeredBy();
      String userId = null;
      if (triggeredBy != null) {
        userId = triggeredBy.getUuid() != null ? triggeredBy.getUuid() : null;
      }

      String deploymentEvent = getSegmentDeploymentEvent(workflowExecution);
      if (deploymentEvent != null) {
        segmentHandler.reportTrackEvent(account, deploymentEvent, userId, properties, integrations);
      } else {
        logger.info("Skipping the deployment track event since the status {} doesn't need to be reported",
            workflowExecution.getStatus());
      }
    } catch (Exception e) {
      logger.error("Exception while reporting track event for deployment {}", workflowExecutionId, e);
    }
  }

  private String getSegmentDeploymentEvent(WorkflowExecution workflowExecution) {
    if (workflowExecution == null || workflowExecution.getStatus() == null) {
      return null;
    }

    switch (workflowExecution.getStatus()) {
      case REJECTED:
        return Keys.REJECTED;
      case FAILED:
        return Keys.FAILED;
      case ABORTED:
        return Keys.ABORTED;
      case EXPIRED:
        return Keys.EXPIRED;
      case SUCCESS:
        return Keys.SUCCESS;
      default:
        return null;
    }
  }

  protected void handlePostExecution(ExecutionContext context) {
    // TODO: this is temporary. this should be part of its own callback and with more precise filter
    try {
      barrierService.updateAllActiveBarriers(context.getAppId());
    } catch (RuntimeException exception) {
      // Do not block the execution for possible exception in the barrier update
      logger.error("Something wrong with barrier update", exception);
    }

    // TODO: this is temporary. this should be part of its own callback and with more precise filter
    try {
      final Set<String> constraintIds =
          resourceConstraintService.updateActiveConstraints(context.getAppId(), workflowExecutionId);

      resourceConstraintService.updateBlockedConstraints(constraintIds);

    } catch (RuntimeException exception) {
      // Do not block the execution for possible exception in the barrier update
      logger.error("Something wrong with resource constraints update", exception);
    }
    try {
      WorkflowExecution workflowExecution =
          workflowExecutionService.getExecutionDetails(appId, workflowExecutionId, true);
      logger.info("Breakdown refresh happened for workflow execution {}", workflowExecution.getUuid());
      if (context.getWorkflowType() == WorkflowType.ORCHESTRATION
          && !featureFlagService.isEnabled(FeatureName.INFRA_MAPPING_REFACTOR, workflowExecution.getAccountId())) {
        executionEventQueue.send(ExecutionEvent.builder()
                                     .appId(appId)
                                     .workflowId(workflowExecution.getWorkflowId())
                                     .infraMappingIds(workflowExecution.getInfraMappingIds())
                                     .infraDefinitionIds(workflowExecution.getInfraDefinitionIds())
                                     .build());
      }
    } catch (Exception e) {
      logger.error("Error in breakdown refresh", e);
    }
  }

  public boolean isNeedToNotifyPipeline() {
    return needToNotifyPipeline;
  }

  public void setNeedToNotifyPipeline(boolean needToNotifyPipeline) {
    this.needToNotifyPipeline = needToNotifyPipeline;
  }
}
