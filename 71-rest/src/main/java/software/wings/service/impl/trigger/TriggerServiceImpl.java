package software.wings.service.impl.trigger;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.exception.WingsException.USER_ADMIN;
import static java.lang.String.format;
import static java.time.Duration.ofHours;
import static java.time.Duration.ofSeconds;
import static java.util.regex.Pattern.compile;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.beans.ExecutionCredential.ExecutionType.SSH;
import static software.wings.beans.OrchestrationWorkflowType.BUILD;
import static software.wings.beans.SSHExecutionCredential.Builder.aSSHExecutionCredential;
import static software.wings.beans.WorkflowType.ORCHESTRATION;
import static software.wings.beans.WorkflowType.PIPELINE;
import static software.wings.beans.trigger.ArtifactSelection.Type.ARTIFACT_SOURCE;
import static software.wings.beans.trigger.ArtifactSelection.Type.LAST_COLLECTED;
import static software.wings.beans.trigger.ArtifactSelection.Type.LAST_DEPLOYED;
import static software.wings.beans.trigger.ArtifactSelection.Type.PIPELINE_SOURCE;
import static software.wings.beans.trigger.ArtifactSelection.Type.WEBHOOK_VARIABLE;
import static software.wings.beans.trigger.TriggerConditionType.SCHEDULED;
import static software.wings.beans.trigger.TriggerConditionType.WEBHOOK;
import static software.wings.beans.trigger.WebhookSource.BITBUCKET;
import static software.wings.service.impl.trigger.TriggerServiceHelper.addParameter;
import static software.wings.service.impl.trigger.TriggerServiceHelper.constructWebhookToken;
import static software.wings.service.impl.trigger.TriggerServiceHelper.notNullCheckWorkflow;
import static software.wings.service.impl.trigger.TriggerServiceHelper.validateAndSetCronExpression;
import static software.wings.service.impl.workflow.WorkflowServiceTemplateHelper.getServiceWorkflowVariables;
import static software.wings.service.impl.workflow.WorkflowServiceTemplateHelper.getTemplatizedEnvVariableName;
import static software.wings.sm.StateType.ENV_STATE;
import static software.wings.utils.Validator.duplicateCheck;
import static software.wings.utils.Validator.equalCheck;
import static software.wings.utils.Validator.notNullCheck;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.distribution.idempotence.IdempotentId;
import io.harness.distribution.idempotence.IdempotentLock;
import io.harness.distribution.idempotence.UnableToRegisterIdempotentOperationException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import org.quartz.TriggerKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Base;
import software.wings.beans.Environment;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.Pipeline;
import software.wings.beans.PipelineStage;
import software.wings.beans.PipelineStage.PipelineStageElement;
import software.wings.beans.Service;
import software.wings.beans.Variable;
import software.wings.beans.WebHookToken;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowType;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactFile;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.trigger.ArtifactSelection;
import software.wings.beans.trigger.ArtifactTriggerCondition;
import software.wings.beans.trigger.NewInstanceTriggerCondition;
import software.wings.beans.trigger.PipelineTriggerCondition;
import software.wings.beans.trigger.ScheduledTriggerCondition;
import software.wings.beans.trigger.ServiceInfraWorkflow;
import software.wings.beans.trigger.Trigger;
import software.wings.beans.trigger.WebHookTriggerCondition;
import software.wings.beans.trigger.WebhookEventType;
import software.wings.beans.trigger.WebhookParameters;
import software.wings.beans.trigger.WebhookSource;
import software.wings.common.MongoIdempotentRegistry;
import software.wings.dl.WingsPersistence;
import software.wings.scheduler.QuartzScheduler;
import software.wings.scheduler.ScheduledTriggerJob;
import software.wings.service.impl.workflow.WorkflowServiceTemplateHelper;
import software.wings.service.intfc.ArtifactCollectionService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.TriggerService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.validation.executable.ValidateOnExecution;

@Singleton
@ValidateOnExecution
public class TriggerServiceImpl implements TriggerService {
  private static final Logger logger = LoggerFactory.getLogger(TriggerServiceImpl.class);
  public static final Duration timeout = ofSeconds(60);

  @Inject private WingsPersistence wingsPersistence;
  @Inject private ExecutorService executorService;
  @Inject private WorkflowExecutionService workflowExecutionService;
  @Inject private ArtifactService artifactService;
  @Inject private ArtifactStreamService artifactStreamService;
  @Inject ArtifactCollectionService artifactCollectionService;
  @Inject private PipelineService pipelineService;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private WorkflowService workflowService;
  @Inject private InfrastructureMappingService infrastructureMappingService;
  @Inject @Named("JobScheduler") private QuartzScheduler jobScheduler;
  @Inject private MongoIdempotentRegistry<String> idempotentRegistry;
  @Inject private TriggerServiceHelper triggerServiceHelper;
  @Inject private EnvironmentService environmentService;

  @Override
  public PageResponse<Trigger> list(PageRequest<Trigger> pageRequest) {
    return wingsPersistence.query(Trigger.class, pageRequest);
  }

  @Override
  public Trigger get(String appId, String triggerId) {
    return wingsPersistence.get(Trigger.class, appId, triggerId);
  }

  @Override
  public Trigger save(Trigger trigger) {
    validateInput(trigger, null);
    Trigger savedTrigger =
        duplicateCheck(() -> wingsPersistence.saveAndGet(Trigger.class, trigger), "name", trigger.getName());
    if (trigger.getCondition().getConditionType().equals(SCHEDULED)) {
      ScheduledTriggerJob.add(
          jobScheduler, savedTrigger.getAppId(), savedTrigger.getUuid(), ScheduledTriggerJob.getQuartzTrigger(trigger));
    }
    return savedTrigger;
  }

