package io.harness.cdng.service.steps;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.ng.core.mapper.TagMapper.convertToList;
import static io.harness.ngpipeline.common.ParameterFieldHelper.getParameterFieldValue;

import static software.wings.beans.LogColor.Cyan;
import static software.wings.beans.LogHelper.color;
import static software.wings.beans.LogWeight.Bold;

import static java.util.stream.Collectors.toList;

import io.harness.accesscontrol.Principal;
import io.harness.accesscontrol.clients.AccessControlClient;
import io.harness.accesscontrol.clients.Resource;
import io.harness.accesscontrol.clients.ResourceScope;
import io.harness.accesscontrol.principals.PrincipalType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.artifact.bean.ArtifactConfig;
import io.harness.cdng.artifact.bean.yaml.ArtifactListConfig;
import io.harness.cdng.artifact.bean.yaml.ArtifactOverrideSetWrapper;
import io.harness.cdng.artifact.bean.yaml.ArtifactOverrideSets;
import io.harness.cdng.artifact.mappers.ArtifactResponseToOutcomeMapper;
import io.harness.cdng.artifact.steps.ArtifactStep;
import io.harness.cdng.artifact.steps.ArtifactStepParameters;
import io.harness.cdng.artifact.utils.ArtifactUtils;
import io.harness.cdng.manifest.state.ManifestStep;
import io.harness.cdng.manifest.yaml.ManifestOutcome;
import io.harness.cdng.manifest.yaml.ManifestsOutcome;
import io.harness.cdng.service.beans.ServiceConfig;
import io.harness.cdng.service.beans.ServiceConfigOutcome;
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
import io.harness.cdng.variables.beans.NGVariableOverrideSetWrapper;
import io.harness.cdng.variables.beans.NGVariableOverrideSets;
import io.harness.cdng.visitor.YamlTypes;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.eventsframework.schemas.entity.EntityDetailProtoDTO;
import io.harness.exception.InvalidRequestException;
import io.harness.executions.steps.ExecutionNodeType;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogLevel;
import io.harness.logging.UnitProgress;
import io.harness.logging.UnitStatus;
import io.harness.logstreaming.LogStreamingStepClientFactory;
import io.harness.logstreaming.NGLogCallback;
import io.harness.ng.core.mapper.TagMapper;
import io.harness.ng.core.service.entity.ServiceEntity;
import io.harness.ng.core.service.services.ServiceEntityService;
import io.harness.ngpipeline.artifact.bean.ArtifactOutcome;
import io.harness.ngpipeline.common.AmbianceHelper;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.contracts.plan.ExecutionPrincipalInfo;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.rbac.PipelineRbacHelper;
import io.harness.pms.rbac.PrincipalTypeProtoToPrincipalTypeMapper;
import io.harness.pms.sdk.core.data.Outcome;
import io.harness.pms.sdk.core.steps.executables.TaskChainResponse;
import io.harness.pms.sdk.core.steps.io.PassThroughData;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepOutcome;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;
import io.harness.pms.yaml.ParameterField;
import io.harness.rbac.CDNGRbacPermissions;
import io.harness.steps.EntityReferenceExtractorUtils;
import io.harness.steps.StepOutcomeGroup;
import io.harness.steps.StepUtils;
import io.harness.steps.executable.TaskChainExecutableWithRbac;
import io.harness.supplier.ThrowingSupplier;
import io.harness.tasks.ResponseData;
import io.harness.yaml.core.variables.NGVariable;
import io.harness.yaml.utils.NGVariablesUtils;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.CDP)
@Slf4j
public class ServiceStep implements TaskChainExecutableWithRbac<ServiceStepParameters> {
  public static final StepType STEP_TYPE = StepType.newBuilder().setType(ExecutionNodeType.SERVICE.getName()).build();

  public static final String SERVICE_STEP_COMMAND_UNIT = "Execute";

  @Inject private ServiceEntityService serviceEntityService;
  @Inject private ArtifactStep artifactStep;
  @Inject private ManifestStep manifestStep;
  @Inject private LogStreamingStepClientFactory logStreamingStepClientFactory;
  @Inject private EntityReferenceExtractorUtils entityReferenceExtractorUtils;
  @Inject @Named("PRIVILEGED") private AccessControlClient accessControlClient;
  @Inject private PipelineRbacHelper pipelineRbacHelper;

