package io.harness.cdng.artifact.steps;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.stream.Collectors.toList;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;

import io.harness.ambiance.Ambiance;
import io.harness.cdng.artifact.bean.ArtifactOutcome;
import io.harness.cdng.artifact.utils.ArtifactUtils;
import io.harness.cdng.service.beans.ServiceOutcome.ArtifactsOutcome;
import io.harness.cdng.service.beans.ServiceOutcome.ArtifactsOutcome.ArtifactsOutcomeBuilder;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
import io.harness.data.Outcome;
import io.harness.delegate.beans.ResponseData;
import io.harness.engine.outcomes.OutcomeService;
import io.harness.exception.FailureType;
import io.harness.execution.status.Status;
import io.harness.facilitator.modes.children.ChildrenExecutable;
import io.harness.facilitator.modes.children.ChildrenExecutableResponse;
import io.harness.facilitator.modes.children.ChildrenExecutableResponse.ChildrenExecutableResponseBuilder;
import io.harness.state.Step;
import io.harness.state.StepType;
import io.harness.state.core.fork.ForkStepParameters;
import io.harness.state.io.FailureInfo;
import io.harness.state.io.StepInputPackage;
import io.harness.state.io.StepOutcomeRef;
import io.harness.state.io.StepParameters;
import io.harness.state.io.StepResponse;
import io.harness.state.io.StepResponse.StepResponseBuilder;
import io.harness.state.io.StepResponseNotifyData;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

public class ArtifactForkStep implements Step, ChildrenExecutable {
  public static final StepType STEP_TYPE = StepType.builder().type("ARTIFACT_FORK_STEP").build();
  @Inject private OutcomeService outcomeService;

  @Override
  public ChildrenExecutableResponse obtainChildren(
      Ambiance ambiance, StepParameters stepParameters, StepInputPackage inputPackage) {
    ForkStepParameters parameters = (ForkStepParameters) stepParameters;
    ChildrenExecutableResponseBuilder responseBuilder = ChildrenExecutableResponse.builder();
    for (String nodeId : parameters.getParallelNodeIds()) {
      responseBuilder.child(ChildrenExecutableResponse.Child.builder().childNodeId(nodeId).build());
    }
    return responseBuilder.build();
  }

  @Override
  public StepResponse handleChildrenResponse(
      Ambiance ambiance, StepParameters stepParameters, Map<String, ResponseData> responseDataMap) {
    StepResponseBuilder responseBuilder = StepResponse.builder().status(Status.SUCCEEDED);
    boolean allChildrenSuccess = true;
    EnumSet<FailureType> failureTypes = EnumSet.noneOf(FailureType.class);
    List<String> errorMessages = new ArrayList<>();
    List<StepResponseNotifyData> responseNotifyDataList = new ArrayList<>();
    for (ResponseData responseData : responseDataMap.values()) {
      StepResponseNotifyData responseNotifyData = (StepResponseNotifyData) responseData;
      responseNotifyDataList.add(responseNotifyData);
      Status executionStatus = responseNotifyData.getStatus();
      if (executionStatus != Status.SUCCEEDED) {
        allChildrenSuccess = false;
        responseBuilder.status(executionStatus);
        errorMessages.add(responseNotifyData.getFailureInfo().getErrorMessage());
        failureTypes.addAll(responseNotifyData.getFailureInfo().getFailureTypes());
      }
    }

    if (!allChildrenSuccess) {
      responseBuilder.failureInfo(
          FailureInfo.builder().errorMessage(String.join(",", errorMessages)).failureTypes(failureTypes).build());
    } else {
      responseBuilder.stepOutcome(StepResponse.StepOutcome.builder()
                                      .name(OutcomeExpressionConstants.ARTIFACTS.getName())
                                      .outcome(createArtifactsOutcome(responseNotifyDataList))
                                      .build());
    }
    return responseBuilder.build();
  }

  @VisibleForTesting
  ArtifactsOutcome createArtifactsOutcome(List<StepResponseNotifyData> responseNotifyDataList) {
    // Fetch all outcomes of the children.
    List<String> outcomeInstanceIds = responseNotifyDataList.stream()
                                          .flatMap(notifyData -> notifyData.getStepOutcomesRefs().stream())
                                          .map(StepOutcomeRef::getInstanceId)
                                          .collect(toList());
    List<Outcome> outcomes = outcomeService.fetchOutcomes(outcomeInstanceIds);

    ArtifactsOutcomeBuilder artifactsBuilder = ArtifactsOutcome.builder();
    if (isNotEmpty(outcomes)) {
      // Handle ArtifactOutcomes
      List<Outcome> artifactOutcomes =
          outcomes.stream().filter(outcome -> outcome instanceof ArtifactOutcome).collect(toList());
      artifactOutcomes.forEach(
          artifactOutcome -> handleArtifactOutcome(artifactsBuilder, (ArtifactOutcome) artifactOutcome));
    }

    return artifactsBuilder.build();
  }

  private void handleArtifactOutcome(ArtifactsOutcomeBuilder artifactsBuilder, ArtifactOutcome artifactOutcome) {
    if (ArtifactUtils.isPrimaryArtifact(artifactOutcome)) {
      artifactsBuilder.primary(artifactOutcome);
    } else {
      artifactsBuilder.sidecar(artifactOutcome.getIdentifier(), artifactOutcome);
    }
  }
}
