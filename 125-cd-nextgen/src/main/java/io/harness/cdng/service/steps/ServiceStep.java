package io.harness.cdng.service.steps;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.ng.core.mapper.TagMapper.convertToList;
import static io.harness.ngpipeline.common.ParameterFieldHelper.getParameterFieldValue;

import static java.util.stream.Collectors.toList;

import io.harness.cdng.artifact.bean.ArtifactOutcome;
import io.harness.cdng.artifact.steps.ArtifactStep;
import io.harness.cdng.artifact.steps.ArtifactStepParameters;
import io.harness.cdng.manifest.state.ManifestStep;
import io.harness.cdng.manifest.yaml.ManifestAttributes;
import io.harness.cdng.manifest.yaml.ManifestOutcome;
import io.harness.cdng.service.beans.ServiceConfig;
import io.harness.cdng.service.beans.ServiceOutcome;
import io.harness.cdng.service.beans.ServiceOutcome.ArtifactsOutcome.ArtifactsOutcomeBuilder;
import io.harness.cdng.service.beans.ServiceOutcome.ServiceOutcomeBuilder;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.executions.steps.ExecutionNodeType;
import io.harness.facilitator.PassThroughData;
import io.harness.facilitator.modes.chain.task.TaskChainExecutable;
import io.harness.facilitator.modes.chain.task.TaskChainResponse;
import io.harness.ng.core.service.entity.ServiceEntity;
import io.harness.ng.core.service.services.ServiceEntityService;
import io.harness.ngpipeline.common.AmbianceHelper;
import io.harness.pms.ambiance.Ambiance;
import io.harness.pms.execution.Status;
import io.harness.pms.sdk.core.data.Outcome;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepOutcome;
import io.harness.pms.steps.StepType;
import io.harness.steps.StepOutcomeGroup;
import io.harness.tasks.ResponseData;
import io.harness.tasks.Task;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ServiceStep implements TaskChainExecutable<ServiceStepParameters> {
  public static final StepType STEP_TYPE = StepType.newBuilder().setType(ExecutionNodeType.SERVICE.getName()).build();

  @Inject private ServiceEntityService serviceEntityService;
  @Inject private ArtifactStep artifactStep;
  @Inject private ManifestStep manifestStep;

  @Override
  public Class<ServiceStepParameters> getStepParametersClass() {
    return ServiceStepParameters.class;
  }

  @Override
  public TaskChainResponse startChainLink(
      Ambiance ambiance, ServiceStepParameters stepParameters, StepInputPackage inputPackage) {
    StepOutcome manifestOutcome = manifestStep.processManifests(stepParameters.getService());

    List<ArtifactStepParameters> artifactsWithCorrespondingOverrides =
        artifactStep.getArtifactsWithCorrespondingOverrides(stepParameters.getService());
    ServiceStepPassThroughData passThroughData =
        ServiceStepPassThroughData.builder()
            .artifactsWithCorrespondingOverrides(artifactsWithCorrespondingOverrides)
            .currentIndex(0)
            .stepOutcome(manifestOutcome)
            .build();

    if (isEmpty(artifactsWithCorrespondingOverrides)) {
      return TaskChainResponse.builder().chainEnd(true).passThroughData(passThroughData).build();
    }

    Task task = artifactStep.getTask(ambiance, artifactsWithCorrespondingOverrides.get(0));
    return TaskChainResponse.builder()
        .task(task)
        .chainEnd(artifactsWithCorrespondingOverrides.size() == 1)
        .passThroughData(passThroughData)
        .build();
  }

  @Override
  public TaskChainResponse executeNextLink(Ambiance ambiance, ServiceStepParameters stepParameters,
      StepInputPackage inputPackage, PassThroughData passThroughData, Map<String, ResponseData> responseDataMap) {
    DelegateResponseData notifyResponseData = (DelegateResponseData) responseDataMap.values().iterator().next();
    ServiceStepPassThroughData serviceStepPassThroughData = (ServiceStepPassThroughData) passThroughData;
    int currentIndex = serviceStepPassThroughData.getCurrentIndex();
    List<ArtifactStepParameters> artifactsWithCorrespondingOverrides =
        serviceStepPassThroughData.getArtifactsWithCorrespondingOverrides();

    StepOutcome stepOutcome =
        artifactStep.processDelegateResponse(notifyResponseData, artifactsWithCorrespondingOverrides.get(currentIndex));
    ((ServiceStepPassThroughData) passThroughData).getStepOutcomes().add(stepOutcome);

    int nextIndex = currentIndex + 1;
    Task task = artifactStep.getTask(ambiance, artifactsWithCorrespondingOverrides.get(nextIndex));
    serviceStepPassThroughData.setCurrentIndex(nextIndex);
    return TaskChainResponse.builder()
        .task(task)
        .chainEnd(artifactsWithCorrespondingOverrides.size() == nextIndex)
        .passThroughData(passThroughData)
        .build();
  }

  @Override
  public StepResponse finalizeExecution(Ambiance ambiance, ServiceStepParameters serviceStepParameters,
      PassThroughData passThroughData, Map<String, ResponseData> responseDataMap) {
    ServiceStepPassThroughData serviceStepPassThroughData = (ServiceStepPassThroughData) passThroughData;

    if (!isEmpty(responseDataMap)) {
      // Artifact task executed
      DelegateResponseData notifyResponseData = (DelegateResponseData) responseDataMap.values().iterator().next();
      int currentIndex = serviceStepPassThroughData.getCurrentIndex();
      List<ArtifactStepParameters> artifactsWithCorrespondingOverrides =
          serviceStepPassThroughData.getArtifactsWithCorrespondingOverrides();

      StepOutcome stepOutcome = artifactStep.processDelegateResponse(
          notifyResponseData, artifactsWithCorrespondingOverrides.get(currentIndex));
      ((ServiceStepPassThroughData) passThroughData).getStepOutcomes().add(stepOutcome);
    }

    ServiceConfig serviceConfig = serviceStepParameters.getServiceOverrides() != null
        ? serviceStepParameters.getService().applyOverrides(serviceStepParameters.getServiceOverrides())
        : serviceStepParameters.getService();
    serviceEntityService.upsert(getServiceEntity(serviceConfig, ambiance));

    return StepResponse.builder()
        .stepOutcome(StepResponse.StepOutcome.builder()
                         .name(OutcomeExpressionConstants.SERVICE)
                         .outcome(createServiceOutcome(serviceConfig, serviceStepPassThroughData.getStepOutcomes()))
                         .group(StepOutcomeGroup.STAGE.name())
                         .build())
        .status(Status.SUCCEEDED)
        .build();
  }

  @VisibleForTesting
  ServiceOutcome createServiceOutcome(ServiceConfig serviceConfig, List<StepOutcome> stepOutcomes) {
    ServiceOutcomeBuilder outcomeBuilder =
        ServiceOutcome.builder()
            .displayName(serviceConfig.getName().getValue())
            .identifier(serviceConfig.getIdentifier().getValue())
            .description(serviceConfig.getDescription() != null ? serviceConfig.getDescription().getValue() : "")
            .deploymentType(serviceConfig.getServiceDefinition().getServiceSpec().getType());

    List<Outcome> outcomes = stepOutcomes.stream().map(StepOutcome::getOutcome).collect(toList());

    if (isNotEmpty(outcomes)) {
      // Handle ArtifactsForkOutcome
      ArtifactsOutcomeBuilder artifactsBuilder = ServiceOutcome.ArtifactsOutcome.builder();
      List<Outcome> artifactOutcomes =
          outcomes.stream().filter(outcome -> outcome instanceof ArtifactOutcome).collect(toList());
      artifactOutcomes.forEach(
          artifactOutcome -> handleArtifactOutcome(artifactsBuilder, (ArtifactOutcome) artifactOutcome));
      outcomeBuilder.artifacts(artifactsBuilder.build());

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

  private void handleArtifactOutcome(ArtifactsOutcomeBuilder artifactsBuilder, ArtifactOutcome artifactOutcome) {
    if (artifactOutcome.isPrimaryArtifact()) {
      artifactsBuilder.primary(artifactOutcome);
    } else {
      artifactsBuilder.sidecar(artifactOutcome.getIdentifier(), artifactOutcome);
    }
  }

  @Data
  @Builder
  public static class ServiceStepPassThroughData implements PassThroughData {
    private List<ArtifactStepParameters> artifactsWithCorrespondingOverrides;
    private int currentIndex;
    @Singular private List<StepOutcome> stepOutcomes;
  }

  private ServiceEntity getServiceEntity(ServiceConfig serviceConfig, Ambiance ambiance) {
    String accountId = AmbianceHelper.getAccountId(ambiance);
    String projectIdentifier = AmbianceHelper.getProjectIdentifier(ambiance);
    String orgIdentifier = AmbianceHelper.getOrgIdentifier(ambiance);

    return ServiceEntity.builder()
        .identifier(getParameterFieldValue(serviceConfig.getIdentifier()))
        .name(getParameterFieldValue(serviceConfig.getName()))
        .description(getParameterFieldValue(serviceConfig.getDescription()))
        .projectIdentifier(projectIdentifier)
        .orgIdentifier(orgIdentifier)
        .accountId(accountId)
        .tags(convertToList(serviceConfig.getTags()))
        .build();
  }
}
