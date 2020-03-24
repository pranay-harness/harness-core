package software.wings.service.impl.trigger;

import static io.harness.beans.OrchestrationWorkflowType.BUILD;
import static io.harness.beans.WorkflowType.ORCHESTRATION;
import static io.harness.beans.WorkflowType.PIPELINE;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.ListUtils.trimStrings;
import static io.harness.exception.WingsException.ExecutionContext.MANAGER;
import static io.harness.exception.WingsException.USER;
import static io.harness.expression.ExpressionEvaluator.matchesVariablePattern;
import static io.harness.govern.Switch.unhandled;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;
import static io.harness.validation.PersistenceValidator.duplicateCheck;
import static io.harness.validation.Validator.equalCheck;
import static io.harness.validation.Validator.notNullCheck;
import static java.time.Duration.ofHours;
import static java.time.Duration.ofSeconds;
import static java.util.regex.Pattern.compile;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.beans.ExecutionCredential.ExecutionType.SSH;
import static software.wings.beans.SSHExecutionCredential.Builder.aSSHExecutionCredential;
import static software.wings.beans.trigger.ArtifactSelection.Type.LAST_COLLECTED;
import static software.wings.beans.trigger.ArtifactSelection.Type.LAST_DEPLOYED;
import static software.wings.beans.trigger.ArtifactSelection.Type.PIPELINE_SOURCE;
import static software.wings.beans.trigger.ArtifactSelection.Type.WEBHOOK_VARIABLE;
import static software.wings.beans.trigger.TriggerConditionType.SCHEDULED;
import static software.wings.beans.trigger.TriggerConditionType.WEBHOOK;
import static software.wings.beans.trigger.WebhookSource.BITBUCKET;
import static software.wings.service.impl.trigger.TriggerServiceHelper.constructWebhookToken;
import static software.wings.service.impl.trigger.TriggerServiceHelper.notNullCheckWorkflow;
import static software.wings.service.impl.trigger.TriggerServiceHelper.overrideTriggerVariables;
import static software.wings.service.impl.trigger.TriggerServiceHelper.validateAndSetCronExpression;
import static software.wings.service.impl.workflow.WorkflowServiceTemplateHelper.getServiceWorkflowVariables;
import static software.wings.service.impl.workflow.WorkflowServiceTemplateHelper.getTemplatizedEnvVariableName;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import io.harness.beans.ExecutionStatus;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.WorkflowType;
import io.harness.distribution.idempotence.IdempotentId;
import io.harness.distribution.idempotence.IdempotentLock;
import io.harness.distribution.idempotence.IdempotentResult;
import io.harness.distribution.idempotence.UnableToRegisterIdempotentOperationException;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.logging.AutoLogContext;
import io.harness.logging.ExceptionLogger;
import io.harness.persistence.AccountLogContext;
import io.harness.scheduler.PersistentScheduler;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.quartz.TriggerKey;
import software.wings.beans.Application;
import software.wings.beans.EntityType;
import software.wings.beans.Environment;
import software.wings.beans.Event.Type;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.FeatureName;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.Pipeline;
import software.wings.beans.Service;
import software.wings.beans.Variable;
import software.wings.beans.WebHookToken;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.deployment.DeploymentMetadata;
import software.wings.beans.deployment.DeploymentMetadata.Include;
import software.wings.beans.instance.dashboard.ArtifactSummary;
import software.wings.beans.trigger.ArtifactSelection;
import software.wings.beans.trigger.ArtifactTriggerCondition;
import software.wings.beans.trigger.NewInstanceTriggerCondition;
import software.wings.beans.trigger.PipelineTriggerCondition;
import software.wings.beans.trigger.ScheduledTriggerCondition;
import software.wings.beans.trigger.ServiceInfraWorkflow;
import software.wings.beans.trigger.Trigger;
import software.wings.beans.trigger.Trigger.TriggerKeys;
import software.wings.beans.trigger.TriggerExecution;
import software.wings.beans.trigger.TriggerExecution.Status;
import software.wings.beans.trigger.WebHookTriggerCondition;
import software.wings.beans.trigger.WebhookEventType;
import software.wings.beans.trigger.WebhookParameters;
import software.wings.beans.trigger.WebhookSource;
import software.wings.common.MongoIdempotentRegistry;
import software.wings.dl.WingsPersistence;
import software.wings.helpers.ext.trigger.response.TriggerDeploymentNeededResponse;
import software.wings.helpers.ext.trigger.response.TriggerResponse;
import software.wings.infra.InfrastructureDefinition;
import software.wings.scheduler.ScheduledTriggerJob;
import software.wings.service.impl.AuditServiceHelper;
import software.wings.service.impl.security.auth.AuthHandler;
import software.wings.service.impl.workflow.WorkflowServiceTemplateHelper;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactCollectionService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.ArtifactStreamServiceBindingService;
import software.wings.service.intfc.AuthService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.HarnessTagService;
import software.wings.service.intfc.InfrastructureDefinitionService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.ResourceLookupService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.TriggerService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;
import software.wings.service.intfc.trigger.TriggerExecutionService;
import software.wings.service.intfc.yaml.YamlPushService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import javax.validation.executable.ValidateOnExecution;

@Singleton
@ValidateOnExecution
@Slf4j
public class TriggerServiceImpl implements TriggerService {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private ExecutorService executorService;
  @Inject private WorkflowExecutionService workflowExecutionService;
  @Inject private ArtifactService artifactService;
  @Inject private ArtifactStreamService artifactStreamService;
  @Inject @Named("AsyncArtifactCollectionService") private ArtifactCollectionService artifactCollectionServiceAsync;
  @Inject private AppService appService;
  @Inject private PipelineService pipelineService;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private WorkflowService workflowService;
  @Inject private InfrastructureMappingService infrastructureMappingService;
  @Inject private InfrastructureDefinitionService infrastructureDefinitionService;
  @Inject @Named("BackgroundJobScheduler") private PersistentScheduler jobScheduler;
  @Inject private FeatureFlagService featureFlagService;
  @Inject private HarnessTagService harnessTagService;
  @Inject private YamlPushService yamlPushService;
  @Inject private ArtifactStreamServiceBindingService artifactStreamServiceBindingService;

  @Value
  @Builder
  public static class TriggerIdempotentResult implements IdempotentResult {
    private String triggerUuid;
  }

  @Inject private MongoIdempotentRegistry<TriggerIdempotentResult> idempotentRegistry;
  @Inject private TriggerServiceHelper triggerServiceHelper;
  @Inject private EnvironmentService environmentService;
  @Inject private WebhookTriggerProcessor webhookTriggerProcessor;
  @Inject private TriggerExecutionService triggerExecutionService;
  @Inject private AuditServiceHelper auditServiceHelper;
  @Inject private AuthService authService;
  @Inject private AuthHandler authHandler;
  @Inject private ResourceLookupService resourceLookupService;
  @Inject private TriggerAuthHandler triggerAuthHandler;

  @Override
  public PageResponse<Trigger> list(PageRequest<Trigger> pageRequest, boolean withTags, String tagFilter) {
    return resourceLookupService.listWithTagFilters(pageRequest, tagFilter, EntityType.TRIGGER, withTags);
  }

  @Override
  public Trigger get(String appId, String triggerId) {
    return wingsPersistence.getWithAppId(Trigger.class, appId, triggerId);
  }

  @Override
  public Trigger save(Trigger trigger) {
    validateInput(trigger, null);
    Trigger savedTrigger =
        duplicateCheck(() -> wingsPersistence.saveAndGet(Trigger.class, trigger), "name", trigger.getName());
    String accountId = appService.getAccountIdByAppId(savedTrigger.getAppId());
    if (trigger.getCondition().getConditionType() == SCHEDULED) {
      ScheduledTriggerJob.add(jobScheduler, accountId, savedTrigger.getAppId(), savedTrigger.getUuid(), trigger);
    }

    if (featureFlagService.isEnabled(FeatureName.TRIGGER_YAML, accountId)) {
      yamlPushService.pushYamlChangeSet(accountId, null, savedTrigger, Type.CREATE, trigger.isSyncFromGit(), false);
    } else {
      // TODO: Once this flag is enabled for all accounts, this can be removed
      auditServiceHelper.reportForAuditingUsingAppId(trigger.getAppId(), null, trigger, Type.CREATE);
    }
    return savedTrigger;
  }

  @Override
  public Trigger update(Trigger trigger, boolean migration) {
    Trigger existingTrigger = wingsPersistence.getWithAppId(Trigger.class, trigger.getAppId(), trigger.getUuid());
    notNullCheck("Trigger was deleted ", existingTrigger, USER);
    equalCheck(trigger.getWorkflowType(), existingTrigger.getWorkflowType());

    validateInput(trigger, existingTrigger);

    Trigger updatedTrigger =
        duplicateCheck(() -> wingsPersistence.saveAndGet(Trigger.class, trigger), "name", trigger.getName());
    addOrUpdateCronForScheduledJob(trigger, existingTrigger);

    String accountId = appService.getAccountIdByAppId(existingTrigger.getAppId());
    boolean isRename = !existingTrigger.getName().equals(trigger.getName());
    if (!migration) {
      if (featureFlagService.isEnabled(FeatureName.TRIGGER_YAML, accountId)) {
        yamlPushService.pushYamlChangeSet(
            accountId, existingTrigger, updatedTrigger, Type.UPDATE, trigger.isSyncFromGit(), isRename);
      } else {
        // TODO: Once this flag is enabled for all accounts, this can be removed
        auditServiceHelper.reportForAuditingUsingAppId(
            updatedTrigger.getAppId(), existingTrigger, updatedTrigger, Type.UPDATE);
      }
    }

    return updatedTrigger;
  }

