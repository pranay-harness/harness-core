package software.wings.sm.states;

import static java.lang.String.valueOf;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.common.Constants.BUILD_NO;
import static software.wings.common.Constants.DEFAULT_ARTIFACT_COLLECTION_STATE_TIMEOUT_MILLIS;
import static software.wings.common.Constants.URL;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;
import static software.wings.sm.ExecutionStatus.SUCCESS;
import static software.wings.sm.StateType.ARTIFACT_COLLECTION;
import static software.wings.utils.Validator.notNullCheck;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import com.github.reinert.jjschema.Attributes;
import io.harness.delegate.task.protocol.ResponseData;
import io.harness.scheduler.PersistentScheduler;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.ArtifactCollectionExecutionData;
import software.wings.beans.BuildExecutionSummary;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.service.impl.ArtifactSourceProvider;
import software.wings.service.impl.DelayEventHelper;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.State;
import software.wings.stencils.DefaultValue;
import software.wings.stencils.EnumData;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by sgurubelli on 11/13/17.
 */
public class ArtifactCollectionState extends State {
  @Transient private static final Logger logger = LoggerFactory.getLogger(ArtifactCollectionState.class);

  public static final String ARTIFACT_STREAM_ID_KEY = "artifactStreamId";
  public static final String BUILD_NO_KEY = "buildNo";

  private static final String LATEST = "LATEST";
  @EnumData(enumDataProvider = ArtifactSourceProvider.class)
  @Attributes(title = "Artifact Source")
  @NotEmpty
  private String artifactStreamId;
  @Attributes(title = "Regex") private boolean regex;
  @Attributes(title = "Build / Tag") private String buildNo;
  @Transient @Inject private ArtifactStreamService artifactStreamService;
  @Transient @Inject private ArtifactService artifactService;
  @Transient @Inject private WorkflowExecutionService workflowExecutionService;
  @Inject @Named("JobScheduler") private transient PersistentScheduler jobScheduler;
  @Inject private transient DelayEventHelper delayEventHelper;
  @Inject private transient FeatureFlagService featureFlagService;

  private static int DELAY_TIME_IN_SEC = 60;

  public ArtifactCollectionState(String name) {
    super(name, ARTIFACT_COLLECTION.name());
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    ArtifactStream artifactStream = artifactStreamService.get(context.getAppId(), artifactStreamId);
    if (artifactStream == null) {
      logger.info("Artifact Stream {} might have been deleted", artifactStreamId);
      return anExecutionResponse()
          .withExecutionStatus(ExecutionStatus.FAILED)
          .withErrorMessage("Artifact source might have been deleted. Please update with the right artifact source.")
          .build();
    }

    String evaluatedBuildNo = getEvaluatedBuildNo(context);
    ArtifactCollectionExecutionData artifactCollectionExecutionData =
        ArtifactCollectionExecutionData.builder()
            .timeout(getTimeoutMillis() != null ? valueOf(getTimeoutMillis()) : null)
            .artifactSource(artifactStream.getSourceName())
            .buildNo(evaluatedBuildNo)
            .message("Waiting for [" + evaluatedBuildNo + "] to be collected from ["
                + artifactStream.getArtifactStreamType() + "] repository")
            .build();

    String resumeId = delayEventHelper.delay(60, Collections.emptyMap());

    return anExecutionResponse()
        .withAsync(true)
        .withCorrelationIds(asList(resumeId))
        .withStateExecutionData(artifactCollectionExecutionData)
        .build();
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, ResponseData> response) {
    ArtifactStream artifactStream = artifactStreamService.get(context.getAppId(), artifactStreamId);
    notNullCheck("ArtifactStream was deleted", artifactStream);

    String evaluatedBuildNo = getEvaluatedBuildNo(context);

    Artifact lastCollectedArtifact =
        getLastCollectedArtifact(context, artifactStream.getUuid(), artifactStream.getSourceName(), evaluatedBuildNo);

    ArtifactCollectionExecutionData artifactCollectionExecutionData =
        ArtifactCollectionExecutionData.builder().artifactStreamId(artifactStreamId).build();

    if (getTimeoutMillis() != null) {
      artifactCollectionExecutionData.setTimeout(valueOf(getTimeoutMillis()));
    }
    artifactCollectionExecutionData.setArtifactSource(artifactStream.getSourceName());

    if (lastCollectedArtifact == null || !lastCollectedArtifact.getStatus().isFinalStatus()) {
      String message = "Waiting for [" + evaluatedBuildNo + "] to be collected from ["
          + artifactStream.getArtifactStreamType() + "] repository";
      logger.info(message);

      artifactCollectionExecutionData.setMessage(message);
      artifactCollectionExecutionData.setBuildNo(evaluatedBuildNo);

      String resumeId = delayEventHelper.delay(DELAY_TIME_IN_SEC, Collections.emptyMap());

      return anExecutionResponse()
          .withAsync(true)
          .withCorrelationIds(asList(resumeId))
          .withStateExecutionData(artifactCollectionExecutionData)
          .build();
    }
    logger.info("Build/Tag {} collected of Artifact Source {}", evaluatedBuildNo, artifactStream.getSourceName());

    artifactCollectionExecutionData.setRevision(lastCollectedArtifact.getRevision());
    artifactCollectionExecutionData.setBuildNo(lastCollectedArtifact.getBuildNo());
    artifactCollectionExecutionData.setMetadata(lastCollectedArtifact.getMetadata());
    artifactCollectionExecutionData.setArtifactId(lastCollectedArtifact.getUuid());

    addBuildExecutionSummary(context, artifactCollectionExecutionData, artifactStream);
    return anExecutionResponse()
        .withStateExecutionData(artifactCollectionExecutionData)
        .withExecutionStatus(SUCCESS)
        .build();
  }