  @Override
  public Class<ServiceStepParameters> getStepParametersClass() {
    return ServiceStepParameters.class;
  }

  @Override
  public void validateResources(Ambiance ambiance, ServiceStepParameters stepParameters) {
    String accountIdentifier = AmbianceUtils.getAccountId(ambiance);
    String orgIdentifier = AmbianceUtils.getOrgIdentifier(ambiance);
    String projectIdentifier = AmbianceUtils.getProjectIdentifier(ambiance);
    ExecutionPrincipalInfo executionPrincipalInfo = ambiance.getMetadata().getPrincipalInfo();
    String principal = executionPrincipalInfo.getPrincipal();
    if (EmptyPredicate.isEmpty(principal)) {
      return;
    }
    PrincipalType principalType = PrincipalTypeProtoToPrincipalTypeMapper.convertToAccessControlPrincipalType(
        executionPrincipalInfo.getPrincipalType());
    Set<EntityDetailProtoDTO> entityDetails =
        entityReferenceExtractorUtils.extractReferredEntities(ambiance, stepParameters.getService());
    pipelineRbacHelper.checkRuntimePermissions(ambiance, entityDetails);
    ServiceConfig serviceConfig = stepParameters.getService();
    if (serviceConfig.getServiceRef() == null || EmptyPredicate.isEmpty(serviceConfig.getServiceRef().getValue())) {
      accessControlClient.checkForAccessOrThrow(Principal.of(principalType, principal),
          ResourceScope.of(accountIdentifier, orgIdentifier, projectIdentifier), Resource.of("SERVICE", null),
          CDNGRbacPermissions.SERVICE_CREATE_PERMISSION, "Validation for Service Step failed");
    }
  }

  @Override
  public TaskChainResponse startChainLinkAfterRbac(
      Ambiance ambiance, ServiceStepParameters stepParameters, StepInputPackage inputPackage) {
    NGLogCallback ngManagerLogCallback =
        new NGLogCallback(logStreamingStepClientFactory, ambiance, SERVICE_STEP_COMMAND_UNIT, true);
    ngManagerLogCallback.saveExecutionLog("Starting Service Step");

    ngManagerLogCallback.saveExecutionLog("Processing Manifests");
    StepOutcome manifestOutcome = manifestStep.processManifests(stepParameters.getService(), ngManagerLogCallback);

    ngManagerLogCallback.saveExecutionLog("Manifests Processed");
    ngManagerLogCallback.saveExecutionLog("Processing Artifacts");
    List<ArtifactStepParameters> artifactsWithCorrespondingOverrides =
        artifactStep.getArtifactsWithCorrespondingOverrides(stepParameters.getService(), ngManagerLogCallback);
    ServiceStepPassThroughData passThroughData =
        ServiceStepPassThroughData.builder()
            .artifactsWithCorrespondingOverrides(artifactsWithCorrespondingOverrides)
            .currentIndex(0)
            .stepOutcome(manifestOutcome)
            .build();

    if (isEmpty(artifactsWithCorrespondingOverrides)) {
      return TaskChainResponse.builder()
          .chainEnd(true)
          .passThroughData(passThroughData)
          .logKeys(StepUtils.generateLogKeys(ambiance, Collections.singletonList(SERVICE_STEP_COMMAND_UNIT)))
          .units(Collections.singletonList(SERVICE_STEP_COMMAND_UNIT))
          .build();
    }

    TaskRequest taskRequest = artifactStep.getTaskRequest(ambiance, artifactsWithCorrespondingOverrides.get(0));
    boolean chainEnd = artifactsWithCorrespondingOverrides.size() == 1;
    ArtifactStepParameters artifactStepParameters =
        passThroughData.artifactsWithCorrespondingOverrides.get(passThroughData.currentIndex);
    String artifactIdentifier = artifactStepParameters.getArtifact() != null
        ? (artifactStepParameters.getArtifact().getIdentifier())
        : (artifactStepParameters.getArtifactOverrideSet() != null
                ? (artifactStepParameters.getArtifactOverrideSet().getIdentifier())
                : (artifactStepParameters.getArtifactStageOverride().getIdentifier()));
    ngManagerLogCallback.saveExecutionLog(
        color("Starting delegate task for fetching details of artifact :" + artifactIdentifier, Cyan, Bold));
    TaskChainResponse taskChainResponse = TaskChainResponse.builder()
                                              .taskRequest(taskRequest)
                                              .chainEnd(chainEnd)
                                              .passThroughData(passThroughData)
                                              .build();
    ngManagerLogCallback.saveExecutionLog(color("Delegate task completed...", Cyan, Bold));
    return taskChainResponse;
  }

