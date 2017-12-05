package software.wings.service.impl;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Arrays.asList;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.ErrorCode.INVALID_REQUEST;
import static software.wings.beans.ExecutionCredential.ExecutionType.SSH;
import static software.wings.beans.SSHExecutionCredential.Builder.aSSHExecutionCredential;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.WorkflowType.ORCHESTRATION;
import static software.wings.beans.WorkflowType.PIPELINE;
import static software.wings.beans.artifact.ArtifactStreamType.AMAZON_S3;
import static software.wings.beans.artifact.ArtifactStreamType.ARTIFACTORY;
import static software.wings.beans.artifact.ArtifactStreamType.DOCKER;
import static software.wings.beans.artifact.ArtifactStreamType.ECR;
import static software.wings.beans.artifact.ArtifactStreamType.GCR;
import static software.wings.beans.artifact.ArtifactStreamType.NEXUS;
import static software.wings.dl.PageRequest.Builder.aPageRequest;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import net.redhogs.cronparser.CronExpressionDescriptor;
import net.redhogs.cronparser.DescriptionTypeEnum;
import net.redhogs.cronparser.I18nMessages;
import net.redhogs.cronparser.Options;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateResults;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.Environment;
import software.wings.beans.ErrorCode;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.Pipeline;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.Service;
import software.wings.beans.SortOrder.OrderType;
import software.wings.beans.WebHookRequest;
import software.wings.beans.WebHookToken;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactFile;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStreamAction;
import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.beans.yaml.Change.ChangeType;
import software.wings.beans.yaml.GitFileChange;
import software.wings.common.Constants;
import software.wings.common.UUIDGenerator;
import software.wings.dl.PageRequest;
import software.wings.dl.PageRequest.Builder;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.scheduler.ArtifactCollectionJob;
import software.wings.scheduler.ArtifactStreamActionJob;
import software.wings.scheduler.QuartzScheduler;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.BuildSourceService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.TriggerService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;
import software.wings.service.intfc.yaml.EntityUpdateService;
import software.wings.service.intfc.yaml.YamlChangeSetService;
import software.wings.service.intfc.yaml.YamlDirectoryService;
import software.wings.sm.ExecutionStatus;
import software.wings.stencils.DataProvider;
import software.wings.stencils.Stencil;
import software.wings.stencils.StencilPostProcessor;
import software.wings.utils.ArtifactType;
import software.wings.utils.CryptoUtil;
import software.wings.utils.JsonUtils;
import software.wings.utils.Util;
import software.wings.utils.Validator;
import software.wings.utils.validation.Create;
import software.wings.utils.validation.Update;
import software.wings.yaml.gitSync.YamlGitConfig;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.validation.executable.ValidateOnExecution;
import javax.ws.rs.NotFoundException;

/**
 * The Class ArtifactStreamServiceImpl.
 */
@Singleton
@ValidateOnExecution
public class ArtifactStreamServiceImpl implements ArtifactStreamService, DataProvider {
  private static final String ARTIFACT_STREAM_CRON_GROUP = "ARTIFACT_STREAM_CRON_GROUP";
  private static final String CRON_PREFIX = "0 "; // 'Second' unit prefix to convert unix to quartz cron expression
  private static final int ARTIFACT_STREAM_POLL_INTERVAL = 60; // in secs
  @Inject private WingsPersistence wingsPersistence;
  @Inject private ExecutorService executorService;
  @Inject @Named("JobScheduler") private QuartzScheduler jobScheduler;
  @Inject private PipelineService pipelineService;
  @Inject private WorkflowService workflowService;
  @Inject private WorkflowExecutionService workflowExecutionService;
  @Inject private ArtifactService artifactService;
  @Inject private EnvironmentService environmentService;
  @Inject private StencilPostProcessor stencilPostProcessor;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private BuildSourceService buildSourceService;
  @Inject private EntityUpdateService entityUpdateService;
  @Inject private YamlDirectoryService yamlDirectoryService;
  @Inject private AppService appService;
  @Inject private TriggerService triggerService;
  @Inject private YamlChangeSetService yamlChangeSetService;

  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Override
  public PageResponse<ArtifactStream> list(PageRequest<ArtifactStream> req) {
    PageResponse<ArtifactStream> pageResponse = wingsPersistence.query(ArtifactStream.class, req);
    return pageResponse;
  }