  @Override
  public boolean delete(String appId, String triggerId) {
    Trigger trigger = get(appId, triggerId);
    boolean answer = triggerServiceHelper.delete(triggerId);

    String accountId = appService.getAccountIdByAppId(appId);
    if (answer) {
      harnessTagService.pruneTagLinks(accountId, triggerId);
    }
    if (featureFlagService.isEnabled(FeatureName.TRIGGER_YAML, accountId) && (trigger != null)) {
      yamlPushService.pushYamlChangeSet(accountId, trigger, null, Type.DELETE, trigger.isSyncFromGit(), false);
    } else {
      // TODO: Once this flag is enabled for all accounts, this can be removed
      auditServiceHelper.reportDeleteForAuditing(trigger.getAppId(), trigger);
    }
    return answer;
  }

  @Override
  public WebHookToken generateWebHookToken(String appId, String triggerId) {
    Trigger trigger = wingsPersistence.getWithAppId(Trigger.class, appId, triggerId);
    notNullCheck("Trigger was deleted", trigger, USER);
    return generateWebHookToken(trigger, null);
  }

  @Override
  public void pruneByApplication(String appId) {
    wingsPersistence.createQuery(Trigger.class).filter(Trigger.APP_ID_KEY, appId).asList().forEach(trigger -> {
      delete(appId, trigger.getUuid());
      auditServiceHelper.reportDeleteForAuditing(appId, trigger);
      harnessTagService.pruneTagLinks(appService.getAccountIdByAppId(appId), trigger.getUuid());
    });
  }

  @Override
  public void pruneByPipeline(String appId, String pipelineId) {
    List<Trigger> triggers = triggerServiceHelper.getTriggersByWorkflow(appId, pipelineId);
    triggers.forEach(trigger -> triggerServiceHelper.delete(trigger.getUuid()));

    triggerServiceHelper.deletePipelineCompletionTriggers(appId, pipelineId);
  }

  @Override
  public void pruneByWorkflow(String appId, String workflowId) {
    List<Trigger> triggers = triggerServiceHelper.getTriggersByWorkflow(appId, workflowId);
    triggers.forEach(trigger -> triggerServiceHelper.delete(trigger.getUuid()));
  }

  @Override
  public void pruneByArtifactStream(String appId, String artifactStreamId) {
    for (Trigger trigger : triggerServiceHelper.getNewArtifactTriggers(appId, artifactStreamId)) {
      triggerServiceHelper.delete(trigger.getUuid());
    }
  }

  @Override
  public List<String> obtainTriggerNamesReferencedByTemplatedEntityId(String appId, String entityId) {
    return triggerServiceHelper.checkTemplatedEntityReferenced(appId, entityId);
  }

  @Override
  public void triggerExecutionPostArtifactCollectionAsync(
      String appId, String artifactStreamId, List<Artifact> artifacts) {
    String accountId = appService.getAccountIdByAppId(appId);
    if (!featureFlagService.isEnabled(FeatureName.TRIGGER_REFACTOR, accountId)) {
      executorService.execute(() -> triggerExecutionPostArtifactCollection(appId, artifactStreamId, artifacts));
    }
  }

  @Override
  public void triggerExecutionPostArtifactCollectionAsync(
      String accountId, String appId, String artifactStreamId, List<Artifact> artifacts) {
    if (!featureFlagService.isEnabled(FeatureName.TRIGGER_REFACTOR, accountId)) {
      executorService.execute(() -> {
        if (featureFlagService.isEnabled(FeatureName.TRIGGER_FOR_ALL_ARTIFACTS, accountId)) {
          triggerExecutionPostArtifactCollectionForAllArtifacts(appId, artifactStreamId, artifacts);
        } else {
          triggerExecutionPostArtifactCollection(appId, artifactStreamId, artifacts);
        }
      });
    }
  }

  private void triggerExecutionPostArtifactCollectionForAllArtifacts(
      String appId, String artifactStreamId, List<Artifact> collectedArtifacts) {
    String accountId = appService.getAccountIdByAppId(appId);
    if (!featureFlagService.isEnabled(FeatureName.TRIGGER_REFACTOR, accountId)) {
      if (isEmpty(collectedArtifacts)) {
        return;
      }
      triggerServiceHelper.getNewArtifactTriggers(appId, artifactStreamId).forEach(trigger -> {
        logger.info("Trigger found with name {} and Id {} for artifactStreamId {}", trigger.getName(),
            trigger.getUuid(), artifactStreamId);
        ArtifactTriggerCondition artifactTriggerCondition = (ArtifactTriggerCondition) trigger.getCondition();
        List<Artifact> artifacts = new ArrayList<>();
        if (isEmpty(artifactTriggerCondition.getArtifactFilter())) {
          logger.info("No artifact filter set. Triggering with all artifacts");
          artifacts.addAll(collectedArtifacts);
        } else {
          logger.info("Artifact filter {} set. Going over all the artifacts to find the matched artifacts",
              artifactTriggerCondition.getArtifactFilter());
          List<Artifact> matchedArtifacts =
              collectedArtifacts.stream()
                  .filter(artifact
                      -> triggerServiceHelper.checkArtifactMatchesArtifactFilter(trigger.getUuid(), artifact,
                          artifactTriggerCondition.getArtifactFilter(), artifactTriggerCondition.isRegex()))
                  .collect(Collectors.toList());
          if (isNotEmpty(matchedArtifacts)) {
            logger.info("Matched artifacts size {}", matchedArtifacts.size());
            artifacts.addAll(matchedArtifacts);
          } else {
            logger.info("Artifacts {} not matched with the given artifact filter", artifacts);
          }
        }
        if (isEmpty(artifacts)) {
          logger.warn(
              "Skipping execution - artifact does not match with the given filter. So, skipping the complete deployment {}",
              artifactTriggerCondition);
          return;
        }
        List<Artifact> artifactsFromSelections = new ArrayList<>();
        if (isNotEmpty(trigger.getArtifactSelections())) {
          logger.info("Artifact selections found collecting artifacts as per artifactStream selections");
          addArtifactsFromSelections(trigger.getAppId(), trigger, artifactsFromSelections);
        }
        if (isNotEmpty(artifacts)) {
          logger.info("The artifacts  set for the trigger {} are {}", trigger.getUuid(),
              artifacts.stream().map(Artifact::getUuid).collect(toList()));
          for (Artifact artifact : artifacts) {
            logger.info("Triggering deployment with artifact {}", artifact.getUuid());
            try {
              List<Artifact> selectedArtifacts = new ArrayList<>(artifactsFromSelections);
              selectedArtifacts.add(artifact);
              triggerDeployment(selectedArtifacts, trigger, null);
            } catch (WingsException exception) {
              exception.addContext(Application.class, trigger.getAppId());
              exception.addContext(ArtifactStream.class, artifactStreamId);
              exception.addContext(Trigger.class, trigger.getUuid());
              ExceptionLogger.logProcessedMessages(exception, MANAGER, logger);
            }
          }
        } else {
          logger.info("No Artifacts matched. Hence Skipping the deployment");
          return;
        }
      });
    }
  }

  @Override
  public void triggerExecutionPostPipelineCompletionAsync(String appId, String sourcePipelineId) {
    String accountId = appService.getAccountIdByAppId(appId);
    if (!featureFlagService.isEnabled(FeatureName.TRIGGER_REFACTOR, accountId)) {
      executorService.submit(() -> triggerExecutionPostPipelineCompletion(appId, sourcePipelineId));
    }
  }

  @Override
  public void triggerScheduledExecutionAsync(Trigger trigger, Date scheduledFireTime) {
    String accountId = appService.getAccountIdByAppId(trigger.getAppId());
    if (!featureFlagService.isEnabled(FeatureName.TRIGGER_REFACTOR, accountId)) {
      if (!featureFlagService.isEnabled(FeatureName.TRIGGER_REFACTOR, accountId)) {
        executorService.submit(() -> triggerScheduledExecution(trigger, scheduledFireTime));
      }
    }
  }

  @Override
  public WorkflowExecution triggerExecutionByWebHook(String appId, String webHookToken,
      Map<String, ArtifactSummary> serviceArtifactMapping, Map<String, String> parameters,
      TriggerExecution triggerExecution) {
    String accountId = appService.getAccountIdByAppId(appId);
    if (!featureFlagService.isEnabled(FeatureName.TRIGGER_REFACTOR, accountId)) {
      List<Artifact> artifacts = new ArrayList<>();
      Trigger trigger = triggerServiceHelper.getTrigger(appId, webHookToken);
      logger.info("Received WebHook request  for the Trigger {} with Service Build Numbers {}  and parameters {}",
          trigger.getPipelineId(), serviceArtifactMapping, parameters);
      if (isNotEmpty(serviceArtifactMapping)) {
        addArtifactsFromVersionsOfWebHook(trigger, serviceArtifactMapping, artifacts);
      }
      addArtifactsFromSelections(appId, trigger, artifacts);
      return triggerDeployment(artifacts, trigger, parameters, triggerExecution);
    } else {
      return null;
    }
  }

  @Override
  public Trigger getTriggerByWebhookToken(String token) {
    return wingsPersistence.createQuery(Trigger.class).filter(TriggerKeys.webHookToken, token).get();
  }

  @Override
  public WorkflowExecution triggerExecutionByWebHook(
      Trigger trigger, Map<String, String> parameters, TriggerExecution triggerExecution) {
    String accountId = appService.getAccountIdByAppId(trigger.getAppId());
    if (!featureFlagService.isEnabled(FeatureName.TRIGGER_REFACTOR, accountId)) {
      return triggerDeployment(null, trigger, parameters, triggerExecution);
    } else {
      return null;
    }
  }

  @Override
  public WebhookParameters listWebhookParameters(String appId, String workflowId, WorkflowType workflowType,
      WebhookSource webhookSource, WebhookEventType eventType) {
    return WebhookParameters.builder()
        .params(new ArrayList<>())
        .expressions(WebhookParameters.suggestExpressions(webhookSource, eventType))
        .build();
  }

  @Override
  public List<Trigger> getTriggersHasPipelineAction(String appId, String pipelineId) {
    return getTriggersHasWorkflowAction(appId, pipelineId);
  }

