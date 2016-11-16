package software.wings.service.impl;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Arrays.asList;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.WorkflowType.ORCHESTRATION;
import static software.wings.beans.WorkflowType.PIPELINE;
import static software.wings.dl.PageRequest.Builder.aPageRequest;

import com.google.inject.Singleton;

import net.redhogs.cronparser.CronExpressionDescriptor;
import net.redhogs.cronparser.DescriptionTypeEnum;
import net.redhogs.cronparser.I18nMessages;
import net.redhogs.cronparser.Options;
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
import software.wings.beans.ExecutionArgs;
import software.wings.beans.PipelineExecution;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.SortOrder.OrderType;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStreamAction;
import software.wings.beans.artifact.JenkinsArtifactStream;
import software.wings.dl.PageRequest;
import software.wings.dl.PageRequest.Builder;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.scheduler.ArtifactCollectionJob;
import software.wings.scheduler.ArtifactStreamActionJob;
import software.wings.scheduler.JobScheduler;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.ExecutionStatus;
import software.wings.stencils.DataProvider;
import software.wings.utils.Validator;
import software.wings.utils.validation.Create;
import software.wings.utils.validation.Update;

import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
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
  public static final String ARTIFACT_STREAM_CRON_GROUP = "ARTIFACT_STREAM_CRON_GROUP";
  @Inject private WingsPersistence wingsPersistence;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private ExecutorService executorService;
  @Inject private JobScheduler jobScheduler;
  @Inject private PipelineService pipelineService;
  @Inject private WorkflowService workflowService;
  @Inject private WorkflowExecutionService workflowExecutionService;
  @Inject private ArtifactService artifactService;
  @Inject private EnvironmentService environmentService;

  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Override
  public PageResponse<ArtifactStream> list(PageRequest<ArtifactStream> req) {
    PageResponse<ArtifactStream> pageResponse = wingsPersistence.query(ArtifactStream.class, req);
    pageResponse.getResponse().forEach(this ::populateStreamSpecificData);
    return pageResponse;
  }

  private void populateStreamSpecificData(ArtifactStream artifactStream) {
    if (artifactStream instanceof JenkinsArtifactStream) {
      ((JenkinsArtifactStream) artifactStream)
          .getArtifactPathServices()
          .forEach(artifactPathServiceEntry
              -> artifactPathServiceEntry.setServices(
                  artifactPathServiceEntry.getServiceIds()
                      .stream()
                      .map(sid -> serviceResourceService.get(artifactStream.getAppId(), sid))
                      .collect(Collectors.toList())));
    }
  }

  @Override
  public ArtifactStream get(String appId, String artifactStreamId) {
    ArtifactStream artifactStream = wingsPersistence.get(ArtifactStream.class, appId, artifactStreamId);
    populateStreamSpecificData(artifactStream);
    return artifactStream;
  }

  @Override
  @ValidationGroups(Create.class)
  public ArtifactStream create(ArtifactStream artifactStream) {
    String id = wingsPersistence.save(artifactStream);
    if (artifactStream.isAutoDownload()) {
      addCronForAutoArtifactCollection(artifactStream);
    }
    return get(artifactStream.getAppId(), id);
  }

  private void addCronForAutoArtifactCollection(ArtifactStream artifactStream) {
    JobDetail job = JobBuilder.newJob(ArtifactCollectionJob.class)
                        .withIdentity(ARTIFACT_STREAM_CRON_GROUP, artifactStream.getUuid())
                        .usingJobData("artifactStreamId", artifactStream.getUuid())
                        .usingJobData("appId", artifactStream.getAppId())
                        .build();

    Trigger trigger =
        TriggerBuilder.newTrigger()
            .withIdentity(artifactStream.getUuid())
            .withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(60 * 5).repeatForever())
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
    artifactStream = create(artifactStream);
    if (!artifactStream.isAutoDownload()) {
      jobScheduler.deleteJob(ARTIFACT_STREAM_CRON_GROUP, savedArtifactStream.getUuid());
    }
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
      jobScheduler.deleteJob(ARTIFACT_STREAM_CRON_GROUP, artifactStream.getUuid());
      artifactStream.getStreamActions().stream().forEach(
          streamAction -> jobScheduler.deleteJob(artifactStreamId, streamAction.getWorkflowId()));
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
    if (artifactStreamAction.isCustomAction() && !isNullOrEmpty(artifactStreamAction.getCronExpression())) {
      artifactStreamAction.setCronExpression("0 " + artifactStreamAction.getCronExpression());
      artifactStreamAction.setCronDescription(getCronDescription(artifactStreamAction.getCronExpression()));
    }

    if (artifactStreamAction.getWorkflowType().equals(ORCHESTRATION)) {
      Workflow workflow = workflowService.readOrchestration(appId, artifactStreamAction.getWorkflowId(), null);
      artifactStreamAction.setWorkflowName(workflow.getName());
      Environment environment = environmentService.get(appId, artifactStreamAction.getEnvId(), false);
      artifactStreamAction.setEnvName(environment.getName());
    } else {
      Workflow workflow = workflowService.readPipeline(appId, artifactStreamAction.getWorkflowId());
      artifactStreamAction.setWorkflowName(workflow.getName());
    }

    Query<ArtifactStream> query = wingsPersistence.createQuery(ArtifactStream.class)
                                      .field("appId")
                                      .equal(appId)
                                      .field(ID_KEY)
                                      .equal(streamId)
                                      .field("streamActions.workflowId")
                                      .notEqual(artifactStreamAction.getWorkflowId());
    UpdateOperations<ArtifactStream> operations =
        wingsPersistence.createUpdateOperations(ArtifactStream.class).add("streamActions", artifactStreamAction);
    UpdateResults update = wingsPersistence.update(query, operations);

    if (artifactStreamAction.isCustomAction()) {
      addCronForScheduledJobExecution(appId, streamId, artifactStreamAction);
    }
    return get(appId, streamId);
  }

  private String getCronDescription(String cronExpression) {
    try {
      return CronExpressionDescriptor.getDescription(
          DescriptionTypeEnum.FULL, cronExpression, new Options(), I18nMessages.DEFAULT_LOCALE);
    } catch (ParseException e) {
      logger.error("Error in translating corn expression " + cronExpression);
      return cronExpression;
    }
  }

  private void addCronForScheduledJobExecution(
      String appId, String streamId, ArtifactStreamAction artifactStreamAction) {
    // Use ArtifactStream uuid as job group name and workflowId as job name

    JobDetail job = JobBuilder.newJob(ArtifactStreamActionJob.class)
                        .withIdentity(streamId, artifactStreamAction.getWorkflowId())
                        .usingJobData("artifactStreamId", streamId)
                        .usingJobData("appId", appId)
                        .usingJobData("workflowId", artifactStreamAction.getWorkflowId())
                        .build();

    Trigger trigger = TriggerBuilder.newTrigger()
                          .withIdentity(streamId, artifactStreamAction.getWorkflowId())
                          .withSchedule(CronScheduleBuilder.cronSchedule(artifactStreamAction.getCronExpression()))
                          .build();

    jobScheduler.scheduleJob(job, trigger);
  }

  @Override
  public ArtifactStream deleteStreamAction(String appId, String streamId, String workflowId) {
    Query<ArtifactStream> query = wingsPersistence.createQuery(ArtifactStream.class)
                                      .field("appId")
                                      .equal(appId)
                                      .field(ID_KEY)
                                      .equal(streamId)
                                      .field("streamActions.workflowId")
                                      .equal(workflowId);

    ArtifactStream artifactStream = query.get();
    Validator.notNullCheck("ArtifactStream", artifactStream);

    ArtifactStreamAction streamAction =
        artifactStream.getStreamActions()
            .stream()
            .filter(artifactStreamAction -> artifactStreamAction.getWorkflowId().equals(workflowId))
            .findFirst()
            .orElseGet(null);
    Validator.notNullCheck("StreamAction", streamAction);

    UpdateOperations<ArtifactStream> operations =
        wingsPersistence.createUpdateOperations(ArtifactStream.class).removeAll("streamActions", streamAction);

    UpdateResults update = wingsPersistence.update(query, operations);

    jobScheduler.deleteJob(streamId, workflowId);

    return get(appId, streamId);
  }

  @Override
  public ArtifactStream updateStreamAction(String appId, String streamId, ArtifactStreamAction artifactStreamAction) {
    deleteStreamAction(appId, streamId, artifactStreamAction.getWorkflowId());
    return addStreamAction(appId, streamId, artifactStreamAction);
  }

  @Override
  public void triggerStreamActionPostArtifactCollectionAsync(Artifact artifact) {
    executorService.execute(() -> triggerStreamActionPostArtifactCollection(artifact));
  }

  @Override
  public void triggerScheduledStreamAction(String appId, String streamId, String workflowId) {
    ArtifactStream artifactStream = get(appId, streamId);
    ArtifactStreamAction artifactStreamAction =
        artifactStream.getStreamActions()
            .stream()
            .filter(asa -> asa.isCustomAction() && asa.getWorkflowId().equals(workflowId))
            .findFirst()
            .orElse(null);

    Artifact latestArtifact = artifactService.fetchLatestArtifactForArtifactStream(appId, streamId);
    int latestArtifactBuildNo = (latestArtifact != null && latestArtifact.getMetadata().get("buildNo") != null)
        ? Integer.parseInt(latestArtifact.getMetadata().get("buildNo"))
        : 0;

    Artifact lastSuccessfullyDeployedArtifact = getLastSuccessfullyDeployedArtifact(appId, artifactStreamAction);
    int lastDeployedArtifactBuildNo = (lastSuccessfullyDeployedArtifact != null
                                          && lastSuccessfullyDeployedArtifact.getMetadata().get("buildNo") != null)
        ? Integer.parseInt(lastSuccessfullyDeployedArtifact.getMetadata().get("buildNo"))
        : 0;

    logger.info("latest collected artifact build#{}, last successfully deployed artifact build#{}",
        latestArtifactBuildNo, lastDeployedArtifactBuildNo);

    if (latestArtifactBuildNo > lastDeployedArtifactBuildNo) {
      logger.info("Trigger stream action with artifact build# {}", latestArtifactBuildNo);
      triggerStreamAction(latestArtifact, artifactStreamAction);
    }
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
        artifactId = response.get(0).getExecutionArgs().getArtifacts().get(0).getUuid();
      }
    }

    return artifactId != null ? artifactService.get(appId, artifactId) : null;
  }

  private void triggerStreamActionPostArtifactCollection(Artifact artifact) {
    ArtifactStream artifactStream = get(artifact.getAppId(), artifact.getArtifactStreamId());
    Validator.notNullCheck("ArtifactStream", artifactStream);
    artifactStream.getStreamActions()
        .stream()
        .filter(streamAction -> !streamAction.isCustomAction())
        .forEach(artifactStreamAction -> triggerStreamAction(artifact, artifactStreamAction));
  }

  private void triggerStreamAction(Artifact artifact, ArtifactStreamAction artifactStreamAction) {
    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setArtifacts(asList(artifact));
    executionArgs.setOrchestrationId(artifactStreamAction.getWorkflowId());
    logger.info("Execute workflow of {} type with id {}", artifactStreamAction.getWorkflowType(),
        artifactStreamAction.getWorkflowId());

    if (artifactStreamAction.getWorkflowType().equals(ORCHESTRATION)) {
      workflowExecutionService.triggerEnvExecution(artifact.getAppId(), artifactStreamAction.getEnvId(), executionArgs);
    } else {
      pipelineService.execute(artifact.getAppId(), artifactStreamAction.getWorkflowId(), executionArgs);
    }
  }

  @Override
  public Map<String, String> getData(String appId, String... params) {
    return (Map<String, String>) list(aPageRequest().addFilter("appId", Operator.EQ, appId).build())
        .getResponse()
        .stream()
        .collect(Collectors.toMap(ArtifactStream::getUuid, ArtifactStream::getSourceName));
  }
}
