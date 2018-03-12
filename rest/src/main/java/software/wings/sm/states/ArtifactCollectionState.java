package software.wings.sm.states;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.common.Constants.BUILD_NO;
import static software.wings.common.Constants.DEFAULT_ARTIFACT_COLLECTION_STATE_TIMEOUT_MILLIS;
import static software.wings.common.Constants.URL;
import static software.wings.common.Constants.WAIT_RESUME_GROUP;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;
import static software.wings.sm.StateType.ARTIFACT_COLLECTION;
import static software.wings.utils.Validator.notNullCheck;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Transient;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.ArtifactCollectionExecutionData;
import software.wings.beans.BuildExecutionSummary;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.scheduler.ArtifactCollectionStateNotifyJob;
import software.wings.scheduler.QuartzScheduler;
import software.wings.service.impl.ArtifactSourceProvider;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.State;
import software.wings.stencils.EnumData;
import software.wings.waitnotify.NotifyResponseData;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by sgurubelli on 11/13/17.
 */
public class ArtifactCollectionState extends State {
  @Transient private static final Logger logger = LoggerFactory.getLogger(ArtifactCollectionState.class);

  private static final String LATEST = "LATEST";
  @EnumData(enumDataProvider = ArtifactSourceProvider.class)
  @Attributes(title = "Artifact Source")
  @NotEmpty
  private String artifactStreamId;
  @Attributes(title = "Build / Tag") private String buildNo;
  @Transient @Inject private ArtifactStreamService artifactStreamService;
  @Transient @Inject private ArtifactService artifactService;
  @Transient @Inject private WorkflowExecutionService workflowExecutionService;
  @javax.inject.Inject @Named("JobScheduler") private QuartzScheduler jobScheduler;

  public ArtifactCollectionState(String name) {
    super(name, ARTIFACT_COLLECTION.name());
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    ArtifactStream artifactStream = artifactStreamService.get(context.getAppId(), artifactStreamId);
    notNullCheck("ArtifactStream", artifactStream);
    ArtifactCollectionExecutionData artifactCollectionExecutionData =
        ArtifactCollectionExecutionData.builder().artifactSource(artifactStream.getSourceName()).build();
    return anExecutionResponse()
        .withAsync(true)
        .withCorrelationIds(asList(scheduleWaitNotify()))
        .withStateExecutionData(artifactCollectionExecutionData)
        .build();
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, NotifyResponseData> response) {
    ArtifactCollectionExecutionData artifactCollectionExecutionData =
        (ArtifactCollectionExecutionData) response.values().iterator().next();
    ArtifactStream artifactStream = artifactStreamService.get(context.getAppId(), artifactStreamId);
    notNullCheck("ArtifactStream", artifactStream);
    Artifact lastCollectedArtifact =
        getLastCollectedArtifact(context, artifactStream.getUuid(), artifactStream.getSourceName());
    if (lastCollectedArtifact == null || !lastCollectedArtifact.getStatus().isFinalStatus()) {
      return anExecutionResponse()
          .withAsync(true)
          .withCorrelationIds(asList(scheduleWaitNotify()))
          .withStateExecutionData(artifactCollectionExecutionData)
          .build();
    }
    artifactCollectionExecutionData.setArtifactSource(lastCollectedArtifact.getArtifactSourceName());
    artifactCollectionExecutionData.setRevision(lastCollectedArtifact.getRevision());
    artifactCollectionExecutionData.setBuildNo(lastCollectedArtifact.getBuildNo());
    artifactCollectionExecutionData.setMetadata(lastCollectedArtifact.getMetadata());
    artifactCollectionExecutionData.setArtifactId(lastCollectedArtifact.getUuid());

    addBuildExecutionSummary(context, artifactCollectionExecutionData, artifactStream);
    return anExecutionResponse()
        .withStateExecutionData(artifactCollectionExecutionData)
        .withExecutionStatus(ExecutionStatus.SUCCESS)
        .build();
  }

  private void addBuildExecutionSummary(ExecutionContext context,
      ArtifactCollectionExecutionData artifactCollectionExecutionData, ArtifactStream artifactStream) {
    Map<String, String> metadata = artifactCollectionExecutionData.getMetadata();
    metadata.remove(BUILD_NO);
    String buildUrl = metadata.get(URL);
    metadata.remove(URL);
    BuildExecutionSummary buildExecutionSummary =
        BuildExecutionSummary.builder()
            .artifactSource(artifactCollectionExecutionData.getArtifactSource())
            .revision(artifactCollectionExecutionData.getRevision() == null
                    ? "N/A"
                    : artifactCollectionExecutionData.getRevision())
            .metadata(metadata.isEmpty()
                    ? "N/A"
                    : artifactCollectionExecutionData.getArtifactSource() + " " + metadata.toString())
            .artifactStreamId(artifactStream.getUuid())
            .buildName(artifactCollectionExecutionData.getArtifactSource() + " ("
                + artifactCollectionExecutionData.getBuildNo() + ")")
            .buildUrl(buildUrl)
            .build();
    workflowExecutionService.refreshBuildExecutionSummary(
        artifactStream.getAppId(), context.getWorkflowExecutionId(), buildExecutionSummary);
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  private String scheduleWaitNotify() {
    String resumeId = generateUuid();
    long wakeupTs = System.currentTimeMillis() + (60 * 1000); // every minute
    JobDetail job = JobBuilder.newJob(ArtifactCollectionStateNotifyJob.class)
                        .withIdentity(resumeId, WAIT_RESUME_GROUP)
                        .usingJobData("correlationId", resumeId)
                        .usingJobData("artifactStreamId", artifactStreamId)
                        .build();
    Trigger trigger = TriggerBuilder.newTrigger()
                          .withIdentity(resumeId, WAIT_RESUME_GROUP)
                          .startAt(new Date(wakeupTs))
                          .forJob(job)
                          .build();
    jobScheduler.scheduleJob(job, trigger);

    return resumeId;
  }

  @Override
  public Map<String, String> validateFields() {
    Map<String, String> invalidFields = new HashMap<>();
    if (isBlank(artifactStreamId)) {
      invalidFields.put("artifactSource", "Artifact Source should not be empty");
    }
    return invalidFields;
  }

  @SchemaIgnore
  @Override
  public Integer getTimeoutMillis() {
    return DEFAULT_ARTIFACT_COLLECTION_STATE_TIMEOUT_MILLIS;
  }

  private Artifact getLastCollectedArtifact(ExecutionContext context, String artifactStreamId, String sourceName) {
    if (isBlank(buildNo) || buildNo.equalsIgnoreCase(LATEST)) {
      return artifactService.fetchLastCollectedArtifactForArtifactStream(
          context.getAppId(), artifactStreamId, sourceName);
    } else {
      return artifactService.getArtifactByBuildNumber(
          context.getAppId(), artifactStreamId, sourceName, context.renderExpression(buildNo));
    }
  }

  public String getArtifactStreamId() {
    return artifactStreamId;
  }

  public void setArtifactStreamId(String artifactStreamId) {
    this.artifactStreamId = artifactStreamId;
  }

  public String getBuildNo() {
    return buildNo;
  }

  public void setBuildNo(String buildNo) {
    this.buildNo = buildNo;
  }
}