  @Override
  public Trigger update(Trigger trigger) {
    Trigger existingTrigger = wingsPersistence.get(Trigger.class, trigger.getAppId(), trigger.getUuid());
    notNullCheck("Trigger was deleted ", existingTrigger, USER);
    equalCheck(trigger.getWorkflowType(), existingTrigger.getWorkflowType());

    validateInput(trigger, existingTrigger);

    Trigger updatedTrigger =
        duplicateCheck(() -> wingsPersistence.saveAndGet(Trigger.class, trigger), "name", trigger.getName());
    addOrUpdateCronForScheduledJob(trigger, existingTrigger);
    return updatedTrigger;
  }

  @Override
  public boolean delete(String appId, String triggerId) {
    return triggerServiceHelper.delete(triggerId);
  }

  @Override
  public WebHookToken generateWebHookToken(String appId, String triggerId) {
    Trigger trigger = wingsPersistence.get(Trigger.class, appId, triggerId);
    notNullCheck("Trigger was deleted", trigger, USER);
    return generateWebHookToken(trigger, null);
  }

  @Override
  public void pruneByApplication(String appId) {
    wingsPersistence.createQuery(Trigger.class)
        .filter(Trigger.APP_ID_KEY, appId)
        .asList()
        .forEach(trigger -> delete(appId, trigger.getUuid()));
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
    triggerServiceHelper.getNewArtifactTriggers(appId, artifactStreamId)
        .forEach(trigger -> triggerServiceHelper.delete(trigger.getUuid()));
  }

  @Override
  public List<String> isEnvironmentReferenced(String appId, String envId) {
    return triggerServiceHelper.isEnvironmentReferenced(appId, envId);
  }

  @Override
  public void triggerExecutionPostArtifactCollectionAsync(Artifact artifact) {
    executorService.execute(() -> triggerExecutionPostArtifactCollection(artifact));
  }

  @Override
  public void triggerExecutionPostPipelineCompletionAsync(String appId, String sourcePipelineId) {
    executorService.submit(() -> triggerExecutionPostPipelineCompletion(appId, sourcePipelineId));
  }

  @Override
  public void triggerScheduledExecutionAsync(Trigger trigger, Date scheduledFireTime) {
    executorService.submit(() -> triggerScheduledExecution(trigger, scheduledFireTime));
  }

  @Override
  public WorkflowExecution triggerExecutionByWebHook(
      String appId, String webHookToken, Map<String, String> serviceBuildNumbers, Map<String, String> parameters) {
    List<Artifact> artifacts = new ArrayList<>();
    Trigger trigger = triggerServiceHelper.getTrigger(appId, webHookToken);
    logger.info(
        "Triggering  the execution for the Trigger {} by webhook with Service Build Numbers {}  and parameters {}",
        trigger.getName(), String.valueOf(serviceBuildNumbers), String.valueOf(parameters));
    if (isNotEmpty(serviceBuildNumbers)) {
      addArtifactsFromVersionsOfWebHook(trigger, serviceBuildNumbers, artifacts);
    }
    boolean artifactNeeded = addArtifactsFromSelections(appId, trigger, artifacts);
    if (artifactNeeded) {
      if (isEmpty(artifacts)) {
        String s =
            "Trigger workflow or pipeline needs artifact. However, no artifact matched or collected. So, skipping the execution.";
        logger.warn(s);
        throw new InvalidRequestException(s, USER_ADMIN);
      }
    }
    return triggerExecution(artifacts, trigger, parameters);
  }

  @Override
  public Trigger getTriggerByWebhookToken(String token) {
    return wingsPersistence.createQuery(Trigger.class).filter("webHookToken", token).get();
  }

  @Override
  public WorkflowExecution triggerExecutionByWebHook(Trigger trigger, Map<String, String> parameters) {
    return triggerExecution(null, trigger, parameters);
  }

  @Override
  public WebhookParameters listWebhookParameters(String appId, String workflowId, WorkflowType workflowType,
      WebhookSource webhookSource, WebhookEventType eventType) {
    workflowType = workflowType == null ? WorkflowType.PIPELINE : workflowType;

    List<String> parameters = new ArrayList<>();
    if (PIPELINE.equals(workflowType)) {
      Pipeline pipeline = validatePipeline(appId, workflowId, true);
      pipeline.getPipelineStages()
          .stream()
          .flatMap(pipelineStage -> pipelineStage.getPipelineStageElements().stream())
          .filter(pipelineStageElement -> ENV_STATE.name().equals(pipelineStageElement.getType()))
          .forEach((PipelineStageElement pipelineStageElement) -> {
            Workflow workflow = workflowService.readWorkflow(
                pipeline.getAppId(), (String) pipelineStageElement.getProperties().get("workflowId"));
            notNullCheckWorkflow(workflow);
            addParameter(parameters, workflow, false);
          });
    } else if (ORCHESTRATION.equals(workflowType)) {
      Workflow workflow = workflowService.readWorkflow(appId, workflowId);
      notNullCheckWorkflow(workflow);
      addParameter(parameters, workflow, true);
    }
    WebhookParameters webhookParameters = WebhookParameters.builder().params(parameters).build();
    webhookParameters.setExpressions(webhookParameters.suggestExpressions(webhookSource, eventType));
    return webhookParameters;
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
    triggerServiceHelper.getTriggersByApp(appId).forEach(this ::update);
  }

  @Override
  public String getCronDescription(String cronExpression) {
    return TriggerServiceHelper.getCronDescription(cronExpression);
  }

