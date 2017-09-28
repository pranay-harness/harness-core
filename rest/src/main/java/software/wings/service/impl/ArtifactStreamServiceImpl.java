package software.wings.service.impl;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Arrays.asList;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.ExecutionCredential.ExecutionType.SSH;
import static software.wings.beans.SSHExecutionCredential.Builder.aSSHExecutionCredential;
import static software.wings.beans.WorkflowType.ORCHESTRATION;
import static software.wings.beans.WorkflowType.PIPELINE;
import static software.wings.beans.artifact.ArtifactStreamType.ARTIFACTORY;
import static software.wings.beans.artifact.ArtifactStreamType.DOCKER;
import static software.wings.beans.artifact.ArtifactStreamType.ECR;
import static software.wings.beans.artifact.ArtifactStreamType.GCR;
import static software.wings.dl.PageRequest.Builder.aPageRequest;

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
import software.wings.beans.PipelineExecution;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.Service;
import software.wings.beans.SortOrder.OrderType;
import software.wings.beans.WebHookToken;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactFile;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStreamAction;
import software.wings.beans.artifact.ArtifactStreamType;
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
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.BuildSourceService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;
import software.wings.service.intfc.yaml.EntityUpdateService;
import software.wings.sm.ExecutionStatus;
import software.wings.stencils.DataProvider;
import software.wings.stencils.Stencil;
import software.wings.stencils.StencilPostProcessor;
import software.wings.utils.ArtifactType;
import software.wings.utils.CryptoUtil;
import software.wings.utils.Validator;
import software.wings.utils.validation.Create;
import software.wings.utils.validation.Update;
import software.wings.yaml.gitSync.EntityUpdateEvent.SourceType;

