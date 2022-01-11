/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cdng.creator.plan.service;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.artifact.bean.yaml.ArtifactListConfig;
import io.harness.cdng.artifact.bean.yaml.ArtifactOverrideSets.ArtifactOverrideSetsStepParametersWrapper;
import io.harness.cdng.creator.plan.PlanCreatorConstants;
import io.harness.cdng.creator.plan.manifest.ManifestsPlanCreator;
import io.harness.cdng.creator.plan.stage.DeploymentStageConfig;
import io.harness.cdng.infra.steps.EnvironmentStep;
import io.harness.cdng.infra.steps.InfraSectionStepParameters;
import io.harness.cdng.licenserestriction.EnforcementValidator;
import io.harness.cdng.manifest.yaml.ManifestOverrideSets.ManifestOverrideSetsStepParametersWrapper;
import io.harness.cdng.service.ServiceSpec;
import io.harness.cdng.service.beans.ServiceConfig;
import io.harness.cdng.service.beans.ServiceUseFromStage;
import io.harness.cdng.service.beans.ServiceYaml;
import io.harness.cdng.service.steps.ServiceConfigStep;
import io.harness.cdng.service.steps.ServiceConfigStepParameters;
import io.harness.cdng.service.steps.ServiceDefinitionStep;
import io.harness.cdng.service.steps.ServiceDefinitionStepParameters;
import io.harness.cdng.service.steps.ServiceSpecStep;
import io.harness.cdng.service.steps.ServiceSpecStepParameters;
import io.harness.cdng.service.steps.ServiceStep;
import io.harness.cdng.service.steps.ServiceStepParameters;
import io.harness.cdng.visitor.YamlTypes;
import io.harness.data.structure.EmptyPredicate;
import io.harness.data.structure.UUIDGenerator;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.InvalidRequestException;
import io.harness.plancreator.stages.stage.StageElementConfig;
import io.harness.pms.contracts.advisers.AdviserObtainment;
import io.harness.pms.contracts.advisers.AdviserType;
import io.harness.pms.contracts.facilitators.FacilitatorObtainment;
import io.harness.pms.contracts.facilitators.FacilitatorType;
import io.harness.pms.contracts.plan.Dependency;
import io.harness.pms.contracts.steps.SkipType;
import io.harness.pms.execution.OrchestrationFacilitatorType;
import io.harness.pms.plan.creation.PlanCreatorUtils;
import io.harness.pms.sdk.core.adviser.OrchestrationAdviserTypes;
import io.harness.pms.sdk.core.adviser.success.OnSuccessAdviserParameters;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;
import io.harness.pms.sdk.core.plan.creation.creators.ChildrenPlanCreator;
import io.harness.pms.yaml.DependenciesUtils;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;
import io.harness.pms.yaml.YamlUtils;
import io.harness.serializer.KryoSerializer;

import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@OwnedBy(HarnessTeam.CDC)
public class ServicePlanCreator extends ChildrenPlanCreator<ServiceConfig> {
  @Inject EnforcementValidator enforcementValidator;
  @Inject KryoSerializer kryoSerializer;
  private ServiceConfig actualServiceConfig;
  private String serviceConfigNodeId;

  @Override
  public Class<ServiceConfig> getFieldClass() {
    return ServiceConfig.class;
  }

  @Override
  public Map<String, Set<String>> getSupportedTypes() {
    return Collections.singletonMap(YamlTypes.SERVICE_CONFIG, Collections.singleton(PlanCreatorUtils.ANY_TYPE));
  }