  private String getEvaluatedBuildNo(ExecutionContext context) {
    String evaluatedBuildNo;
    if (isBlank(buildNo) || buildNo.equalsIgnoreCase(LATEST)) {
      evaluatedBuildNo = LATEST;
    } else {
      evaluatedBuildNo = context.renderExpression(buildNo);
    }
    return evaluatedBuildNo;
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
  public void handleAbortEvent(ExecutionContext context) {
    if (context == null || context.getStateExecutionData() == null) {
      return;
    }
    logger.info("Action aborted either due to timeout or manual user abort");
    ArtifactCollectionExecutionData artifactCollectionExecutionData =
        (ArtifactCollectionExecutionData) context.getStateExecutionData();
    artifactCollectionExecutionData.setMessage("Failed to collect artifact from Artifact Server. Please verify tag ["
        + artifactCollectionExecutionData.getBuildNo() + "] exists");
  }

  @Override
  public Map<String, String> validateFields() {
    Map<String, String> invalidFields = new HashMap<>();
    if (isBlank(artifactStreamId)) {
      invalidFields.put("artifactSource", "Artifact Source should not be empty");
    }
    return invalidFields;
  }

  @Attributes(title = "Timeout (ms)")
  @DefaultValue("" + DEFAULT_ARTIFACT_COLLECTION_STATE_TIMEOUT_MILLIS)
  @Override
  public Integer getTimeoutMillis() {
    if (super.getTimeoutMillis() == null) {
      return Math.toIntExact(DEFAULT_ARTIFACT_COLLECTION_STATE_TIMEOUT_MILLIS);
    }
    return super.getTimeoutMillis();
  }

  private Artifact getLastCollectedArtifact(
      ExecutionContext context, String artifactStreamId, String sourceName, String buildNo) {
    if (isBlank(buildNo) || buildNo.equalsIgnoreCase(LATEST)) {
      return artifactService.fetchLastCollectedApprovedArtifactForArtifactStream(
          context.getAppId(), artifactStreamId, sourceName);
    } else {
      return artifactService.getArtifactByBuildNumber(context.getAppId(), artifactStreamId, sourceName, buildNo, regex);
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

  public boolean isRegex() {
    return regex;
  }

  public void setRegex(boolean regex) {
    this.regex = regex;
  }
}
