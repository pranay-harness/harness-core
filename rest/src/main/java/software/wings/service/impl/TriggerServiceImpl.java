package software.wings.service.impl;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.stream.Collectors.toList;
import static net.redhogs.cronparser.CronExpressionDescriptor.getDescription;
import static software.wings.beans.ErrorCode.INVALID_ARGUMENT;
import static software.wings.beans.ErrorCode.INVALID_REQUEST;
import static software.wings.beans.ExecutionCredential.ExecutionType.SSH;
import static software.wings.beans.SSHExecutionCredential.Builder.aSSHExecutionCredential;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.SortOrder.Builder.aSortOrder;
import static software.wings.beans.SortOrder.OrderType.DESC;
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

import com.google.gson.Gson;
import com.google.inject.name.Named;

import net.redhogs.cronparser.DescriptionTypeEnum;
import net.redhogs.cronparser.I18nMessages;
import net.redhogs.cronparser.Options;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.Pipeline;
import software.wings.beans.Service;
import software.wings.beans.WebHookToken;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactFile;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.trigger.ArtifactSelection;
import software.wings.beans.trigger.ArtifactTriggerCondition;
import software.wings.beans.trigger.PipelineTriggerCondition;
import software.wings.beans.trigger.ScheduledTriggerCondition;
import software.wings.beans.trigger.Trigger;
import software.wings.beans.trigger.WebHookTriggerCondition;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.scheduler.QuartzScheduler;
import software.wings.scheduler.ScheduledTriggerJob;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.TriggerService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.utils.CryptoUtil;
import software.wings.utils.Misc;
import software.wings.utils.Validator;

import java.text.ParseException;
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
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.executable.ValidateOnExecution;

/**
 * Handles Triggers
 * Created by Srinivas on 10/26/17.
 */

@Singleton
@ValidateOnExecution
public class TriggerServiceImpl implements TriggerService {
  public static final String SCHEDULED_TRIGGER_CRON_GROUP = "SCHEDULED_TRIGGER_CRON_GROUP";
  private static final String CRON_PREFIX = "0 "; // 'Second' unit prefix to convert unix to quartz cron expression
  private static final Logger logger = LoggerFactory.getLogger(TriggerServiceImpl.class);

  @Inject private WingsPersistence wingsPersistence;
  @Inject @Named("JobScheduler") private QuartzScheduler jobScheduler;
  @Inject private ExecutorService executorService;
  @Inject private WorkflowExecutionService workflowExecutionService;
  @Inject private ArtifactService artifactService;
  @Inject private ArtifactStreamService artifactStreamService;

