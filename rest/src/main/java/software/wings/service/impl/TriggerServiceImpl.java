package software.wings.service.impl;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.stream.Collectors.toList;
import static net.redhogs.cronparser.CronExpressionDescriptor.getDescription;
import static software.wings.beans.ErrorCode.INVALID_ARGUMENT;
import static software.wings.beans.ErrorCode.INVALID_REQUEST;
import static software.wings.beans.ExecutionCredential.ExecutionType.SSH;
import static software.wings.beans.OrchestrationWorkflowType.BUILD;
import static software.wings.beans.SSHExecutionCredential.Builder.aSSHExecutionCredential;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.SortOrder.Builder.aSortOrder;
import static software.wings.beans.SortOrder.OrderType.DESC;
import static software.wings.beans.WorkflowType.ORCHESTRATION;
import static software.wings.beans.WorkflowType.PIPELINE;
import static software.wings.beans.trigger.ArtifactSelection.Type.ARTIFACT_SOURCE;
import static software.wings.beans.trigger.ArtifactSelection.Type.LAST_COLLECTED;
import static software.wings.beans.trigger.ArtifactSelection.Type.LAST_DEPLOYED;
import static software.wings.beans.trigger.ArtifactSelection.Type.PIPELINE_SOURCE;
import static software.wings.beans.trigger.ArtifactSelection.Type.WEBHOOK_VARIABLE;
import static software.wings.beans.trigger.TriggerConditionType.NEW_ARTIFACT;
import static software.wings.beans.trigger.TriggerConditionType.PIPELINE_COMPLETION;
import static software.wings.beans.trigger.TriggerConditionType.SCHEDULED;
import static software.wings.beans.trigger.TriggerConditionType.WEBHOOK;
import static software.wings.dl.PageRequest.Builder.aPageRequest;
import static software.wings.sm.ExecutionStatus.SUCCESS;
import static software.wings.sm.StateType.ENV_STATE;
import static software.wings.utils.Validator.duplicateCheck;
import static software.wings.utils.Validator.notNullCheck;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import net.redhogs.cronparser.DescriptionTypeEnum;
import net.redhogs.cronparser.I18nMessages;
import net.redhogs.cronparser.Options;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.quartz.CronScheduleBuilder;
import org.quartz.TriggerKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.Pipeline;
import software.wings.beans.PipelineStage;
import software.wings.beans.PipelineStage.PipelineStageElement;
import software.wings.beans.Service;
import software.wings.beans.VariableType;
import software.wings.beans.WebHookToken;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowType;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactFile;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.trigger.ArtifactSelection;
import software.wings.beans.trigger.ArtifactTriggerCondition;
import software.wings.beans.trigger.PipelineTriggerCondition;
import software.wings.beans.trigger.ScheduledTriggerCondition;
import software.wings.beans.trigger.Trigger;
import software.wings.beans.trigger.WebHookTriggerCondition;
import software.wings.beans.trigger.WebhookParameters;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.scheduler.QuartzScheduler;
import software.wings.scheduler.ScheduledTriggerJob;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.TriggerService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;
import software.wings.utils.CryptoUtil;
import software.wings.utils.Misc;
import software.wings.utils.Validator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.validation.executable.ValidateOnExecution;

/**
 * Handles Triggers
 * Created by Srinivas on 10/26/17.
 */

@Singleton
@ValidateOnExecution
public class TriggerServiceImpl implements TriggerService {
  private static final Logger logger = LoggerFactory.getLogger(TriggerServiceImpl.class);

  @Inject private WingsPersistence wingsPersistence;
  @Inject private ExecutorService executorService;
  @Inject private WorkflowExecutionService workflowExecutionService;
  @Inject private ArtifactService artifactService;
  @Inject private ArtifactStreamService artifactStreamService;
  @Inject private PipelineService pipelineService;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private WorkflowService workflowService;