  @Override
  public List<Trigger> getTriggersHasWorkflowAction(String appId, String workflowId) {
    return triggerServiceHelper.getTriggersHasWorkflowAction(appId, workflowId);
  }

  @Override
  public List<Trigger> getTriggersHasArtifactStreamAction(String appId, String artifactStreamId) {
    return triggerServiceHelper.getTriggersHasArtifactStreamAction(appId, artifactStreamId);
  }

  @Override
  public void updateByApp(String appId) {
    triggerServiceHelper.getTriggersByApp(appId).forEach(trigger -> update(trigger, false));
  }

  @Override
  public void updateByArtifactStream(String artifactStreamId) {
    triggerServiceHelper.getTriggersByArtifactStream(artifactStreamId).forEach(trigger -> update(trigger, false));
  }

  @Override
  public String getCronDescription(String cronExpression) {
    return TriggerServiceHelper.getCronDescription(cronExpression);
  }

  private void triggerExecutionPostArtifactCollection(
      String appId, String artifactStreamId, List<Artifact> collectedArtifacts) {
    triggerServiceHelper.getNewArtifactTriggers(appId, artifactStreamId).forEach(trigger -> {
      logger.info("Trigger found with name {} and Id {} for artifactStreamId {}", trigger.getName(), trigger.getUuid(),
          artifactStreamId);
      if (trigger.isDisabled()) {
        logger.info("Trigger rejected due to slowness in the product");
        return;
      }
      ArtifactTriggerCondition artifactTriggerCondition = (ArtifactTriggerCondition) trigger.getCondition();
      List<Artifact> artifacts = new ArrayList<>();
      if (isEmpty(artifactTriggerCondition.getArtifactFilter())) {
        logger.info("No artifact filter set. Triggering with the collected artifact {}",
            collectedArtifacts.get(collectedArtifacts.size() - 1).getUuid());
        artifacts.add(collectedArtifacts.get(collectedArtifacts.size() - 1));
      } else {
        logger.info("Artifact filter {} set. Going over all the artifacts to find the matched artifacts",
            artifactTriggerCondition.getArtifactFilter());
        List<Artifact> matchedArtifacts =
            collectedArtifacts.stream()
                .filter(artifact
                    -> triggerServiceHelper.checkArtifactMatchesArtifactFilter(trigger.getUuid(), artifact,
                        artifactTriggerCondition.getArtifactFilter(), artifactTriggerCondition.isRegex()))
                .collect(Collectors.toList());
        if (isNotEmpty(matchedArtifacts)) {
          logger.info(
              "Matched artifacts {}. Selecting the latest artifact", matchedArtifacts.get(matchedArtifacts.size() - 1));
          artifacts.add(matchedArtifacts.get(matchedArtifacts.size() - 1));
        } else {
          logger.info("Artifacts {} not matched with the given artifact filter", artifacts);
        }
      }
      if (isEmpty(artifacts)) {
        logger.warn(
            "Skipping execution - artifact does not match with the given filter. So, skipping the complete deployment {}",
            artifactTriggerCondition);
        return;
      }
      if (isNotEmpty(trigger.getArtifactSelections())) {
        logger.info("Artifact selections found collecting artifacts as per artifactStream selections");
        addArtifactsFromSelections(trigger.getAppId(), trigger, artifacts);
      }
      if (isNotEmpty(artifacts)) {
        logger.info("The artifacts  set for the trigger {} are {}", trigger.getUuid(),
            artifacts.stream().map(Artifact::getUuid).collect(toList()));
        try {
          triggerDeployment(artifacts, trigger, null);
        } catch (WingsException exception) {
          exception.addContext(Application.class, trigger.getAppId());
          exception.addContext(ArtifactStream.class, artifactStreamId);
          exception.addContext(Trigger.class, trigger.getUuid());
          ExceptionLogger.logProcessedMessages(exception, MANAGER, logger);
        }
      } else {
        logger.info("No Artifacts matched. Hence Skipping the deployment");
        return;
      }
    });
  }

  private void triggerExecutionPostPipelineCompletion(String appId, String sourcePipelineId) {
    triggerServiceHelper.getTriggersMatchesWorkflow(appId, sourcePipelineId).forEach(trigger -> {
      if (trigger.isDisabled()) {
        logger.info("Trigger rejected due to slowness in the product.");
        return;
      }
      List<ArtifactSelection> artifactSelections = trigger.getArtifactSelections();
      if (isEmpty(artifactSelections)) {
        logger.info("No artifactSelection configuration setup found. Executing pipeline {} from source pipeline {}",
            trigger.getWorkflowId(), sourcePipelineId);
        List<Artifact> lastDeployedArtifacts = getLastDeployedArtifacts(appId, sourcePipelineId, null);
        if (isEmpty(lastDeployedArtifacts)) {
          logger.warn(
              "No last deployed artifacts found. Triggering execution {} without artifacts", trigger.getWorkflowId());
        }
        triggerPostPipelineCompletionDeployment(sourcePipelineId, trigger, lastDeployedArtifacts);
      } else {
        List<Artifact> artifacts = new ArrayList<>();
        if (artifactSelections.stream().anyMatch(artifactSelection -> artifactSelection.getType() == PIPELINE_SOURCE)) {
          logger.info("Adding last deployed artifacts from source pipeline {} ", sourcePipelineId);
          addLastDeployedArtifacts(appId, sourcePipelineId, null, artifacts);
        }
        addArtifactsFromSelections(trigger.getAppId(), trigger, artifacts);
        triggerPostPipelineCompletionDeployment(sourcePipelineId, trigger, artifacts);
      }
    });
  }

  private void triggerPostPipelineCompletionDeployment(
      String sourcePipelineId, Trigger trigger, List<Artifact> artifacts) {
    try {
      triggerDeployment(artifacts, trigger, null);
    } catch (WingsException ex) {
      ex.addContext(Application.class, trigger.getAppId());
      ex.addContext(Pipeline.class, sourcePipelineId);
      ex.addContext(Trigger.class, trigger.getUuid());
      ExceptionLogger.logProcessedMessages(ex, MANAGER, logger);
    }
  }

  private void triggerScheduledExecution(Trigger trigger, Date scheduledFireTime) {
    IdempotentId idempotentid = new IdempotentId(trigger.getUuid() + ":" + scheduledFireTime.getTime());
    try (IdempotentLock<TriggerIdempotentResult> idempotent =
             idempotentRegistry.create(idempotentid, ofSeconds(10), ofSeconds(1), ofHours(1))) {
      if (idempotent.alreadyExecuted()) {
        return;
      }
      if (trigger.isDisabled()) {
        logger.info("Trigger rejected due to slowness in the product");
        return;
      }
      logger.info("Received scheduled trigger for appId {} and Trigger Id {} with the scheduled fire time {} ",
          trigger.getAppId(), trigger.getUuid(), scheduledFireTime.getTime());
      List<Artifact> artifacts = new ArrayList<>();
      addArtifactsFromSelections(trigger.getAppId(), trigger, artifacts);

      ScheduledTriggerCondition scheduledTriggerCondition = (ScheduledTriggerCondition) trigger.getCondition();
      if (!scheduledTriggerCondition.isOnNewArtifactOnly() || isEmpty(trigger.getArtifactSelections())) {
        triggerScheduledDeployment(trigger, artifacts);
      } else {
        List<Artifact> lastDeployedArtifacts =
            getLastDeployedArtifacts(trigger.getAppId(), trigger.getWorkflowId(), null);
        List<String> lastDeployedArtifactIds =
            lastDeployedArtifacts.stream().map(Artifact::getUuid).distinct().collect(toList());
        List<String> artifactIds = artifacts.stream().map(Artifact::getUuid).distinct().collect(toList());
        if (lastDeployedArtifactIds.containsAll(artifactIds)) {
          logger.info("No new version of artifacts found from the last successful execution "
                  + "of pipeline/ workflow {}. So, not triggering execution",
              trigger.getWorkflowId());
        } else {
          logger.info("New version of artifacts found from the last successful execution "
                  + "of pipeline/ workflow {}. So, triggering  execution",
              trigger.getWorkflowId());
          triggerScheduledDeployment(trigger, artifacts);
        }
      }
      logger.info("Scheduled trigger for appId {} and Trigger Id {} complete", trigger.getAppId(), trigger.getUuid());
      idempotent.succeeded(TriggerIdempotentResult.builder().triggerUuid(trigger.getUuid()).build());
    } catch (UnableToRegisterIdempotentOperationException e) {
      logger.error("Failed to trigger scheduled trigger {}", trigger.getName(), e);
    }
  }

  private void triggerScheduledDeployment(Trigger trigger, List<Artifact> artifacts) {
    try {
      triggerDeployment(artifacts, trigger, null);
    } catch (WingsException ex) {
      ex.addContext(Application.class, trigger.getAppId());
      ex.addContext(Trigger.class, trigger.getUuid());
      ExceptionLogger.logProcessedMessages(ex, MANAGER, logger);
    }
  }

  private void addArtifactsFromSelections(String appId, Trigger trigger, List<Artifact> artifacts) {
    if (isEmpty(trigger.getArtifactSelections())) {
      return;
    }
    trigger.getArtifactSelections().forEach(artifactSelection -> {
      if (artifactSelection.getType() == LAST_COLLECTED) {
        addLastCollectedArtifact(appId, artifactSelection, artifacts);
      } else if (artifactSelection.getType() == LAST_DEPLOYED) {
        addLastDeployedArtifacts(appId, artifactSelection.getWorkflowId(), artifactSelection.getServiceId(), artifacts);
      }
    });
  }