  @Inject private PipelineService pipelineService;

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
        Validator.duplicateCheck(() -> wingsPersistence.saveAndGet(Trigger.class, trigger), "name", trigger.getName());
    Validator.notNullCheck("Trigger", savedTrigger);
    if (trigger.getCondition().getConditionType().equals(SCHEDULED)) {
      addCronForScheduledJobExecution(savedTrigger.getAppId(), savedTrigger);
    }
    return savedTrigger;
  }

  private void validateInput(Trigger trigger) {
    Pipeline executePipeline = validatePipeline(trigger.getAppId(), trigger.getPipelineId(), true);
    trigger.setPielineName(executePipeline.getName());

    validateAndSetTriggerCondition(trigger);
    validateAndSetArtifactSelections(trigger, executePipeline.getServices());
    validateAndSetCronExpression(trigger);
  }

  @Override
  public Trigger update(Trigger trigger) {
    validateInput(trigger);

    Trigger existingTrigger = wingsPersistence.get(Trigger.class, trigger.getAppId(), trigger.getUuid());
    Validator.notNullCheck("Trigger", existingTrigger);
    Trigger updatedTrigger =
        Validator.duplicateCheck(() -> wingsPersistence.saveAndGet(Trigger.class, trigger), "name", trigger.getName());
    addOrUpdateCronForScheduledJob(trigger, existingTrigger);
    return updatedTrigger;
  }

  @Override
  public boolean delete(String appId, String triggerId) {
    Trigger trigger = wingsPersistence.get(Trigger.class, appId, triggerId);
    if (trigger == null) {
      return true;
    }
    return deleteTrigger(triggerId, trigger);
  }

  private boolean deleteTrigger(String triggerId, Trigger trigger) {
    boolean deleted = wingsPersistence.delete(Trigger.class, triggerId);
    if (deleted) {
      if (trigger.getCondition().getConditionType().equals(SCHEDULED)) {
        jobScheduler.deleteJob(triggerId, SCHEDULED_TRIGGER_CRON_GROUP);
      }
    }
    return deleted;
  }

  @Override
  public void deleteByApp(String appId) {
    wingsPersistence.createQuery(Trigger.class)
        .field("appId")
        .equal(appId)
        .asList()
        .forEach(trigger -> delete(appId, trigger.getUuid()));
  }

  @Override
  public void deleteTriggersForPipeline(String appId, String pipelineId) {
    List<Trigger> triggers = wingsPersistence.createQuery(Trigger.class)
                                 .field("appId")
                                 .equal(appId)
                                 .field("pipelineId")
                                 .equal(pipelineId)
                                 .asList();
    triggers.forEach(trigger -> deleteTrigger(trigger.getUuid(), trigger));

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
  public void deleteTriggersForArtifactStream(String appId, String artifactStreamId) {
    List<Trigger> triggers = getTriggersByApp(appId);

    triggers.stream()
        .filter(trigger
            -> trigger.getCondition().getConditionType().equals(NEW_ARTIFACT)
                && ((ArtifactTriggerCondition) trigger.getCondition()).getArtifactStreamId().equals(artifactStreamId))
        .filter(Objects::nonNull)
        .collect(toList())
        .forEach(trigger -> deleteTrigger(trigger.getUuid(), trigger));
  }

  private List<Trigger> getTriggersByApp(String appId) {
    return wingsPersistence.query(Trigger.class, aPageRequest().addFilter("appId", EQ, appId).build()).getResponse();
  }

  @Override
  public WebHookToken generateWebHookToken(String appId, String pipelineId) {
    Pipeline pipeline = validatePipeline(appId, pipelineId, true);

    WebHookToken webHookToken =
        WebHookToken.builder().httpMethod("POST").webHookToken(CryptoUtil.secureRandAlphaNumString(40)).build();
    Map<String, Object> payload = new HashMap<>();
    payload.put("application", appId);

    List<Service> services = pipeline.getServices();
    List<Map<String, String>> artifactList = new ArrayList();
    if (services != null) {
      for (Service service : services) {
        Map<String, String> artifacts = new HashMap<>();
        artifacts.put("service", service.getName());
        artifacts.put("buildNumber", service.getName() + "_BUILD_NUMBER_PLACE_HOLDER");
        artifactList.add(artifacts);
      }
    }
    payload.put("artifacts", artifactList);

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
  public void triggerScheduledExecutionAsync(String appId, String triggerId) {
    executorService.submit(() -> triggerScheduledExecution(appId, triggerId));
  }

  @Override
  public WorkflowExecution triggerExecutionByWebHook(
      String appId, String webHookToken, Map<String, String> serviceBuildNumbers, Map<String, String> parameters) {
    List<Artifact> artifacts = new ArrayList<>();
    Trigger trigger = getTrigger(appId, webHookToken);
    addArtifactsFromVersionsOfWebHook(trigger, serviceBuildNumbers, artifacts);
    addArtifactsFromSelections(appId, trigger.getArtifactSelections(), artifacts);
    return triggerExecution(artifacts, trigger, parameters);
  }

  @Override
  public WorkflowExecution triggerExecutionByWebHook(
      String appId, String webHookToken, Artifact artifact, Map<String, String> parameters) {
    Trigger trigger = getTrigger(appId, webHookToken);
    List<Artifact> artifacts = new ArrayList<>();
    if (artifact != null) {
      artifacts.add(artifact);
    }
    return triggerExecution(artifacts, trigger, parameters);
  }

  @Override
  public List<Trigger> getTriggersHasPipelineAction(String appId, String pipelineId) {
    return getTriggersByApp(appId)
        .stream()
        .filter(trigger
            -> trigger.getArtifactSelections().stream().anyMatch(artifactSelection
                -> artifactSelection.getType().equals(LAST_DEPLOYED)
                    && artifactSelection.getPipelineId().equals(pipelineId)))
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
      throw new WingsException("Invalid WebHook token");
    }
    return trigger;
  }

  private void addArtifactsFromVersionsOfWebHook(
      Trigger trigger, Map<String, String> serviceBuildNumbers, List<Artifact> artifacts) {
    if (serviceBuildNumbers == null || serviceBuildNumbers.size() == 0) {
      return;
    }
    Pipeline pipeline = pipelineService.readPipeline(trigger.getAppId(), trigger.getPipelineId(), true);
    if (pipeline == null) {
      throw new WingsException("Pipeline does not exist any more");
    }
    Map<String, String> services =
        pipeline.getServices().stream().collect(Collectors.toMap(o -> o.getUuid(), o -> o.getName()));
    trigger.getArtifactSelections()
        .stream()
        .filter(artifactSelection -> artifactSelection.getType().equals(WEBHOOK_VARIABLE))
        .forEach((ArtifactSelection artifactSelection) -> {
          Artifact artifact;
          String serviceName = services.get(artifactSelection.getServiceId());
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
        addArtifactsFromSelections(trigger1.getAppId(), artifactSelections, artifacts);
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
        .filter(distinctByKey(trigger -> trigger.getPipelineId()))
        .collect(toList())
        .forEach(trigger -> {

          List<ArtifactSelection> artifactSelections = trigger.getArtifactSelections();
          if (CollectionUtils.isEmpty(artifactSelections)) {
            logger.info("No artifactSelection configuration setup found. Executing pipeline {} from source pipeline {}",
                trigger.getPipelineId(), sourcePipelineId);
            triggerExecution(getLastDeployedArtifacts(appId, sourcePipelineId, null), trigger);
          } else {
            List<Artifact> artifacts = new ArrayList<>();
            if (artifactSelections.stream().anyMatch(
                    artifactSelection -> artifactSelection.getType().equals(PIPELINE_SOURCE))) {
              addLastDeployedArtifacts(appId, sourcePipelineId, null, artifacts);
            }
            addArtifactsFromSelections(trigger.getAppId(), artifactSelections, artifacts);
            triggerExecution(artifacts, trigger);
          }
        });
  }

  private void triggerScheduledExecution(String appId, String triggerId) {
    logger.info("Triggering scheduled job for appId {} and triggerId {}", appId, triggerId);
    Trigger trigger = wingsPersistence.get(Trigger.class, appId, triggerId);
    if (trigger == null || !trigger.getCondition().getConditionType().equals(SCHEDULED)) {
      logger.info("Trigger not found or wrong type. Deleting job associated to it");
      jobScheduler.deleteJob(triggerId, SCHEDULED_TRIGGER_CRON_GROUP);
      return;
    }
    List<Artifact> lastDeployedArtifacts = getLastDeployedArtifacts(appId, trigger.getPipelineId(), null);

    ScheduledTriggerCondition scheduledTriggerCondition = (ScheduledTriggerCondition) trigger.getCondition();
    List<ArtifactSelection> artifactSelections = trigger.getArtifactSelections();
    if (artifactSelections == null || artifactSelections.size() == 0) {
      logger.info("No artifactSelection configuration setup found. Executing pipeline {}", trigger.getPipelineId());
      triggerExecution(lastDeployedArtifacts, trigger, null);
    } else {
      List<Artifact> artifacts = new ArrayList<>();
      addArtifactsFromSelections(appId, artifactSelections, artifacts);
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

  private void addArtifactsFromSelections(
      String appId, List<ArtifactSelection> artifactSelections, List<Artifact> artifacts) {
    for (ArtifactSelection artifactSelection : artifactSelections) {
      if (artifactSelection.getType().equals(LAST_COLLECTED)) {
        addLastCollectedArtifact(appId, artifactSelection, artifacts);
      } else if (artifactSelection.getType().equals(LAST_DEPLOYED)) {
        addLastDeployedArtifacts(appId, artifactSelection.getPipelineId(), artifactSelection.getServiceId(), artifacts);
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
    Validator.notNullCheck("artifactStreamId", artifactStreamId);
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
   * @param pipelineId
   * @return List<Artifact></Artifact>
   */
  private void addLastDeployedArtifacts(String appId, String pipelineId, String serviceId, List<Artifact> artifacts) {
    artifacts.addAll(getLastDeployedArtifacts(appId, pipelineId, serviceId));
  }

  /**
   * Last successfully deployed or last deployed
   *
   * @param appId
   * @param pipelineId
   * @param serviceId
   * @return List<Artifact></Artifact>
   */
  private List<Artifact> getLastDeployedArtifacts(String appId, String pipelineId, String serviceId) {
    List<Artifact> lastDeployedArtifacts = new ArrayList<>();
    PageRequest<WorkflowExecution> pageRequest = aPageRequest()
                                                     .withLimit("1")
                                                     .addFilter("workflowType", EQ, PIPELINE)
                                                     .addFilter("workflowId", EQ, pipelineId)
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
    return lastDeployedArtifacts;
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
    executionArgs.setArtifacts(
        artifacts.stream().filter(distinctByKey(artifact -> artifact.getUuid())).collect(toList()));
    String pipelineId = trigger.getPipelineId();
    executionArgs.setOrchestrationId(pipelineId);
    executionArgs.setExecutionCredential(aSSHExecutionCredential().withExecutionType(SSH).build());
    executionArgs.setWorkflowType(PIPELINE);
    if (parameters != null) {
      executionArgs.setWorkflowVariables(parameters);
    }
    logger.info("Triggering Pipeline execution of appId {} with stream pipeline id {}", trigger.getAppId(), pipelineId);
    workflowExecution =
        workflowExecutionService.triggerPipelineExecution(trigger.getAppId(), pipelineId, executionArgs);
    logger.info("Pipeline execution of appId {} with stream pipeline id {} triggered", trigger.getAppId(), pipelineId);
    return workflowExecution;
  }

  private void addCronForScheduledJobExecution(String appId, Trigger trigger) {
    logger.info("Adding scheduled cron name {} for appId {}", trigger.getName(), trigger.getAppId());
    JobDetail job = JobBuilder.newJob(ScheduledTriggerJob.class)
                        .withIdentity(trigger.getUuid(), SCHEDULED_TRIGGER_CRON_GROUP)
                        .usingJobData("triggerId", trigger.getUuid())
                        .usingJobData("appId", appId)
                        .build();
    org.quartz.Trigger quartzTrigger = getQuartzTrigger(trigger);
    jobScheduler.scheduleJob(job, quartzTrigger);
  }

  private void addOrUpdateCronForScheduledJob(Trigger trigger, Trigger existingTrigger) {
    if (existingTrigger.getCondition().getConditionType().equals(SCHEDULED)) {
      if (trigger.getCondition().getConditionType().equals(SCHEDULED)) {
        TriggerKey triggerKey = new TriggerKey(trigger.getUuid(), SCHEDULED_TRIGGER_CRON_GROUP);
        org.quartz.Trigger quartzTrigger = getQuartzTrigger(trigger);
        jobScheduler.rescheduleJob(triggerKey, quartzTrigger);
      } else {
        jobScheduler.deleteJob(existingTrigger.getUuid(), SCHEDULED_TRIGGER_CRON_GROUP);
      }
    } else if (trigger.getCondition().getConditionType().equals(SCHEDULED)) {
      addCronForScheduledJobExecution(trigger.getAppId(), trigger);
    }
  }

  private org.quartz.Trigger getQuartzTrigger(Trigger trigger) {
    return TriggerBuilder.newTrigger()
        .withIdentity(trigger.getUuid(), SCHEDULED_TRIGGER_CRON_GROUP)
        .withSchedule(CronScheduleBuilder.cronSchedule(
            CRON_PREFIX + ((ScheduledTriggerCondition) trigger.getCondition()).getCronExpression()))
        .build();
  }

  private void validateAndSetCronExpression(Trigger trigger) {
    try {
      if (trigger == null || !trigger.getCondition().getConditionType().equals(SCHEDULED)) {
        return;
      }
      ScheduledTriggerCondition scheduledTriggerCondition = (ScheduledTriggerCondition) trigger.getCondition();
      if (!isNullOrEmpty(scheduledTriggerCondition.getCronExpression())) {
        CronScheduleBuilder.cronSchedule(CRON_PREFIX + scheduledTriggerCondition.getCronExpression());
        scheduledTriggerCondition.setCronDescription(
            getCronDescription(CRON_PREFIX + scheduledTriggerCondition.getCronExpression()));
      }
    } catch (Exception ex) {
      throw new WingsException(INVALID_ARGUMENT, "args", "Invalid cron expression");
    }
  }

  private String getCronDescription(String cronExpression) {
    try {
      String description =
          getDescription(DescriptionTypeEnum.FULL, cronExpression, new Options(), I18nMessages.DEFAULT_LOCALE);
      return StringUtils.lowerCase("" + description.charAt(0)) + description.substring(1);
    } catch (ParseException e) {
      logger.error("Error parsing cron expression: " + cronExpression, e);
      return cronExpression;
    }
  }

  private void validateAndSetArtifactSelections(Trigger trigger, List<Service> services) {
    List<ArtifactSelection> artifactSelections = trigger.getArtifactSelections();
    if (CollectionUtils.isEmpty(artifactSelections)) {
      return;
    }
    if (CollectionUtils.isEmpty(services)) {
      throw new WingsException(INVALID_REQUEST, "message", "Pipeline services can not be empty");
    }
    Map<String, String> serviceIdNames =
        services.stream().collect(Collectors.toMap(Service::getUuid, Service::getName));
    artifactSelections.forEach(artifactSelection -> {
      switch (artifactSelection.getType()) {
        case LAST_DEPLOYED:
          if (Misc.isNullOrEmpty(artifactSelection.getPipelineId())) {
            throw new WingsException(INVALID_REQUEST, "message", "Pipeline cannot be empty for Last deployed type");
          }
          Pipeline pipeline =
              pipelineService.readPipeline(trigger.getAppId(), artifactSelection.getPipelineId(), false);
          Validator.notNullCheck("LastDeployedPipeline", pipeline);
          artifactSelection.setPipelineName(pipeline.getName());
          break;
        case LAST_COLLECTED:
          if (Misc.isNullOrEmpty(artifactSelection.getArtifactStreamId())) {
            throw new WingsException(
                INVALID_REQUEST, "message", "Artifact Source cannot be empty for Last collected type");
          }
          artifactSelection.setArtifactSourceName(
              validateArtifactStream(trigger.getAppId(), artifactSelection.getArtifactStreamId()).getSourceName());
          break;
        case WEBHOOK_VARIABLE:
          if (Misc.isNullOrEmpty(artifactSelection.getArtifactStreamId())) {
            throw new WingsException(
                INVALID_REQUEST, "message", "Artifact Source cannot be empty for Last collected type");
          }
          break;
        case ARTIFACT_SOURCE:
        case PIPELINE_SOURCE:
          break;
        default:
          throw new WingsException(INVALID_REQUEST, "message", "Invalid artifact selection type");
      }
      artifactSelection.setServiceName(serviceIdNames.get(artifactSelection.getServiceId()));
    });
  }

  private ArtifactStream validateArtifactStream(String appId, String artifactStreamId) {
    ArtifactStream artifactStream = artifactStreamService.get(appId, artifactStreamId);
    Validator.notNullCheck("ArtifactStream", artifactStream);
    return artifactStream;
  }

  private void validateAndSetTriggerCondition(Trigger trigger) {
    switch (trigger.getCondition().getConditionType()) {
      case NEW_ARTIFACT:
        ArtifactTriggerCondition artifactTriggerCondition = (ArtifactTriggerCondition) trigger.getCondition();
        ArtifactStream artifactStream =
            validateArtifactStream(trigger.getAppId(), artifactTriggerCondition.getArtifactStreamId());
        artifactTriggerCondition.setArtifactSourceName(artifactStream.getSourceName());
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
          webHookTriggerCondition.setWebHookToken(generateWebHookToken(trigger.getAppId(), trigger.getPipelineId()));
        }
        break;
      default:
        throw new WingsException(INVALID_REQUEST, "message", "Invalid artifact selection type");
    }
  }

  private Pipeline validatePipeline(String appId, String pipelineId, boolean withServices) {
    Pipeline pipeline = pipelineService.readPipeline(appId, pipelineId, withServices);
    Validator.notNullCheck("Pipeline", pipeline);
    return pipeline;
  }

  public <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
    Map<Object, Boolean> seen = new ConcurrentHashMap<>();
    return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
  }
}