  @Override
  public LinkedHashMap<String, PlanCreationResponse> createPlanForChildrenNodes(
      PlanCreationContext ctx, ServiceConfig serviceConfig) {
    LinkedHashMap<String, PlanCreationResponse> planCreationResponseMap = new LinkedHashMap<>();

    // enforcement validator
    enforcementValidator.validate(ctx.getMetadata().getAccountIdentifier(), ctx.getMetadata().getOrgIdentifier(),
        ctx.getMetadata().getProjectIdentifier(), ctx.getMetadata().getMetadata().getPipelineIdentifier(),
        ctx.getYaml(), ctx.getMetadata().getMetadata().getExecutionUuid());

    YamlField serviceField = ctx.getCurrentField();

    // Fetching infraSectionParameters dependency
    InfraSectionStepParameters infraSectionStepParameters =
        (InfraSectionStepParameters) kryoSerializer.asInflatedObject(
            ctx.getDependency().getMetadataMap().get(YamlTypes.INFRASTRUCTURE_STEP_PARAMETERS).toByteArray());

    YamlNode serviceNode = serviceField.getNode();
    actualServiceConfig = getActualServiceConfig(serviceConfig, serviceField);
    actualServiceConfig = applyUseFromStageOverrides(actualServiceConfig);

    List<String> serviceSpecChildrenIds = new ArrayList<>();
    PlanCreationResponse response;

    Map<String, ByteString> metadataDependency = new HashMap<>();

    boolean createPlanForArtifacts = validateCreatePlanNodeForArtifacts(actualServiceConfig);
    if (createPlanForArtifacts) {
      YamlField artifactYamlField = fetchArtifactYamlField(ctx.getCurrentField(), actualServiceConfig);
      String artifactsPlanNodeId = UUIDGenerator.generateUuid();

      prepareMetadataForArtifactsPlanCreator(artifactsPlanNodeId, actualServiceConfig, metadataDependency);

      Map<String, YamlField> dependenciesMap = new HashMap<>();
      dependenciesMap.put(artifactsPlanNodeId, artifactYamlField);
      planCreationResponseMap.put(artifactsPlanNodeId,
          PlanCreationResponse.builder()
              .dependencies(DependenciesUtils.toDependenciesProto(dependenciesMap)
                                .toBuilder()
                                .putDependencyMetadata(artifactsPlanNodeId,
                                    Dependency.newBuilder().putAllMetadata(metadataDependency).build())
                                .build())
              .build());
      serviceSpecChildrenIds.add(artifactsPlanNodeId);
    }

    final String finalManifestId = "manifests-" + UUIDGenerator.generateUuid();
    response = ManifestsPlanCreator.createPlanForManifestsNode(actualServiceConfig, finalManifestId);
    if (response != null && isNotEmpty(response.getNodes())) {
      serviceSpecChildrenIds.add(finalManifestId);
      planCreationResponseMap.put(finalManifestId, response);
    }

    serviceConfigNodeId = serviceNode.getUuid();
    String serviceDefinitionNodeId = addServiceDefinitionNode(
        actualServiceConfig, planCreationResponseMap, serviceSpecChildrenIds, infraSectionStepParameters);
    addServiceNode(actualServiceConfig, planCreationResponseMap, serviceDefinitionNodeId);

    return planCreationResponseMap;
  }