  private void addLastCollectedArtifact(String appId, ArtifactSelection artifactSelection, List<Artifact> artifacts) {
    ArtifactStream artifactStream = artifactStreamService.get(artifactSelection.getArtifactStreamId());
    notNullCheck("ArtifactStream was deleted", artifactStream, USER);
    Artifact lastCollectedArtifact;
    if (isEmpty(artifactSelection.getArtifactFilter())) {
      lastCollectedArtifact = artifactService.fetchLastCollectedArtifact(artifactStream);
      if (lastCollectedArtifact != null
          && artifactStreamServiceBindingService.listArtifactStreamIds(appId, artifactSelection.getServiceId())
                 .contains(lastCollectedArtifact.getArtifactStreamId())) {
        artifacts.add(lastCollectedArtifact);
      }
    } else {
      lastCollectedArtifact = artifactService.getArtifactByBuildNumber(
          artifactStream, artifactSelection.getArtifactFilter(), artifactSelection.isRegex());
      if (lastCollectedArtifact != null) {
        artifacts.add(lastCollectedArtifact);
      }
    }
  }

  private void addLastDeployedArtifacts(String appId, String workflowId, String serviceId, List<Artifact> artifacts) {
    logger.info("Adding last deployed artifacts for appId {}, workflowid {}", appId, workflowId);
    artifacts.addAll(getLastDeployedArtifacts(appId, workflowId, serviceId));
  }

  private List<Artifact> getLastDeployedArtifacts(String appId, String workflowId, String serviceId) {
    List<Artifact> lastDeployedArtifacts = workflowExecutionService.obtainLastGoodDeployedArtifacts(appId, workflowId);
    if (lastDeployedArtifacts != null && serviceId != null) {
      List<String> artifactStreamIds = artifactStreamServiceBindingService.listArtifactStreamIds(appId, serviceId);
      if (isEmpty(artifactStreamIds)) {
        lastDeployedArtifacts = new ArrayList<>();
      } else {
        lastDeployedArtifacts = lastDeployedArtifacts.stream()
                                    .filter(artifact1 -> artifactStreamIds.contains(artifact1.getArtifactStreamId()))
                                    .collect(toList());
      }
    }
    return lastDeployedArtifacts == null ? new ArrayList<>() : lastDeployedArtifacts;
  }

  private WorkflowExecution triggerDeployment(
      List<Artifact> artifacts, Trigger trigger, TriggerExecution triggerExecution) {
    return triggerDeployment(artifacts, trigger, null, triggerExecution);
  }

  private WorkflowExecution triggerDeployment(
      List<Artifact> artifacts, Trigger trigger, Map<String, String> parameters, TriggerExecution triggerExecution) {
    ExecutionArgs executionArgs = new ExecutionArgs();

    if (isNotEmpty(artifacts)) {
      executionArgs.setArtifacts(
          artifacts.stream().filter(triggerServiceHelper.distinctByKey(Artifact::getUuid)).collect(toList()));
    }
    executionArgs.setOrchestrationId(trigger.getWorkflowId());
    executionArgs.setExecutionCredential(aSSHExecutionCredential().withExecutionType(SSH).build());
    executionArgs.setWorkflowType(trigger.getWorkflowType());
    executionArgs.setExcludeHostsWithSameArtifact(trigger.isExcludeHostsWithSameArtifact());

    if (parameters != null) {
      executionArgs.setWorkflowVariables(parameters);
    }

    WorkflowExecution workflowExecution;
    if (ORCHESTRATION == trigger.getWorkflowType()) {
      workflowExecution = triggerOrchestrationDeployment(trigger, executionArgs, triggerExecution);
    } else {
      workflowExecution = triggerPipelineDeployment(trigger, triggerExecution, executionArgs);
    }
    return workflowExecution;
  }