  private void triggerExecutionPostArtifactCollection(Artifact artifact) {
    for (Trigger trigger :
        triggerServiceHelper.getNewArtifactTriggers(artifact.getAppId(), artifact.getArtifactStreamId())) {
      logger.info("Trigger found for artifact streamId {}", artifact.getArtifactStreamId());
      ArtifactTriggerCondition artifactTriggerCondition = (ArtifactTriggerCondition) trigger.getCondition();
      List<Artifact> artifacts = new ArrayList<>();
      List<ArtifactSelection> artifactSelections = trigger.getArtifactSelections();
      if (isEmpty(artifactSelections)) {
        logger.info("No artifact selections found so executing pipeline with the collected artifact");
        addIfArtifactFilterMatches(
            artifact, artifactTriggerCondition.getArtifactFilter(), artifactTriggerCondition.isRegex(), artifacts);
        if (isEmpty(artifacts)) {
          logger.warn(
              "Skipping execution - artifact does not match with the given filter {}", artifactTriggerCondition);
          continue;
        }
      } else {
        logger.info("Artifact selections found collecting artifacts as per artifactStream selections ");
        if (artifactSelections.stream().anyMatch(artifactSelection
                -> artifactSelection.getType().equals(ARTIFACT_SOURCE)
                    && artifact.getServiceIds().contains(artifactSelection.getServiceId()))) {
          addIfArtifactFilterMatches(
              artifact, artifactTriggerCondition.getArtifactFilter(), artifactTriggerCondition.isRegex(), artifacts);
          if (isEmpty(artifacts)) {
            logger.warn(
                "Skipping execution - artifact does not match with the given filter {}", artifactTriggerCondition);
            continue;
          }
        }
        if (addArtifactsFromSelections(trigger.getAppId(), trigger, artifacts)) {
          if (isEmpty(artifacts)) {
            logger.warn(
                "Skipping execution - artifact does not match with the given filter {}", artifactTriggerCondition);
            continue;
          }
        }
      }
      triggerExecution(artifacts, trigger);
    }
  }

  /**
   * Trigger execution post pipeline completion
   *
   * @param appId            AppId
   * @param sourcePipelineId SourcePipelineId
   */
  private void triggerExecutionPostPipelineCompletion(String appId, String sourcePipelineId) {
    triggerServiceHelper.getTriggersMatchesWorkflow(appId, sourcePipelineId).forEach(trigger -> {
      List<ArtifactSelection> artifactSelections = trigger.getArtifactSelections();
      if (isEmpty(artifactSelections)) {
        logger.info("No artifactSelection configuration setup found. Executing pipeline {} from source pipeline {}",
            trigger.getWorkflowId(), sourcePipelineId);
        List<Artifact> lastDeployedArtifacts = getLastDeployedArtifacts(appId, sourcePipelineId, null);
        if (isEmpty(lastDeployedArtifacts)) {
          logger.warn(
              "No last deployed artifacts found. Triggering execution {} without artifacts", trigger.getWorkflowId());
        }
        triggerExecution(lastDeployedArtifacts, trigger);
      } else {
        List<Artifact> artifacts = new ArrayList<>();
        boolean artifactNeeded = false;
        if (artifactSelections.stream().anyMatch(
                artifactSelection -> artifactSelection.getType().equals(PIPELINE_SOURCE))) {
          logger.info("Adding last deployed artifacts from source pipeline {} ", sourcePipelineId);
          artifactNeeded = true;
          addLastDeployedArtifacts(appId, sourcePipelineId, null, artifacts);
        }
        if (addArtifactsFromSelections(trigger.getAppId(), trigger, artifacts)) {
          artifactNeeded = true;
        }
        if (artifactNeeded) {
          if (artifacts.size() == 0) {
            logger.warn(
                "No artifacts supplied. Skipped triggering the execution for workflow {}", trigger.getWorkflowId());
            return;
          }
        }
        triggerExecution(artifacts, trigger);
      }
    });
  }

  private void triggerScheduledExecution(Trigger trigger, Date scheduledFireTime) {
    IdempotentId idempotentid = new IdempotentId(trigger.getUuid() + ":" + scheduledFireTime.getTime());

    try (IdempotentLock<String> idempotent =
             idempotentRegistry.create(idempotentid, ofSeconds(10), ofSeconds(1), ofHours(1))) {
      if (idempotent.alreadyExecuted()) {
        return;
      }
      logger.info("Received scheduled trigger for appId {} and Trigger Id {}", trigger.getAppId(), trigger.getUuid());
      List<Artifact> lastDeployedArtifacts =
          getLastDeployedArtifacts(trigger.getAppId(), trigger.getWorkflowId(), null);
      ScheduledTriggerCondition scheduledTriggerCondition = (ScheduledTriggerCondition) trigger.getCondition();
      List<Artifact> artifacts = new ArrayList<>();
      boolean artifactNeeded = addArtifactsFromSelections(trigger.getAppId(), trigger, artifacts);
      if (!artifactNeeded) {
        logger.info("No artifactSelection configuration setup found. Executing workflow / pipeline {} ",
            trigger.getWorkflowId());
        triggerExecution(lastDeployedArtifacts, trigger, null);
      } else if (isNotEmpty(artifacts)) {
        if (!scheduledTriggerCondition.isOnNewArtifactOnly()) {
          triggerExecution(artifacts, trigger);
        } else {
          List<String> lastDeployedArtifactIds =
              lastDeployedArtifacts.stream().map(Artifact::getUuid).distinct().collect(toList());
          List<String> artifactIds = artifacts.stream().map(Artifact::getUuid).distinct().collect(toList());
          if (!lastDeployedArtifactIds.containsAll(artifactIds)) {
            logger.info("New version of artifacts found from the last successful execution "
                    + "of pipeline/ workflow {}. So, triggering  execution",
                trigger.getWorkflowId());
            triggerExecution(artifacts, trigger);
          } else {
            logger.info("No new version of artifacts found from the last successful execution "
                    + "of pipeline/ workflow {}. So, not triggering execution",
                trigger.getWorkflowId());
          }
        }
      } else {
        logger.warn("No artifacts set. So, skipping the execution");
      }
      logger.info("Scheduled trigger for appId {} and Trigger Id {} complete", trigger.getAppId(), trigger.getUuid());
      idempotent.succeeded(trigger.getUuid());
    } catch (UnableToRegisterIdempotentOperationException e) {
      logger.error(format("Failed to trigger scheduled trigger %s", trigger.getName()), e);
    }
  }