  @Override
  public ArtifactStream get(String appId, String artifactStreamId) {
    return wingsPersistence.get(ArtifactStream.class, appId, artifactStreamId);
  }

  @Override
  public ArtifactStream getArtifactStreamByName(String appId, String serviceId, String artifactStreamName) {
    return wingsPersistence.createQuery(ArtifactStream.class)
        .field("appId")
        .equal(appId)
        .field("serviceId")
        .equal(serviceId)
        .field("sourceName")
        .equal(artifactStreamName)
        .get();
  }

  @Override
  @ValidationGroups(Create.class)
  public ArtifactStream create(ArtifactStream artifactStream) {
    if (DOCKER.name().equals(artifactStream.getArtifactStreamType())
        || ECR.name().equals(artifactStream.getArtifactStreamType())
        || GCR.name().equals(artifactStream.getArtifactStreamType())
        || ARTIFACTORY.name().equals(artifactStream.getArtifactStreamType())) {
      buildSourceService.validateArtifactSource(
          artifactStream.getAppId(), artifactStream.getSettingId(), artifactStream.getArtifactStreamAttributes());
    }

    artifactStream.setSourceName(artifactStream.generateSourceName());
    if (artifactStream.isAutoPopulate()) {
      setAutoPopulatedName(artifactStream);
    }

    String id = wingsPersistence.save(artifactStream);
    addCronForAutoArtifactCollection(artifactStream);

    executorService.submit(() -> {
      String accountId = appService.getAccountIdByAppId(artifactStream.getAppId());
      YamlGitConfig ygs = yamlDirectoryService.weNeedToPushChanges(accountId);
      if (ygs != null) {
        List<GitFileChange> changeSet = new ArrayList<>();

        // add GitSyncFiles for trigger (artifact stream)
        changeSet.add(entityUpdateService.getArtifactStreamGitSyncFile(accountId, artifactStream, ChangeType.MODIFY));

        yamlChangeSetService.queueChangeSet(ygs, changeSet);
      }
    });

    return get(artifactStream.getAppId(), id);
  }

  /**
   * This method gets the default name, checks if another entry exists with the same name, if exists, it parses and
   * extracts the revision and creates a name with the next revision.
   *
   * @param artifactStream
   */
  private void setAutoPopulatedName(ArtifactStream artifactStream) {
    String name = artifactStream.generateName();

    String escapedString = Pattern.quote(name);

    // We need to check if the name exists in case of auto generate, if it exists, we need to add a suffix to the name.
    PageRequest<ArtifactStream> pageRequest = PageRequest.Builder.aPageRequest()
                                                  .addFilter("appId", Operator.EQ, artifactStream.getAppId())
                                                  .addFilter("serviceId", Operator.EQ, artifactStream.getServiceId())
                                                  .addFilter("name", Operator.STARTS_WITH, escapedString)
                                                  .addOrder("name", OrderType.DESC)
                                                  .build();
    PageResponse<ArtifactStream> response = wingsPersistence.query(ArtifactStream.class, pageRequest);

    // If an entry exists with the given default name
    if (response != null && response.size() > 0) {
      String existingName = response.get(0).getName();
      name = Util.getNameWithNextRevision(existingName, name);
    }

    artifactStream.setName(name);
  }

  private void addCronForAutoArtifactCollection(ArtifactStream artifactStream) {
    JobDetail job = JobBuilder.newJob(ArtifactCollectionJob.class)
                        .withIdentity(artifactStream.getUuid(), ARTIFACT_STREAM_CRON_GROUP)
                        .usingJobData("artifactStreamId", artifactStream.getUuid())
                        .usingJobData("appId", artifactStream.getAppId())
                        .build();

    Trigger trigger = TriggerBuilder.newTrigger()
                          .withIdentity(artifactStream.getUuid(), ARTIFACT_STREAM_CRON_GROUP)
                          .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                                            .withIntervalInSeconds(ARTIFACT_STREAM_POLL_INTERVAL)
                                            .repeatForever()
                                            .withMisfireHandlingInstructionNowWithExistingCount())
                          .build();

