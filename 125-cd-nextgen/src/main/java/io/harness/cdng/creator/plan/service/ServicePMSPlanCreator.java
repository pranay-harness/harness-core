/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cdng.creator.plan.service;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.artifact.bean.yaml.ArtifactOverrideSets.ArtifactOverrideSetsStepParametersWrapper;
import io.harness.cdng.creator.plan.PlanCreatorConstants;
import io.harness.cdng.creator.plan.artifact.ArtifactsPlanCreator;
import io.harness.cdng.creator.plan.manifest.ManifestsPlanCreator;
import io.harness.cdng.creator.plan.stage.DeploymentStageConfig;
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
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.InvalidRequestException;
import io.harness.plancreator.stages.stage.StageElementConfig;
import io.harness.pms.contracts.advisers.AdviserObtainment;
import io.harness.pms.contracts.advisers.AdviserType;
import io.harness.pms.contracts.facilitators.FacilitatorObtainment;
import io.harness.pms.contracts.facilitators.FacilitatorType;
import io.harness.pms.contracts.steps.SkipType;
import io.harness.pms.execution.OrchestrationFacilitatorType;
import io.harness.pms.plan.creation.PlanCreatorUtils;
import io.harness.pms.sdk.core.adviser.OrchestrationAdviserTypes;
import io.harness.pms.sdk.core.adviser.success.OnSuccessAdviserParameters;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;
import io.harness.pms.yaml.YamlUtils;
import io.harness.serializer.KryoSerializer;

import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.CDC)
@UtilityClass
public class ServicePMSPlanCreator {
  public PlanCreationResponse createPlanForServiceNode(
      YamlField serviceField, ServiceConfig serviceConfig, KryoSerializer kryoSerializer) {
    YamlNode serviceNode = serviceField.getNode();
    ServiceConfig actualServiceConfig = getActualServiceConfig(serviceConfig, serviceField);
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

    Map<String, PlanNode> planNodes = new HashMap<>();
    List<String> serviceSpecChildrenIds = new ArrayList<>();
    PlanCreationResponse response = ArtifactsPlanCreator.createPlanForArtifactsNode(actualServiceConfig);
    if (response != null && EmptyPredicate.isNotEmpty(response.getNodes())) {
      planNodes.putAll(response.getNodes());
      if (EmptyPredicate.isNotEmpty(response.getStartingNodeId())) {
        serviceSpecChildrenIds.add(response.getStartingNodeId());
      }
    }

    response = ManifestsPlanCreator.createPlanForManifestsNode(actualServiceConfig);
    if (response != null && EmptyPredicate.isNotEmpty(response.getNodes())) {
      planNodes.putAll(response.getNodes());
      if (EmptyPredicate.isNotEmpty(response.getStartingNodeId())) {
        serviceSpecChildrenIds.add(response.getStartingNodeId());
      }
    }

    String serviceYamlNodeId = serviceNode.getUuid();
    String serviceDefinitionNodeId =
        addServiceDefinitionNode(actualServiceConfig, planNodes, serviceYamlNodeId, serviceSpecChildrenIds);
    String serviceNodeId =
        addServiceNode(actualServiceConfig, planNodes, serviceYamlNodeId, serviceDefinitionNodeId, kryoSerializer);

    ServiceConfigStepParameters serviceConfigStepParameters = ServiceConfigStepParameters.builder()
                                                                  .useFromStage(actualServiceConfig.getUseFromStage())
                                                                  .serviceRef(actualServiceConfig.getServiceRef())
                                                                  .childNodeId(serviceNodeId)
                                                                  .build();
    PlanNode serviceConfigPlanNode =
        PlanNode.builder()
            .uuid(serviceNode.getUuid())
            .stepType(ServiceConfigStep.STEP_TYPE)
            .name(PlanCreatorConstants.SERVICE_NODE_NAME)
            .identifier(YamlTypes.SERVICE_CONFIG)
            .stepParameters(serviceConfigStepParameters)
            .facilitatorObtainment(
                FacilitatorObtainment.newBuilder()
                    .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.CHILD).build())
                    .build())
            .adviserObtainments(getAdviserObtainmentFromMetaData(serviceNode, kryoSerializer))
            .skipExpressionChain(false)
            .build();
    planNodes.put(serviceConfigPlanNode.getUuid(), serviceConfigPlanNode);
    return PlanCreationResponse.builder().nodes(planNodes).startingNodeId(serviceConfigPlanNode.getUuid()).build();
  }

  private String addServiceNode(ServiceConfig actualServiceConfig, Map<String, PlanNode> planNodes,
      String serviceNodeId, String serviceDefinitionNodeId, KryoSerializer kryoSerializer) {
    ServiceStepParameters stepParameters = ServiceStepParameters.fromServiceConfig(actualServiceConfig);
    PlanNode node =
        PlanNode.builder()
            .uuid("service-" + serviceNodeId)
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
    planNodes.put(node.getUuid(), node);
    return node.getUuid();
  }

  private String addServiceDefinitionNode(ServiceConfig actualServiceConfig, Map<String, PlanNode> planNodes,
      String serviceNodeId, List<String> serviceSpecChildrenIds) {
    String serviceSpecNodeId =
        addServiceSpecNode(actualServiceConfig, planNodes, serviceNodeId, serviceSpecChildrenIds);
    ServiceDefinitionStepParameters stepParameters =
        ServiceDefinitionStepParameters.builder()
            .type(actualServiceConfig.getServiceDefinition().getType().getYamlName())
            .childNodeId(serviceSpecNodeId)
            .build();
    PlanNode node =
        PlanNode.builder()
            .uuid("service-definition-" + serviceNodeId)
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
    planNodes.put(node.getUuid(), node);
    return node.getUuid();
  }

  private String addServiceSpecNode(ServiceConfig actualServiceConfig, Map<String, PlanNode> planNodes,
      String serviceNodeId, List<String> serviceSpecChildrenIds) {
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
    planNodes.put(node.getUuid(), node);
    return node.getUuid();
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

  private List<AdviserObtainment> getAdviserObtainmentFromMetaData(
      YamlNode currentNode, KryoSerializer kryoSerializer) {
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
