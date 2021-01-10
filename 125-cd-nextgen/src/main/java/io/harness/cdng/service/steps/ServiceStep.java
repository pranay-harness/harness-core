package io.harness.cdng.service.steps;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.ng.core.mapper.TagMapper.convertToList;
import static io.harness.ngpipeline.common.ParameterFieldHelper.getParameterFieldValue;

import static java.util.stream.Collectors.toList;

import io.harness.cdng.artifact.bean.ArtifactConfig;
import io.harness.cdng.artifact.bean.ArtifactOutcome;
import io.harness.cdng.artifact.bean.yaml.ArtifactListConfig;
import io.harness.cdng.artifact.bean.yaml.ArtifactOverrideSets;
import io.harness.cdng.artifact.mappers.ArtifactResponseToOutcomeMapper;
import io.harness.cdng.artifact.steps.ArtifactStep;
import io.harness.cdng.artifact.steps.ArtifactStepParameters;
import io.harness.cdng.artifact.utils.ArtifactUtils;
import io.harness.cdng.manifest.state.ManifestStep;
import io.harness.cdng.manifest.yaml.ManifestOutcome;
import io.harness.cdng.manifest.yaml.ManifestsOutcome;
import io.harness.cdng.service.beans.ServiceConfig;
import io.harness.cdng.service.beans.ServiceOutcome;
import io.harness.cdng.service.beans.ServiceOutcome.ArtifactsOutcome;
import io.harness.cdng.service.beans.ServiceOutcome.ArtifactsOutcome.ArtifactsOutcomeBuilder;
import io.harness.cdng.service.beans.ServiceOutcome.ArtifactsWrapperOutcome;
import io.harness.cdng.service.beans.ServiceOutcome.ManifestsWrapperOutcome;
import io.harness.cdng.service.beans.ServiceOutcome.ServiceOutcomeBuilder;
import io.harness.cdng.service.beans.ServiceOutcome.StageOverridesOutcome;
import io.harness.cdng.service.beans.ServiceOutcome.StageOverridesOutcome.StageOverridesOutcomeBuilder;
import io.harness.cdng.service.beans.ServiceOutcome.VariablesWrapperOutcome;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
import io.harness.cdng.variables.beans.NGVariableOverrideSets;
import io.harness.cdng.visitor.YamlTypes;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.exception.InvalidRequestException;
import io.harness.executions.steps.ExecutionNodeType;
import io.harness.ng.core.service.entity.ServiceEntity;
import io.harness.ng.core.service.services.ServiceEntityService;
import io.harness.ngpipeline.common.AmbianceHelper;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.data.Outcome;
import io.harness.pms.sdk.core.steps.executables.TaskChainExecutable;
import io.harness.pms.sdk.core.steps.executables.TaskChainResponse;
import io.harness.pms.sdk.core.steps.io.PassThroughData;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepOutcome;
import io.harness.pms.yaml.ParameterField;
import io.harness.steps.StepOutcomeGroup;
import io.harness.tasks.ResponseData;
import io.harness.yaml.core.variables.NGVariable;
import io.harness.yaml.utils.JsonPipelineUtils;
import io.harness.yaml.utils.NGVariablesUtils;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import lombok.SneakyThrows;
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

    TaskRequest taskRequest = artifactStep.getTaskRequest(ambiance, artifactsWithCorrespondingOverrides.get(0));
    return TaskChainResponse.builder()
        .taskRequest(taskRequest)
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

    StepOutcome artifactOutcome =
        artifactStep.processDelegateResponse(notifyResponseData, artifactsWithCorrespondingOverrides.get(currentIndex));
    List<StepOutcome> stepOutcomes = new ArrayList<>(((ServiceStepPassThroughData) passThroughData).getStepOutcomes());
    stepOutcomes.add(artifactOutcome);
    ((ServiceStepPassThroughData) passThroughData).setStepOutcomes(stepOutcomes);

    int nextIndex = currentIndex + 1;
    TaskRequest taskRequest = artifactStep.getTaskRequest(ambiance, artifactsWithCorrespondingOverrides.get(nextIndex));
    serviceStepPassThroughData.setCurrentIndex(nextIndex);
    return TaskChainResponse.builder()
        .taskRequest(taskRequest)
        .chainEnd(artifactsWithCorrespondingOverrides.size() == nextIndex + 1)
        .passThroughData(passThroughData)
        .build();
  }

  @SneakyThrows
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

      StepOutcome artifactOutcome = artifactStep.processDelegateResponse(
          notifyResponseData, artifactsWithCorrespondingOverrides.get(currentIndex));

      List<StepOutcome> stepOutcomes =
          new ArrayList<>(((ServiceStepPassThroughData) passThroughData).getStepOutcomes());
      stepOutcomes.add(artifactOutcome);
      ((ServiceStepPassThroughData) passThroughData).setStepOutcomes(stepOutcomes);
    }

    ServiceConfig serviceConfig = serviceStepParameters.getServiceOverrides() != null
        ? serviceStepParameters.getService().applyOverrides(serviceStepParameters.getServiceOverrides())
        : serviceStepParameters.getService();
    serviceEntityService.upsert(getServiceEntity(serviceConfig, ambiance));

    return StepResponse.builder()
        .stepOutcome(StepResponse.StepOutcome.builder()
                         .name(OutcomeExpressionConstants.SERVICE)
                         .outcome(createServiceOutcome(serviceConfig, serviceStepPassThroughData.getStepOutcomes(),
                             Integer.parseInt(AmbianceHelper.getExpressionFunctorToken(ambiance))))
                         .group(StepOutcomeGroup.STAGE.name())
                         .build())
        .status(Status.SUCCEEDED)
        .build();
  }

  @VisibleForTesting
  ServiceOutcome createServiceOutcome(
      ServiceConfig serviceConfig, List<StepOutcome> stepOutcomes, int expressionFunctorToken) throws IOException {
    ServiceOutcomeBuilder outcomeBuilder =
        ServiceOutcome.builder()
            .name(serviceConfig.getService().getName())
            .identifier(serviceConfig.getService().getIdentifier())
            .description(serviceConfig.getService().getDescription() != null
                    ? serviceConfig.getService().getDescription().getValue()
                    : "")
            .type(serviceConfig.getServiceDefinition().getServiceSpec().getType())
            .tags(serviceConfig.getTags())
            .variables(NGVariablesUtils.getMapOfVariables(
                serviceConfig.getServiceDefinition().getServiceSpec().getVariables(), expressionFunctorToken));

    List<Outcome> outcomes = stepOutcomes.stream().map(StepOutcome::getOutcome).collect(toList());
    if (EmptyPredicate.isEmpty(outcomes)) {
      outcomes = new LinkedList<>();
    }
    // Handle ArtifactsForkOutcome
    ArtifactsOutcomeBuilder artifactsBuilder = ArtifactsOutcome.builder();
    List<ArtifactOutcome> artifactOutcomes = outcomes.stream()
                                                 .filter(outcome -> outcome instanceof ArtifactOutcome)
                                                 .map(a -> (ArtifactOutcome) a)
                                                 .collect(toList());
    handleArtifactOutcome(outcomeBuilder, artifactOutcomes, serviceConfig);
    outcomeBuilder.artifactsResult(artifactsBuilder.build());

    // Handle ManifestOutcome
    Optional<Outcome> optionalManifestOutcome =
        outcomes.stream().filter(outcome -> outcome instanceof ManifestsOutcome).findFirst();
    ManifestsOutcome manifestsOutcome = (ManifestsOutcome) optionalManifestOutcome.orElse(
        ManifestsOutcome.builder().manifestOutcomeList(Collections.emptyList()).build());
    handleManifestOutcome(manifestsOutcome, outcomeBuilder);

    handleVariablesOutcome(outcomeBuilder, serviceConfig, expressionFunctorToken);
    handlePublishingStageOverrides(outcomeBuilder, manifestsOutcome, serviceConfig, expressionFunctorToken);
    return outcomeBuilder.build();
  }

  private void handlePublishingStageOverrides(ServiceOutcomeBuilder outcomeBuilder, ManifestsOutcome manifestsOutcome,
      ServiceConfig serviceConfig, int expressionFunctorToken) {
    if (serviceConfig.getStageOverrides() == null) {
      return;
    }

    // Adding manifests stage overrides.
    StageOverridesOutcomeBuilder stageOverridesOutcomeBuilder = StageOverridesOutcome.builder();
    if (manifestsOutcome != null && EmptyPredicate.isNotEmpty(manifestsOutcome.getManifestStageOverridesList())) {
      manifestsOutcome.getManifestStageOverridesList().forEach(
          manifestOutcome -> stageOverridesOutcomeBuilder.manifest(manifestOutcome.getIdentifier(), manifestOutcome));
    }
    stageOverridesOutcomeBuilder.useManifestOverrideSets(
        serviceConfig.getStageOverrides().getUseManifestOverrideSets());

    // Adding artifact Stage overrides.
    ArtifactListConfig stageOverrideArtifacts = serviceConfig.getStageOverrides().getArtifacts();
    if (stageOverrideArtifacts != null) {
      List<ArtifactConfig> artifactConfigs = ArtifactUtils.convertArtifactListIntoArtifacts(stageOverrideArtifacts);
      ArtifactsOutcomeBuilder artifactsOutcomeBuilder = ArtifactsOutcome.builder();
      for (ArtifactConfig artifactConfig : artifactConfigs) {
        ArtifactOutcome stageArtifactOutcome =
            ArtifactResponseToOutcomeMapper.toArtifactOutcome(artifactConfig, null, false);
        if (stageArtifactOutcome.isPrimaryArtifact()) {
          artifactsOutcomeBuilder.primary(stageArtifactOutcome);
        } else {
          artifactsOutcomeBuilder.sidecar(stageArtifactOutcome.getIdentifier(), stageArtifactOutcome);
        }
      }
      stageOverridesOutcomeBuilder.artifacts(artifactsOutcomeBuilder.build());
    }
    stageOverridesOutcomeBuilder.useArtifactOverrideSets(
        serviceConfig.getStageOverrides().getUseArtifactOverrideSets());

    // Adding variable stage overrides.
    List<NGVariable> stageOverrideVariables = serviceConfig.getStageOverrides().getVariables();
    if (EmptyPredicate.isNotEmpty(stageOverrideVariables)) {
      stageOverridesOutcomeBuilder.variables(
          NGVariablesUtils.getMapOfVariables(stageOverrideVariables, expressionFunctorToken));
    }
    stageOverridesOutcomeBuilder.useVariableOverrideSets(
        serviceConfig.getStageOverrides().getUseVariableOverrideSets());

    outcomeBuilder.stageOverrides(stageOverridesOutcomeBuilder.build());
  }

  private void handleVariablesOutcome(
      ServiceOutcomeBuilder outcomeBuilder, ServiceConfig serviceConfig, int expressionFunctorToken) {
    List<NGVariable> variables = serviceConfig.getServiceDefinition().getServiceSpec().getVariables();
    Map<String, Object> mapOfVariables = new HashMap<>();
    if (EmptyPredicate.isNotEmpty(variables)) {
      mapOfVariables.putAll(NGVariablesUtils.getMapOfVariables(variables, expressionFunctorToken));
      // original Variables
      outcomeBuilder.variables(mapOfVariables);
    }

    List<NGVariable> applicableVariableOverrideSets = getApplicableVariableOverrideSets(serviceConfig);
    mapOfVariables = NGVariablesUtils.applyVariableOverrides(mapOfVariables, applicableVariableOverrideSets);

    if (serviceConfig.getStageOverrides() != null) {
      List<NGVariable> stageVariablesOverrides = serviceConfig.getStageOverrides().getVariables();
      mapOfVariables = NGVariablesUtils.applyVariableOverrides(mapOfVariables, stageVariablesOverrides);
    }

    // Publishing final override variables against "output" key.
    if (EmptyPredicate.isNotEmpty(mapOfVariables)) {
      for (Map.Entry<String, Object> entry : mapOfVariables.entrySet()) {
        outcomeBuilder.variable(YamlTypes.OUTPUT + YamlTypes.PATH_CONNECTOR + entry.getKey(), entry.getValue());
      }
    }

    List<NGVariableOverrideSets> variableOverrideSets =
        serviceConfig.getServiceDefinition().getServiceSpec().getVariableOverrideSets();
    if (variableOverrideSets != null) {
      for (NGVariableOverrideSets variableOverrideSet : variableOverrideSets) {
        outcomeBuilder.variableOverrideSet(variableOverrideSet.getIdentifier(),
            VariablesWrapperOutcome.builder()
                .variables(
                    NGVariablesUtils.getMapOfVariables(variableOverrideSet.getVariables(), expressionFunctorToken))
                .build());
      }
    }
  }

  private void handleManifestOutcome(ManifestsOutcome outcome, ServiceOutcomeBuilder outcomeBuilder)
      throws IOException {
    List<ManifestOutcome> manifestOutcomeList =
        isNotEmpty(outcome.getManifestOutcomeList()) ? outcome.getManifestOutcomeList() : Collections.emptyList();

    List<ManifestOutcome> manifestOriginalList =
        isNotEmpty(outcome.getManifestOriginalList()) ? outcome.getManifestOriginalList() : Collections.emptyList();

    for (ManifestOutcome manifestOutcome : manifestOriginalList) {
      outcomeBuilder.manifest(
          manifestOutcome.getIdentifier(), JsonPipelineUtils.read(manifestOutcome.toJson(), Map.class));
    }
    for (ManifestOutcome manifestOutcome : manifestOutcomeList) {
      Map<String, Object> valueMap = new HashMap<>();
      valueMap.put(YamlTypes.OUTPUT, JsonPipelineUtils.read(manifestOutcome.toJson(), Map.class));
      outcomeBuilder.manifest(manifestOutcome.getIdentifier(), valueMap);
    }

    manifestOutcomeList.forEach(
        manifestOutcome -> outcomeBuilder.manifestResult(manifestOutcome.getIdentifier(), manifestOutcome));

    Map<String, List<ManifestOutcome>> manifestOverrideSets = outcome.getManifestOverrideSets();
    for (Map.Entry<String, List<ManifestOutcome>> entry : manifestOverrideSets.entrySet()) {
      outcomeBuilder.manifestOverrideSet(entry.getKey(),
          ManifestsWrapperOutcome.builder()
              .manifests(entry.getValue().stream().collect(Collectors.toMap(ManifestOutcome::getIdentifier, m -> m)))
              .build());
    }
  }

  private void handleArtifactOutcome(ServiceOutcomeBuilder outcomeBuilder, List<ArtifactOutcome> artifactOutcomes,
      ServiceConfig serviceConfig) throws IOException {
    ArtifactsOutcomeBuilder artifactsBuilder = ArtifactsOutcome.builder();
    for (ArtifactOutcome artifactOutcome : artifactOutcomes) {
      if (artifactOutcome.isPrimaryArtifact()) {
        artifactsBuilder.primary(artifactOutcome);

        Map<String, Object> valueMap = new HashMap<>();
        valueMap.put(YamlTypes.OUTPUT, JsonPipelineUtils.read(artifactOutcome.toJson(), Map.class));
        outcomeBuilder.artifact(YamlTypes.PRIMARY_ARTIFACT, valueMap);
      } else {
        artifactsBuilder.sidecar(artifactOutcome.getIdentifier(), artifactOutcome);

        Map<String, Object> valueMap = new HashMap<>();
        valueMap.put(YamlTypes.OUTPUT, JsonPipelineUtils.read(artifactOutcome.toJson(), Map.class));
        outcomeBuilder.artifact(
            YamlTypes.SIDECARS_ARTIFACT_CONFIG + YamlTypes.PATH_CONNECTOR + artifactOutcome.getIdentifier(), valueMap);
      }
    }
    outcomeBuilder.artifactsResult(artifactsBuilder.build());

    ArtifactListConfig originalArtifacts = serviceConfig.getServiceDefinition().getServiceSpec().getArtifacts();
    if (originalArtifacts != null) {
      List<ArtifactConfig> artifactConfigs = ArtifactUtils.convertArtifactListIntoArtifacts(originalArtifacts);
      for (ArtifactConfig artifactConfig : artifactConfigs) {
        ArtifactOutcome artifactOutcome =
            ArtifactResponseToOutcomeMapper.toArtifactOutcome(artifactConfig, null, false);
        if (artifactOutcome.isPrimaryArtifact()) {
          outcomeBuilder.artifact(
              YamlTypes.PRIMARY_ARTIFACT, JsonPipelineUtils.read(artifactOutcome.toJson(), Map.class));
        } else {
          outcomeBuilder.artifact(
              YamlTypes.SIDECARS_ARTIFACT_CONFIG + YamlTypes.PATH_CONNECTOR + artifactOutcome.getIdentifier(),
              JsonPipelineUtils.read(artifactOutcome.toJson(), Map.class));
        }
      }
    }

    List<ArtifactOverrideSets> artifactOverrideSets =
        serviceConfig.getServiceDefinition().getServiceSpec().getArtifactOverrideSets();
    if (artifactOverrideSets != null) {
      for (ArtifactOverrideSets artifactOverrideSet : artifactOverrideSets) {
        ArtifactListConfig artifacts = artifactOverrideSet.getArtifacts();
        List<ArtifactConfig> artifactConfigs = ArtifactUtils.convertArtifactListIntoArtifacts(artifacts);
        artifactsBuilder = ArtifactsOutcome.builder();
        for (ArtifactConfig artifactConfig : artifactConfigs) {
          ArtifactOutcome overrideArtifactOutcome =
              ArtifactResponseToOutcomeMapper.toArtifactOutcome(artifactConfig, null, false);
          if (overrideArtifactOutcome.isPrimaryArtifact()) {
            artifactsBuilder.primary(overrideArtifactOutcome);
          } else {
            artifactsBuilder.sidecar(overrideArtifactOutcome.getIdentifier(), overrideArtifactOutcome);
          }
        }
        outcomeBuilder.artifactOverrideSet(artifactOverrideSet.getIdentifier(),
            ArtifactsWrapperOutcome.builder().artifacts(artifactsBuilder.build()).build());
      }
    }
  }

  private List<NGVariable> getApplicableVariableOverrideSets(ServiceConfig serviceConfig) {
    List<NGVariable> variableOverrideSets = new LinkedList<>();
    if (serviceConfig.getStageOverrides() != null
        && !ParameterField.isNull(serviceConfig.getStageOverrides().getUseVariableOverrideSets())) {
      serviceConfig.getStageOverrides()
          .getUseVariableOverrideSets()
          .getValue()
          .stream()
          .map(useVariableOverrideSet
              -> serviceConfig.getServiceDefinition()
                     .getServiceSpec()
                     .getVariableOverrideSets()
                     .stream()
                     .filter(o -> o.getIdentifier().equals(useVariableOverrideSet))
                     .findFirst())
          .forEachOrdered(optionalVariableOverrideSets -> {
            if (!optionalVariableOverrideSets.isPresent()) {
              throw new InvalidRequestException("Service Variable Override Set is not defined.");
            }
            variableOverrideSets.addAll(optionalVariableOverrideSets.get().getVariables());
          });
    }
    return variableOverrideSets;
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
        .identifier(serviceConfig.getService().getIdentifier())
        .name(serviceConfig.getService().getName())
        .description(getParameterFieldValue(serviceConfig.getService().getDescription()))
        .projectIdentifier(projectIdentifier)
        .orgIdentifier(orgIdentifier)
        .accountId(accountId)
        .tags(convertToList(serviceConfig.getTags()))
        .build();
  }
}