    jobScheduler.scheduleJob(job, trigger);
  }

  @Override
  @ValidationGroups(Update.class)
  public ArtifactStream update(ArtifactStream artifactStream) {
    ArtifactStream savedArtifactStream =
        wingsPersistence.get(ArtifactStream.class, artifactStream.getAppId(), artifactStream.getUuid());
    if (savedArtifactStream == null) {
      throw new NotFoundException("Artifact stream with id " + artifactStream.getUuid() + " not found");
    }
    if (DOCKER.name().equals(artifactStream.getArtifactStreamType())
        || ECR.name().equals(artifactStream.getArtifactStreamType())
        || GCR.name().equals(artifactStream.getArtifactStreamType())
        || ARTIFACTORY.name().equals(artifactStream.getArtifactStreamType())) {
      buildSourceService.validateArtifactSource(
          artifactStream.getAppId(), artifactStream.getSettingId(), artifactStream.getArtifactStreamAttributes());
    }

    artifactStream.setSourceName(artifactStream.generateSourceName());
    if (artifactStream.isAutoPopulate()) {
      setAutoPopulatedName(artifactStream);
    }

    artifactStream = wingsPersistence.saveAndGet(ArtifactStream.class, artifactStream);
    if (!artifactStream.isAutoDownload()) {
      jobScheduler.deleteJob(savedArtifactStream.getUuid(), ARTIFACT_STREAM_CRON_GROUP);
    }

    if (savedArtifactStream.getSourceName().equals(artifactStream.getSourceName())) {
      executorService.submit(() -> triggerService.updateByApp(savedArtifactStream.getAppId()));
    }

    ArtifactStream finalArtifactStream = artifactStream;
    executorService.submit(() -> {
      String accountId = appService.getAccountIdByAppId(finalArtifactStream.getAppId());
      YamlGitConfig ygs = yamlDirectoryService.weNeedToPushChanges(accountId);
      if (ygs != null) {
        List<GitFileChange> changeSet = new ArrayList<>();

        changeSet.add(
            entityUpdateService.getArtifactStreamGitSyncFile(accountId, finalArtifactStream, ChangeType.MODIFY));

        yamlChangeSetService.queueChangeSet(ygs, changeSet);
      }
    });

    return artifactStream;
  }

  @Override
  public boolean delete(String appId, String artifactStreamId) {
    return delete(appId, artifactStreamId, false);
  }

  private boolean delete(String appId, String artifactStreamId, boolean forceDelete) {
    ArtifactStream artifactStream = get(appId, artifactStreamId);
    if (artifactStream == null) {
      return true;
    }
    if (!forceDelete) {
      List<software.wings.beans.trigger.Trigger> triggers =
          triggerService.getTriggersHasArtifactStreamAction(appId, artifactStreamId);
      if (!CollectionUtils.isEmpty(triggers)) {
        List<String> triggerNames =
            triggers.stream().map(software.wings.beans.trigger.Trigger::getName).collect(Collectors.toList());
        throw new WingsException(INVALID_REQUEST, "message",
            String.format(
                "Artifact Source associated as a trigger action to triggers %", Joiner.on(", ").join(triggerNames)));
      }
    }
    boolean deleted = wingsPersistence.delete(wingsPersistence.createQuery(ArtifactStream.class)
                                                  .field(ID_KEY)
                                                  .equal(artifactStreamId)
                                                  .field("appId")
                                                  .equal(appId));
    if (deleted) {
      jobScheduler.deleteJob(artifactStream.getUuid(), ARTIFACT_STREAM_CRON_GROUP);
      artifactStream.getStreamActions().forEach(
          streamAction -> jobScheduler.deleteJob(streamAction.getWorkflowId(), artifactStreamId));
      triggerService.deleteTriggersForArtifactStream(appId, artifactStreamId);

      executorService.submit(() -> {
        String accountId = appService.getAccountIdByAppId(artifactStream.getAppId());
        YamlGitConfig ygs = yamlDirectoryService.weNeedToPushChanges(accountId);
        if (ygs != null) {
          List<GitFileChange> changeSet = new ArrayList<>();

          changeSet.add(entityUpdateService.getArtifactStreamGitSyncFile(accountId, artifactStream, ChangeType.DELETE));

          yamlChangeSetService.queueChangeSet(ygs, changeSet);
        }
      });
    }
    return deleted;
  }

  @Override
  public void deleteByApplication(String appId) {
    wingsPersistence.createQuery(ArtifactStream.class)
        .field("appId")
        .equal(appId)
        .asList()
        .forEach(artifactSource -> delete(appId, artifactSource.getUuid(), true));
  }

  @Override
  public ArtifactStream addStreamAction(String appId, String streamId, ArtifactStreamAction artifactStreamAction) {
    if (artifactStreamAction.getUuid() == null || artifactStreamAction.getUuid().isEmpty()) {
      artifactStreamAction.setUuid(UUIDGenerator.getUuid());
    }

    if (artifactStreamAction.isWebHook()) {
      if (artifactStreamAction.getWebHookToken() == null || artifactStreamAction.getWebHookToken().isEmpty()) {
        throw new WingsException(ErrorCode.INVALID_REQUEST);
      }
    }

    String cronExpression = CRON_PREFIX + artifactStreamAction.getCronExpression();

    if (artifactStreamAction.isCustomAction() && !isNullOrEmpty(cronExpression)) {
      validateCronExpression(cronExpression);
      artifactStreamAction.setCronDescription(getCronDescription(cronExpression));
    }

    ArtifactStream artifactStream = get(appId, streamId);
    if (artifactStreamAction.getWorkflowType().equals(ORCHESTRATION)) {
      Workflow workflow = workflowService.readWorkflow(appId, artifactStreamAction.getWorkflowId(), null);
      Environment environment = environmentService.get(appId, artifactStreamAction.getEnvId(), false);

      if (!environment.getUuid().equals(workflow.getEnvId())) {
        throw new WingsException(ErrorCode.INVALID_REQUEST, "message",
            String.format("Workflow [%s] can not be added to Env [%s]", workflow.getName(), environment.getName()));
      }
      if (workflow.getServices().stream().noneMatch(
              service -> artifactStream.getServiceId().equals(service.getUuid()))) {
        throw new WingsException(ErrorCode.INVALID_REQUEST, "message",
            String.format("Service in workflow [%s] and Artifact Source [%s] do not match", workflow.getName(),
                artifactStream.getSourceName()));
      }

      artifactStreamAction.setWorkflowName(workflow.getName());
      if (artifactStreamAction.isWebHook() && artifactStreamAction.getRequestBody() != null
          && workflow.getOrchestrationWorkflow() != null
          && workflow.getOrchestrationWorkflow().getUserVariables() != null
          && !workflow.getOrchestrationWorkflow().getUserVariables().isEmpty()) {
        Map<String, String> parameters = new HashMap<>();
        workflow.getOrchestrationWorkflow().getUserVariables().forEach(
            uservariable -> { parameters.put(uservariable.getName(), uservariable.getName() + "_placeholder"); });
        WebHookRequest webHookRequest = JsonUtils.asObject(artifactStreamAction.getRequestBody(), WebHookRequest.class);
        webHookRequest.setParameters(parameters);
        artifactStreamAction.setRequestBody(JsonUtils.asJson(webHookRequest));
      }

      artifactStreamAction.setEnvName(environment.getName());

    } else {
      Pipeline pipeline = pipelineService.readPipeline(appId, artifactStreamAction.getWorkflowId(), true);
      if (pipeline.getServices().stream().noneMatch(
              service -> artifactStream.getServiceId().equals(service.getUuid()))) {
        throw new WingsException(ErrorCode.INVALID_REQUEST, "message",
            String.format("Service in pipeline [%s] and Artifact Source [%s] do not match", pipeline.getName(),
                artifactStream.getSourceName()));
      }
      artifactStreamAction.setWorkflowName(pipeline.getName());
    }

    Query<ArtifactStream> query = wingsPersistence.createQuery(ArtifactStream.class)
                                      .field("appId")
                                      .equal(appId)
                                      .field(ID_KEY)
                                      .equal(streamId)
                                      .field("streamActions.uuid")
                                      .notEqual(artifactStreamAction.getUuid());
    UpdateOperations<ArtifactStream> operations =
        wingsPersistence.createUpdateOperations(ArtifactStream.class).add("streamActions", artifactStreamAction);
    UpdateResults update = wingsPersistence.update(query, operations);

    if (artifactStreamAction.isCustomAction()) {
      addCronForScheduledJobExecution(appId, streamId, artifactStreamAction, cronExpression);
    }
    return get(appId, streamId);
  }

  private void validateCronExpression(String cronExpression) {
    try {
      CronScheduleBuilder.cronSchedule(cronExpression);
    } catch (Exception ex) {
      throw new WingsException(ErrorCode.INVALID_ARGUMENT, "args", "Invalid cron expression");
    }
  }

  private String getCronDescription(String cronExpression) {
    try {
      String description = CronExpressionDescriptor.getDescription(
          DescriptionTypeEnum.FULL, cronExpression, new Options(), I18nMessages.DEFAULT_LOCALE);
      return StringUtils.lowerCase("" + description.charAt(0)) + description.substring(1);
    } catch (ParseException e) {
      logger.error("Error parsing cron expression: " + cronExpression, e);
      return cronExpression;
    }
  }

  private void addCronForScheduledJobExecution(
      String appId, String streamId, ArtifactStreamAction artifactStreamAction, String cronExpression) {
    JobDetail job = JobBuilder.newJob(ArtifactStreamActionJob.class)
                        .withIdentity(artifactStreamAction.getUuid(), streamId)
                        .usingJobData("artifactStreamId", streamId)
                        .usingJobData("appId", appId)
                        .usingJobData("workflowId", artifactStreamAction.getWorkflowId())
                        .usingJobData("actionId", artifactStreamAction.getUuid())
                        .build();

    Trigger trigger = TriggerBuilder.newTrigger()
                          .withIdentity(artifactStreamAction.getUuid(), streamId)
                          .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression))
                          .build();

    jobScheduler.scheduleJob(job, trigger);
  }

  @Override public ArtifactStream deleteStreamAction(String appId, String streamId, String actionId) { // TODO::simplify
    Query<ArtifactStream> query = wingsPersistence.createQuery(ArtifactStream.class)
                                      .field("appId")
                                      .equal(appId)
                                      .field(ID_KEY)
                                      .equal(streamId)
                                      .field("streamActions.uuid")
                                      .equal(actionId);

    ArtifactStream artifactStream = query.get();
    Validator.notNullCheck("Artifact Stream", artifactStream);

    ArtifactStreamAction streamAction =
        artifactStream.getStreamActions()
            .stream()
            .filter(artifactStreamAction -> actionId.equals(artifactStreamAction.getUuid()))
            .findFirst()
            .orElseGet(null);
    Validator.notNullCheck("Stream Action", streamAction);

    UpdateOperations<ArtifactStream> operations = wingsPersistence.createUpdateOperations(ArtifactStream.class)
                                                      .removeAll("streamActions", ImmutableMap.of("uuid", actionId));

    UpdateResults update = wingsPersistence.update(query, operations);

    if (update.getUpdatedCount() == 1) {
      jobScheduler.deleteJob(actionId, streamId);
    } else {
      logger.warn("Could not delete Artifact Stream trigger. streamId: [{}], actionId: [{}]", streamId, actionId);
    }

    return get(appId, streamId);
  }

  @Override
  public void deleteStreamActionForWorkflow(String appId, String workflowId) {
    List<ArtifactStream> artifactStreams = wingsPersistence.createQuery(ArtifactStream.class)
                                               .field("appId")
                                               .equal(appId)
                                               .field("streamActions.workflowId")
                                               .equal(workflowId)
                                               .asList();
    artifactStreams.forEach(artifactStream
        -> artifactStream.getStreamActions()
               .stream()
               .filter(artifactStreamAction -> artifactStreamAction.getWorkflowId().equals(workflowId))
               .forEach(artifactStreamAction -> {
                 deleteStreamAction(appId, artifactStream.getUuid(), artifactStreamAction.getUuid());
               }));
  }

  @Override
  public WebHookToken generateWebHookToken(String appId, String streamId) {
    ArtifactStream artifactStream = get(appId, streamId);
    Validator.notNullCheck("Artifact Source", artifactStream);
    Service service = serviceResourceService.get(appId, artifactStream.getServiceId());
    Validator.notNullCheck("Service", service);

    WebHookToken webHookToken =
        WebHookToken.builder().httpMethod("POST").webHookToken(CryptoUtil.secureRandAlphaNumString(40)).build();

    Map<String, String> payload = new HashMap<>();
    payload.put("application", appId);
    payload.put("artifactSource", streamId);

    if (service.getArtifactType().equals(ArtifactType.DOCKER)) {
      payload.put("dockerImageTag", "dockerImageTag_placeholder");
    } else {
      payload.put("buildNumber", "buildNumber_placeholder");
    }

    webHookToken.setPayload(new Gson().toJson(payload));
    return webHookToken;
  }

  @Override
  public List<ArtifactStream> getArtifactStreamsForService(String appId, String serviceId) {
    PageRequest pageRequest = Builder.aPageRequest()
                                  .addFilter("appId", Operator.EQ, appId)
                                  .addFilter("serviceId", Operator.EQ, serviceId)
                                  .addOrder("createdAt", OrderType.ASC)
                                  .build();
    PageResponse pageResponse = wingsPersistence.query(ArtifactStream.class, pageRequest);
    return pageResponse.getResponse();
  }

  @Override
  public ArtifactStream updateStreamAction(String appId, String streamId, ArtifactStreamAction artifactStreamAction) {
    ArtifactStream artifactStream = get(appId, streamId);
    if (artifactStream != null) {
      ArtifactStreamAction existingStreamAction =
          artifactStream.getStreamActions()
              .stream()
              .filter(streamAction -> streamAction.getUuid().equals(artifactStreamAction.getUuid()))
              .findFirst()
              .orElse(null);

      deleteStreamAction(appId, streamId, artifactStreamAction.getUuid());
      if (existingStreamAction.getWebHookToken() != null) {
        artifactStreamAction.setWebHookToken(existingStreamAction.getWebHookToken());
      }
      return addStreamAction(appId, streamId, artifactStreamAction);
    }
    return null;
  }

  @Override
  public void triggerStreamActionPostArtifactCollectionAsync(Artifact artifact) {
    executorService.execute(() -> triggerStreamActionPostArtifactCollection(artifact));
  }

  @Override
  public void triggerScheduledStreamAction(String appId, String streamId, String actionId) {
    logger.info("Triggering scheduled action for app Id {} streamId {} and action id {} ", appId, streamId, actionId);
    ArtifactStream artifactStream = get(appId, streamId);
    if (artifactStream == null) {
      logger.info("Artifact stream does not exist. Hence deleting associated job");
      jobScheduler.deleteJob(actionId, streamId);
      return;
    }
    ArtifactStreamAction artifactStreamAction =
        artifactStream.getStreamActions()
            .stream()
            .filter(asa -> asa.isCustomAction() && asa.getUuid().equals(actionId))
            .findFirst()
            .orElse(null);
    if (artifactStreamAction == null) {
      logger.info("Artifact stream does not have trigger anymore. Deleting associated job");
      jobScheduler.deleteJob(actionId, streamId);
      return;
    }
    Artifact latestArtifact =
        artifactService.fetchLatestArtifactForArtifactStream(appId, streamId, artifactStream.getSourceName());
    String latestArtifactBuildNo =
        (latestArtifact != null && latestArtifact.getMetadata().get(Constants.BUILD_NO) != null)
        ? latestArtifact.getMetadata().get(Constants.BUILD_NO)
        : "";

    Artifact lastSuccessfullyDeployedArtifact = getLastSuccessfullyDeployedArtifact(appId, artifactStreamAction);
    String lastDeployedArtifactBuildNo =
        (lastSuccessfullyDeployedArtifact != null
            && lastSuccessfullyDeployedArtifact.getMetadata().get(Constants.BUILD_NO) != null)
        ? lastSuccessfullyDeployedArtifact.getMetadata().get(Constants.BUILD_NO)
        : "";
    if (latestArtifactBuildNo.compareTo(lastDeployedArtifactBuildNo) > 0) {
      logger.info("latest collected artifact build#{}, last successfully deployed artifact build#{}",
          latestArtifactBuildNo, lastDeployedArtifactBuildNo);
      logger.info("Trigger stream action with artifact build# {}", latestArtifactBuildNo);
      triggerStreamAction(appId, latestArtifact, artifactStreamAction);
    } else {
      logger.info(
          "latest collected artifact build#{}, last successfully deployed artifact build#{} are the same or less. So, not triggering deployment",
          latestArtifactBuildNo, lastDeployedArtifactBuildNo);
    }
  }

  @Override
  public List<Stencil> getArtifactStreamSchema(String appId, String serviceId) {
    return stencilPostProcessor.postProcess(Arrays.asList(ArtifactStreamType.values()), appId, serviceId);
  }

  private Artifact getLastSuccessfullyDeployedArtifact(String appId, ArtifactStreamAction artifactStreamAction) {
    PageRequest pageRequest = aPageRequest()
                                  .addFilter("appId", EQ, appId)
                                  .addFilter("status", EQ, ExecutionStatus.SUCCESS)
                                  .addOrder("createdAt", OrderType.DESC)
                                  .withLimit("1")
                                  .build();

    String artifactId = null;

    if (artifactStreamAction.getWorkflowType().equals(PIPELINE)) {
      pageRequest.addFilter("workflowId", artifactStreamAction.getWorkflowId(), EQ);
      List<WorkflowExecution> response = workflowExecutionService.listExecutions(pageRequest, false).getResponse();
      if (response.size() == 1 && response.get(0).getExecutionArgs() != null
          && response.get(0).getExecutionArgs().getArtifacts() != null
          && !response.get(0).getExecutionArgs().getArtifacts().isEmpty()
          && response.get(0).getExecutionArgs().getArtifacts().get(0) != null) {
        artifactId = response.get(0).getExecutionArgs().getArtifacts().get(0).getUuid();
      }
    } else {
      pageRequest.addFilter("workflowId", artifactStreamAction.getWorkflowId(), EQ);
      pageRequest.addFilter("envId", artifactStreamAction.getEnvId(), EQ);
      List<WorkflowExecution> response = workflowExecutionService.listExecutions(pageRequest, false).getResponse();
      if (response.size() == 1) {
        artifactId = response.get(0).getExecutionArgs().getArtifactIdNames().keySet().stream().findFirst().orElse(null);
      }
    }

    return artifactId != null ? artifactService.get(appId, artifactId) : null;
  }

  private void triggerStreamActionPostArtifactCollection(Artifact artifact) {
    ArtifactStream artifactStream = get(artifact.getAppId(), artifact.getArtifactStreamId());
    Validator.notNullCheck("Artifact Stream", artifactStream);
    artifactStream.getStreamActions()
        .stream()
        .filter(streamAction -> !streamAction.isCustomAction())
        .forEach(artifactStreamAction -> {
          logger.info("Triggering Post Artifact Collection action for app Id {} and stream Id {}", artifact.getAppId(),
              artifact.getArtifactStreamId());
          triggerStreamAction(artifact.getAppId(), artifact, artifactStreamAction);
          logger.info("Post Artifact Collection action triggered");
        });
  }

  @Override
  public WorkflowExecution triggerStreamAction(
      String appId, Artifact artifact, ArtifactStreamAction artifactStreamAction) {
    return triggerStreamAction(appId, artifact, artifactStreamAction, null);
  }

  @Override
  public WorkflowExecution triggerStreamAction(
      String appId, Artifact artifact, ArtifactStreamAction artifactStreamAction, Map<String, String> parameters) {
    WorkflowExecution workflowExecution = null;
    if (!artifactFilterMatches(artifact, artifactStreamAction)) {
      logger.warn("Skipping execution - no matching artifact: {}", artifact);
      return workflowExecution;
    }

    ExecutionArgs executionArgs = new ExecutionArgs();
    if (artifact != null) {
      executionArgs.setArtifacts(asList(artifact));
    }
    executionArgs.setOrchestrationId(artifactStreamAction.getWorkflowId());
    executionArgs.setWorkflowType(artifactStreamAction.getWorkflowType());
    executionArgs.setExecutionCredential(aSSHExecutionCredential().withExecutionType(SSH).build());
    if (parameters != null) {
      executionArgs.setWorkflowVariables(parameters);
    }
    if (artifactStreamAction.getWorkflowType().equals(ORCHESTRATION)) {
      logger.info("Triggering Workflow execution of appId {}  with workflow id {}", appId,
          artifactStreamAction.getWorkflowId());
      executionArgs.setWorkflowType(ORCHESTRATION);
      workflowExecution =
          workflowExecutionService.triggerEnvExecution(appId, artifactStreamAction.getEnvId(), executionArgs);
      logger.info(
          "Workflow execution of appId {} with workflow id {} triggered", appId, artifactStreamAction.getWorkflowId());
    } else {
      logger.info("Triggering Pipeline execution of appId {} with stream pipeline id {}", appId,
          artifactStreamAction.getWorkflowId());
      executionArgs.setWorkflowType(PIPELINE);
      workflowExecution =
          workflowExecutionService.triggerPipelineExecution(appId, artifactStreamAction.getWorkflowId(), executionArgs);
      logger.info("Pipeline execution of appId {} of  {} type with stream pipeline id {} triggered", appId,
          artifactStreamAction.getWorkflowId());
    }
    return workflowExecution;
  }

  private boolean artifactFilterMatches(Artifact artifact, ArtifactStreamAction artifactStreamAction) {
    if (StringUtils.isEmpty(artifactStreamAction.getArtifactFilter())) {
      return true;
    } else {
      logger.info("Artifact filter {} set for artifact stream action {}", artifactStreamAction.getArtifactFilter(),
          artifactStreamAction);
      Pattern pattern = Pattern.compile(
          artifactStreamAction.getArtifactFilter().replace(".", "\\.").replace("?", ".?").replace("*", ".*?"));
      if (CollectionUtils.isEmpty(artifact.getArtifactFiles())) {
        if (pattern.matcher(artifact.getBuildNo()).find()) {
          logger.info("Artifact filter {} matching with artifact name/ tag / buildNo {}",
              artifactStreamAction.getArtifactFilter(), artifact.getBuildNo());
          return true;
        }
      } else {
        logger.info("Comparing artifact file name matches with the given artifact filter");
        List<ArtifactFile> artifactFiles = artifact.getArtifactFiles()
                                               .stream()
                                               .filter(artifactFile -> pattern.matcher(artifactFile.getName()).find())
                                               .collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(artifactFiles)) {
          logger.info("Artifact file names matches with the given artifact filter");
          artifact.setArtifactFiles(artifactFiles);
          return true;
        } else {
          logger.info(
              "Artifact does not have artifact files matching with the given artifact filter. So, not triggering either workflow or pipeline");
        }
      }
    }
    return false;
  }

  @Override
  public Map<String, String> getSupportedBuildSourceTypes(String appId, String serviceId) {
    Service service = serviceResourceService.get(appId, serviceId);
    // Observed NPE in logs due to invalid service id provided by the ui due to a stale screen.
    if (service == null) {
      throw new WingsException("Service " + serviceId + "for the given app " + appId + "doesnt exist ");
    }
    if (service.getArtifactType().equals(ArtifactType.DOCKER)) {
      return ImmutableMap.of(DOCKER.name(), DOCKER.name(), ECR.name(), ECR.name(), GCR.name(), GCR.name(),
          ARTIFACTORY.name(), ARTIFACTORY.name(), NEXUS.name(), NEXUS.name());
    } else if (service.getArtifactType().equals(ArtifactType.AWS_LAMBDA)) {
      return ImmutableMap.of(AMAZON_S3.name(), AMAZON_S3.name());
    } else {
      return ImmutableMap.of(ArtifactStreamType.JENKINS.name(), ArtifactStreamType.JENKINS.name(),
          ArtifactStreamType.BAMBOO.name(), ArtifactStreamType.BAMBOO.name(), ArtifactStreamType.NEXUS.name(),
          ArtifactStreamType.NEXUS.name(), ArtifactStreamType.ARTIFACTORY.name(), ArtifactStreamType.ARTIFACTORY.name(),
          AMAZON_S3.name(), AMAZON_S3.name());
    }
  }

  @Override
  public void deleteByService(String appId, String serviceId) {
    wingsPersistence.createQuery(ArtifactStream.class)
        .field("appId")
        .equal(appId)
        .field("serviceId")
        .equal(serviceId)
        .asList()
        .forEach(artifactSource -> delete((String) appId, (String) artifactSource.getUuid()));
  }

  @Override
  public Map<String, String> getData(String appId, String... params) {
    return (Map<String, String>) list(aPageRequest().addFilter("appId", EQ, appId).build())
        .getResponse()
        .stream()
        .collect(Collectors.toMap(ArtifactStream::getUuid, ArtifactStream::getSourceName));
  }
}