  @Override
  public TaskChainResponse executeNextLink(Ambiance ambiance, ServiceStepParameters stepParameters,
      StepInputPackage inputPackage, PassThroughData passThroughData, ThrowingSupplier<ResponseData> responseSupplier)
      throws Exception {
    NGLogCallback ngManagerLogCallback =
        new NGLogCallback(logStreamingStepClientFactory, ambiance, SERVICE_STEP_COMMAND_UNIT, false);
    DelegateResponseData notifyResponseData = (DelegateResponseData) responseSupplier.get();
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
    boolean chainEnd = artifactsWithCorrespondingOverrides.size() == nextIndex + 1;
    ngManagerLogCallback.saveExecutionLog(color("Starting delegate task for fetching details of artifact: "
            + ((ServiceStepPassThroughData) passThroughData)
                  .artifactsWithCorrespondingOverrides.get(((ServiceStepPassThroughData) passThroughData).currentIndex)
                  .getArtifact()
                  .getIdentifier(),
        Cyan, Bold));
    TaskChainResponse taskChainResponse = TaskChainResponse.builder()
                                              .taskRequest(taskRequest)
                                              .chainEnd(chainEnd)
                                              .passThroughData(passThroughData)
                                              .build();
    ngManagerLogCallback.saveExecutionLog(color("Delegate task completed...", Cyan, Bold));
    return taskChainResponse;
  }

