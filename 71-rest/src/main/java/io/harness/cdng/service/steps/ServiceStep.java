package io.harness.cdng.service.steps;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.stream.Collectors.toList;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;

import io.harness.ambiance.Ambiance;
import io.harness.cdng.manifest.yaml.ManifestAttributes;
import io.harness.cdng.manifest.yaml.ManifestOutcome;
import io.harness.cdng.service.ServiceConfig;
import io.harness.cdng.service.beans.ServiceOutcome;
import io.harness.cdng.service.beans.ServiceOutcome.ArtifactsOutcome;
import io.harness.cdng.service.beans.ServiceOutcome.ServiceOutcomeBuilder;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
import io.harness.data.Outcome;
import io.harness.delegate.beans.ResponseData;
import io.harness.engine.outcomes.OutcomeService;
import io.harness.exception.FailureType;
import io.harness.execution.status.Status;
import io.harness.executionplan.plancreator.beans.StepGroup;
import io.harness.facilitator.modes.children.ChildrenExecutable;
import io.harness.facilitator.modes.children.ChildrenExecutableResponse;
import io.harness.facilitator.modes.children.ChildrenExecutableResponse.ChildrenExecutableResponseBuilder;
import io.harness.state.Step;
import io.harness.state.StepType;
import io.harness.state.io.FailureInfo;
import io.harness.state.io.StepInputPackage;
import io.harness.state.io.StepOutcomeRef;
import io.harness.state.io.StepParameters;
import io.harness.state.io.StepResponse;
import io.harness.state.io.StepResponse.StepResponseBuilder;
import io.harness.state.io.StepResponseNotifyData;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class ServiceStep implements Step, ChildrenExecutable {
  public static final StepType STEP_TYPE = StepType.builder().type("SERVICE_STEP").build();
  @Inject private OutcomeService outcomeService;

  @Override
  public ChildrenExecutableResponse obtainChildren(
      Ambiance ambiance, StepParameters stepParameters, StepInputPackage inputPackage) {
    ServiceStepParameters parameters = (ServiceStepParameters) stepParameters;
    logger.info("Executing deployment stage with params [{}]", parameters);
    // TODO(archit): save service entity.

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
    ServiceStepParameters parameters = (ServiceStepParameters) stepParameters;
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
      ServiceConfig serviceConfig = parameters.getServiceOverrides() != null
          ? parameters.getService().applyOverrides(parameters.getServiceOverrides())
          : parameters.getService();
      responseBuilder.stepOutcome(StepResponse.StepOutcome.builder()
                                      .name(OutcomeExpressionConstants.SERVICE.getName())
                                      .outcome(createServiceOutcome(serviceConfig, responseNotifyDataList))
                                      .group(StepGroup.STAGE.name())
                                      .build());
    }
    return responseBuilder.build();
  }

  @VisibleForTesting
  ServiceOutcome createServiceOutcome(
      ServiceConfig serviceConfig, List<StepResponseNotifyData> responseNotifyDataList) {
    ServiceOutcomeBuilder outcomeBuilder = ServiceOutcome.builder()
                                               .displayName(serviceConfig.getDisplayName())
                                               .identifier(serviceConfig.getIdentifier())
                                               .description(serviceConfig.getDescription())
                                               .deploymentType(serviceConfig.getServiceSpec().getDeploymentType());

    // Fetch all outcomes of the children.
    List<String> outcomeInstanceIds = responseNotifyDataList.stream()
                                          .flatMap(notifyData -> notifyData.getStepOutcomesRefs().stream())
                                          .map(StepOutcomeRef::getInstanceId)
                                          .collect(toList());
    List<Outcome> outcomes = outcomeService.fetchOutcomes(outcomeInstanceIds);

    if (isNotEmpty(outcomes)) {
      // Handle ArtifactsForkOutcome
      Optional<Outcome> artifactsOutcome =
          outcomes.stream().filter(outcome -> outcome instanceof ArtifactsOutcome).findFirst();
      handleArtifactOutcome(
          (ArtifactsOutcome) artifactsOutcome.orElse(ArtifactsOutcome.builder().build()), outcomeBuilder);

      // Handle ManifestOutcome
      Optional<Outcome> manifestOutcome =
          outcomes.stream().filter(outcome -> outcome instanceof ManifestOutcome).findFirst();
      handleManifestOutcome((ManifestOutcome) manifestOutcome.orElse(
                                ManifestOutcome.builder().manifestAttributes(Collections.emptyList()).build()),
          outcomeBuilder);
    }

    return outcomeBuilder.build();
  }

  private void handleManifestOutcome(ManifestOutcome outcome, ServiceOutcomeBuilder outcomeBuilder) {
    List<ManifestAttributes> manifestAttributesList =
        isNotEmpty(outcome.getManifestAttributes()) ? outcome.getManifestAttributes() : Collections.emptyList();

    outcomeBuilder.manifests(manifestAttributesList);
  }

  private void handleArtifactOutcome(ArtifactsOutcome artifactsOutcome, ServiceOutcomeBuilder outcomeBuilder) {
    outcomeBuilder.artifacts(artifactsOutcome);
  }
}
