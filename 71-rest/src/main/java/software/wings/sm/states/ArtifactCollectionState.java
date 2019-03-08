package software.wings.sm.states;

import static io.harness.beans.ExecutionStatus.SUCCESS;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.lang.String.valueOf;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.common.Constants.BUILD_NO;
import static software.wings.common.Constants.DEFAULT_ARTIFACT_COLLECTION_STATE_TIMEOUT_MILLIS;
import static software.wings.common.Constants.URL;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;
import static software.wings.sm.StateType.ARTIFACT_COLLECTION;
import static software.wings.utils.Validator.notNullCheck;

import com.google.inject.Inject;

import com.github.reinert.jjschema.Attributes;
import io.harness.beans.ExecutionStatus;
import io.harness.delegate.beans.ResponseData;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.NotEmpty;
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
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;
import software.wings.stencils.DefaultValue;
import software.wings.stencils.EnumData;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ArtifactCollectionState extends State {
  private static final Logger logger = LoggerFactory.getLogger(ArtifactCollectionState.class);

  @EnumData(enumDataProvider = ArtifactSourceProvider.class)
  @Attributes(title = "Artifact Source")
  @NotEmpty
  @Getter
  @Setter
  private String artifactStreamId;

  @Attributes(title = "Regex") @Getter @Setter private boolean regex;
  @Attributes(title = "Build / Tag") @Getter @Setter private String buildNo;

  @Inject private transient ArtifactStreamService artifactStreamService;
  @Inject private transient ArtifactService artifactService;
  @Inject private transient WorkflowExecutionService workflowExecutionService;
  @Inject private transient DelayEventHelper delayEventHelper;

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

    Artifact lastCollectedArtifact = fetchCollectedArtifact(artifactStream, evaluatedBuildNo);

    if (lastCollectedArtifact != null) {
      ArtifactCollectionExecutionData artifactCollectionExecutionData =
          ArtifactCollectionExecutionData.builder().artifactStreamId(artifactStreamId).build();
      if (getTimeoutMillis() != null) {
        artifactCollectionExecutionData.setTimeout(valueOf(getTimeoutMillis()));
      }
      artifactCollectionExecutionData.setArtifactSource(artifactStream.getSourceName());
      artifactCollectionExecutionData.setRevision(lastCollectedArtifact.getRevision());
      artifactCollectionExecutionData.setBuildNo(lastCollectedArtifact.getBuildNo());
      artifactCollectionExecutionData.setMetadata(lastCollectedArtifact.getMetadata());
      artifactCollectionExecutionData.setArtifactId(lastCollectedArtifact.getUuid());

      addBuildExecutionSummary(context, artifactCollectionExecutionData, artifactStream);
      return anExecutionResponse()
          .withExecutionStatus(ExecutionStatus.SUCCESS)
          .withStateExecutionData(artifactCollectionExecutionData)
          .withErrorMessage("Collected artifact [" + lastCollectedArtifact.getBuildNo() + "] for artifact source ["
              + lastCollectedArtifact.getArtifactSourceName() + "]")
          .build();
    }

    ArtifactCollectionExecutionData artifactCollectionExecutionData =
        ArtifactCollectionExecutionData.builder()
            .timeout(getTimeoutMillis() != null ? valueOf(getTimeoutMillis()) : null)
            .artifactSource(artifactStream.getSourceName())
            .buildNo(evaluatedBuildNo)
            .message(String.format("Waiting for [%s] to be collected from [%s] repository",
                evaluatedBuildNo == null ? "latest artifact" : evaluatedBuildNo,
                artifactStream.getArtifactStreamType()))
            .build();

    String resumeId = delayEventHelper.delay(60, Collections.emptyMap());

    return anExecutionResponse()
        .withAsync(true)
        .withCorrelationIds(singletonList(resumeId))
        .withStateExecutionData(artifactCollectionExecutionData)
        .build();
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, ResponseData> response) {
    ArtifactStream artifactStream = artifactStreamService.get(context.getAppId(), artifactStreamId);
    notNullCheck("ArtifactStream was deleted", artifactStream);

    String evaluatedBuildNo = getEvaluatedBuildNo(context);

    Artifact lastCollectedArtifact = fetchCollectedArtifact(artifactStream, evaluatedBuildNo);

    ArtifactCollectionExecutionData artifactCollectionExecutionData =
        ArtifactCollectionExecutionData.builder().artifactStreamId(artifactStreamId).build();

    if (getTimeoutMillis() != null) {
      artifactCollectionExecutionData.setTimeout(valueOf(getTimeoutMillis()));
    }
    artifactCollectionExecutionData.setArtifactSource(artifactStream.getSourceName());

    if (lastCollectedArtifact == null || !lastCollectedArtifact.getStatus().isFinalStatus()) {
      artifactCollectionExecutionData.setMessage(String.format("Waiting for [%s] to be collected from [%s] repository",
          evaluatedBuildNo == null ? "latest artifact" : evaluatedBuildNo, artifactStream.getArtifactStreamType()));
      artifactCollectionExecutionData.setBuildNo(evaluatedBuildNo);

      String resumeId = delayEventHelper.delay(DELAY_TIME_IN_SEC, Collections.emptyMap());

      return anExecutionResponse()
          .withAsync(true)
          .withCorrelationIds(asList(resumeId))
          .withStateExecutionData(artifactCollectionExecutionData)
          .build();
    }

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
    if (isBlank(buildNo)) {
      evaluatedBuildNo = buildNo;
    } else {
      evaluatedBuildNo = context.renderExpression(buildNo);
    }
    return evaluatedBuildNo;
  }

  private void addBuildExecutionSummary(ExecutionContext context,
      ArtifactCollectionExecutionData artifactCollectionExecutionData, ArtifactStream artifactStream) {
    Map<String, String> metadata = new HashMap<>();
    if (isNotEmpty(artifactCollectionExecutionData.getMetadata())) {
      metadata.putAll(artifactCollectionExecutionData.getMetadata());
    }
    String buildUrl = metadata.get(URL);
    // Rove the the following as no need to store in build execution summary
    metadata.remove(BUILD_NO);
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
    artifactCollectionExecutionData.setMessage(
        "Failed to collect artifact from Artifact Server. Please verify Build No/Tag ["
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

  private Artifact fetchCollectedArtifact(ArtifactStream artifactStream, String buildNo) {
    if (isBlank(buildNo)) {
      return artifactService.fetchLastCollectedApprovedArtifactForArtifactStream(artifactStream);
    } else {
      return artifactService.getArtifactByBuildNumber(artifactStream, buildNo, isRegex());
    }
  }
}