  private boolean addArtifactsFromSelections(String appId, Trigger trigger, List<Artifact> artifacts) {
    if (isEmpty(trigger.getArtifactSelections())) {
      return false;
    }
    boolean artifactNeeded = false;
    for (ArtifactSelection artifactSelection : trigger.getArtifactSelections()) {
      if (artifactSelection.getType().equals(LAST_COLLECTED)) {
        addLastCollectedArtifact(appId, artifactSelection, artifacts);
        artifactNeeded = true;
      } else if (artifactSelection.getType().equals(LAST_DEPLOYED)) {
        addLastDeployedArtifacts(appId, artifactSelection.getWorkflowId(), artifactSelection.getServiceId(), artifacts);
        artifactNeeded = true;
      }
    }
    return artifactNeeded;
  }

  /**
   * Last collected artifact for the given artifact stream
   *
   * @param appId             AppId
   * @param artifactSelection
   * @param artifacts
   */
  private void addLastCollectedArtifact(String appId, ArtifactSelection artifactSelection, List<Artifact> artifacts) {
    ArtifactStream artifactStream = artifactStreamService.get(appId, artifactSelection.getArtifactStreamId());
    notNullCheck("ArtifactStream was deleted", artifactStream, USER_ADMIN);
    Artifact lastCollectedArtifact;
    if (isEmpty(artifactSelection.getArtifactFilter())) {
      lastCollectedArtifact = artifactService.fetchLastCollectedArtifact(
          appId, artifactSelection.getArtifactStreamId(), artifactStream.getSourceName());
      if (lastCollectedArtifact != null
          && lastCollectedArtifact.getServiceIds().contains(artifactSelection.getServiceId())) {
        addIfArtifactFilterMatches(
            lastCollectedArtifact, artifactSelection.getArtifactFilter(), artifactSelection.isRegex(), artifacts);
      }
    } else {
      lastCollectedArtifact = artifactService.getArtifactByBuildNumber(appId, artifactSelection.getArtifactStreamId(),
          artifactStream.getSourceName(), artifactSelection.getArtifactFilter(), artifactSelection.isRegex());
      if (lastCollectedArtifact != null) {
        artifacts.add(lastCollectedArtifact);
      }
    }
  }

  /**
   * Last successfully deployed or last deployed
   *
   * @param appId
   * @param workflowId
   * @param serviceId
   * @return List<Artifact></Artifact>
   */
  private void addLastDeployedArtifacts(String appId, String workflowId, String serviceId, List<Artifact> artifacts) {
    logger.info("Adding last deployed artifacts for appId {}, workflowid {}", appId, workflowId);
    artifacts.addAll(getLastDeployedArtifacts(appId, workflowId, serviceId));
  }

  /**
   * Last successfully deployed or last deployed
   *
   * @param appId
   * @param workflowId
   * @param serviceId
   * @return List<Artifact></Artifact>
   */
  private List<Artifact> getLastDeployedArtifacts(String appId, String workflowId, String serviceId) {
    List<Artifact> lastDeployedArtifacts = workflowExecutionService.obtainLastGoodDeployedArtifacts(appId, workflowId);
    if (lastDeployedArtifacts != null) {
      if (serviceId != null) {
        lastDeployedArtifacts = lastDeployedArtifacts.stream()
                                    .filter(artifact1 -> artifact1.getServiceIds().contains(serviceId))
                                    .collect(toList());
      }
    }
    return lastDeployedArtifacts == null ? new ArrayList<>() : lastDeployedArtifacts;
  }

  private void addIfArtifactFilterMatches(
      Artifact artifact, String artifactFilter, boolean isRegEx, List<Artifact> artifacts) {
    if (isNotEmpty(artifactFilter)) {
      logger.info("Artifact filter {} set for artifact stream id {}", artifactFilter, artifact.getArtifactStreamId());
      Pattern pattern;
      if (isRegEx) {
        pattern = compile(artifactFilter);
      } else {
        pattern = compile(artifactFilter.replace(".", "\\.").replace("?", ".?").replace("*", ".*?"));
      }
      if (isEmpty(artifact.getArtifactFiles())) {
        if (pattern.matcher(artifact.getBuildNo()).find()) {
          logger.info("Artifact filter {} matching with artifact name/ tag / buildNo {}", artifactFilter,
              artifact.getBuildNo());
          artifacts.add(artifact);
        }
      } else {
        logger.info("Comparing artifact file name matches with the given artifact filter");
        List<ArtifactFile> artifactFiles = artifact.getArtifactFiles()
                                               .stream()
                                               .filter(artifactFile -> pattern.matcher(artifactFile.getName()).find())
                                               .collect(toList());
        if (isNotEmpty(artifactFiles)) {
          logger.info("Artifact file names matches with the given artifact filter");
          artifact.setArtifactFiles(artifactFiles);
          artifacts.add(artifact);
        }
      }
    } else {
      artifacts.add(artifact);
    }
  }

  private WorkflowExecution triggerExecution(List<Artifact> artifacts, Trigger trigger) {
    return triggerExecution(artifacts, trigger, null);
  }

