package software.wings.sm.states;

import static io.harness.beans.ExecutionStatus.SUCCESS;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.validation.Validator.notNullCheck;
import static java.lang.String.valueOf;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.sm.StateType.ARTIFACT_COLLECTION;

import com.google.inject.Inject;

import com.github.reinert.jjschema.Attributes;
import io.harness.beans.ExecutionStatus;
import io.harness.delegate.beans.ResponseData;
import io.harness.expression.ExpressionEvaluator;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.api.ArtifactCollectionExecutionData;
import software.wings.beans.BuildExecutionSummary;
import software.wings.beans.EntityType;
import software.wings.beans.FeatureName;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.Artifact.ArtifactMetadataKeys;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.service.impl.DelayEventHelper;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;
import software.wings.stencils.DefaultValue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class ArtifactCollectionState extends State {
  @Attributes(title = "Entity Type") @Getter @Setter private EntityType entityType;
  @Attributes(title = "Entity") @Getter @Setter private String entityId;
  @Attributes(title = "Service") @Getter @Setter private String serviceId;
  @Attributes(title = "Artifact Variable Name") @Getter @Setter private String artifactVariableName;

  @Attributes(title = "Artifact Source") @NotEmpty @Getter @Setter private String artifactStreamId;

  @Attributes(title = "Regex") @Getter @Setter private boolean regex;
  @Attributes(title = "Build / Tag") @Getter @Setter private String buildNo;

  @Inject private transient ArtifactStreamService artifactStreamService;
  @Inject private transient ArtifactService artifactService;
  @Inject private transient WorkflowExecutionService workflowExecutionService;
  @Inject private transient DelayEventHelper delayEventHelper;
  @Inject private transient FeatureFlagService featureFlagService;

  private static int DELAY_TIME_IN_SEC = 60;
  public static final long DEFAULT_ARTIFACT_COLLECTION_STATE_TIMEOUT_MILLIS = 5L * 60L * 1000L; // 5 minutes

  public ArtifactCollectionState(String name) {
    super(name, ARTIFACT_COLLECTION.name());
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    ArtifactStream artifactStream = artifactStreamService.get(artifactStreamId);
    if (artifactStream == null) {
      logger.info("Artifact Stream {} might have been deleted", artifactStreamId);
      return ExecutionResponse.builder()
          .executionStatus(ExecutionStatus.FAILED)
          .errorMessage("Artifact source might have been deleted. Please update with the right artifact source.")
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
      updateArtifactCollectionExecutionData(context, artifactCollectionExecutionData);

      addBuildExecutionSummary(context, artifactCollectionExecutionData, artifactStream);
      return ExecutionResponse.builder()
          .executionStatus(ExecutionStatus.SUCCESS)
          .stateExecutionData(artifactCollectionExecutionData)
          .errorMessage("Collected artifact [" + lastCollectedArtifact.getBuildNo() + "] for artifact source ["
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

    return ExecutionResponse.builder()
        .async(true)
        .correlationIds(singletonList(resumeId))
        .stateExecutionData(artifactCollectionExecutionData)
        .build();
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, ResponseData> response) {
    ArtifactStream artifactStream = artifactStreamService.get(artifactStreamId);
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

      return ExecutionResponse.builder()
          .async(true)
          .correlationIds(asList(resumeId))
          .stateExecutionData(artifactCollectionExecutionData)
          .build();
    }

    artifactCollectionExecutionData.setRevision(lastCollectedArtifact.getRevision());
    artifactCollectionExecutionData.setBuildNo(lastCollectedArtifact.getBuildNo());
    artifactCollectionExecutionData.setMetadata(lastCollectedArtifact.getMetadata());
    artifactCollectionExecutionData.setArtifactId(lastCollectedArtifact.getUuid());
    updateArtifactCollectionExecutionData(context, artifactCollectionExecutionData);

    addBuildExecutionSummary(context, artifactCollectionExecutionData, artifactStream);
    return ExecutionResponse.builder()
        .stateExecutionData(artifactCollectionExecutionData)
        .executionStatus(SUCCESS)
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
    String buildUrl = metadata.get(ArtifactMetadataKeys.url);
    // Rove the the following as no need to store in build execution summary
    metadata.remove(ArtifactMetadataKeys.buildNo);
    metadata.remove(ArtifactMetadataKeys.url);
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
    workflowExecutionService.refreshBuildExecutionSummary(context.getWorkflowExecutionId(), buildExecutionSummary);
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

  private void updateArtifactCollectionExecutionData(
      ExecutionContext context, ArtifactCollectionExecutionData artifactCollectionExecutionData) {
    if (isMultiArtifact(context.getAccountId())) {
      artifactCollectionExecutionData.setEntityType(fetchEntityType());
      artifactCollectionExecutionData.setEntityId(fetchEntityId());
      artifactCollectionExecutionData.setServiceId(fetchServiceId());
      artifactCollectionExecutionData.setArtifactVariableName(fetchArtifactVariableName());
    }
  }

  private Artifact fetchCollectedArtifact(ArtifactStream artifactStream, String buildNo) {
    if (isBlank(buildNo)) {
      return artifactService.fetchLastCollectedApprovedArtifactForArtifactStream(artifactStream);
    } else {
      return artifactService.getArtifactByBuildNumber(artifactStream, buildNo, isRegex());
    }
  }

  private EntityType fetchEntityType() {
    // TODO: ASR: observations:
    //   1. if entityType is present, entityId should not be blank
    //   2. if entityType is WORKFLOW, serviceId is ignored
    return entityType == null ? EntityType.SERVICE : entityType;
  }

  private String fetchEntityId() {
    return isBlank(entityId) ? fetchServiceId() : entityId;
  }

  private String fetchServiceId() {
    return serviceId;
  }

  private String fetchArtifactVariableName() {
    return isBlank(artifactVariableName) ? ExpressionEvaluator.DEFAULT_ARTIFACT_VARIABLE_NAME : artifactVariableName;
  }

  private boolean isMultiArtifact(String accountId) {
    return featureFlagService.isEnabled(FeatureName.ARTIFACT_STREAM_REFACTOR, accountId);
  }
}