  @Override
  public PlanNode createPlanForParentNode(
      PlanCreationContext ctx, ServiceConfig serviceConfig, List<String> childrenNodeIds) {
    YamlField serviceField = ctx.getCurrentField();
    YamlNode serviceNode = serviceField.getNode();

    actualServiceConfig = getActualServiceConfig(serviceConfig, serviceField);
    actualServiceConfig = applyUseFromStageOverrides(actualServiceConfig);

    String serviceNodeUuId = "service-" + serviceConfigNodeId;
    ServiceConfigStepParameters serviceConfigStepParameters = ServiceConfigStepParameters.builder()
                                                                  .useFromStage(actualServiceConfig.getUseFromStage())
                                                                  .serviceRef(actualServiceConfig.getServiceRef())
                                                                  .childNodeId(serviceNodeUuId)
                                                                  .build();
    return PlanNode.builder()
        .uuid(serviceConfigNodeId)
        .stepType(ServiceConfigStep.STEP_TYPE)
        .name(PlanCreatorConstants.SERVICE_NODE_NAME)
        .identifier(YamlTypes.SERVICE_CONFIG)
        .stepParameters(serviceConfigStepParameters)
        .facilitatorObtainment(
            FacilitatorObtainment.newBuilder()
                .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.CHILD).build())
                .build())
        .adviserObtainments(getAdviserObtainmentFromMetaData(serviceNode))
        .skipExpressionChain(false)
        .build();
  }

  public void prepareMetadataForArtifactsPlanCreator(
      String artifactsPlanNodeId, ServiceConfig actualServiceConfig, Map<String, ByteString> metadataDependency) {
    metadataDependency.put(YamlTypes.UUID, ByteString.copyFrom(kryoSerializer.asDeflatedBytes(artifactsPlanNodeId)));
    // TODO: Find an efficient way to not pass whole service config
    metadataDependency.put(
        YamlTypes.SERVICE_CONFIG, ByteString.copyFrom(kryoSerializer.asDeflatedBytes(actualServiceConfig)));
  }

  public YamlField fetchArtifactYamlField(YamlField serviceField, ServiceConfig actualServiceConfig) {
    if (actualServiceConfig.getUseFromStage() == null) {
      return serviceField.getNode()
          .getField(YamlTypes.SERVICE_DEFINITION)
          .getNode()
          .getField(YamlTypes.SPEC)
          .getNode()
          .getField(YamlTypes.ARTIFACT_LIST_CONFIG);
    } else {
      // pass the original stage artifacts yaml field
      String stage = actualServiceConfig.getUseFromStage().getStage();
      YamlField stageYamlField = PlanCreatorUtils.getStageConfig(serviceField, stage);

      return stageYamlField.getNode()
          .getField(YamlTypes.SPEC)
          .getNode()
          .getField(YamlTypes.SERVICE_CONFIG)
          .getNode()
          .getField(YamlTypes.SERVICE_DEFINITION)
          .getNode()
          .getField(YamlTypes.SPEC)
          .getNode()
          .getField(YamlTypes.ARTIFACT_LIST_CONFIG);
    }
  }

  public boolean validateCreatePlanNodeForArtifacts(ServiceConfig actualServiceConfig) {
    ArtifactListConfig artifactListConfig = actualServiceConfig.getServiceDefinition().getServiceSpec().getArtifacts();

    // Contains either primary artifacts or side-car artifacts
    if (artifactListConfig != null) {
      if (artifactListConfig.getPrimary() != null || EmptyPredicate.isNotEmpty(artifactListConfig.getSidecars())) {
        return true;
      }
    }

    if (actualServiceConfig.getStageOverrides() != null
        && actualServiceConfig.getStageOverrides().getArtifacts() != null) {
      if (actualServiceConfig.getStageOverrides().getArtifacts().getPrimary() != null
          || EmptyPredicate.isNotEmpty(actualServiceConfig.getStageOverrides().getArtifacts().getSidecars())) {
        return true;
      }
    }

    return false;
  }

  private String addServiceNode(ServiceConfig actualServiceConfig,
      LinkedHashMap<String, PlanCreationResponse> planCreationResponseMap, String serviceDefinitionNodeId) {
    ServiceStepParameters stepParameters = ServiceStepParameters.fromServiceConfig(actualServiceConfig);
    PlanNode node =
        PlanNode.builder()
            .uuid("service-" + serviceConfigNodeId)
            .stepType(ServiceStep.STEP_TYPE)
            .name(PlanCreatorConstants.SERVICE_NODE_NAME)
            .identifier(YamlTypes.SERVICE_ENTITY)
            .stepParameters(stepParameters)
            .facilitatorObtainment(
                FacilitatorObtainment.newBuilder()
                    .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.SYNC).build())
                    .build())
            .adviserObtainment(
                AdviserObtainment.newBuilder()
                    .setType(AdviserType.newBuilder().setType(OrchestrationAdviserTypes.ON_SUCCESS.name()).build())
                    .setParameters(ByteString.copyFrom(kryoSerializer.asBytes(
                        OnSuccessAdviserParameters.builder().nextNodeId(serviceDefinitionNodeId).build())))
                    .build())
            .skipExpressionChain(false)
            .skipGraphType(SkipType.SKIP_TREE)
            .build();
    planCreationResponseMap.put(node.getUuid(), PlanCreationResponse.builder().node(node.getUuid(), node).build());
    return node.getUuid();
  }

  private String addServiceDefinitionNode(ServiceConfig actualServiceConfig,
      LinkedHashMap<String, PlanCreationResponse> planCreationResponseMap, List<String> serviceSpecChildrenIds,
      InfraSectionStepParameters infraSectionStepParameters) {
    String serviceSpecNodeId =
        addServiceSpecNode(actualServiceConfig, planCreationResponseMap, serviceConfigNodeId, serviceSpecChildrenIds);
    String environmentStepNodeId =
        addEnvironmentStepNode(infraSectionStepParameters, planCreationResponseMap, kryoSerializer, serviceSpecNodeId);
    ServiceDefinitionStepParameters stepParameters =
        ServiceDefinitionStepParameters.builder()
            .type(actualServiceConfig.getServiceDefinition().getType().getYamlName())
            .childNodeId(environmentStepNodeId)
            .build();
    PlanNode node =
        PlanNode.builder()
            .uuid("service-definition-" + serviceConfigNodeId)
            .stepType(ServiceDefinitionStep.STEP_TYPE)
            .name(PlanCreatorConstants.SERVICE_DEFINITION_NODE_NAME)
            .identifier(YamlTypes.SERVICE_DEFINITION)
            .stepParameters(stepParameters)
            .facilitatorObtainment(
                FacilitatorObtainment.newBuilder()
                    .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.CHILD).build())
                    .build())
            .skipExpressionChain(false)
            .skipGraphType(SkipType.SKIP_TREE)
            .build();
    planCreationResponseMap.put(node.getUuid(), PlanCreationResponse.builder().node(node.getUuid(), node).build());
    return node.getUuid();
  }

  private String addEnvironmentStepNode(InfraSectionStepParameters infraSectionStepParameters,
      LinkedHashMap<String, PlanCreationResponse> planCreationResponseMap, KryoSerializer kryoSerializer,
      String serviceSpecNodeUuid) {
    PlanNode node =
        PlanNode.builder()
            .uuid(UUIDGenerator.generateUuid())
            .stepType(EnvironmentStep.STEP_TYPE)
            .name(PlanCreatorConstants.ENVIRONMENT_NODE_NAME)
            .identifier(YamlTypes.ENVIRONMENT_YAML)
            .stepParameters(infraSectionStepParameters)
            .facilitatorObtainment(
                FacilitatorObtainment.newBuilder()
                    .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.SYNC).build())
                    .build())
            .adviserObtainment(
                AdviserObtainment.newBuilder()
                    .setType(AdviserType.newBuilder().setType(OrchestrationAdviserTypes.ON_SUCCESS.name()).build())
                    .setParameters(ByteString.copyFrom(kryoSerializer.asBytes(
                        OnSuccessAdviserParameters.builder().nextNodeId(serviceSpecNodeUuid).build())))
                    .build())
            .skipExpressionChain(false)
            .build();
    planCreationResponseMap.put(node.getUuid(), PlanCreationResponse.builder().node(node.getUuid(), node).build());
    return node.getUuid();
  }

  private String addServiceSpecNode(ServiceConfig actualServiceConfig,
      LinkedHashMap<String, PlanCreationResponse> planCreationResponseMap, String serviceNodeId,
      List<String> serviceSpecChildrenIds) {
    ServiceSpec serviceSpec = actualServiceConfig.getServiceDefinition().getServiceSpec();
    ServiceSpecStepParameters stepParameters =
        ServiceSpecStepParameters.builder()
            .originalVariables(ParameterField.createValueField(serviceSpec.getVariables()))
            .originalVariableOverrideSets(ParameterField.createValueField(serviceSpec.getVariableOverrideSets()))
            .stageOverrideVariables(actualServiceConfig.getStageOverrides() == null
                    ? null
                    : ParameterField.createValueField(actualServiceConfig.getStageOverrides().getVariables()))
            .stageOverridesUseVariableOverrideSets(actualServiceConfig.getStageOverrides() == null
                    ? null
                    : actualServiceConfig.getStageOverrides().getUseVariableOverrideSets())
            .artifactOverrideSets(serviceSpec.getArtifactOverrideSets() == null
                    ? null
                    : serviceSpec.getArtifactOverrideSets().stream().collect(Collectors.toMap(overrideSet
                        -> overrideSet.getOverrideSet().getIdentifier(),
                        overrideSet
                        -> ArtifactOverrideSetsStepParametersWrapper.fromArtifactOverrideSets(
                            overrideSet.getOverrideSet()))))
            .manifestOverrideSets(serviceSpec.getManifestOverrideSets() == null
                    ? null
                    : serviceSpec.getManifestOverrideSets().stream().collect(Collectors.toMap(overrideSet
                        -> overrideSet.getOverrideSet().getIdentifier(),
                        overrideSet
                        -> ManifestOverrideSetsStepParametersWrapper.fromManifestOverrideSets(
                            overrideSet.getOverrideSet()))))
            .childrenNodeIds(serviceSpecChildrenIds)
            .build();
    PlanNode node =
        PlanNode.builder()
            .uuid("service-spec-" + serviceNodeId)
            .stepType(ServiceSpecStep.STEP_TYPE)
            .name(PlanCreatorConstants.SERVICE_SPEC_NODE_NAME)
            .identifier(YamlTypes.SERVICE_SPEC)
            .stepParameters(stepParameters)
            .facilitatorObtainment(
                FacilitatorObtainment.newBuilder()
                    .setType(EmptyPredicate.isEmpty(serviceSpecChildrenIds)
                            ? FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.SYNC).build()
                            : FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.CHILDREN).build())
                    .build())
            .skipExpressionChain(false)
            .build();
    planCreationResponseMap.put(node.getUuid(), PlanCreationResponse.builder().node(node.getUuid(), node).build());
    return node.getUuid();
  }

  private ServiceConfig applyUseFromStageOverrides(ServiceConfig actualServiceConfig) {
    ServiceYaml actualServiceYaml = actualServiceConfig.getService();
    if (actualServiceYaml != null && EmptyPredicate.isEmpty(actualServiceYaml.getName())) {
      actualServiceYaml.setName(actualServiceYaml.getIdentifier());
    }

    // Apply useFromStage overrides.
    ServiceConfig serviceOverrides;
    if (actualServiceConfig.getUseFromStage() != null) {
      ServiceUseFromStage.Overrides overrides = actualServiceConfig.getUseFromStage().getOverrides();
      if (overrides != null) {
        ServiceYaml overriddenEntity =
            ServiceYaml.builder().name(overrides.getName().getValue()).description(overrides.getDescription()).build();
        serviceOverrides = ServiceConfig.builder().service(overriddenEntity).build();
        actualServiceConfig = actualServiceConfig.applyOverrides(serviceOverrides);
      }
    }
    return actualServiceConfig;
  }

  /** Method returns actual Service object by resolving useFromStage if present. */
  private ServiceConfig getActualServiceConfig(ServiceConfig serviceConfig, YamlField serviceField) {
    if (serviceConfig.getUseFromStage() == null) {
      if (serviceConfig.getServiceDefinition() == null) {
        throw new InvalidArgumentsException(
            "Either Service Definition or useFromStage should be present in the given stage");
      }
      return serviceConfig;
    }

    if (serviceConfig.getServiceDefinition() != null) {
      throw new InvalidArgumentsException("Service definition should not exist along with useFromStage");
    }

    String stage = serviceConfig.getUseFromStage().getStage();
    if (stage == null) {
      throw new InvalidRequestException("Stage identifier not present in useFromStage");
    }

    try {
      //  Add validation for not chaining of stages
      StageElementConfig stageElementConfig = YamlUtils.read(
          PlanCreatorUtils.getStageConfig(serviceField, stage).getNode().toString(), StageElementConfig.class);
      DeploymentStageConfig deploymentStage = (DeploymentStageConfig) stageElementConfig.getStageType();
      if (deploymentStage != null) {
        return serviceConfig.applyUseFromStage(deploymentStage.getServiceConfig());
      } else {
        throw new InvalidArgumentsException("Stage identifier given in useFromStage doesn't exist");
      }
    } catch (IOException ex) {
      throw new InvalidRequestException("Cannot parse stage: " + stage);
    }
  }

  private List<AdviserObtainment> getAdviserObtainmentFromMetaData(YamlNode currentNode) {
    List<AdviserObtainment> adviserObtainments = new ArrayList<>();
    if (currentNode != null) {
      YamlField siblingField = currentNode.nextSiblingNodeFromParentObject("infrastructure");
      if (siblingField != null && siblingField.getNode().getUuid() != null) {
        adviserObtainments.add(
            AdviserObtainment.newBuilder()
                .setType(AdviserType.newBuilder().setType(OrchestrationAdviserTypes.ON_SUCCESS.name()).build())
                .setParameters(ByteString.copyFrom(kryoSerializer.asBytes(
                    OnSuccessAdviserParameters.builder().nextNodeId(siblingField.getNode().getUuid()).build())))
                .build());
      }
    }
    return adviserObtainments;
  }
}