  private WorkflowExecution triggerExecution(
      List<Artifact> artifacts, Trigger trigger, Map<String, String> parameters) {
    WorkflowExecution workflowExecution;
    ExecutionArgs executionArgs = new ExecutionArgs();
    prepareExecutionArgs(artifacts, trigger, parameters, executionArgs);
    if (ORCHESTRATION.equals(trigger.getWorkflowType())) {
      workflowExecution = triggerOrchestrationExecution(trigger, executionArgs);
    } else {
      logger.info(
          "Triggering  execution of appId {} with  pipeline id {}", trigger.getAppId(), trigger.getWorkflowId());
      resolveTriggerPipelineVariables(trigger, executionArgs);
      workflowExecution = workflowExecutionService.triggerPipelineExecution(
          trigger.getAppId(), trigger.getWorkflowId(), executionArgs, trigger);

      logger.info(
          "Pipeline execution of appId {} with  pipeline id {} triggered", trigger.getAppId(), trigger.getWorkflowId());
    }
    return workflowExecution;
  }

  private void prepareExecutionArgs(
      List<Artifact> artifacts, Trigger trigger, Map<String, String> parameters, ExecutionArgs executionArgs) {
    if (artifacts != null) {
      executionArgs.setArtifacts(
          artifacts.stream().filter(triggerServiceHelper.distinctByKey(Base::getUuid)).collect(toList()));
    }
    executionArgs.setOrchestrationId(trigger.getWorkflowId());
    executionArgs.setExecutionCredential(aSSHExecutionCredential().withExecutionType(SSH).build());
    executionArgs.setWorkflowType(trigger.getWorkflowType());
    executionArgs.setExcludeHostsWithSameArtifact(trigger.isExcludeHostsWithSameArtifact());
    if (parameters != null) {
      executionArgs.setWorkflowVariables(parameters);
    }
  }