  @Inject @Named("JobScheduler") private QuartzScheduler jobScheduler;

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
    validateInput(trigger);
    Trigger savedTrigger =
        duplicateCheck(() -> wingsPersistence.saveAndGet(Trigger.class, trigger), "name", trigger.getName());
    notNullCheck("Trigger", savedTrigger);
    if (trigger.getCondition().getConditionType().equals(SCHEDULED)) {
      ScheduledTriggerJob.add(
          jobScheduler, savedTrigger.getAppId(), savedTrigger.getUuid(), ScheduledTriggerJob.getQuartzTrigger(trigger));
    }
    return savedTrigger;
  }

  @Override
  public Trigger update(Trigger trigger) {
    Trigger existingTrigger = wingsPersistence.get(Trigger.class, trigger.getAppId(), trigger.getUuid());
    notNullCheck("Trigger", existingTrigger);
    Validator.equalCheck(trigger.getWorkflowType(), existingTrigger.getWorkflowType());

    validateInput(trigger);

    Trigger updatedTrigger =
        duplicateCheck(() -> wingsPersistence.saveAndGet(Trigger.class, trigger), "name", trigger.getName());
    addOrUpdateCronForScheduledJob(trigger, existingTrigger);
    return updatedTrigger;
  }

  @Override
  public boolean delete(String appId, String triggerId) {
    Trigger trigger = wingsPersistence.get(Trigger.class, appId, triggerId);
    if (trigger == null) {
      return true;
    }
    return deleteTrigger(triggerId);
  }

  @Override
  public WebHookToken generateWebHookToken(String appId, String triggerId) {
    Trigger trigger = wingsPersistence.get(Trigger.class, appId, triggerId);
    Validator.notNullCheck("Trigger", trigger);
    return generateWebHookToken(trigger);
  }

  private boolean deleteTrigger(String triggerId) {
    return wingsPersistence.delete(Trigger.class, triggerId);
  }

  @Override
  public void pruneByApplication(String appId) {
    wingsPersistence.createQuery(Trigger.class)
        .field(Trigger.APP_ID_KEY)
        .equal(appId)
        .asList()
        .forEach(trigger -> delete(appId, trigger.getUuid()));
  }

  @Override
  public void pruneByPipeline(String appId, String pipelineId) {
    List<Trigger> triggers = wingsPersistence.createQuery(Trigger.class)
                                 .field(Trigger.APP_ID_KEY)
                                 .equal(appId)
                                 .field("pipelineId")
                                 .equal(pipelineId)
                                 .asList();
    triggers.forEach(trigger -> deleteTrigger(trigger.getUuid()));

    // Verify if there are any triggers existed on post pipeline completion
    getTriggersByApp(appId)
        .stream()
        .filter(trigger
            -> trigger.getCondition().getConditionType().equals(PIPELINE_COMPLETION)
                && ((PipelineTriggerCondition) trigger.getCondition()).getPipelineId().equals(pipelineId))
        .filter(Objects::nonNull)
        .collect(toList())
        .forEach(trigger -> delete(appId, trigger.getUuid()));
  }

  @Override
  public void pruneByWorkflow(String appId, String workflowId) {
    List<Trigger> triggers = wingsPersistence.createQuery(Trigger.class)
                                 .field(Trigger.APP_ID_KEY)
                                 .equal(appId)
                                 .field("workflowId")
                                 .equal(workflowId)
                                 .asList();
    triggers.forEach(trigger -> deleteTrigger(trigger.getUuid()));
  }

  @Override
  public void pruneByArtifactStream(String appId, String artifactStreamId) {
    List<Trigger> triggers = getTriggersByApp(appId);

    triggers.stream()
        .filter(trigger
            -> trigger.getCondition().getConditionType().equals(NEW_ARTIFACT)
                && ((ArtifactTriggerCondition) trigger.getCondition()).getArtifactStreamId().equals(artifactStreamId))
        .filter(Objects::nonNull)
        .collect(toList())
        .forEach(trigger -> deleteTrigger(trigger.getUuid()));
  }

  private List<Trigger> getTriggersByApp(String appId) {
    return wingsPersistence.query(Trigger.class, aPageRequest().addFilter("appId", EQ, appId).build()).getResponse();
  }

  private WebHookToken generateWebHookToken(Trigger trigger) {
    List<Service> services = null;
    boolean artifactNeeded = true;
    Map<String, String> parameters = new HashMap<>();
    if (PIPELINE.equals(trigger.getWorkflowType())) {
      Pipeline pipeline = validatePipeline(trigger.getAppId(), trigger.getWorkflowId(), true);
      services = pipeline.getServices();
      for (PipelineStage pipelineStage : pipeline.getPipelineStages()) {
        for (PipelineStageElement pipelineStageElement : pipelineStage.getPipelineStageElements()) {
          if (ENV_STATE.name().equals(pipelineStageElement.getType())) {
            try {
              Workflow workflow = workflowService.readWorkflow(
                  pipeline.getAppId(), (String) pipelineStageElement.getProperties().get("workflowId"));
              Validator.notNullCheck("workflow", workflow);
              Validator.notNullCheck("orchestrationWorkflow", workflow.getOrchestrationWorkflow());
              if (BUILD.equals(workflow.getOrchestrationWorkflow().getOrchestrationWorkflowType())) {
                artifactNeeded = false;
              }
              workflow.getOrchestrationWorkflow().getUserVariables().forEach(uservariable -> {
                if (!uservariable.getType().equals(VariableType.ENTITY)) {
                  parameters.put(uservariable.getName(), uservariable.getName() + "_placeholder");
                }
              });
            } catch (Exception ex) {
              logger.warn("Exception occurred while reading workflow associated to the pipeline {}", pipeline);
            }
          }
        }
      }
    } else if (ORCHESTRATION.equals(trigger.getWorkflowType())) {
      Workflow workflow = validateWorkflow(trigger.getAppId(), trigger.getWorkflowId());
      services = workflow.getServices();
      Map<String, String> workflowVariables = trigger.getWorkflowVariables();
      if (workflowVariables != null && workflowVariables.size() != 0) {
        if (!BUILD.equals(workflow.getOrchestrationWorkflow().getOrchestrationWorkflowType())) {
          if (workflow.getOrchestrationWorkflow().isServiceTemplatized()) {
            services = workflowService.resolveServices(workflow, workflowVariables);
          }
        }
      }
    }
    WebHookToken webHookToken =
        WebHookToken.builder().httpMethod("POST").webHookToken(CryptoUtil.secureRandAlphaNumString(40)).build();
    Map<String, Object> payload = new HashMap<>();
    payload.put("application", trigger.getAppId());

    List<Map<String, String>> artifactList = new ArrayList();
    if (services != null) {
      for (Service service : services) {
        Map<String, String> artifacts = new HashMap<>();
        artifacts.put("service", service.getName());
        artifacts.put("buildNumber", service.getName() + "_BUILD_NUMBER_PLACE_HOLDER");
        artifactList.add(artifacts);
      }
    }
    if (artifactList.size() != 0 && artifactNeeded) {
      payload.put("artifacts", artifactList);
    }
    if (parameters.size() != 0) {
      payload.put("parameters", parameters);
    }
    webHookToken.setPayload(new Gson().toJson(payload));
    return webHookToken;
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
  public void triggerScheduledExecutionAsync(Trigger trigger) {
    executorService.submit(() -> triggerScheduledExecution(trigger));
  }

  @Override
  public WorkflowExecution triggerExecutionByWebHook(
      String appId, String webHookToken, Map<String, String> serviceBuildNumbers, Map<String, String> parameters) {
    List<Artifact> artifacts = new ArrayList<>();
    Trigger trigger = getTrigger(appId, webHookToken);
    if (trigger == null) {
      throw new WingsException("No Trigger associated to the given token");
    }
    logger.info("Triggering  the execution for the Trigger {} by webhook", trigger.getName());
    addArtifactsFromVersionsOfWebHook(trigger, serviceBuildNumbers, artifacts);
    addArtifactsFromSelections(appId, trigger, artifacts);
    return triggerExecution(artifacts, trigger, parameters);
  }

  @Override
  public WorkflowExecution triggerExecutionByWebHook(
      String appId, String webHookToken, Artifact artifact, Map<String, String> parameters) {
    Trigger trigger = getTrigger(appId, webHookToken);
    if (trigger == null) {
      throw new WingsException("No Trigger associated to the given token");
    }
    logger.info("Triggering  the execution for the Trigger {} by webhook", trigger.getName());
    List<Artifact> artifacts = new ArrayList<>();
    if (artifact != null) {
      artifacts.add(artifact);
    }
    return triggerExecution(artifacts, trigger, parameters);
  }

  @Override
  public List<Trigger> getTriggersHasPipelineAction(String appId, String pipelineId) {
    return getTriggersHasWorkflowAction(appId, pipelineId);
  }

  @Override
  public List<Trigger> getTriggersHasWorkflowAction(String appId, String workflowId) {
    return getTriggersByApp(appId)
        .stream()
        .filter(trigger
            -> trigger.getArtifactSelections().stream().anyMatch(artifactSelection
                -> artifactSelection.getType().equals(LAST_DEPLOYED)
                    && artifactSelection.getWorkflowId().equals(workflowId)))
        .collect(toList());
  }

  @Override
  public List<Trigger> getTriggersHasArtifactStreamAction(String appId, String artifactStreamId) {
    return getTriggersByApp(appId)
        .stream()
        .filter(trigger
            -> trigger.getArtifactSelections().stream().anyMatch(artifactSelection
                -> artifactSelection.getType().equals(LAST_COLLECTED)
                    && artifactSelection.getArtifactStreamId().equals(artifactStreamId)))
        .collect(toList());
  }

  @Override
  public void updateByApp(String appId) {
    getTriggersByApp(appId).forEach(this ::update);
  }

  private Trigger getTrigger(String appId, String webHookToken) {
    List<Trigger> triggers = getTriggersByApp(appId);
    Trigger trigger = triggers.stream()
                          .filter(tr
                              -> tr.getCondition().getConditionType().equals(WEBHOOK)
                                  && ((WebHookTriggerCondition) tr.getCondition())
                                         .getWebHookToken()
                                         .getWebHookToken()
                                         .equals(webHookToken))
                          .findFirst()
                          .orElse(null);
    if (trigger == null) {
      throw new WingsException("Trigger does not exist or Invalid WebHook token");
    }
    return trigger;
  }

  private void addArtifactsFromVersionsOfWebHook(
      Trigger trigger, Map<String, String> serviceBuildNumbers, List<Artifact> artifacts) {
    if (MapUtils.isEmpty(serviceBuildNumbers)) {
      return;
    }
    Map<String, String> services = new HashMap<>();
    if (ORCHESTRATION.equals(trigger.getWorkflowType())) {
      List<Service> workflowServices = null;
      Workflow workflow = validateWorkflow(trigger.getAppId(), trigger.getWorkflowId());
      try {
        workflow = validateWorkflow(trigger.getAppId(), trigger.getWorkflowId());
        workflowServices = workflow.getServices();
      } catch (Exception e) {
        logger.warn("Error occurred while retrieving Pipeline {} ", trigger.getWorkflowId());
      }
      if (workflow == null) {
        throw new WingsException("Workflow " + trigger.getWorkflowName() + "does not exist.");
      }
      if (!BUILD.equals(workflow.getOrchestrationWorkflow().getOrchestrationWorkflowType())) {
        if (workflow.getOrchestrationWorkflow().isServiceTemplatized()) {
          workflowServices = workflowService.resolveServices(workflow, trigger.getWorkflowVariables());
        }
      }
      if (workflowServices != null) {
        services = workflowServices.stream().collect(Collectors.toMap(o -> o.getUuid(), o -> o.getName()));
      }
    } else {
      Pipeline pipeline = null;
      try {
        pipeline = validatePipeline(trigger.getAppId(), trigger.getWorkflowId(), true);
      } catch (WingsException e) {
        logger.warn("Error occurred while retrieving Pipeline {} ", trigger.getWorkflowId());
      }
      if (pipeline == null) {
        throw new WingsException("Pipeline " + trigger.getWorkflowName() + " does not exist.");
      }
      services = pipeline.getServices().stream().collect(Collectors.toMap(o -> o.getUuid(), o -> o.getName()));
    }
    Map<String, String> finalServices = services;
    trigger.getArtifactSelections()
        .stream()
        .filter(artifactSelection -> artifactSelection.getType().equals(WEBHOOK_VARIABLE))
        .forEach((ArtifactSelection artifactSelection) -> {
          Artifact artifact;
          String serviceName = finalServices.get(artifactSelection.getServiceId());
          String buildNumber = serviceBuildNumbers.get(serviceName);
          if (Misc.isNullOrEmpty(buildNumber)) {
            ArtifactStream artifactStream =
                artifactStreamService.get(trigger.getAppId(), artifactSelection.getArtifactStreamId());
            if (artifactStream != null) {
              artifact = artifactService.fetchLatestArtifactForArtifactStream(
                  trigger.getAppId(), artifactSelection.getArtifactStreamId(), artifactStream.getSourceName());
              if (artifact != null) {
                artifacts.add(artifact);
              }
            }
          } else {
            artifact = artifactService.getArtifactByBuildNumber(
                trigger.getAppId(), artifactSelection.getArtifactStreamId(), buildNumber);
            if (artifact != null) {
              artifacts.add(artifact);
            }
          }
        });
  }

  private void triggerExecutionPostArtifactCollection(Artifact artifact) {
    String appId = artifact.getAppId();
    List<Trigger> triggers = getTriggersByApp(appId);

    for (Trigger trigger1 : triggers.stream()
                                .filter(trigger
                                    -> trigger.getCondition().getConditionType().equals(NEW_ARTIFACT)
                                        && ((ArtifactTriggerCondition) trigger.getCondition())
                                               .getArtifactStreamId()
                                               .equals(artifact.getArtifactStreamId()))
                                .filter(Objects::nonNull)
                                .collect(toList())) {
      logger.info("Trigger found for artifact streamId {}", artifact.getArtifactStreamId());
      ArtifactTriggerCondition artifactTriggerCondition = (ArtifactTriggerCondition) trigger1.getCondition();
      List<Artifact> artifacts = new ArrayList<>();
      List<ArtifactSelection> artifactSelections = trigger1.getArtifactSelections();
      if (CollectionUtils.isEmpty(artifactSelections)) {
        logger.info("No artifact selections found so executing pipeline with the collected artifact");
        addIfArtifactFilterMatches(artifact, artifactTriggerCondition.getArtifactFilter(), artifacts);
        if (CollectionUtils.isEmpty(artifacts)) {
          logger.warn(
              "Skipping execution - artifact does not match with the given filter {}", artifactTriggerCondition);
          continue;
        }
      } else {
        logger.info("Artifact selections found collecting artifacts as per artifactStream selections ");
        if (artifactSelections.stream().anyMatch(artifactSelection
                -> artifactSelection.getType().equals(ARTIFACT_SOURCE)
                    && artifact.getServiceIds().contains(artifactSelection.getServiceId()))) {
          addIfArtifactFilterMatches(artifact, artifactTriggerCondition.getArtifactFilter(), artifacts);
          if (CollectionUtils.isEmpty(artifacts)) {
            logger.warn(
                "Skipping execution - artifact does not match with the given filter {}", artifactTriggerCondition);
            continue;
          }
        }
        addArtifactsFromSelections(trigger1.getAppId(), trigger1, artifacts);
      }
      triggerExecution(artifacts, trigger1);
    }
  }

  /**
   * Trigger execution post pipeline completion
   *
   * @param appId            AppId
   * @param sourcePipelineId SourcePipelineId
   */
  private void triggerExecutionPostPipelineCompletion(String appId, String sourcePipelineId) {
    List<Trigger> triggers = getTriggersByApp(appId);
    triggers.stream()
        .filter(trigger
            -> trigger.getCondition().getConditionType().equals(PIPELINE_COMPLETION)
                && ((PipelineTriggerCondition) trigger.getCondition()).getPipelineId().equals(sourcePipelineId))
        .filter(Objects::nonNull)
        .filter(distinctByKey(trigger -> trigger.getWorkflowId()))
        .collect(toList())
        .forEach(trigger -> {

          List<ArtifactSelection> artifactSelections = trigger.getArtifactSelections();
          if (CollectionUtils.isEmpty(artifactSelections)) {
            logger.info("No artifactSelection configuration setup found. Executing pipeline {} from source pipeline {}",
                trigger.getWorkflowId(), sourcePipelineId);
            triggerExecution(
                getLastDeployedArtifacts(appId, sourcePipelineId, trigger.getWorkflowType(), null), trigger);
          } else {
            List<Artifact> artifacts = new ArrayList<>();
            if (artifactSelections.stream().anyMatch(
                    artifactSelection -> artifactSelection.getType().equals(PIPELINE_SOURCE))) {
              addLastDeployedArtifacts(appId, sourcePipelineId, trigger.getWorkflowType(), null, artifacts);
            }
            addArtifactsFromSelections(trigger.getAppId(), trigger, artifacts);
            triggerExecution(artifacts, trigger);
          }
        });
  }

  private void triggerScheduledExecution(Trigger trigger) {
    List<Artifact> lastDeployedArtifacts =
        getLastDeployedArtifacts(trigger.getAppId(), trigger.getWorkflowId(), trigger.getWorkflowType(), null);

    ScheduledTriggerCondition scheduledTriggerCondition = (ScheduledTriggerCondition) trigger.getCondition();
    List<ArtifactSelection> artifactSelections = trigger.getArtifactSelections();
    if (CollectionUtils.isEmpty(artifactSelections)) {
      logger.info("No artifactSelection configuration setup found. Executing pipeline {}", trigger.getWorkflowId());
      if (!CollectionUtils.isEmpty(lastDeployedArtifacts)) {
        triggerExecution(lastDeployedArtifacts, trigger, null);
      }
    } else {
      List<Artifact> artifacts = new ArrayList<>();
      addArtifactsFromSelections(trigger.getAppId(), trigger, artifacts);
      if (CollectionUtils.isNotEmpty(artifacts)) {
        if (!scheduledTriggerCondition.isOnNewArtifactOnly()) {
          triggerExecution(artifacts, trigger);
        } else {
          List<String> lastDeployedArtifactIds =
              lastDeployedArtifacts.stream().map(Artifact::getUuid).distinct().collect(toList());
          List<String> artifactIds = artifacts.stream().map(Artifact::getUuid).distinct().collect(toList());
          if (!lastDeployedArtifactIds.containsAll(artifactIds)) {
            logger.info(
                "No new version of artifacts found from the last successful execution of pipeline {}. So, not triggering pipeline execution");
          }
          logger.info(
              "New version of artifacts found from the last successful execution of pipeline {}. So, triggering pipeline execution");
          triggerExecution(artifacts, trigger);
        }
      }
    }
  }

  private void addArtifactsFromSelections(String appId, Trigger trigger, List<Artifact> artifacts) {
    for (ArtifactSelection artifactSelection : trigger.getArtifactSelections()) {
      if (artifactSelection.getType().equals(LAST_COLLECTED)) {
        addLastCollectedArtifact(appId, artifactSelection, artifacts);
      } else if (artifactSelection.getType().equals(LAST_DEPLOYED)) {
        addLastDeployedArtifacts(appId, artifactSelection.getWorkflowId(), trigger.getWorkflowType(),
            artifactSelection.getServiceId(), artifacts);
      }
    }
  }

  /**
   * Last collected artifact for the given artifact stream
   *
   * @param appId             AppId
   * @param artifactSelection
   * @param artifacts
   */
  private void addLastCollectedArtifact(String appId, ArtifactSelection artifactSelection, List<Artifact> artifacts) {
    String artifactStreamId = artifactSelection.getArtifactStreamId();
    notNullCheck("artifactStreamId", artifactStreamId);
    ArtifactStream artifactStream = validateArtifactStream(appId, artifactStreamId);
    Artifact lastCollectedArtifact = artifactService.fetchLastCollectedArtifactForArtifactStream(
        appId, artifactStreamId, artifactStream.getSourceName());
    if (lastCollectedArtifact != null
        && lastCollectedArtifact.getServiceIds().contains(artifactSelection.getServiceId())) {
      addIfArtifactFilterMatches(lastCollectedArtifact, artifactSelection.getArtifactFilter(), artifacts);
    }
  }

  /**
   * Last successfully deployed or last deployed
   *
   * @param appId
   * @param workflowId
   * @param workflowType
   * @param serviceId
   * @return List<Artifact></Artifact>
   */
  private void addLastDeployedArtifacts(
      String appId, String workflowId, WorkflowType workflowType, String serviceId, List<Artifact> artifacts) {
    artifacts.addAll(getLastDeployedArtifacts(appId, workflowId, workflowType, serviceId));
  }

  /**
   * Last successfully deployed or last deployed
   *
   * @param appId
   * @param workflowId
   * @param workflowType
   * @param serviceId
   * @return List<Artifact></Artifact>
   */
  private List<Artifact> getLastDeployedArtifacts(
      String appId, String workflowId, WorkflowType workflowType, String serviceId) {
    List<Artifact> lastDeployedArtifacts = new ArrayList<>();
    PageRequest<WorkflowExecution> pageRequest = aPageRequest()
                                                     .withLimit("1")
                                                     .addFilter("workflowType", EQ, workflowType)
                                                     .addFilter("workflowId", EQ, workflowId)
                                                     .addFilter("appId", EQ, appId)
                                                     .addFilter("status", EQ, SUCCESS)
                                                     .addOrder(aSortOrder().withField("createdAt", DESC).build())
                                                     .build();

    PageResponse<WorkflowExecution> pageResponse =
        workflowExecutionService.listExecutions(pageRequest, false, false, false, false);
    if (pageResponse != null && pageResponse.getResponse() != null && pageResponse.getResponse().size() != 0) {
      if (pageResponse.getResponse().get(0).getExecutionArgs() != null) {
        lastDeployedArtifacts = pageResponse.getResponse().get(0).getExecutionArgs().getArtifacts();
        if (lastDeployedArtifacts != null) {
          if (serviceId != null) {
            lastDeployedArtifacts = lastDeployedArtifacts.stream()
                                        .filter(artifact1 -> artifact1.getServiceIds().contains(serviceId))
                                        .collect(toList());
          }
        }
      }
    }
    return lastDeployedArtifacts == null ? new ArrayList<>() : lastDeployedArtifacts;
  }

  private void addIfArtifactFilterMatches(Artifact artifact, String artifactFilter, List<Artifact> artifacts) {
    if (!StringUtils.isEmpty(artifactFilter)) {
      logger.info("Artifact filter {} set for artifact stream id {}", artifactFilter, artifact.getArtifactStreamId());
      Pattern pattern = Pattern.compile(artifactFilter.replace(".", "\\.").replace("?", ".?").replace("*", ".*?"));
      if (CollectionUtils.isEmpty(artifact.getArtifactFiles())) {
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
        if (!CollectionUtils.isEmpty(artifactFiles)) {
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
    if (artifacts != null) {
      executionArgs.setArtifacts(
          artifacts.stream().filter(distinctByKey(artifact -> artifact.getUuid())).collect(toList()));
    }
    executionArgs.setOrchestrationId(trigger.getWorkflowId());
    executionArgs.setExecutionCredential(aSSHExecutionCredential().withExecutionType(SSH).build());
    executionArgs.setWorkflowType(trigger.getWorkflowType());
    if (parameters != null) {
      executionArgs.setWorkflowVariables(parameters);
    }
    if (ORCHESTRATION.equals(trigger.getWorkflowType())) {
      logger.info("Triggering  workflow execution of appId {} with with workflow id {}", trigger.getAppId(),
          trigger.getWorkflowId());
      Workflow workflow = wingsPersistence.get(Workflow.class, trigger.getAppId(), trigger.getWorkflowId());
      Validator.notNullCheck("Workflow", workflow);
      Map<String, String> workflowVariables = executionArgs.getWorkflowVariables();
      Map<String, String> triggerWorkflowVariables = trigger.getWorkflowVariables();
      if (triggerWorkflowVariables != null) {
        if (workflowVariables != null) {
          for (String s : triggerWorkflowVariables.keySet()) {
            if (!workflowVariables.containsKey(s)) {
              workflowVariables.put(s, triggerWorkflowVariables.get(s));
            }
          }
        } else {
          workflowVariables = triggerWorkflowVariables;
        }
      }
      executionArgs.setWorkflowVariables(workflowVariables);
      workflowExecution =
          workflowExecutionService.triggerEnvExecution(trigger.getAppId(), workflow.getEnvId(), executionArgs);
      logger.info("Trigger workflow execution of appId {} with workflow id {} triggered", trigger.getAppId(),
          trigger.getWorkflowId());

    } else {
      logger.info(
          "Triggering  execution of appId {} with stream pipeline id {}", trigger.getAppId(), trigger.getWorkflowId());
      workflowExecution =
          workflowExecutionService.triggerPipelineExecution(trigger.getAppId(), trigger.getWorkflowId(), executionArgs);
      logger.info("Pipeline execution of appId {} with stream pipeline id {} triggered", trigger.getAppId(),
          trigger.getWorkflowId());
    }
    return workflowExecution;
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

  private void validateAndSetCronExpression(Trigger trigger) {
    try {
      if (trigger == null || !trigger.getCondition().getConditionType().equals(SCHEDULED)) {
        return;
      }
      ScheduledTriggerCondition scheduledTriggerCondition = (ScheduledTriggerCondition) trigger.getCondition();
      if (!isNullOrEmpty(scheduledTriggerCondition.getCronExpression())) {
        CronScheduleBuilder.cronSchedule(ScheduledTriggerJob.PREFIX + scheduledTriggerCondition.getCronExpression());
        scheduledTriggerCondition.setCronDescription(
            getCronDescription(ScheduledTriggerJob.PREFIX + scheduledTriggerCondition.getCronExpression()));
      }
    } catch (Exception ex) {
      throw new WingsException(INVALID_ARGUMENT).addParam("args", "Invalid cron expression");
    }
  }

  public String getCronDescription(String cronExpression) {
    try {
      String description =
          getDescription(DescriptionTypeEnum.FULL, cronExpression, new Options(), I18nMessages.DEFAULT_LOCALE);
      return StringUtils.lowerCase("" + description.charAt(0)) + description.substring(1);
    } catch (Exception e) {
      logger.error("Error parsing cron expression: " + cronExpression, e);
      throw new WingsException(INVALID_ARGUMENT).addParam("args", "Invalid cron expression");
    }
  }

  @Override
  public Trigger getTriggerByWebhookToken(String token) {
    return wingsPersistence.executeGetOneQuery(
        wingsPersistence.createQuery(Trigger.class).field("webHookToken").equal(token));
  }

  @Override
  public WorkflowExecution triggerExecutionByWebHook(Trigger trigger, Map<String, String> parameters) {
    return triggerExecution(null, trigger, parameters);
  }

  @Override
  public WebhookParameters listWebhookParameters(String appId, String workflowId, WorkflowType workflowType) {
    if (workflowType == null) {
      workflowType = WorkflowType.PIPELINE;
    }
    List<String> parameters = new ArrayList<>();
    if (PIPELINE.equals(workflowType)) {
      Pipeline pipeline = validatePipeline(appId, workflowId, true);
      for (PipelineStage pipelineStage : pipeline.getPipelineStages()) {
        for (PipelineStageElement pipelineStageElement : pipelineStage.getPipelineStageElements()) {
          if (ENV_STATE.name().equals(pipelineStageElement.getType())) {
            try {
              Workflow workflow = workflowService.readWorkflow(
                  pipeline.getAppId(), (String) pipelineStageElement.getProperties().get("workflowId"));
              notNullCheck("workflow", workflow);
              notNullCheck("orchestrationWorkflow", workflow.getOrchestrationWorkflow());
              workflow.getOrchestrationWorkflow().getUserVariables().forEach(uservariable -> {
                if (!uservariable.getType().equals(VariableType.ENTITY)) {
                  if (!parameters.contains(uservariable.getName())) {
                    parameters.add(uservariable.getName());
                  }
                }
              });
            } catch (Exception ex) {
              logger.warn("Exception occurred while reading workflow associated to the pipeline {}", pipeline);
            }
          }
        }
      }
    } else if (ORCHESTRATION.equals(workflowType)) {
      Workflow workflow = workflowService.readWorkflow(appId, workflowId);
      notNullCheck("workflow", workflow);
      notNullCheck("orchestrationWorkflow", workflow.getOrchestrationWorkflow());
      workflow.getOrchestrationWorkflow().getUserVariables().forEach(uservariable -> {
        if (!uservariable.getType().equals(VariableType.ENTITY)) {
          if (!parameters.contains(uservariable.getName())) {
            parameters.add(uservariable.getName());
          }
        }
      });
    }
    WebhookParameters webhookParameters = new WebhookParameters();
    webhookParameters.setParams(parameters);
    webhookParameters.setExpressions(webhookParameters.pullRequestExpressions());
    return webhookParameters;
  }

  private ArtifactStream validateArtifactStream(String appId, String artifactStreamId) {
    ArtifactStream artifactStream = artifactStreamService.get(appId, artifactStreamId);
    notNullCheck("ArtifactStream", artifactStream);
    return artifactStream;
  }

  private void validateAndSetTriggerCondition(Trigger trigger, List<Service> services) {
    switch (trigger.getCondition().getConditionType()) {
      case NEW_ARTIFACT:
        ArtifactTriggerCondition artifactTriggerCondition = (ArtifactTriggerCondition) trigger.getCondition();
        ArtifactStream artifactStream =
            validateArtifactStream(trigger.getAppId(), artifactTriggerCondition.getArtifactStreamId());
        Service service = serviceResourceService.get(trigger.getAppId(), artifactStream.getServiceId(), false);
        notNullCheck("Service", service);
        artifactTriggerCondition.setArtifactSourceName(artifactStream.getSourceName() + " (" + service.getName() + ")");
        break;
      case PIPELINE_COMPLETION:
        PipelineTriggerCondition pipelineTriggerCondition = (PipelineTriggerCondition) trigger.getCondition();
        Pipeline pipeline = validatePipeline(trigger.getAppId(), pipelineTriggerCondition.getPipelineId(), false);
        pipelineTriggerCondition.setPipelineName(pipeline.getName());
        break;
      case WEBHOOK:
        WebHookTriggerCondition webHookTriggerCondition = (WebHookTriggerCondition) trigger.getCondition();
        if (webHookTriggerCondition.getWebHookToken() == null
            || Misc.isNullOrEmpty(webHookTriggerCondition.getWebHookToken().getWebHookToken())) {
          WebHookToken webHookToken = generateWebHookToken(trigger);
          webHookTriggerCondition.setWebHookToken(webHookToken);
        }
        trigger.setWebHookToken(webHookTriggerCondition.getWebHookToken().getWebHookToken());
        break;
      case SCHEDULED:
        ScheduledTriggerCondition scheduledTriggerCondition = (ScheduledTriggerCondition) trigger.getCondition();
        notNullCheck("CronExpression", scheduledTriggerCondition.getCronExpression());
        break;
      default:
        throw new WingsException(INVALID_REQUEST).addParam("message", "Invalid trigger condition type");
    }
  }

  private void validateAndSetArtifactSelections(Trigger trigger, List<Service> services) {
    List<ArtifactSelection> artifactSelections = trigger.getArtifactSelections();
    if (CollectionUtils.isEmpty(artifactSelections)) {
      return;
    }
    if (CollectionUtils.isEmpty(services)) {
      throw new WingsException(INVALID_REQUEST).addParam("message", "Pipeline services can not be empty");
    }

    Map<String, String> serviceIdNames =
        services.stream().collect(Collectors.toMap(Service::getUuid, Service::getName));
    artifactSelections.forEach(artifactSelection -> {
      ArtifactStream artifactStream;
      Service service;
      switch (artifactSelection.getType()) {
        case LAST_DEPLOYED:
          if (Misc.isNullOrEmpty(artifactSelection.getWorkflowId())) {
            throw new WingsException(INVALID_REQUEST)
                .addParam("message", "Pipeline cannot be empty for Last deployed type");
          }
          if (ORCHESTRATION.equals(trigger.getWorkflowType())) {
            Workflow workflow =
                wingsPersistence.get(Workflow.class, trigger.getAppId(), artifactSelection.getWorkflowId());
            notNullCheck("LastDeployedWorkflow", workflow);
            artifactSelection.setWorkflowName(workflow.getName());
          } else {
            Pipeline pipeline =
                pipelineService.readPipeline(trigger.getAppId(), artifactSelection.getWorkflowId(), false);
            notNullCheck("LastDeployedPipeline", pipeline);
            artifactSelection.setWorkflowName(pipeline.getName());
          }
          break;
        case LAST_COLLECTED:
          if (Misc.isNullOrEmpty(artifactSelection.getArtifactStreamId())) {
            throw new WingsException(INVALID_REQUEST)
                .addParam("message", "Artifact Source cannot be empty for Last collected type");
          }
          artifactStream = validateArtifactStream(trigger.getAppId(), artifactSelection.getArtifactStreamId());
          service = serviceResourceService.get(trigger.getAppId(), artifactStream.getServiceId(), false);
          notNullCheck("Service", service);
          artifactSelection.setArtifactSourceName(artifactStream.getSourceName() + " (" + service.getName() + ")");
          break;
        case WEBHOOK_VARIABLE:
          if (Misc.isNullOrEmpty(artifactSelection.getArtifactStreamId())) {
            throw new WingsException(INVALID_REQUEST)
                .addParam("message", "Artifact Source cannot be empty for Webhook Variable type");
          }
          artifactStream = validateArtifactStream(trigger.getAppId(), artifactSelection.getArtifactStreamId());
          service = serviceResourceService.get(trigger.getAppId(), artifactStream.getServiceId(), false);
          notNullCheck("Service", service);
          artifactSelection.setArtifactSourceName(artifactStream.getSourceName() + " (" + service.getName() + ")");
          break;
        case ARTIFACT_SOURCE:
        case PIPELINE_SOURCE:
          break;
        default:
          throw new WingsException(INVALID_REQUEST).addParam("message", "Invalid artifact selection type");
      }
      if (serviceIdNames.get(artifactSelection.getServiceId()) == null) {
        service = serviceResourceService.get(trigger.getAppId(), artifactSelection.getServiceId(), false);
        notNullCheck("Service", service);
        artifactSelection.setServiceName(service.getName());
      } else {
        artifactSelection.setServiceName(serviceIdNames.get(artifactSelection.getServiceId()));
      }
    });
  }

  private void validateInput(Trigger trigger) {
    List<Service> services;
    if (PIPELINE.equals(trigger.getWorkflowType())) {
      Pipeline executePipeline = validatePipeline(trigger.getAppId(), trigger.getWorkflowId(), true);
      trigger.setWorkflowName(executePipeline.getName());
      services = executePipeline.getServices();
      validateAndSetArtifactSelections(trigger, services);
    } else if (ORCHESTRATION.equals(trigger.getWorkflowType())) {
      Workflow workflow = validateWorkflow(trigger.getAppId(), trigger.getWorkflowId());
      if (workflow.isTemplatized()) {
        trigger.setWorkflowName(workflow.getName() + " (TEMPLATE)");
      } else {
        trigger.setWorkflowName(workflow.getName());
      }
      services = workflow.getServices();
      validateAndSetArtifactSelections(trigger, services);
    } else {
      throw new WingsException(INVALID_ARGUMENT)
          .addParam("args", "Workflow Type" + trigger.getWorkflowType() + "not yet supported");
    }
    validateAndSetTriggerCondition(trigger, services);
    validateAndSetCronExpression(trigger);
  }

  private Pipeline validatePipeline(String appId, String pipelineId, boolean withServices) {
    Pipeline pipeline = pipelineService.readPipeline(appId, pipelineId, withServices);
    notNullCheck("Pipeline", pipeline);
    return pipeline;
  }

  private Workflow validateWorkflow(String appId, String workflowId) {
    Workflow workflow = workflowService.readWorkflow(appId, workflowId);
    Validator.notNullCheck("Workflow", workflow);
    return workflow;
  }

  public <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
    Map<Object, Boolean> seen = new ConcurrentHashMap<>();
    return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
  }
}