import java.text.ParseException;
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
  @ValidationGroups(Create.class)
  public ArtifactStream create(ArtifactStream artifactStream) {
    if (DOCKER.name().equals(artifactStream.getArtifactStreamType())
        || ECR.name().equals(artifactStream.getArtifactStreamType())
        || GCR.name().equals(artifactStream.getArtifactStreamType())) {
      buildSourceService.validateArtifactSource(
          artifactStream.getAppId(), artifactStream.getSettingId(), artifactStream.getArtifactStreamAttributes());
    }
    String id = wingsPersistence.save(artifactStream);
    addCronForAutoArtifactCollection(artifactStream);

    return get(artifactStream.getAppId(), id);
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
                                            .repeatForever())
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
        || GCR.name().equals(artifactStream.getArtifactStreamType())) {
      buildSourceService.validateArtifactSource(
          artifactStream.getAppId(), artifactStream.getSettingId(), artifactStream.getArtifactStreamAttributes());
    }
    artifactStream = create(artifactStream);
    if (!artifactStream.isAutoDownload()) {
      jobScheduler.deleteJob(savedArtifactStream.getUuid(), ARTIFACT_STREAM_CRON_GROUP);
    }

    // see if we need to perform any Git Sync operations
    entityUpdateService.triggerUpdate(artifactStream, SourceType.ENTITY_UPDATE);

    return artifactStream;
  }

  @Override
  public boolean delete(String appId, String artifactStreamId) {
    ArtifactStream artifactStream = get(appId, artifactStreamId);
    boolean deleted = wingsPersistence.delete(wingsPersistence.createQuery(ArtifactStream.class)
                                                  .field(ID_KEY)
                                                  .equal(artifactStreamId)
                                                  .field("appId")
                                                  .equal(appId));
    if (deleted) {
      jobScheduler.deleteJob(artifactStream.getUuid(), ARTIFACT_STREAM_CRON_GROUP);
      artifactStream.getStreamActions().forEach(
          streamAction -> jobScheduler.deleteJob(streamAction.getWorkflowId(), artifactStreamId));
    }
    return deleted;
  }

  @Override
  public void deleteByApplication(String appId) {
    wingsPersistence.createQuery(ArtifactStream.class)
        .field("appId")
        .equal(appId)
        .asList()
        .forEach(artifactSource -> delete(appId, artifactSource.getUuid()));
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
      payload.put("dockerImageTag", "${DOCKER_IMAGE_TAG}");
    } else {
      payload.put("buildNumber", "${BUILD_NUMBER}");
    }

    webHookToken.setPayload(new Gson().toJson(payload));
    return webHookToken;
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
    Artifact latestArtifact = artifactService.fetchLatestArtifactForArtifactStream(appId, streamId);
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
      triggerStreamAction(latestArtifact, artifactStreamAction);
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
    PageRequest pageRequest = Builder.aPageRequest()
                                  .addFilter("appId", Operator.EQ, appId)
                                  .addFilter("status", Operator.EQ, ExecutionStatus.SUCCESS)
                                  .addOrder("createdAt", OrderType.DESC)
                                  .withLimit("1")
                                  .build();

    String artifactId = null;

    if (artifactStreamAction.getWorkflowType().equals(PIPELINE)) {
      pageRequest.addFilter("pipelineId", artifactStreamAction.getWorkflowId(), Operator.EQ);
      List<PipelineExecution> response = pipelineService.listPipelineExecutions(pageRequest).getResponse();
      if (response.size() == 1) {
        artifactId = response.get(0).getArtifactId();
      }
    } else {
      pageRequest.addFilter("workflowId", artifactStreamAction.getWorkflowId(), Operator.EQ);
      pageRequest.addFilter("envId", artifactStreamAction.getEnvId(), Operator.EQ);
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
          triggerStreamAction(artifact, artifactStreamAction);
          logger.info("Post Artifact Collection action triggered");
        });
  }

  @Override
  public WorkflowExecution triggerStreamAction(Artifact artifact, ArtifactStreamAction artifactStreamAction) {
    WorkflowExecution workflowExecution = null;
    if (artifactFilterMatches(artifact, artifactStreamAction)) {
      ExecutionArgs executionArgs = new ExecutionArgs();
      executionArgs.setArtifacts(asList(artifact));
      executionArgs.setOrchestrationId(artifactStreamAction.getWorkflowId());
      executionArgs.setWorkflowType(artifactStreamAction.getWorkflowType());
      executionArgs.setExecutionCredential(aSSHExecutionCredential().withExecutionType(SSH).build());
      if (artifactStreamAction.getWorkflowType().equals(ORCHESTRATION)) {
        logger.info("Triggering Workflow execution of appId {}  with workflow id {}", artifact.getAppId(),
            artifactStreamAction.getWorkflowId());
        workflowExecution = workflowExecutionService.triggerEnvExecution(
            artifact.getAppId(), artifactStreamAction.getEnvId(), executionArgs);
        logger.info("Workflow execution of appId {} with workflow id {} triggered", artifact.getAppId(),
            artifactStreamAction.getWorkflowId());
      } else {
        logger.info("Triggering Pipeline execution of appId {} with stream pipeline id {}", artifact.getAppId(),
            artifactStreamAction.getWorkflowId());
        workflowExecution =
            pipelineService.execute(artifact.getAppId(), artifactStreamAction.getWorkflowId(), executionArgs);
        logger.info("Pipeline execution of appId {} of  {} type with stream pipeline id {} triggered",
            artifact.getAppId(), artifactStreamAction.getWorkflowId());
      }
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
          ARTIFACTORY.name(), ARTIFACTORY.name());
    } else {
      return ImmutableMap.of(ArtifactStreamType.JENKINS.name(), ArtifactStreamType.JENKINS.name(),
          ArtifactStreamType.BAMBOO.name(), ArtifactStreamType.BAMBOO.name(), ArtifactStreamType.NEXUS.name(),
          ArtifactStreamType.NEXUS.name(), ArtifactStreamType.ARTIFACTORY.name(), ArtifactStreamType.ARTIFACTORY.name(),
          ArtifactStreamType.AMAZON_S3.name(), ArtifactStreamType.AMAZON_S3.name());
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
        .forEach(artifactSource -> delete(appId, artifactSource.getUuid()));
  }

  @Override
  public Map<String, String> getData(String appId, String... params) {
    return (Map<String, String>) list(aPageRequest().addFilter("appId", Operator.EQ, appId).build())
        .getResponse()
        .stream()
        .collect(Collectors.toMap(ArtifactStream::getUuid, ArtifactStream::getSourceName));
  }
}