  private void resolveTriggerPipelineVariables(Trigger trigger, ExecutionArgs executionArgs) {
    Pipeline pipeline = pipelineService.readPipeline(trigger.getAppId(), trigger.getWorkflowId(), true);
    notNullCheck("Pipeline was deleted or does not exist", pipeline, USER_ADMIN);
    Map<String, String> triggerWorkflowVariableValues = overrideTriggerVariables(trigger, executionArgs);
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
        logger.warn(msg);
        throw new WingsException(msg, USER);
      }
      envId = resolveEnvId(trigger, envNameOrId);
      triggerWorkflowVariableValues.put(templatizedEnvName, envId);
    }

    resolveServices(trigger, triggerWorkflowVariableValues, pipelineVariables);
    resolveServiceInfrastructures(trigger, triggerWorkflowVariableValues, envId, pipelineVariables);

    executionArgs.setWorkflowVariables(triggerWorkflowVariableValues);
  }

  private void resolveServices(
      Trigger trigger, Map<String, String> triggerWorkflowVariableValues, List<Variable> variables) {
    List<String> serviceWorkflowVariables = getServiceWorkflowVariables(variables);
    for (String serviceVarName : serviceWorkflowVariables) {
      String serviceIdOrName = triggerWorkflowVariableValues.get(serviceVarName);
      notNullCheck("There is no corresponding Workflow Variable associated to service", serviceIdOrName);
      logger.info("Checking  service {} can be found by id first.", serviceIdOrName);
      Service service = serviceResourceService.get(trigger.getAppId(), serviceIdOrName, false);
      if (service == null) {
        logger.info("Service does not exist by Id, checking if environment {} can be found by name.", serviceIdOrName);
        service = serviceResourceService.getServiceByName(trigger.getAppId(), serviceIdOrName, false);
      }
      notNullCheck("Service [" + serviceIdOrName + "] does not exist", service, USER_ADMIN);
      triggerWorkflowVariableValues.put(serviceVarName, service.getUuid());
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
        environment);

    return environment.getUuid();
  }

  private void resolveServiceInfrastructures(
      Trigger trigger, Map<String, String> triggerWorkflowVariableValues, String envId, List<Variable> variables) {
    List<String> serviceInfraWorkflowVariables =
        WorkflowServiceTemplateHelper.getServiceInfrastructureWorkflowVariables(variables);
    for (String serviceInfraVarName : serviceInfraWorkflowVariables) {
      String serviceInfraIdOrName = triggerWorkflowVariableValues.get(serviceInfraVarName);
      notNullCheck("There is no corresponding Workflow Variable associated to Service Infrastructure",
          serviceInfraIdOrName, USER);
      logger.info("Checking  Service Infrastructure {} can be found by id first.", serviceInfraIdOrName);
      InfrastructureMapping infrastructureMapping =
          infrastructureMappingService.get(trigger.getAppId(), serviceInfraIdOrName);
      if (infrastructureMapping == null) {
        logger.info(
            "Service Infrastructure does not exist by Id, checking if service infrastructure {} can be found by name.",
            serviceInfraIdOrName);
        infrastructureMapping =
            infrastructureMappingService.getInfraMappingByName(trigger.getAppId(), envId, serviceInfraIdOrName);
      }
      notNullCheck("Service Infrastructure [" + serviceInfraIdOrName + "] does not exist", infrastructureMapping, USER);
      triggerWorkflowVariableValues.put(serviceInfraVarName, infrastructureMapping.getUuid());
    }
  }
  private WorkflowExecution triggerOrchestrationExecution(Trigger trigger, ExecutionArgs executionArgs) {
    logger.info("Triggering  workflow execution of appId {} with with workflow id {}", trigger.getAppId(),
        trigger.getWorkflowId());

    Workflow workflow = workflowService.readWorkflow(trigger.getAppId(), trigger.getWorkflowId());
    notNullCheck("Workflow was deleted", workflow, USER);
    notNullCheck("Orchestration Workflow not present", workflow.getOrchestrationWorkflow(), USER);
    Map<String, String> triggerWorkflowVariableValues = overrideTriggerVariables(trigger, executionArgs);

    String envId = null;
    if (!BUILD.equals(workflow.getOrchestrationWorkflow().getOrchestrationWorkflowType())) {
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
      resolveServiceInfrastructures(trigger, triggerWorkflowVariableValues, envId, workflowVariables);
    }

    executionArgs.setWorkflowVariables(triggerWorkflowVariableValues);
    logger.info("Triggering workflow execution of appId {} with workflow id {} triggered", trigger.getAppId(),
        trigger.getWorkflowId());
    return workflowExecutionService.triggerEnvExecution(trigger.getAppId(), envId, executionArgs, trigger);
  }

  private Map<String, String> overrideTriggerVariables(Trigger trigger, ExecutionArgs executionArgs) {
    // Workflow variables come from Webhook
    Map<String, String> webhookVariableValues =
        executionArgs.getWorkflowVariables() == null ? new HashMap<>() : executionArgs.getWorkflowVariables();

    // Workflow variables associated with the trigger
    Map<String, String> triggerWorkflowVariableValues =
        trigger.getWorkflowVariables() == null ? new HashMap<>() : trigger.getWorkflowVariables();

    for (Entry<String, String> entry : webhookVariableValues.entrySet()) {
      if (isNotEmpty(entry.getValue())) {
        triggerWorkflowVariableValues.put(entry.getKey(), entry.getValue());
      }
    }
    triggerWorkflowVariableValues = triggerWorkflowVariableValues.entrySet()
                                        .stream()
                                        .filter(variableEntry -> isNotEmpty(variableEntry.getValue()))
                                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    return triggerWorkflowVariableValues;
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
      if (serviceInfraWorkflow.getWorkflowType() == null
          || serviceInfraWorkflow.getWorkflowType().equals(ORCHESTRATION)) {
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

  private void validateAndSetTriggerCondition(Trigger trigger, Trigger existingTrigger) {
    switch (trigger.getCondition().getConditionType()) {
      case NEW_ARTIFACT:
        validateAndSetNewArtifactCondition(trigger);
        break;
      case PIPELINE_COMPLETION:
        PipelineTriggerCondition pipelineTriggerCondition = (PipelineTriggerCondition) trigger.getCondition();
        Pipeline pipeline = validatePipeline(trigger.getAppId(), pipelineTriggerCondition.getPipelineId(), false);
        pipelineTriggerCondition.setPipelineName(pipeline.getName());
        break;
      case WEBHOOK:
        WebHookTriggerCondition webHookTriggerCondition = (WebHookTriggerCondition) trigger.getCondition();
        WebHookToken webHookToken = generateWebHookToken(trigger, getExistingWebhookToken(existingTrigger));
        webHookTriggerCondition.setWebHookToken(webHookToken);
        if (BITBUCKET.equals(webHookTriggerCondition.getWebhookSource())) {
          if (isNotEmpty(webHookTriggerCondition.getActions())) {
            throw new InvalidRequestException("Actions not supported for Bit Bucket", USER);
          }
        }
        trigger.setWebHookToken(webHookTriggerCondition.getWebHookToken().getWebHookToken());
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
    if (existingTrigger != null) {
      if (existingTrigger.getCondition().getConditionType().equals(WEBHOOK)) {
        WebHookTriggerCondition existingTriggerCondition = (WebHookTriggerCondition) existingTrigger.getCondition();
        existingWebhookToken = existingTriggerCondition.getWebHookToken();
      }
    }
    return existingWebhookToken;
  }

  private void validateAndSetNewArtifactCondition(Trigger trigger) {
    ArtifactTriggerCondition artifactTriggerCondition = (ArtifactTriggerCondition) trigger.getCondition();
    ArtifactStream artifactStream =
        artifactStreamService.get(trigger.getAppId(), artifactTriggerCondition.getArtifactStreamId());
    notNullCheck("Artifact Source is mandatory for New Artifact Condition Trigger", artifactStream, USER);
    Service service = serviceResourceService.get(trigger.getAppId(), artifactStream.getServiceId(), false);
    notNullCheck("Service does not exist", service, USER);
    artifactTriggerCondition.setArtifactSourceName(artifactStream.getSourceName() + " (" + service.getName() + ")");
  }

  private void validateAndSetArtifactSelections(Trigger trigger, List<Service> services) {
    List<ArtifactSelection> artifactSelections = trigger.getArtifactSelections();
    if (isEmpty(artifactSelections)) {
      return;
    }
    if (isEmpty(services)) {
      throw new InvalidRequestException("Pipeline services can not be empty", USER);
    }

    artifactSelections.forEach(artifactSelection -> {
      switch (artifactSelection.getType()) {
        case LAST_DEPLOYED:
          validateAndSetLastDeployedArtifactSelection(trigger, artifactSelection);
          break;
        case LAST_COLLECTED:
          validateAndSetLastCollectedArifactSelection(
              trigger, artifactSelection, "Artifact Source cannot be empty for Last collected type");
          break;
        case WEBHOOK_VARIABLE:
          WebHookTriggerCondition webHookTriggerCondition = (WebHookTriggerCondition) trigger.getCondition();
          if (webHookTriggerCondition.getWebhookSource() == null) {
            validateAndSetLastCollectedArifactSelection(
                trigger, artifactSelection, "Artifact Source cannot be empty for Webhook Variable type");
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

  private void setServiceName(Trigger trigger, List<Service> services, ArtifactSelection artifactSelection) {
    Map<String, String> serviceIdNames =
        services.stream().collect(Collectors.toMap(Service::getUuid, Service::getName));
    Service service;
    if (serviceIdNames.get(artifactSelection.getServiceId()) == null) {
      service = serviceResourceService.get(trigger.getAppId(), artifactSelection.getServiceId(), false);
      notNullCheck("Service might have been deleted", service, USER);
      artifactSelection.setServiceName(service.getName());
    } else {
      artifactSelection.setServiceName(serviceIdNames.get(artifactSelection.getServiceId()));
    }
  }

  private void validateAndSetLastCollectedArifactSelection(
      Trigger trigger, ArtifactSelection artifactSelection, String s) {
    ArtifactStream artifactStream;
    Service service;
    if (isBlank(artifactSelection.getArtifactStreamId())) {
      throw new InvalidRequestException(s, USER);
    }
    artifactStream = validateArtifactStream(trigger.getAppId(), artifactSelection.getArtifactStreamId());
    service = serviceResourceService.get(trigger.getAppId(), artifactStream.getServiceId(), false);
    notNullCheck("Service might have been deleted", service, USER);
    artifactSelection.setArtifactSourceName(artifactStream.getSourceName() + " (" + service.getName() + ")");
  }

  private void validateAndSetLastDeployedArtifactSelection(Trigger trigger, ArtifactSelection artifactSelection) {
    if (isBlank(artifactSelection.getWorkflowId())) {
      throw new InvalidRequestException("Pipeline cannot be empty for Last deployed type", USER);
    }
    if (ORCHESTRATION.equals(trigger.getWorkflowType())) {
      Workflow workflow =
          workflowService.readWorkflowWithoutOrchestration(trigger.getAppId(), artifactSelection.getWorkflowId());
      notNullCheck("LastDeployedWorkflow does not exist", workflow, USER);
      artifactSelection.setWorkflowName(workflow.getName());
    } else {
      Pipeline pipeline = pipelineService.readPipeline(trigger.getAppId(), artifactSelection.getWorkflowId(), false);
      notNullCheck("LastDeployedPipeline does not exist", pipeline, USER);
      artifactSelection.setWorkflowName(pipeline.getName());
    }
  }

  private void validateAndSetServiceInfraWorkflows(Trigger trigger) {
    List<ServiceInfraWorkflow> serviceInfraWorkflows = trigger.getServiceInfraWorkflows();
    if (serviceInfraWorkflows != null) {
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
    } else {
      throw new WingsException("ServiceInfra and Workflow Mapping can not be empty.", USER);
    }
  }

  private void validateInput(Trigger trigger, Trigger existingTrigger) {
    List<Service> services;
    if (PIPELINE.equals(trigger.getWorkflowType())) {
      Pipeline executePipeline = validatePipeline(trigger.getAppId(), trigger.getWorkflowId(), true);
      trigger.setWorkflowName(executePipeline.getName());
      services = executePipeline.getServices();
      validateAndSetArtifactSelections(trigger, services);
    } else if (ORCHESTRATION.equals(trigger.getWorkflowType())) {
      Workflow workflow = validateAndGetWorkflow(trigger.getAppId(), trigger.getWorkflowId());
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

  private Pipeline validatePipeline(String appId, String pipelineId, boolean withServices) {
    Pipeline pipeline = pipelineService.readPipeline(appId, pipelineId, withServices);
    notNullCheck("Pipeline", pipeline, USER);
    return pipeline;
  }

  private Workflow validateAndGetWorkflow(String appId, String workflowId) {
    Workflow workflow = workflowService.readWorkflow(appId, workflowId);
    notNullCheck("Workflow", workflow, USER);
    return workflow;
  }

  private WebHookToken generateWebHookToken(Trigger trigger, WebHookToken existingToken) {
    List<Service> services = null;
    boolean artifactNeeded = true;
    Map<String, String> parameters = new LinkedHashMap<>();
    if (PIPELINE.equals(trigger.getWorkflowType())) {
      Pipeline pipeline = validatePipeline(trigger.getAppId(), trigger.getWorkflowId(), true);
      services = pipeline.getServices();

      for (PipelineStage pipelineStage : pipeline.getPipelineStages()) {
        for (PipelineStageElement pipelineStageElement : pipelineStage.getPipelineStageElements()) {
          if (ENV_STATE.name().equals(pipelineStageElement.getType())) {
            Workflow workflow = workflowService.readWorkflow(
                pipeline.getAppId(), (String) pipelineStageElement.getProperties().get("workflowId"));

            notNullCheckWorkflow(workflow);

            if (BUILD.equals(workflow.getOrchestrationWorkflow().getOrchestrationWorkflowType())) {
              artifactNeeded = false;
            }
            addVariables(parameters, pipeline.getPipelineVariables());
          }
        }
      }
    } else if (ORCHESTRATION.equals(trigger.getWorkflowType())) {
      Workflow workflow = validateAndGetWorkflow(trigger.getAppId(), trigger.getWorkflowId());
      services = workflow.getServices();
      Map<String, String> workflowVariables = trigger.getWorkflowVariables();
      if (isNotEmpty(workflowVariables)) {
        if (!BUILD.equals(workflow.getOrchestrationWorkflow().getOrchestrationWorkflowType())) {
          if (workflow.getOrchestrationWorkflow().isServiceTemplatized()) {
            services = workflowService.getResolvedServices(workflow, workflowVariables);
          }
        } else {
          artifactNeeded = false;
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
    if (existingTrigger.getCondition().getConditionType().equals(SCHEDULED)) {
      if (trigger.getCondition().getConditionType().equals(SCHEDULED)) {
        TriggerKey triggerKey = new TriggerKey(trigger.getUuid(), ScheduledTriggerJob.GROUP);
        jobScheduler.rescheduleJob(triggerKey, ScheduledTriggerJob.getQuartzTrigger(trigger));
      } else {
        jobScheduler.deleteJob(existingTrigger.getUuid(), ScheduledTriggerJob.GROUP);
      }
    } else if (trigger.getCondition().getConditionType().equals(SCHEDULED)) {
      ScheduledTriggerJob.add(
          jobScheduler, trigger.getAppId(), trigger.getUuid(), ScheduledTriggerJob.getQuartzTrigger(trigger));
    }
  }

  private ArtifactStream validateArtifactStream(String appId, String artifactStreamId) {
    ArtifactStream artifactStream = artifactStreamService.get(appId, artifactStreamId);
    notNullCheck("Artifact Source does not exist", artifactStream, USER);
    return artifactStream;
  }

  private void collectArtifacts(Trigger trigger, Map<String, String> serviceBuildNumbers, List<Artifact> artifacts,
      Map<String, String> services) {
    trigger.getArtifactSelections()
        .stream()
        .filter(artifactSelection -> artifactSelection.getType().equals(WEBHOOK_VARIABLE))
        .forEach((ArtifactSelection artifactSelection) -> {
          Artifact artifact;
          String serviceName = services.get(artifactSelection.getServiceId());
          String buildNumber = serviceBuildNumbers.get(serviceName);
          if (isBlank(buildNumber)) {
            throw new WingsException("Webhook services " + serviceBuildNumbers.keySet()
                + " do not match with the trigger services " + services.values());
          } else {
            artifact = fetchMatchedArtifact(trigger, artifactSelection, buildNumber);
            if (artifact != null) {
              artifacts.add(artifact);
            } else {
              triggerCollectionAndCollect(trigger, artifacts, artifactSelection, buildNumber);
            }
          }
        });
  }

  private Artifact fetchMatchedArtifact(Trigger trigger, ArtifactSelection artifactSelection, String buildNumber) {
    Artifact artifact;
    ArtifactStream artifactStream = validateArtifactStream(trigger.getAppId(), artifactSelection.getArtifactStreamId());
    notNullCheck("Artifact stream was deleted", artifactStream, USER);
    artifact = artifactService.getArtifactByBuildNumber(trigger.getAppId(), artifactSelection.getArtifactStreamId(),
        artifactStream.getSourceName(), buildNumber, artifactSelection.isRegex());
    return artifact;
  }

  private void addArtifactsFromVersionsOfWebHook(
      Trigger trigger, Map<String, String> serviceBuildNumbers, List<Artifact> artifacts) {
    Map<String, String> services;
    if (ORCHESTRATION.equals(trigger.getWorkflowType())) {
      services = resolveWorkflowServices(trigger);
    } else {
      Pipeline pipeline = null;
      try {
        pipeline = validatePipeline(trigger.getAppId(), trigger.getWorkflowId(), true);
      } catch (WingsException e) {
        logger.warn("Error occurred while retrieving Pipeline {} ", trigger.getWorkflowId());
      }
      if (pipeline == null) {
        throw new WingsException("Pipeline " + trigger.getWorkflowName() + " does not exist.", USER);
      }
      services = pipeline.getServices().stream().collect(Collectors.toMap(Base::getUuid, Service::getName));
    }
    services = services == null ? new HashMap<>() : services;
    collectArtifacts(trigger, serviceBuildNumbers, artifacts, services);
  }

  private Map<String, String> resolveWorkflowServices(Trigger trigger) {
    Map<String, String> services = new HashMap<>();
    List<Service> workflowServices = null;
    Workflow workflow = validateAndGetWorkflow(trigger.getAppId(), trigger.getWorkflowId());
    try {
      workflow = validateAndGetWorkflow(trigger.getAppId(), trigger.getWorkflowId());
      workflowServices = workflow.getServices();
    } catch (Exception e) {
      logger.warn("Error occurred while retrieving Pipeline {} ", trigger.getWorkflowId());
    }
    if (workflow == null) {
      throw new WingsException("Workflow " + trigger.getWorkflowName() + " does not exist.", USER);
    }
    if (!BUILD.equals(workflow.getOrchestrationWorkflow().getOrchestrationWorkflowType())) {
      if (workflow.getOrchestrationWorkflow().isServiceTemplatized()) {
        workflowServices = workflowService.getResolvedServices(workflow, trigger.getWorkflowVariables());
      }
    }
    if (workflowServices != null) {
      services = workflowServices.stream().collect(Collectors.toMap(Base::getUuid, Service::getName));
    }
    return services;
  }

  private void triggerCollectionAndCollect(
      Trigger trigger, List<Artifact> artifacts, ArtifactSelection artifactSelection, String buildNumber) {
    // Initiate the artifact collection. Right now, it is sync call. If changed to async
    artifactCollectionService.collectNewArtifacts(trigger.getAppId(), artifactSelection.getArtifactStreamId());
    Artifact artifact = fetchMatchedArtifact(trigger, artifactSelection, buildNumber);
    if (artifact != null) {
      artifacts.add(artifact);
      logger.info("Artifact collected for the build number {} of stream id {}", buildNumber,
          artifactSelection.getArtifactStreamId());
    } else {
      logger.info(
          "Artifact collection invoked. However, Artifact not yet collected for the build number {} of stream id {}",
          buildNumber, artifactSelection.getArtifactStreamId());
      throw new WingsException("Artifact [" + buildNumber + "] does not exist for the artifact source + ["
              + artifactSelection.getArtifactSourceName()
              + "]. Please make sure artifact exists in the artifact server",
          USER);
    }
  }
}
