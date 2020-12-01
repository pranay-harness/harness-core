package io.harness.cdng.artifact.steps;

import io.harness.beans.ParameterField;
import io.harness.cdng.artifact.bean.ArtifactConfig;
import io.harness.cdng.artifact.bean.ArtifactOutcome;
import io.harness.cdng.artifact.bean.yaml.ArtifactListConfig;
import io.harness.cdng.artifact.bean.yaml.ArtifactOverrideSets;
import io.harness.cdng.artifact.mappers.ArtifactResponseToOutcomeMapper;
import io.harness.cdng.artifact.steps.ArtifactStepParameters.ArtifactStepParametersBuilder;
import io.harness.cdng.artifact.utils.ArtifactStepHelper;
import io.harness.cdng.artifact.utils.ArtifactUtils;
import io.harness.cdng.service.beans.ServiceConfig;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.task.artifacts.ArtifactSourceDelegateRequest;
import io.harness.delegate.task.artifacts.ArtifactTaskType;
import io.harness.delegate.task.artifacts.request.ArtifactTaskParameters;
import io.harness.delegate.task.artifacts.response.ArtifactTaskResponse;
import io.harness.exception.ArtifactServerException;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.InvalidRequestException;
import io.harness.ngpipeline.common.AmbianceHelper;
import io.harness.pms.ambiance.Ambiance;
import io.harness.state.io.StepResponse.StepOutcome;
import io.harness.steps.StepUtils;
import io.harness.tasks.Cd1SetupFields;
import io.harness.tasks.Task;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class ArtifactStep {
  @Inject private ArtifactStepHelper artifactStepHelper;
  // Default timeout of 1 minute.
  private static final long DEFAULT_TIMEOUT = TimeUnit.MINUTES.toMillis(1);

  public Task getTask(Ambiance ambiance, ArtifactStepParameters stepParameters) {
    log.info("Executing deployment stage with params [{}]", stepParameters);
    ArtifactConfig finalArtifact = applyArtifactsOverlay(stepParameters);
    String accountId = AmbianceHelper.getAccountId(ambiance);
    ArtifactSourceDelegateRequest artifactSourceDelegateRequest =
        artifactStepHelper.toSourceDelegateRequest(finalArtifact, ambiance);
    final ArtifactTaskParameters taskParameters = ArtifactTaskParameters.builder()
                                                      .accountId(accountId)
                                                      .artifactTaskType(ArtifactTaskType.GET_LAST_SUCCESSFUL_BUILD)
                                                      .attributes(artifactSourceDelegateRequest)
                                                      .build();
    final TaskData taskData = TaskData.builder()
                                  .async(true)
                                  .taskType(artifactStepHelper.getArtifactStepTaskType(finalArtifact))
                                  .parameters(new Object[] {taskParameters})
                                  .timeout(DEFAULT_TIMEOUT)
                                  .build();

    return StepUtils.prepareDelegateTaskInput(
        accountId, taskData, ImmutableMap.of(Cd1SetupFields.APP_ID_FIELD, accountId));
  }

  public StepOutcome processDelegateResponse(
      DelegateResponseData notifyResponseData, ArtifactStepParameters stepParameters) {
    if (notifyResponseData instanceof ArtifactTaskResponse) {
      ArtifactTaskResponse taskResponse = (ArtifactTaskResponse) notifyResponseData;
      switch (taskResponse.getCommandExecutionStatus()) {
        case SUCCESS:
          return getStepOutcome(taskResponse, stepParameters);
        case FAILURE:
          throw new ArtifactServerException("Delegate task failed with msg: " + taskResponse.getErrorMessage());
        default:
          throw new ArtifactServerException(
              "Unhandled type CommandExecutionStatus: " + taskResponse.getCommandExecutionStatus().name());
      }
    } else if (notifyResponseData instanceof ErrorNotifyResponseData) {
      throw new ArtifactServerException(
          "Delegate task failed with msg: " + ((ErrorNotifyResponseData) notifyResponseData).getErrorMessage());
    } else {
      throw new ArtifactServerException(
          "Unhandled DelegateResponseData class " + notifyResponseData.getClass().getCanonicalName());
    }
  }

  public StepOutcome getStepOutcome(ArtifactTaskResponse taskResponse, ArtifactStepParameters stepParameters) {
    ArtifactOutcome artifact = ArtifactResponseToOutcomeMapper.toArtifactOutcome(applyArtifactsOverlay(stepParameters),
        taskResponse.getArtifactTaskExecutionResponse().getArtifactDelegateResponses().get(0));
    String outcomeKey =
        OutcomeExpressionConstants.ARTIFACTS + ArtifactUtils.SIDECAR_ARTIFACT + "." + artifact.getIdentifier();
    if (artifact.isPrimaryArtifact()) {
      outcomeKey = OutcomeExpressionConstants.ARTIFACTS + ArtifactUtils.PRIMARY_ARTIFACT;
    }
    return StepOutcome.builder().name(outcomeKey).outcome(artifact).build();
  }

  @VisibleForTesting
  ArtifactConfig applyArtifactsOverlay(ArtifactStepParameters stepParameters) {
    List<ArtifactConfig> artifactList = new LinkedList<>();
    if (stepParameters.getArtifact() != null) {
      artifactList.add(stepParameters.getArtifact());
    }
    if (stepParameters.getArtifactOverrideSet() != null) {
      artifactList.add(stepParameters.getArtifactOverrideSet());
    }
    if (stepParameters.getArtifactStageOverride() != null) {
      artifactList.add(stepParameters.getArtifactStageOverride());
    }
    if (EmptyPredicate.isEmpty(artifactList)) {
      throw new InvalidArgumentsException("No Artifact details defined.");
    }
    ArtifactConfig resultantArtifact = artifactList.get(0);
    for (ArtifactConfig artifact : artifactList) {
      resultantArtifact = resultantArtifact.applyOverrides(artifact);
    }
    return resultantArtifact;
  }

  public List<ArtifactConfig> getArtifactOverrideSetsApplicable(ServiceConfig serviceConfig) {
    List<ArtifactConfig> artifacts = new LinkedList<>();
    if (serviceConfig.getStageOverrides() != null
        && !ParameterField.isNull(serviceConfig.getStageOverrides().getUseArtifactOverrideSets())) {
      for (String useArtifactOverrideSet : serviceConfig.getStageOverrides().getUseArtifactOverrideSets().getValue()) {
        List<ArtifactOverrideSets> artifactOverrideSetsList =
            serviceConfig.getServiceDefinition()
                .getServiceSpec()
                .getArtifactOverrideSets()
                .stream()
                .filter(o -> o.getIdentifier().equals(useArtifactOverrideSet))
                .collect(Collectors.toList());
        if (artifactOverrideSetsList.size() != 1) {
          throw new InvalidRequestException("Artifact Override Set is not defined properly.");
        }
        ArtifactListConfig artifactListConfig = artifactOverrideSetsList.get(0).getArtifacts();
        artifacts.addAll(ArtifactUtils.convertArtifactListIntoArtifacts(artifactListConfig));
      }
    }
    return artifacts;
  }

  public void mapArtifactsToIdentifier(Map<String, ArtifactStepParametersBuilder> artifactsMap,
      List<ArtifactConfig> artifactsList, BiConsumer<ArtifactStepParametersBuilder, ArtifactConfig> consumer) {
    if (EmptyPredicate.isNotEmpty(artifactsList)) {
      for (ArtifactConfig artifact : artifactsList) {
        String key = ArtifactUtils.getArtifactKey(artifact);
        if (artifactsMap.containsKey(key)) {
          consumer.accept(artifactsMap.get(key), artifact);
        } else {
          ArtifactStepParametersBuilder builder = ArtifactStepParameters.builder();
          consumer.accept(builder, artifact);
          artifactsMap.put(key, builder);
        }
      }
    }
  }

  public List<ArtifactStepParameters> getArtifactsWithCorrespondingOverrides(ServiceConfig serviceConfig) {
    Map<String, ArtifactStepParametersBuilder> artifactsMap = new HashMap<>();
    ArtifactListConfig artifacts = serviceConfig.getServiceDefinition().getServiceSpec().getArtifacts();
    if (artifacts != null) {
      if (artifacts.getPrimary() == null) {
        throw new InvalidArgumentsException("Primary artifact cannot be null.");
      }
      // Add service artifacts.
      List<ArtifactConfig> serviceSpecArtifacts = ArtifactUtils.convertArtifactListIntoArtifacts(artifacts);
      mapArtifactsToIdentifier(artifactsMap, serviceSpecArtifacts, ArtifactStepParametersBuilder::artifact);
    }

    // Add Artifact Override Sets.
    List<ArtifactConfig> artifactOverrideSetsApplicable = getArtifactOverrideSetsApplicable(serviceConfig);
    mapArtifactsToIdentifier(
        artifactsMap, artifactOverrideSetsApplicable, ArtifactStepParametersBuilder::artifactOverrideSet);

    // Add Stage Overrides.
    if (serviceConfig.getStageOverrides() != null && serviceConfig.getStageOverrides().getArtifacts() != null) {
      ArtifactListConfig stageOverrides = serviceConfig.getStageOverrides().getArtifacts();
      List<ArtifactConfig> stageOverridesArtifacts = ArtifactUtils.convertArtifactListIntoArtifacts(stageOverrides);
      mapArtifactsToIdentifier(
          artifactsMap, stageOverridesArtifacts, ArtifactStepParametersBuilder::artifactStageOverride);
    }

    List<ArtifactStepParameters> mappedArtifacts = new LinkedList<>();
    artifactsMap.forEach((key, value) -> mappedArtifacts.add(value.build()));
    return mappedArtifacts;
  }
}