  private WorkflowExecution triggerPipelineDeployment(
      Trigger trigger, TriggerExecution triggerExecution, ExecutionArgs executionArgs) {
    WorkflowExecution workflowExecution;
    String accountId = appService.getAccountIdByAppId(trigger.getAppId());
    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      logger.info("Triggering  execution of appId {} with  pipeline id {} , trigger type {}", trigger.getAppId(),
          trigger.getWorkflowId(), trigger.getCondition().getConditionType().name());
    }
    boolean infraDefEnabled = featureFlagService.isEnabled(FeatureName.INFRA_MAPPING_REFACTOR, accountId);
    resolveTriggerPipelineVariables(trigger, executionArgs, infraDefEnabled);
    executionArgs.setPipelineId(trigger.getWorkflowId());
    if (webhookTriggerProcessor.checkFileContentOptionSelected(trigger)) {
      logger.info("Check file content option selected. Invoking delegate task to verify the file content.");
      TriggerExecution lastTriggerExecution = webhookTriggerProcessor.fetchLastExecutionForContentChanged(trigger);
      if (lastTriggerExecution == null) {
        triggerExecution.setExecutionArgs(executionArgs);
        triggerExecution.setStatus(Status.SUCCESS);
        triggerExecutionService.save(triggerExecution);
        workflowExecution =
            workflowExecutionService.triggerEnvExecution(trigger.getAppId(), null, executionArgs, trigger);
      } else {
        triggerExecution.setExecutionArgs(executionArgs);
        webhookTriggerProcessor.initiateTriggerContentChangeDelegateTask(
            trigger, lastTriggerExecution, triggerExecution, trigger.getAppId());
        workflowExecution = WorkflowExecution.builder().status(ExecutionStatus.NEW).build();
      }
    } else {
      workflowExecution =
          workflowExecutionService.triggerEnvExecution(trigger.getAppId(), null, executionArgs, trigger);
    }
    logger.info(
        "Pipeline execution of appId {} with  pipeline id {} triggered", trigger.getAppId(), trigger.getWorkflowId());
    return workflowExecution;
  }

  private void resolveTriggerPipelineVariables(Trigger trigger, ExecutionArgs executionArgs, boolean infraDefEnabled) {
    Pipeline pipeline = pipelineService.readPipeline(trigger.getAppId(), trigger.getWorkflowId(), true);
    notNullCheck("Pipeline was deleted or does not exist", pipeline, USER);

    Map<String, String> triggerWorkflowVariableValues =
        overrideTriggerVariables(infraDefEnabled, trigger, executionArgs, pipeline.getPipelineVariables());

    List<Variable> pipelineVariables = pipeline.getPipelineVariables();
    String envId = null;
    String templatizedEnvName = getTemplatizedEnvVariableName(pipelineVariables);
    if (templatizedEnvName != null) {
      logger.info("One of the environment is parameterized in the pipeline and Variable name {}", templatizedEnvName);
      String envNameOrId = triggerWorkflowVariableValues.get(templatizedEnvName);
      if (envNameOrId == null) {
        String msg = "Pipeline contains environment as variable [" + templatizedEnvName
            + "]. However, there is no mapping associated in the trigger."
            + " Please update the trigger";
        throw new WingsException(msg, USER);
      }
      envId = resolveEnvId(trigger, envNameOrId);
      triggerWorkflowVariableValues.put(templatizedEnvName, envId);
    }

    resolveServices(trigger, triggerWorkflowVariableValues, pipelineVariables);
    if (infraDefEnabled) {
      resolveInfraDefinitions(trigger.getAppId(), triggerWorkflowVariableValues, envId, pipelineVariables);
    } else {
      resolveServiceInfrastructures(trigger.getAppId(), triggerWorkflowVariableValues, envId, pipelineVariables);
    }

    executionArgs.setWorkflowVariables(triggerWorkflowVariableValues);

    if (pipeline.isHasBuildWorkflow() || pipeline.isEnvParameterized()) {
      // TODO: Once artifact needed serviceIds implemented for templated pipeline then this logic has to be modified
      return;
    }
    List<String> artifactNeededServiceIds = isEmpty(pipeline.getServices())
        ? new ArrayList<>()
        : pipeline.getServices().stream().map(Service::getUuid).collect(toList());

    validateRequiredArtifacts(trigger, executionArgs, artifactNeededServiceIds);
  }

  private void resolveServices(Trigger trigger, Map<String, String> triggerVariableValues, List<Variable> variables) {
    List<String> serviceWorkflowVariables = getServiceWorkflowVariables(variables);
    for (String serviceVarName : serviceWorkflowVariables) {
      String serviceIdOrName = triggerVariableValues.get(serviceVarName);
      notNullCheck("There is no corresponding Workflow Variable associated to service", serviceIdOrName);
      logger.info("Checking  service {} can be found by id first.", serviceIdOrName);
      Service service = serviceResourceService.get(trigger.getAppId(), serviceIdOrName, false);
      if (service == null) {
        logger.info("Service does not exist by Id, checking if environment {} can be found by name.", serviceIdOrName);
        service = serviceResourceService.getServiceByName(trigger.getAppId(), serviceIdOrName, false);
      }
      notNullCheck("Service [" + serviceIdOrName + "] does not exist", service, USER);
      triggerVariableValues.put(serviceVarName, service.getUuid());
    }
  }

  private String resolveEnvId(Trigger trigger, String envNameOrId) {
    Environment environment;
    logger.info("Checking  environment {} can be found by id first.", envNameOrId);
    environment = environmentService.get(trigger.getAppId(), envNameOrId);
    if (environment == null) {
      logger.info("Environment does not exist by Id, checking if environment {} can be found by name.", envNameOrId);
      environment = environmentService.getEnvironmentByName(trigger.getAppId(), envNameOrId, false);
    }
    notNullCheck("Resolved environment [" + envNameOrId
            + "] does not exist. Please ensure the environment variable mapped to the right payload value in the trigger",
        environment, USER);

    return environment.getUuid();
  }

  private void resolveServiceInfrastructures(
      String appId, Map<String, String> triggerWorkflowVariableValues, String envId, List<Variable> variables) {
    List<String> serviceInfraWorkflowVariables =
        WorkflowServiceTemplateHelper.getServiceInfrastructureWorkflowVariables(variables);
    for (String serviceInfraVarName : serviceInfraWorkflowVariables) {
      String serviceInfraIdOrName = triggerWorkflowVariableValues.get(serviceInfraVarName);
      notNullCheck("There is no corresponding Workflow Variable associated to Service Infrastructure",
          serviceInfraIdOrName, USER);
      logger.info("Checking  Service Infrastructure {} can be found by id first.", serviceInfraIdOrName);
      InfrastructureMapping infrastructureMapping = getInfrastructureMapping(appId, envId, serviceInfraIdOrName);
      notNullCheck("Service Infrastructure [" + serviceInfraIdOrName + "] does not exist", infrastructureMapping, USER);
      triggerWorkflowVariableValues.put(serviceInfraVarName, infrastructureMapping.getUuid());
    }
  }

  void resolveInfraDefinitions(
      String appId, Map<String, String> triggerWorkflowVariableValues, String envId, List<Variable> variables) {
    for (Variable variable : WorkflowServiceTemplateHelper.getInfraDefCompleteWorkflowVariables(variables)) {
      String infraEnvId = null;
      String infraDefVarName = variable.getName();
      String infraDefIdOrName = triggerWorkflowVariableValues.get(infraDefVarName);

      if (isNotEmpty(variable.getMetadata()) && variable.getMetadata().get(Variable.ENV_ID) != null) {
        infraEnvId = variable.getMetadata().get(Variable.ENV_ID).toString();
      } else {
        infraEnvId = envId;
      }

      if (isEmpty(infraDefVarName) || matchesVariablePattern(infraDefIdOrName)) {
        String infraMappingVarName = infraDefVarName.replace("InfraDefinition", "ServiceInfra");
        String infraMappingIdOrName = triggerWorkflowVariableValues.get(infraMappingVarName);
        InfrastructureMapping infrastructureMapping = getInfrastructureMapping(appId, infraEnvId, infraMappingIdOrName);
        notNullCheck(
            "Service Infrastructure [" + infraMappingIdOrName + "] does not exist", infrastructureMapping, USER);
        triggerWorkflowVariableValues.put(infraDefVarName, infrastructureMapping.getInfrastructureDefinitionId());
      } else {
        InfrastructureDefinition infrastructureDefinition =
            getInfrastructureDefinition(appId, infraEnvId, infraDefIdOrName);
        if (infrastructureDefinition == null) {
          InfrastructureMapping infrastructureMapping = getInfrastructureMapping(appId, infraEnvId, infraDefIdOrName);
          notNullCheck("Service Infrastructure [" + infraDefIdOrName + "] does not exist", infrastructureMapping, USER);
          triggerWorkflowVariableValues.put(infraDefVarName, infrastructureMapping.getInfrastructureDefinitionId());
        } else {
          triggerWorkflowVariableValues.put(infraDefVarName, infrastructureDefinition.getUuid());
        }
      }
    }
  }

  InfrastructureDefinition getInfrastructureDefinition(String appId, String envId, String infraDefIdOrName) {
    InfrastructureDefinition infrastructureDefinition = infrastructureDefinitionService.get(appId, infraDefIdOrName);
    if (infrastructureDefinition == null) {
      logger.info("InfraDefinition does not exist by Id, checking if infra definition {} can be found by name.",
          infraDefIdOrName);
      infrastructureDefinition = infrastructureDefinitionService.getInfraDefByName(appId, envId, infraDefIdOrName);
    }
    return infrastructureDefinition;
  }

  InfrastructureMapping getInfrastructureMapping(String appId, String envId, String infraDefIdOrName) {
    InfrastructureMapping infrastructureMapping = infrastructureMappingService.get(appId, infraDefIdOrName);
    if (infrastructureMapping == null) {
      logger.info(
          "Service Infrastructure does not exist by Id, checking if service infrastructure {} can be found by name.",
          infraDefIdOrName);
      infrastructureMapping = infrastructureMappingService.getInfraMappingByName(appId, envId, infraDefIdOrName);
    }
    return infrastructureMapping;
  }

  private WorkflowExecution triggerOrchestrationDeployment(
      Trigger trigger, ExecutionArgs executionArgs, TriggerExecution triggerExecution) {
    String accountId = appService.getAccountIdByAppId(trigger.getAppId());
    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      logger.info("Triggering workflow execution of appId {} with with workflow id {} , trigger type {}",
          trigger.getAppId(), trigger.getWorkflowId(), trigger.getCondition().getConditionType().name());
    }

    Workflow workflow = workflowService.readWorkflow(trigger.getAppId(), trigger.getWorkflowId());
    notNullCheck("Workflow was deleted", workflow, USER);
    notNullCheck("Orchestration Workflow not present", workflow.getOrchestrationWorkflow(), USER);
    boolean infraDefEnabled = featureFlagService.isEnabled(FeatureName.INFRA_MAPPING_REFACTOR, workflow.getAccountId());

    Map<String, String> triggerWorkflowVariableValues = overrideTriggerVariables(
        infraDefEnabled, trigger, executionArgs, workflow.getOrchestrationWorkflow().getUserVariables());

    String envId = null;
    if (BUILD == workflow.getOrchestrationWorkflow().getOrchestrationWorkflowType()) {
      executionArgs.setArtifacts(new ArrayList<>());
    } else {
      List<Variable> workflowVariables = workflow.getOrchestrationWorkflow().getUserVariables();
      if (workflow.checkEnvironmentTemplatized()) {
        String templatizedEnvName = getTemplatizedEnvVariableName(workflowVariables);
        String envNameOrId = triggerWorkflowVariableValues.get(templatizedEnvName);
        notNullCheck(
            "Workflow Environment is templatized. However, there is no corresponding mapping associated in the trigger. "
                + " Please update the trigger",
            envNameOrId, USER);
        envId = resolveEnvId(trigger, envNameOrId);
        triggerWorkflowVariableValues.put(templatizedEnvName, envId);
      } else {
        envId = workflow.getEnvId();
      }
      notNullCheck("Environment  [" + envId + "] might have been deleted", envId, USER);

      resolveServices(trigger, triggerWorkflowVariableValues, workflowVariables);
      if (infraDefEnabled) {
        resolveInfraDefinitions(trigger.getAppId(), triggerWorkflowVariableValues, envId, workflowVariables);
      } else {
        resolveServiceInfrastructures(trigger.getAppId(), triggerWorkflowVariableValues, envId, workflowVariables);
      }

      /* Fetch the deployment data to find out the required entity types */
      DeploymentMetadata deploymentMetadata = workflowService.fetchDeploymentMetadata(
          trigger.getAppId(), workflow, triggerWorkflowVariableValues, null, null, Include.ARTIFACT_SERVICE);

      // Fetch the service
      List<String> artifactNeededServiceIds =
          deploymentMetadata == null ? new ArrayList<>() : deploymentMetadata.getArtifactRequiredServiceIds();
      validateRequiredArtifacts(trigger, executionArgs, artifactNeededServiceIds);
    }

    executionArgs.setWorkflowVariables(triggerWorkflowVariableValues);
    // Validate if the file path content changed
    logger.info("Triggering workflow execution of appId {} with workflow id {} triggered", trigger.getAppId(),
        trigger.getWorkflowId());
    WorkflowExecution workflowExecution;
    if (webhookTriggerProcessor.checkFileContentOptionSelected(trigger)) {
      TriggerExecution lastTriggerExecution = webhookTriggerProcessor.fetchLastExecutionForContentChanged(trigger);
      if (lastTriggerExecution == null) {
        triggerExecution.setStatus(Status.SUCCESS);
        triggerExecution.setExecutionArgs(executionArgs);
        triggerExecution.setEnvId(envId);
        triggerExecutionService.save(triggerExecution);
        workflowExecution =
            workflowExecutionService.triggerEnvExecution(trigger.getAppId(), envId, executionArgs, trigger);
      } else {
        logger.info("Check file content option selected. Invoking delegate task to verify the file content.");
        triggerExecution.setExecutionArgs(executionArgs);
        webhookTriggerProcessor.initiateTriggerContentChangeDelegateTask(
            trigger, lastTriggerExecution, triggerExecution, trigger.getAppId());
        workflowExecution = WorkflowExecution.builder().status(ExecutionStatus.NEW).build();
      }
    } else {
      workflowExecution =
          workflowExecutionService.triggerEnvExecution(trigger.getAppId(), envId, executionArgs, trigger);
    }
    return workflowExecution;
  }

  private void validateRequiredArtifacts(
      Trigger trigger, ExecutionArgs executionArgs, List<String> artifactNeededServiceIds) {
    // Artifact serviceIds
    List<String> collectedArtifactServiceIds = triggerServiceHelper.obtainCollectedArtifactServiceIds(executionArgs);
    String accountId = appService.getAccountIdByAppId(trigger.getAppId());
    if (isEmpty(artifactNeededServiceIds) && isNotEmpty(collectedArtifactServiceIds)) {
      WorkflowType workflowType = trigger.getWorkflowType() == null ? PIPELINE : trigger.getWorkflowType();
      StringBuilder msg = new StringBuilder(128);
      msg.append("Trigger [")
          .append(trigger.getName())
          .append("] rejected. Reason: ")
          .append(PIPELINE == workflowType ? "Pipeline" : "Workflow")
          .append(" [")
          .append(trigger.fetchWorkflowOrPipelineName())
          .append("] does not need artifacts. However, trigger received with the artifacts");

      if (featureFlagService.isEnabled(FeatureName.REJECT_TRIGGER_IF_ARTIFACTS_NOT_MATCH, accountId)) {
        logger.warn(msg.toString());
        throw new WingsException(msg.toString());
      }
    }
    List<String> missingServiceIds = new ArrayList<>();
    for (String artifactNeededServiceId : artifactNeededServiceIds) {
      if (collectedArtifactServiceIds.contains(artifactNeededServiceId)) {
        collectedArtifactServiceIds.remove(artifactNeededServiceId);
      } else {
        missingServiceIds.add(artifactNeededServiceId);
      }
    }
    if (isNotEmpty(missingServiceIds)) {
      logger.info(
          "Artifact needed serviceIds {} do not match with the collected artifact serviceIds {}. Rejecting the trigger {} execution",
          artifactNeededServiceIds, collectedArtifactServiceIds, trigger.getUuid());
      List<String> missingServiceNames =
          serviceResourceService.fetchServiceNamesByUuids(trigger.getAppId(), missingServiceIds);

      if (featureFlagService.isEnabled(FeatureName.REJECT_TRIGGER_IF_ARTIFACTS_NOT_MATCH, accountId)) {
        logger.warn("Trigger rejected. Reason: Artifacts are missing for service name(s) {}", missingServiceNames);
        String message = "Trigger [" + trigger.getName()
            + " ] rejected. Reason: Artifacts are missing for service name(s)" + missingServiceNames;
        throw new WingsException(message, USER);
      }
    }
    if (isNotEmpty(collectedArtifactServiceIds)) {
      WorkflowType workflowType = trigger.getWorkflowType() == null ? PIPELINE : trigger.getWorkflowType();
      StringBuilder msg =
          new StringBuilder("Trigger rejected. Reason: More artifacts received than required artifacts for ");
      msg.append(PIPELINE == workflowType ? "Pipeline" : "Workflow")
          .append(" [")
          .append(trigger.fetchWorkflowOrPipelineName())
          .append(']');

      if (featureFlagService.isEnabled(FeatureName.REJECT_TRIGGER_IF_ARTIFACTS_NOT_MATCH, accountId)) {
        logger.warn(msg.toString());
        throw new WingsException(msg.toString());
      }
    }
  }

  @Override
  public boolean triggerExecutionByServiceInfra(String appId, String infraMappingId) {
    logger.info("Received the trigger execution for appId {} and infraMappingId {}", appId, infraMappingId);
    InfrastructureMapping infrastructureMapping = infrastructureMappingService.get(appId, infraMappingId);
    if (infrastructureMapping == null) {
      throw new InvalidRequestException("Infrastructure Mapping" + infraMappingId + " does not exist", USER);
    }

    List<ServiceInfraWorkflow> serviceInfraWorkflows =
        triggerServiceHelper.getServiceInfraWorkflows(appId, infraMappingId);

    List<String> serviceIds = Collections.singletonList(infrastructureMapping.getServiceId());
    List<String> envIds = Collections.singletonList(infrastructureMapping.getEnvId());

    serviceInfraWorkflows.forEach((ServiceInfraWorkflow serviceInfraWorkflow) -> {
      if (serviceInfraWorkflow.getWorkflowType() == null || serviceInfraWorkflow.getWorkflowType() == ORCHESTRATION) {
        logger.info("Retrieving the last workflow execution for workflowId {} and infraMappingId {}",
            serviceInfraWorkflow.getWorkflowId(), infraMappingId);
        WorkflowExecution workflowExecution = workflowExecutionService.fetchWorkflowExecution(
            appId, serviceIds, envIds, serviceInfraWorkflow.getWorkflowId());
        if (workflowExecution == null) {
          logger.warn("No Last workflow execution found for workflowId {} and infraMappingId {}",
              serviceInfraWorkflow.getWorkflowId(), serviceInfraWorkflow.getInfraMappingId());
        } else {
          logger.info("Triggering workflow execution {}  for appId {} and infraMappingId {}",
              workflowExecution.getUuid(), workflowExecution.getWorkflowId(), infraMappingId);
          // TODO: Refactor later
          workflowExecutionService.triggerEnvExecution(
              appId, workflowExecution.getEnvId(), workflowExecution.getExecutionArgs(), null);
        }
      }
    });
    return true;
  }

  @Override
  public void handleTriggerTaskResponse(String appId, String triggerExecutionId, TriggerResponse triggerResponse) {
    logger.info("Received the call back from delegate with the task response {}", triggerResponse);
    TriggerExecution triggerExecution = triggerExecutionService.get(appId, triggerExecutionId);
    notNullCheck("Trigger execution might have pruned", triggerExecution);
    Trigger trigger = get(triggerExecution.getAppId(), triggerExecution.getTriggerId());
    notNullCheck("Trigger might have been deleted", trigger);
    ExecutionStatus taskStatus = triggerResponse.getExecutionStatus();
    if (ExecutionStatus.SUCCESS == taskStatus) {
      if (triggerResponse instanceof TriggerDeploymentNeededResponse) {
        TriggerDeploymentNeededResponse triggerDeploymentNeededResponse =
            (TriggerDeploymentNeededResponse) triggerResponse;
        if (triggerDeploymentNeededResponse.isDeploymentNeeded()) {
          try {
            logger.info("File path content changed for the trigger {}.", trigger.getUuid());
            switch (trigger.getWorkflowType()) {
              case ORCHESTRATION:
                logger.info("Starting deployment for the workflow {}", trigger.getWorkflowId());
                workflowExecutionService.triggerEnvExecution(
                    trigger.getAppId(), triggerExecution.getEnvId(), triggerExecution.getExecutionArgs(), trigger);
                triggerExecutionService.updateStatus(appId, triggerExecutionId, Status.SUCCESS, "File content changed");
                break;
              case PIPELINE:
                logger.info("Starting deployment for the pipeline {}", trigger.getPipelineId());
                triggerExecution.getExecutionArgs().setPipelineId(trigger.getWorkflowId());
                workflowExecutionService.triggerEnvExecution(
                    trigger.getAppId(), null, triggerExecution.getExecutionArgs(), trigger);
                triggerExecutionService.updateStatus(appId, triggerExecutionId, Status.SUCCESS, "File content changed");
                break;
              default:
                unhandled(triggerExecution.getWorkflowType());
            }
          } catch (WingsException exception) {
            // Update trigger Status Failed
            triggerExecutionService.updateStatus(
                appId, triggerExecutionId, Status.FAILED, ExceptionUtils.getMessage(exception));
            exception.addContext(Application.class, trigger.getAppId());
            exception.addContext(TriggerExecution.class, triggerExecution.getUuid());
            exception.addContext(Trigger.class, trigger.getUuid());
            ExceptionLogger.logProcessedMessages(exception, MANAGER, logger);
          } catch (Exception exception) {
            triggerExecutionService.updateStatus(
                appId, triggerExecutionId, Status.FAILED, ExceptionUtils.getMessage(exception));
            logger.error("Exception occurred while starting deployment of the trigger execution {}",
                triggerExecution.getUuid(), exception);
          }
        } else {
          logger.info("File  content not changed for the trigger {}. Skipping the execution", trigger.getUuid());
          triggerExecutionService.updateStatus(
              appId, triggerExecutionId, Status.SUCCESS, "File content not changed. Skipped deployment");
        }
      } else {
        logger.error("Wrong Response {} Received from trigger callback", triggerResponse);
      }
    } else {
      triggerExecutionService.updateStatus(appId, triggerExecutionId, Status.FAILED, triggerResponse.getErrorMsg());
    }
  }

  private void validateAndSetTriggerCondition(Trigger trigger, Trigger existingTrigger) {
    switch (trigger.getCondition().getConditionType()) {
      case NEW_ARTIFACT:
        validateAndSetNewArtifactCondition(trigger);
        break;
      case PIPELINE_COMPLETION:
        PipelineTriggerCondition triggerCondition = (PipelineTriggerCondition) trigger.getCondition();
        triggerCondition.setPipelineName(
            pipelineService.fetchPipelineName(trigger.getAppId(), triggerCondition.getPipelineId()));
        break;
      case WEBHOOK:
        WebHookTriggerCondition webHookTriggerCondition = (WebHookTriggerCondition) trigger.getCondition();
        WebHookToken webHookToken = generateWebHookToken(trigger, getExistingWebhookToken(existingTrigger));
        webHookTriggerCondition.setWebHookToken(webHookToken);
        if (BITBUCKET == webHookTriggerCondition.getWebhookSource()
            && isNotEmpty(webHookTriggerCondition.getActions())) {
          throw new InvalidRequestException("Actions not supported for Bit Bucket", USER);
        }
        trigger.setWebHookToken(webHookTriggerCondition.getWebHookToken().getWebHookToken());
        if (webHookTriggerCondition.isCheckFileContentChanged()) {
          logger.info("File paths to watch selected");
          List<String> filePaths = trimStrings(webHookTriggerCondition.getFilePaths());
          if (isEmpty(filePaths)) {
            throw new InvalidRequestException("At least one file path is required to check content changed");
          }
          if (isEmpty(webHookTriggerCondition.getGitConnectorId())) {
            throw new InvalidRequestException("Git connector is required to check content changed");
          }
          String branchName = StringUtils.trim(webHookTriggerCondition.getBranchName());
          if (isEmpty(branchName)) {
            throw new InvalidRequestException("Branch name is required to check content changed");
          } else {
            webHookTriggerCondition.setBranchName(branchName);
          }
          if (isEmpty(webHookTriggerCondition.getEventTypes())
              || !webHookTriggerCondition.getEventTypes().contains(WebhookEventType.PUSH)) {
            throw new InvalidRequestException("File content check supported only for PUSH events");
          }
        } else {
          // Clear the content
          webHookTriggerCondition.setFilePaths(null);
          webHookTriggerCondition.setBranchName(null);
        }
        break;
      case SCHEDULED:
        ScheduledTriggerCondition scheduledTriggerCondition = (ScheduledTriggerCondition) trigger.getCondition();
        notNullCheck("CronExpression", scheduledTriggerCondition.getCronExpression(), USER);
        break;
      case NEW_INSTANCE:
        NewInstanceTriggerCondition newInstanceTriggerCondition = (NewInstanceTriggerCondition) trigger.getCondition();
        notNullCheck("NewInstanceTriggerCondition", newInstanceTriggerCondition, USER);
        validateAndSetServiceInfraWorkflows(trigger);
        break;
      default:
        throw new InvalidRequestException("Invalid trigger condition type", USER);
    }
  }

  private WebHookToken getExistingWebhookToken(Trigger existingTrigger) {
    WebHookToken existingWebhookToken = null;
    if (existingTrigger != null && existingTrigger.getCondition().getConditionType() == WEBHOOK) {
      WebHookTriggerCondition existingTriggerCondition = (WebHookTriggerCondition) existingTrigger.getCondition();
      existingWebhookToken = existingTriggerCondition.getWebHookToken();
    }
    return existingWebhookToken;
  }

  private void validateAndSetNewArtifactCondition(Trigger trigger) {
    ArtifactTriggerCondition artifactTriggerCondition = (ArtifactTriggerCondition) trigger.getCondition();
    String artifactFilter = artifactTriggerCondition.getArtifactFilter();
    if (isNotEmpty(artifactFilter)) {
      validateArtifactFilter(artifactTriggerCondition.isRegex(), artifactFilter);
    }
    ArtifactStream artifactStream = artifactStreamService.get(artifactTriggerCondition.getArtifactStreamId());
    notNullCheck("Artifact Source is mandatory for New Artifact Condition Trigger", artifactStream, USER);
    Service service =
        artifactStreamServiceBindingService.getService(trigger.getAppId(), artifactStream.getUuid(), false);
    notNullCheck("Service does not exist", service, USER);
    artifactTriggerCondition.setArtifactSourceName(artifactStream.getSourceName() + " (" + service.getName() + ")");
  }

  private void validateArtifactFilter(Boolean isRegex, String artifactFilter) {
    if (isRegex) {
      try {
        compile(artifactFilter);
      } catch (PatternSyntaxException pe) {
        throw new WingsException("Invalid Build/Tag Filter, Please provide a valid regex", USER);
      }
    } else {
      try {
        compile(artifactFilter.replace(".", "\\.").replace("?", ".?").replace("*", ".*?"));
      } catch (PatternSyntaxException pe) {
        throw new WingsException("Invalid Build/Tag Filter", USER);
      }
    }
  }

  private void validateAndSetArtifactSelections(Trigger trigger, List<Service> services) {
    List<ArtifactSelection> artifactSelections = trigger.getArtifactSelections();
    if (isEmpty(artifactSelections)) {
      return;
    }

    artifactSelections.forEach(artifactSelection -> {
      switch (artifactSelection.getType()) {
        case LAST_DEPLOYED:
          validateAndSetLastDeployedArtifactSelection(trigger, artifactSelection);
          break;
        case LAST_COLLECTED:
          if (isEmpty(artifactSelection.getArtifactStreamId())) {
            throw new WingsException("Artifact Source cannot be empty for Last collected type");
          }
          setArtifactSourceName(trigger, artifactSelection);
          break;
        case WEBHOOK_VARIABLE:
          WebHookTriggerCondition webHookTriggerCondition = (WebHookTriggerCondition) trigger.getCondition();
          if (webHookTriggerCondition.getWebhookSource() == null) {
            if (isNotEmpty(artifactSelection.getArtifactStreamId())) {
              setArtifactSourceName(trigger, artifactSelection);
            }
          }
          break;
        case ARTIFACT_SOURCE:
        case PIPELINE_SOURCE:
          break;
        default:
          throw new InvalidRequestException("Invalid artifact selection type", USER);
      }
      setServiceName(trigger, services, artifactSelection);
    });
  }

  private void setArtifactSourceName(Trigger trigger, ArtifactSelection artifactSelection) {
    ArtifactStream artifactStream;
    Service service;
    artifactStream = artifactStreamService.get(artifactSelection.getArtifactStreamId());
    notNullCheck("Artifact Source does not exist", artifactStream, USER);
    service = artifactStreamServiceBindingService.getService(trigger.getAppId(), artifactStream.getUuid(), false);
    notNullCheck("Service might have been deleted", service, USER);
    artifactSelection.setArtifactSourceName(artifactStream.getSourceName() + " (" + service.getName() + ")");
  }

  private void setServiceName(Trigger trigger, List<Service> services, ArtifactSelection artifactSelection) {
    Map<String, String> serviceIdNames = services.stream().collect(toMap(Service::getUuid, Service::getName));
    Service service;
    if (serviceIdNames.get(artifactSelection.getServiceId()) == null) {
      service = serviceResourceService.get(trigger.getAppId(), artifactSelection.getServiceId(), false);
      notNullCheck("Service might have been deleted", service, USER);
      artifactSelection.setServiceName(service.getName());
    } else {
      artifactSelection.setServiceName(serviceIdNames.get(artifactSelection.getServiceId()));
    }
  }

  private void validateAndSetLastDeployedArtifactSelection(Trigger trigger, ArtifactSelection artifactSelection) {
    if (isBlank(artifactSelection.getWorkflowId())) {
      throw new InvalidRequestException("Workflow/Pipeline cannot be empty for Last deployed type", USER);
    }
    if (ORCHESTRATION == trigger.getWorkflowType()) {
      artifactSelection.setWorkflowName(
          workflowService.fetchWorkflowName(trigger.getAppId(), artifactSelection.getWorkflowId()));
    } else {
      artifactSelection.setWorkflowName(
          pipelineService.fetchPipelineName(trigger.getAppId(), artifactSelection.getWorkflowId()));
    }
  }

  private void validateAndSetServiceInfraWorkflows(Trigger trigger) {
    List<ServiceInfraWorkflow> serviceInfraWorkflows = trigger.getServiceInfraWorkflows();
    if (serviceInfraWorkflows == null) {
      throw new WingsException("ServiceInfra and Workflow Mapping can not be empty.", USER);
    } else {
      serviceInfraWorkflows.forEach(serviceInfraWorkflow -> {
        InfrastructureMapping infrastructureMapping =
            infrastructureMappingService.get(trigger.getAppId(), serviceInfraWorkflow.getInfraMappingId());
        notNullCheck("ServiceInfraStructure", infrastructureMapping, USER);
        serviceInfraWorkflow.setInfraMappingName(infrastructureMapping.getName());
        Workflow workflow = workflowService.readWorkflow(trigger.getAppId(), serviceInfraWorkflow.getWorkflowId());
        notNullCheck("Workflow", workflow, USER);
        if (workflow.isTemplatized()) {
          serviceInfraWorkflow.setWorkflowName(workflow.getName() + " (TEMPLATE)");
        } else {
          serviceInfraWorkflow.setWorkflowName(workflow.getName());
        }
      });
    }
  }

  private void validateInput(Trigger trigger, Trigger existingTrigger) {
    List<Service> services;
    if (PIPELINE == trigger.getWorkflowType()) {
      Pipeline executePipeline = pipelineService.readPipeline(trigger.getAppId(), trigger.getWorkflowId(), true);
      trigger.setWorkflowName(executePipeline.getName());
      services = executePipeline.getServices();
      validateAndSetArtifactSelections(trigger, services);
    } else if (ORCHESTRATION == trigger.getWorkflowType()) {
      Workflow workflow = workflowService.readWorkflow(trigger.getAppId(), trigger.getWorkflowId());
      notNullCheckWorkflow(workflow);
      if (workflow.isTemplatized()) {
        trigger.setWorkflowName(workflow.getName() + " (TEMPLATE)");
      } else {
        trigger.setWorkflowName(workflow.getName());
      }
      services = workflow.getServices();
      validateAndSetArtifactSelections(trigger, services);
    }
    validateAndSetTriggerCondition(trigger, existingTrigger);
    validateAndSetCronExpression(trigger);
  }

  private WebHookToken generateWebHookToken(Trigger trigger, WebHookToken existingToken) {
    List<Service> services = null;
    boolean artifactNeeded = true;
    Map<String, String> parameters = new LinkedHashMap<>();
    if (PIPELINE == trigger.getWorkflowType()) {
      Pipeline pipeline = pipelineService.readPipeline(trigger.getAppId(), trigger.getWorkflowId(), true);
      services = pipeline.getServices();
      if (pipeline.isHasBuildWorkflow()) {
        artifactNeeded = false;
      }
      addVariables(parameters, pipeline.getPipelineVariables());
    } else if (ORCHESTRATION == trigger.getWorkflowType()) {
      Workflow workflow = workflowService.readWorkflow(trigger.getAppId(), trigger.getWorkflowId());
      services = workflow.getServices();
      Map<String, String> workflowVariables = trigger.getWorkflowVariables();
      if (isNotEmpty(workflowVariables)) {
        if (BUILD == workflow.getOrchestrationWorkflow().getOrchestrationWorkflowType()) {
          artifactNeeded = false;
        } else {
          if (workflow.getOrchestrationWorkflow().isServiceTemplatized()) {
            services = workflowService.getResolvedServices(workflow, workflowVariables);
          }
        }
      }
      addVariables(parameters, workflow.getOrchestrationWorkflow().getUserVariables());
    }
    return constructWebhookToken(trigger, existingToken, services, artifactNeeded, parameters);
  }

  private void addVariables(Map<String, String> parameters, List<Variable> variables) {
    if (isNotEmpty(variables)) {
      variables.forEach(variable -> { parameters.put(variable.getName(), variable.getName() + "_placeholder"); });
    }
  }

  private void addOrUpdateCronForScheduledJob(Trigger trigger, Trigger existingTrigger) {
    if (existingTrigger.getCondition().getConditionType() == SCHEDULED) {
      if (trigger.getCondition().getConditionType() == SCHEDULED) {
        TriggerKey triggerKey = new TriggerKey(trigger.getUuid(), ScheduledTriggerJob.GROUP);
        jobScheduler.rescheduleJob(triggerKey, ScheduledTriggerJob.getQuartzTrigger(trigger));
      } else {
        jobScheduler.deleteJob(existingTrigger.getUuid(), ScheduledTriggerJob.GROUP);
      }
    } else if (trigger.getCondition().getConditionType() == SCHEDULED) {
      String accountId = appService.getAccountIdByAppId(trigger.getAppId());
      ScheduledTriggerJob.add(jobScheduler, accountId, trigger.getAppId(), trigger.getUuid(), trigger);
    }
  }

  private void collectArtifacts(Trigger trigger, Map<String, ArtifactSummary> serviceArtifactMapping,
      List<Artifact> artifacts, Map<String, String> services) {
    boolean collectedArtifactsFromPayload = false;
    for (ArtifactSelection artifactSelection : trigger.getArtifactSelections()) {
      if (artifactSelection.getType() == WEBHOOK_VARIABLE) {
        if (isNotEmpty(artifactSelection.getArtifactStreamId())) {
          ArtifactSummary artifactSummary = serviceArtifactMapping.get(artifactSelection.getServiceId());
          if (artifactSummary == null) {
            throw new InvalidRequestException("Artifact Service not matching with Trigger Service ["
                + services.get(artifactSelection.getServiceId()) + "]");
          }
          String buildNumber = artifactSummary.getBuildNo();
          if (isBlank(buildNumber)) {
            throw new InvalidRequestException("Build Number is Mandatory", USER);
          } else {
            Artifact artifact = getAlreadyCollectedOrCollectNewArtifactForBuildNumber(
                trigger.getAppId(), artifactSelection.getArtifactStreamId(), buildNumber);
            if (artifact != null) {
              artifacts.add(artifact);
            }
          }
        } else {
          // Prepare artifact Stream BuildNumber Mapping
          if (!collectedArtifactsFromPayload) {
            List<Artifact> artifactList =
                collectArtifactsFromPayload(trigger.getAppId(), serviceArtifactMapping, services);
            collectedArtifactsFromPayload = true;
            artifacts.addAll(artifactList);
          }
        }
      }
    }

    try {
      if (!collectedArtifactsFromPayload && isNotEmpty(serviceArtifactMapping)
          && (trigger.getCondition().getConditionType() == WEBHOOK)) {
        collectArtifactsForTemplatizedService(trigger, serviceArtifactMapping, artifacts, services);
      }
    } catch (Exception e) {
      logger.error("failed to process artifacts and services from webhook", e);
    }
  }

  private void collectArtifactsForTemplatizedService(Trigger trigger,
      Map<String, ArtifactSummary> serviceArtifactMapping, List<Artifact> artifacts, Map<String, String> services) {
    Map<String, ArtifactSummary> filteredServiceArtifactMapping =
        serviceArtifactMapping.entrySet()
            .stream()
            .filter(map -> {
              String serviceId = map.getKey();
              for (ArtifactSelection artifactSelection : trigger.getArtifactSelections()) {
                if (artifactSelection.getServiceId().equals(serviceId)) {
                  return false;
                }
              }
              return true;
            })
            .collect(Collectors.toMap(map -> map.getKey(), map -> map.getValue()));

    if (isNotEmpty(filteredServiceArtifactMapping)) {
      List<Artifact> artifactList =
          collectArtifactsFromPayload(trigger.getAppId(), filteredServiceArtifactMapping, services);
      artifacts.addAll(artifactList);
    }
  }

  private List<Artifact> collectArtifactsFromPayload(
      String appId, Map<String, ArtifactSummary> serviceArtifactMapping, Map<String, String> services) {
    ArtifactStream artifactStream;
    List<Artifact> artifacts = new ArrayList<>();
    for (Entry<String, ArtifactSummary> serviceArtifactSummaryEntry : serviceArtifactMapping.entrySet()) {
      ArtifactSummary artifactSummary = serviceArtifactSummaryEntry.getValue();
      if (isNotEmpty(artifactSummary.getName())) {
        artifactStream = artifactStreamService.getArtifactStreamByName(
            appId, serviceArtifactSummaryEntry.getKey(), artifactSummary.getName());
      } else {
        List<String> artifactStreamIds =
            artifactStreamService.fetchArtifactStreamIdsForService(appId, serviceArtifactSummaryEntry.getKey());
        if (isEmpty(artifactStreamIds)) {
          throw new InvalidRequestException("No artifact sources defined for the service ["
              + services.get(serviceArtifactSummaryEntry.getKey()) + "]");
        }
        if (artifactStreamIds.size() > 1) {
          throw new InvalidRequestException("More than one artifact source defined for the service ["
              + serviceArtifactSummaryEntry.getKey() + "]. Please provide artifact source name");
        }
        artifactStream = artifactStreamService.get(artifactStreamIds.get(0));
      }
      if (artifactStream == null) {
        throw new InvalidRequestException("Artifact Source [" + artifactSummary.getName()
                + "] does not exist for the Service +[" + services.get(serviceArtifactSummaryEntry.getKey()) + "]",
            USER);
      }
      Artifact artifact = getAlreadyCollectedOrCollectNewArtifactForBuildNumber(
          appId, artifactStream.getUuid(), artifactSummary.getBuildNo());
      if (artifact != null) {
        artifacts.add(artifact);
      }
    }

    return artifacts;
  }

  private Artifact getAlreadyCollectedOrCollectNewArtifactForBuildNumber(
      String appId, String artifactStreamId, String buildNumber) {
    ArtifactStream artifactStream = artifactStreamService.get(artifactStreamId);
    notNullCheck("Artifact Source doesn't exist", artifactStream, USER);
    Artifact collectedArtifactForBuildNumber =
        artifactService.getArtifactByBuildNumber(artifactStream, buildNumber, false);

    return collectedArtifactForBuildNumber != null
        ? collectedArtifactForBuildNumber
        : collectNewArtifactForBuildNumber(appId, artifactStream, buildNumber);
  }

  private Artifact collectNewArtifactForBuildNumber(String appId, ArtifactStream artifactStream, String buildNumber) {
    Artifact artifact = artifactCollectionServiceAsync.collectNewArtifacts(appId, artifactStream, buildNumber);
    if (artifact != null) {
      logger.info("Artifact {} collected for the build number {} of stream id {}", artifact, buildNumber,
          artifactStream.getUuid());
    } else {
      logger.warn(
          "Artifact collection invoked. However, Artifact not yet collected for the build number {} of stream id {}",
          buildNumber, artifactStream.getUuid());
    }
    return artifact;
  }

  private void addArtifactsFromVersionsOfWebHook(
      Trigger trigger, Map<String, ArtifactSummary> serviceArtifactMapping, List<Artifact> artifacts) {
    Map<String, String> services;
    if (ORCHESTRATION == trigger.getWorkflowType()) {
      services = resolveWorkflowServices(trigger);
    } else {
      Pipeline pipeline = pipelineService.readPipeline(trigger.getAppId(), trigger.getWorkflowId(), true);
      services = pipeline.getServices().stream().collect(toMap(Service::getUuid, Service::getName));
    }
    collectArtifacts(trigger, serviceArtifactMapping, artifacts, services);
  }

  private Map<String, String> resolveWorkflowServices(Trigger trigger) {
    Workflow workflow = workflowService.readWorkflow(trigger.getAppId(), trigger.getWorkflowId());
    if (workflow == null) {
      throw new WingsException("Workflow " + trigger.fetchWorkflowOrPipelineName() + " does not exist.", USER);
    }
    List<Service> workflowServices = workflow.getServices();
    if (BUILD != workflow.getOrchestrationWorkflow().getOrchestrationWorkflowType()
        && workflow.getOrchestrationWorkflow().isServiceTemplatized()) {
      workflowServices = workflowService.getResolvedServices(workflow, trigger.getWorkflowVariables());
    }
    return isEmpty(workflowServices) ? new HashMap<>()
                                     : workflowServices.stream().collect(toMap(Service::getUuid, Service::getName));
  }

  @Override
  public boolean triggerActionExists(Trigger trigger) {
    WorkflowType workflowType = trigger.getWorkflowType();
    if (PIPELINE == workflowType) {
      return pipelineService.pipelineExists(trigger.getAppId(), trigger.getPipelineId());
    } else if (WorkflowType.ORCHESTRATION == workflowType) {
      return workflowService.workflowExists(trigger.getAppId(), trigger.getWorkflowId());
    }

    return true;
  }

  @Override
  public void authorize(Trigger trigger, boolean existing) {
    WorkflowType workflowType = trigger.getWorkflowType();
    try {
      triggerAuthHandler.authorizeWorkflowOrPipeline(trigger.getAppId(), trigger.getWorkflowId(), existing);
    } catch (WingsException ex) {
      throw new WingsException("User does not have deployment execution permission on "
              + (workflowType == PIPELINE ? "Pipeline" : "Workflow"),
          USER);
    }
    boolean envParamaterized;
    List<Variable> variables;
    if (PIPELINE == workflowType) {
      Pipeline pipeline = pipelineService.readPipeline(trigger.getAppId(), trigger.getWorkflowId(), true);
      notNullCheck("Pipeline does not exist", pipeline, USER);
      envParamaterized = pipeline.isEnvParameterized();
      variables = pipeline.getPipelineVariables();
    } else if (WorkflowType.ORCHESTRATION == workflowType) {
      Workflow workflow = workflowService.readWorkflow(trigger.getAppId(), trigger.getWorkflowId());
      notNullCheck("Workflow does not exist", workflow, USER);
      notNullCheck("Orchestration workflow does not exist", workflow.getOrchestrationWorkflow(), USER);
      envParamaterized = workflow.checkEnvironmentTemplatized();
      variables = workflow.getOrchestrationWorkflow().getUserVariables();
    } else {
      logger.error("WorkflowType {} not supported", workflowType);
      throw new WingsException("Workflow Type [" + workflowType + "] not supported", USER);
    }
    if (envParamaterized) {
      validateAndAuthorizeEnvironment(trigger, existing, variables);
    }
  }

  private void validateAndAuthorizeEnvironment(Trigger trigger, boolean existing, List<Variable> variables) {
    String templatizedEnvVariableName = WorkflowServiceTemplateHelper.getTemplatizedEnvVariableName(variables);
    if (isNotEmpty(templatizedEnvVariableName)) {
      Map<String, String> workflowVariables = trigger.getWorkflowVariables();
      if (isEmpty(workflowVariables)) {
        if (existing) {
          return;
        }
        throw new WingsException("Please select a value for entity type variables.", USER);
      }
      String environment = workflowVariables.get(templatizedEnvVariableName);
      if (isEmpty(environment)) {
        if (existing) {
          return;
        }
        throw new WingsException("Environment is parameterized. Please select a value in the format ${varName}.", USER);
      }
      triggerAuthHandler.authorizeEnvironment(trigger.getAppId(), environment);
    }
  }
}