  @SneakyThrows
  @Override
  public StepResponse finalizeExecution(Ambiance ambiance, ServiceStepParameters serviceStepParameters,
      PassThroughData passThroughData, ThrowingSupplier<ResponseData> responseDataSupplier) throws Exception {
    long startTime = System.currentTimeMillis();
    ServiceStepPassThroughData serviceStepPassThroughData = (ServiceStepPassThroughData) passThroughData;
    NGLogCallback managerLogCallback =
        new NGLogCallback(logStreamingStepClientFactory, ambiance, SERVICE_STEP_COMMAND_UNIT, false);
    ResponseData data = responseDataSupplier.get();
    if (data != null) {
      // Artifact task executed
      DelegateResponseData notifyResponseData = (DelegateResponseData) data;
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

    ServiceOutcome serviceOutcome = createServiceOutcome(ambiance, serviceConfig,
        serviceStepPassThroughData.getStepOutcomes(), ambiance.getExpressionFunctorToken(), managerLogCallback);
    managerLogCallback.saveExecutionLog("Service Step Succeeded", LogLevel.INFO, CommandExecutionStatus.SUCCESS);
    return StepResponse.builder()
        .stepOutcome(StepOutcome.builder()
                         .name(OutcomeExpressionConstants.SERVICE)
                         .outcome(serviceOutcome)
                         .group(StepOutcomeGroup.STAGE.name())
                         .build())
        .stepOutcome(StepOutcome.builder()
                         .name(YamlTypes.SERVICE_CONFIG)
                         .outcome(ServiceConfigOutcome.builder()
                                      .service(serviceOutcome)
                                      .variables(serviceOutcome.getVariables())
                                      .artifacts(serviceOutcome.getArtifacts())
                                      .manifests(serviceOutcome.getManifests())
                                      .artifactsResult(serviceOutcome.getArtifactsResult())
                                      .manifestResults(serviceOutcome.getManifestResults())
                                      .artifactOverrideSets(serviceOutcome.getArtifactOverrideSets())
                                      .manifestOverrideSets(serviceOutcome.getManifestOverrideSets())
                                      .variableOverrideSets(serviceOutcome.getVariableOverrideSets())
                                      .stageOverrides(serviceOutcome.getStageOverrides())
                                      .build())
                         .group(StepOutcomeGroup.STAGE.name())
                         .build())
        .status(Status.SUCCEEDED)
        .unitProgressList(Collections.singletonList(UnitProgress.newBuilder()
                                                        .setEndTime(System.currentTimeMillis())
                                                        .setStartTime(startTime)
                                                        .setStatus(UnitStatus.SUCCESS)
                                                        .setUnitName(SERVICE_STEP_COMMAND_UNIT)
                                                        .build()))
        .build();
  }

  @VisibleForTesting
  ServiceOutcome createServiceOutcome(Ambiance ambiance, ServiceConfig serviceConfig, List<StepOutcome> stepOutcomes,
      long expressionFunctorToken, NGLogCallback managerLogCallback) throws IOException {
    ServiceEntity serviceEntity = getServiceEntity(serviceConfig, ambiance);
    ServiceOutcomeBuilder outcomeBuilder =
        ServiceOutcome.builder()
            .name(serviceEntity.getName())
            .identifier(serviceEntity.getIdentifier())
            .description(serviceEntity.getDescription() != null ? serviceEntity.getDescription() : "")
            .type(serviceConfig.getServiceDefinition().getServiceSpec().getType())
            .tags(TagMapper.convertToMap(serviceEntity.getTags()))
            .variables(NGVariablesUtils.getMapOfVariables(
                serviceConfig.getServiceDefinition().getServiceSpec().getVariables(), expressionFunctorToken));

    List<Outcome> outcomes = stepOutcomes.stream().map(StepOutcome::getOutcome).collect(toList());
    if (EmptyPredicate.isEmpty(outcomes)) {
      outcomes = new LinkedList<>();
    }
    // Handle ArtifactsForkOutcome
    List<ArtifactOutcome> artifactOutcomes = outcomes.stream()
                                                 .filter(outcome -> outcome instanceof ArtifactOutcome)
                                                 .map(a -> (ArtifactOutcome) a)
                                                 .collect(toList());
    handleArtifactOutcome(outcomeBuilder, artifactOutcomes, serviceConfig, managerLogCallback);

    // Handle ManifestOutcome
    Optional<Outcome> optionalManifestOutcome =
        outcomes.stream().filter(outcome -> outcome instanceof ManifestsOutcome).findFirst();
    ManifestsOutcome manifestsOutcome = (ManifestsOutcome) optionalManifestOutcome.orElse(
        ManifestsOutcome.builder().manifestOutcomeList(Collections.emptyList()).build());
    handleManifestOutcome(manifestsOutcome, outcomeBuilder);

    handleVariablesOutcome(outcomeBuilder, serviceConfig, expressionFunctorToken);
    handlePublishingStageOverrides(
        outcomeBuilder, manifestsOutcome, serviceConfig, expressionFunctorToken, managerLogCallback);
    return outcomeBuilder.build();
  }

  private void handlePublishingStageOverrides(ServiceOutcomeBuilder outcomeBuilder, ManifestsOutcome manifestsOutcome,
      ServiceConfig serviceConfig, long expressionFunctorToken, NGLogCallback managerLogCallback) {
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
      List<ArtifactConfig> artifactConfigs =
          ArtifactUtils.convertArtifactListIntoArtifacts(stageOverrideArtifacts, managerLogCallback);
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
      ServiceOutcomeBuilder outcomeBuilder, ServiceConfig serviceConfig, long expressionFunctorToken) {
    List<NGVariable> originalVariables = serviceConfig.getServiceDefinition().getServiceSpec().getVariables();
    Map<String, Object> mapOfVariables = new HashMap<>();
    if (EmptyPredicate.isNotEmpty(originalVariables)) {
      mapOfVariables.putAll(NGVariablesUtils.getMapOfVariables(originalVariables, expressionFunctorToken));
      // original Variables
      outcomeBuilder.variables(mapOfVariables);
    }

    List<NGVariable> applicableVariableOverrideSets = getApplicableVariableOverrideSets(serviceConfig);
    mapOfVariables = NGVariablesUtils.applyVariableOverrides(mapOfVariables, applicableVariableOverrideSets);

    if (serviceConfig.getStageOverrides() != null) {
      List<NGVariable> stageVariablesOverrides = serviceConfig.getStageOverrides().getVariables();
      mapOfVariables = NGVariablesUtils.applyVariableOverrides(mapOfVariables, stageVariablesOverrides);
    }

    // Publishing final override originalVariables against "output" key.
    if (EmptyPredicate.isNotEmpty(mapOfVariables)) {
      outcomeBuilder.variable(YamlTypes.OUTPUT, mapOfVariables);
    }

    List<NGVariableOverrideSetWrapper> variableOverrideSetWrappers =
        serviceConfig.getServiceDefinition().getServiceSpec().getVariableOverrideSets();

    List<NGVariableOverrideSets> variableOverrideSets = variableOverrideSetWrappers == null
        ? new ArrayList<>()
        : variableOverrideSetWrappers.stream().map(NGVariableOverrideSetWrapper::getOverrideSet).collect(toList());

    if (EmptyPredicate.isNotEmpty(variableOverrideSets)) {
      for (NGVariableOverrideSets variableOverrideSet : variableOverrideSets) {
        outcomeBuilder.variableOverrideSet(variableOverrideSet.getIdentifier(),
            VariablesWrapperOutcome.builder()
                .variables(
                    NGVariablesUtils.getMapOfVariables(variableOverrideSet.getVariables(), expressionFunctorToken))
                .build());
      }
    }
  }

  private void handleManifestOutcome(ManifestsOutcome outcome, ServiceOutcomeBuilder outcomeBuilder) {
    List<ManifestOutcome> manifestOutcomeList =
        isNotEmpty(outcome.getManifestOutcomeList()) ? outcome.getManifestOutcomeList() : Collections.emptyList();

    List<ManifestOutcome> manifestOriginalList =
        isNotEmpty(outcome.getManifestOriginalList()) ? outcome.getManifestOriginalList() : Collections.emptyList();

    Map<String, Map<String, Object>> manifestsMap = new HashMap<>();

    for (ManifestOutcome manifestOutcome : manifestOriginalList) {
      addValuesToMap(
          manifestsMap, manifestOutcome.getIdentifier(), RecastOrchestrationUtils.toDocument(manifestOutcome));
    }
    for (ManifestOutcome manifestOutcome : manifestOutcomeList) {
      Map<String, Object> valueMap = new HashMap<>();
      valueMap.put(YamlTypes.OUTPUT, RecastOrchestrationUtils.toDocument(manifestOutcome));
      addValuesToMap(manifestsMap, manifestOutcome.getIdentifier(), valueMap);
    }
    outcomeBuilder.manifests(manifestsMap);

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
      ServiceConfig serviceConfig, NGLogCallback managerLogCallback) {
    ArtifactsOutcomeBuilder artifactsBuilder = ArtifactsOutcome.builder();
    Map<String, Map<String, Object>> artifactsMap = new HashMap<>();
    for (ArtifactOutcome artifactOutcome : artifactOutcomes) {
      if (artifactOutcome.isPrimaryArtifact()) {
        artifactsBuilder.primary(artifactOutcome);

        Map<String, Object> valueMap = new HashMap<>();
        valueMap.put(YamlTypes.OUTPUT, RecastOrchestrationUtils.toDocument(artifactOutcome));
        addValuesToMap(artifactsMap, YamlTypes.PRIMARY_ARTIFACT, valueMap);
      } else {
        artifactsBuilder.sidecar(artifactOutcome.getIdentifier(), artifactOutcome);

        Map<String, Object> valueMap = new HashMap<>();
        valueMap.put(YamlTypes.OUTPUT, RecastOrchestrationUtils.toDocument(artifactOutcome));
        Map<String, Object> sidecarsMap = new HashMap<>();
        sidecarsMap.put(artifactOutcome.getIdentifier(), valueMap);
        addValuesToMap(artifactsMap, YamlTypes.SIDECARS_ARTIFACT_CONFIG, sidecarsMap);
      }
    }
    outcomeBuilder.artifactsResult(artifactsBuilder.build());

    ArtifactListConfig originalArtifacts = serviceConfig.getServiceDefinition().getServiceSpec().getArtifacts();
    if (originalArtifacts != null) {
      List<ArtifactConfig> artifactConfigs =
          ArtifactUtils.convertArtifactListIntoArtifacts(originalArtifacts, managerLogCallback);
      for (ArtifactConfig artifactConfig : artifactConfigs) {
        ArtifactOutcome artifactOutcome =
            ArtifactResponseToOutcomeMapper.toArtifactOutcome(artifactConfig, null, false);
        if (artifactOutcome.isPrimaryArtifact()) {
          addValuesToMap(
              artifactsMap, YamlTypes.PRIMARY_ARTIFACT, RecastOrchestrationUtils.toDocument(artifactOutcome));
        } else {
          Map<String, Object> sidecarsMap = new HashMap<>();
          sidecarsMap.put(artifactOutcome.getIdentifier(), RecastOrchestrationUtils.toDocument(artifactOutcome));
          addValuesToMap(artifactsMap, YamlTypes.SIDECARS_ARTIFACT_CONFIG, sidecarsMap);
        }
      }
    }
    outcomeBuilder.artifacts(artifactsMap);

    List<ArtifactOverrideSetWrapper> artifactOverrideSetsWrappers =
        serviceConfig.getServiceDefinition().getServiceSpec().getArtifactOverrideSets();

    List<ArtifactOverrideSets> artifactOverrideSets = artifactOverrideSetsWrappers == null
        ? new ArrayList<>()
        : artifactOverrideSetsWrappers.stream().map(ArtifactOverrideSetWrapper::getOverrideSet).collect(toList());

    if (EmptyPredicate.isNotEmpty(artifactOverrideSets)) {
      managerLogCallback.saveExecutionLog("Found artifact overrides\n", LogLevel.INFO);
      for (ArtifactOverrideSets artifactOverrideSet : artifactOverrideSets) {
        ArtifactListConfig artifacts = artifactOverrideSet.getArtifacts();
        List<ArtifactConfig> artifactConfigs =
            ArtifactUtils.convertArtifactListIntoArtifacts(artifacts, managerLogCallback);
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
                     .filter(o -> o.getOverrideSet().getIdentifier().equals(useVariableOverrideSet))
                     .findFirst())
          .forEachOrdered(optionalVariableOverrideSets -> {
            if (!optionalVariableOverrideSets.isPresent()) {
              throw new InvalidRequestException("Service Variable Override Set is not defined.");
            }
            variableOverrideSets.addAll(optionalVariableOverrideSets.get().getOverrideSet().getVariables());
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

    if (serviceConfig.getServiceRef() != null && EmptyPredicate.isNotEmpty(serviceConfig.getServiceRef().getValue())) {
      String serviceIdentifier = serviceConfig.getServiceRef().getValue();
      Optional<ServiceEntity> serviceEntity =
          serviceEntityService.get(accountId, orgIdentifier, projectIdentifier, serviceIdentifier, false);
      if (serviceEntity.isPresent()) {
        return serviceEntity.get();
      } else {
        throw new InvalidRequestException("Service with identifier " + serviceIdentifier + " does not exist");
      }
    }

    return ServiceEntity.builder()
        .identifier(serviceConfig.getService().getIdentifier())
        .name(serviceConfig.getService().getName())
        .description(getParameterFieldValue(serviceConfig.getService().getDescription()))
        .projectIdentifier(projectIdentifier)
        .orgIdentifier(orgIdentifier)
        .accountId(accountId)
        .tags(convertToList(serviceConfig.getService().getTags()))
        .build();
  }

  private void addValuesToMap(Map<String, Map<String, Object>> map, String key, Map<String, Object> value) {
    if (map.containsKey(key)) {
      Map<String, Object> alreadyExistedValue = map.get(key);
      alreadyExistedValue.putAll(value);
      map.put(key, alreadyExistedValue);
    } else {
      map.put(key, value);
    }
  }
}
