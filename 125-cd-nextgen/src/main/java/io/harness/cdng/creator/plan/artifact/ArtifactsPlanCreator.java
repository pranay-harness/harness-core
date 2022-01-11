/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cdng.creator.plan.artifact;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.artifact.bean.ArtifactConfig;
import io.harness.cdng.artifact.bean.yaml.ArtifactListConfig;
import io.harness.cdng.artifact.bean.yaml.ArtifactOverrideSetWrapper;
import io.harness.cdng.artifact.bean.yaml.ArtifactOverrideSets;
import io.harness.cdng.artifact.bean.yaml.PrimaryArtifact;
import io.harness.cdng.artifact.bean.yaml.SidecarArtifact;
import io.harness.cdng.artifact.bean.yaml.SidecarArtifactWrapper;
import io.harness.cdng.artifact.steps.ArtifactStep;
import io.harness.cdng.artifact.steps.ArtifactStepParameters;
import io.harness.cdng.artifact.steps.ArtifactStepParameters.ArtifactStepParametersBuilder;
import io.harness.cdng.artifact.steps.ArtifactsStep;
import io.harness.cdng.artifact.steps.SidecarsStep;
import io.harness.cdng.creator.plan.PlanCreatorConstants;
import io.harness.cdng.service.beans.ServiceConfig;
import io.harness.cdng.utilities.PrimaryArtifactsUtility;
import io.harness.cdng.visitor.YamlTypes;
import io.harness.data.structure.EmptyPredicate;
import io.harness.data.structure.UUIDGenerator;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.contracts.facilitators.FacilitatorObtainment;
import io.harness.pms.contracts.facilitators.FacilitatorType;
import io.harness.pms.contracts.plan.Dependency;
import io.harness.pms.contracts.plan.YamlUpdates;
import io.harness.pms.execution.OrchestrationFacilitatorType;
import io.harness.pms.plan.creation.PlanCreatorUtils;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;
import io.harness.pms.sdk.core.plan.creation.creators.ChildrenPlanCreator;
import io.harness.pms.yaml.DependenciesUtils;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.YamlField;
import io.harness.serializer.KryoSerializer;
import io.harness.steps.fork.ForkStepParameters;

import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@OwnedBy(HarnessTeam.CDC)
public class ArtifactsPlanCreator extends ChildrenPlanCreator<ArtifactListConfig> {
  @Inject KryoSerializer kryoSerializer;

  private PlanCreationResponse createPlanForSidecarsNode(String sideCarsPlanNodeId, ArtifactList artifactList) {
    Map<String, PlanNode> planNodes = new HashMap<>();
    for (Map.Entry<String, ArtifactInfo> entry : artifactList.getSidecars().entrySet()) {
      PlanNode sideCarPlanNode = createPlanForArtifactNode(entry.getKey(), entry.getValue());
      planNodes.put(sideCarPlanNode.getUuid(), sideCarPlanNode);
    }

    ForkStepParameters stepParameters =
        ForkStepParameters.builder()
            .parallelNodeIds(planNodes.values().stream().map(PlanNode::getUuid).collect(Collectors.toList()))
            .build();
    PlanNode sidecarsNode =
        PlanNode.builder()
            .uuid(sideCarsPlanNodeId)
            .stepType(SidecarsStep.STEP_TYPE)
            .name(PlanCreatorConstants.SIDECARS_NODE_NAME)
            .identifier(YamlTypes.SIDECARS_ARTIFACT_CONFIG)
            .stepParameters(stepParameters)
            .facilitatorObtainment(
                FacilitatorObtainment.newBuilder()
                    .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.CHILDREN).build())
                    .build())
            .skipExpressionChain(false)
            .build();
    planNodes.put(sidecarsNode.getUuid(), sidecarsNode);
    return PlanCreationResponse.builder().nodes(planNodes).build();
  }

  private PlanNode createPlanForArtifactNode(String identifier, ArtifactInfo artifactInfo) {
    return PlanNode.builder()
        .uuid(UUIDGenerator.generateUuid())
        .stepType(ArtifactStep.STEP_TYPE)
        .name(PlanCreatorConstants.ARTIFACT_NODE_NAME)
        .identifier(identifier)
        .stepParameters(artifactInfo.getParams())
        .facilitatorObtainment(
            FacilitatorObtainment.newBuilder()
                .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.TASK).build())
                .build())
        .skipExpressionChain(false)
        .build();
  }

  @Override
  public Class<ArtifactListConfig> getFieldClass() {
    return ArtifactListConfig.class;
  }

  @Override
  public Map<String, Set<String>> getSupportedTypes() {
    return Collections.singletonMap(YamlTypes.ARTIFACT_LIST_CONFIG, Collections.singleton(PlanCreatorUtils.ANY_TYPE));
  }

  public Map<String, ByteString> prepareMetadataForPrimaryArtifactPlanCreator(
      String primaryId, ArtifactStepParameters params) {
    Map<String, ByteString> metadataDependency = new HashMap<>();
    metadataDependency.put(YamlTypes.UUID, ByteString.copyFrom(kryoSerializer.asDeflatedBytes(primaryId)));
    // TODO: Find an efficient way to not pass whole service config
    metadataDependency.put(
        YamlTypes.PRIMARY_STEP_PARAMETERS, ByteString.copyFrom(kryoSerializer.asDeflatedBytes(params)));
    return metadataDependency;
  }

  @Override
  public LinkedHashMap<String, PlanCreationResponse> createPlanForChildrenNodes(
      PlanCreationContext ctx, ArtifactListConfig config) {
    LinkedHashMap<String, PlanCreationResponse> planCreationResponseMap = new LinkedHashMap<>();

    String artifactsId = (String) kryoSerializer.asInflatedObject(
        ctx.getDependency().getMetadataMap().get(YamlTypes.UUID).toByteArray());
    ServiceConfig serviceConfig = (ServiceConfig) kryoSerializer.asInflatedObject(
        ctx.getDependency().getMetadataMap().get(YamlTypes.SERVICE_CONFIG).toByteArray());

    ArtifactListConfig artifactListConfig = serviceConfig.getServiceDefinition().getServiceSpec().getArtifacts();
    ArtifactListBuilder artifactListBuilder = new ArtifactListBuilder(artifactListConfig);
    artifactListBuilder.addOverrideSets(serviceConfig);
    artifactListBuilder.addStageOverrides(serviceConfig);
    ArtifactList artifactList = artifactListBuilder.build();
    if (artifactList.getPrimary() == null && EmptyPredicate.isEmpty(artifactList.getSidecars())) {
      return planCreationResponseMap;
    }

    if (artifactList.getPrimary() != null) {
      String primaryPlanNodeId =
          addDependenciesForPrimaryNode(ctx.getCurrentField(), artifactList.getPrimary(), planCreationResponseMap);
    }
    if (EmptyPredicate.isNotEmpty(artifactList.getSidecars())) {
      final String sideCarsPlanNodeId = "sidecars-" + artifactsId;
      PlanCreationResponse sideCarsPlanCreationResponse = createPlanForSidecarsNode(sideCarsPlanNodeId, artifactList);
      planCreationResponseMap.put(sideCarsPlanNodeId, sideCarsPlanCreationResponse);
    }
    return planCreationResponseMap;
  }

  private String addDependenciesForPrimaryNode(YamlField artifactField, ArtifactInfo primaryInfo,
      LinkedHashMap<String, PlanCreationResponse> planCreationResponseMap) {
    YamlUpdates.Builder yamlUpdates = YamlUpdates.newBuilder(); //.build()
    YamlField primaryYamlField =
        PrimaryArtifactsUtility.fetchPrimaryArtifactYamlFieldAndSetYamlUpdate(artifactField, yamlUpdates);

    String primaryId = primaryYamlField.getNode().getUuid();
    Map<String, ByteString> metadataDependency =
        prepareMetadataForPrimaryArtifactPlanCreator(primaryId, primaryInfo.getParams());

    Map<String, YamlField> dependenciesMap = new HashMap<>();
    dependenciesMap.put(primaryId, primaryYamlField);
    PlanCreationResponse.PlanCreationResponseBuilder primaryPlanCreationResponse =
        PlanCreationResponse.builder().dependencies(
            DependenciesUtils.toDependenciesProto(dependenciesMap)
                .toBuilder()
                .putDependencyMetadata(primaryId, Dependency.newBuilder().putAllMetadata(metadataDependency).build())
                .build());
    if (yamlUpdates.getFqnToYamlCount() > 0) {
      primaryPlanCreationResponse.yamlUpdates(yamlUpdates.build());
    }
    planCreationResponseMap.put(primaryId, primaryPlanCreationResponse.build());
    return primaryId;
  }

  @Override
  public PlanNode createPlanForParentNode(
      PlanCreationContext ctx, ArtifactListConfig config, List<String> childrenNodeIds) {
    String artifactsId = (String) kryoSerializer.asInflatedObject(
        ctx.getDependency().getMetadataMap().get(YamlTypes.UUID).toByteArray());

    ForkStepParameters stepParameters = ForkStepParameters.builder().parallelNodeIds(childrenNodeIds).build();
    return PlanNode.builder()
        .uuid(artifactsId)
        .stepType(ArtifactsStep.STEP_TYPE)
        .name(PlanCreatorConstants.ARTIFACTS_NODE_NAME)
        .identifier(YamlTypes.ARTIFACT_LIST_CONFIG)
        .stepParameters(stepParameters)
        .facilitatorObtainment(
            FacilitatorObtainment.newBuilder()
                .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.CHILDREN).build())
                .build())
        .skipExpressionChain(false)
        .build();
  }

  @Value
  private static class ArtifactList {
    ArtifactInfo primary;
    Map<String, ArtifactInfo> sidecars;
  }

  @Data
  @FieldDefaults(level = AccessLevel.PRIVATE)
  private static class ArtifactListBuilder {
    ArtifactInfoBuilder primary;
    Map<String, ArtifactInfoBuilder> sidecars;

    ArtifactListBuilder(ArtifactListConfig artifactListConfig) {
      if (artifactListConfig == null) {
        this.primary = null;
        this.sidecars = null;
        return;
      }

      PrimaryArtifact primarySpecWrapper = artifactListConfig.getPrimary();
      if (primarySpecWrapper != null) {
        this.primary = new ArtifactInfoBuilder(ArtifactStepParameters.builder()
                                                   .type(primarySpecWrapper.getSourceType())
                                                   .spec(primarySpecWrapper.getSpec()));
      } else {
        this.primary = null;
      }

      this.sidecars = new HashMap<>();
      if (EmptyPredicate.isNotEmpty(artifactListConfig.getSidecars())) {
        artifactListConfig.getSidecars().forEach(sc -> {
          SidecarArtifact sidecar = sc.getSidecar();
          this.sidecars.put(sidecar.getIdentifier(),
              new ArtifactInfoBuilder(ArtifactStepParameters.builder()
                                          .identifier(sidecar.getIdentifier())
                                          .type(sidecar.getSourceType())
                                          .spec(sidecar.getSpec())));
        });
      }
    }

    ArtifactList build() {
      return new ArtifactList(primary == null ? null : primary.build(),
          sidecars == null
              ? null
              : sidecars.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().build())));
    }

    void addOverrideSets(ServiceConfig serviceConfig) {
      if (serviceConfig.getStageOverrides() == null
          || ParameterField.isNull(serviceConfig.getStageOverrides().getUseArtifactOverrideSets())) {
        return;
      }

      for (String useArtifactOverrideSet : serviceConfig.getStageOverrides().getUseArtifactOverrideSets().getValue()) {
        List<ArtifactOverrideSets> artifactOverrideSetsList =
            serviceConfig.getServiceDefinition()
                .getServiceSpec()
                .getArtifactOverrideSets()
                .stream()
                .map(ArtifactOverrideSetWrapper::getOverrideSet)
                .filter(overrideSet -> overrideSet.getIdentifier().equals(useArtifactOverrideSet))
                .collect(Collectors.toList());
        if (artifactOverrideSetsList.size() == 0) {
          throw new InvalidRequestException(
              String.format("Invalid identifier [%s] in artifact override sets", useArtifactOverrideSet));
        }
        if (artifactOverrideSetsList.size() > 1) {
          throw new InvalidRequestException(
              String.format("Duplicate identifier [%s] in artifact override sets", useArtifactOverrideSet));
        }

        ArtifactListConfig artifactListConfig = artifactOverrideSetsList.get(0).getArtifacts();
        addOverrides(artifactListConfig, ArtifactStepParametersBuilder::overrideSet);
      }
    }

    void addStageOverrides(ServiceConfig serviceConfig) {
      if (serviceConfig.getStageOverrides() == null || serviceConfig.getStageOverrides().getArtifacts() == null) {
        return;
      }
      ArtifactListConfig artifactListConfig = serviceConfig.getStageOverrides().getArtifacts();
      addOverrides(artifactListConfig, ArtifactStepParametersBuilder::stageOverride);
    }

    private void addOverrides(
        ArtifactListConfig artifactListConfig, BiConsumer<ArtifactStepParametersBuilder, ArtifactConfig> consumer) {
      PrimaryArtifact primarySpecWrapper = artifactListConfig.getPrimary();
      if (primarySpecWrapper != null) {
        if (primary == null) {
          primary = new ArtifactInfoBuilder(ArtifactStepParameters.builder());
        }
        consumer.accept(primary.getBuilder(), primarySpecWrapper.getSpec());
      }

      if (EmptyPredicate.isNotEmpty(artifactListConfig.getSidecars())) {
        for (SidecarArtifactWrapper sidecarWrapper : artifactListConfig.getSidecars()) {
          SidecarArtifact sidecar = sidecarWrapper.getSidecar();
          ArtifactInfoBuilder artifactInfoBuilder = sidecars.computeIfAbsent(sidecar.getIdentifier(),
              identifier -> new ArtifactInfoBuilder(ArtifactStepParameters.builder().identifier(identifier)));
          consumer.accept(artifactInfoBuilder.getBuilder(), sidecar.getSpec());
        }
      }
    }
  }

  @Value
  private static class ArtifactInfo {
    ArtifactStepParameters params;
  }

  @Value
  private static class ArtifactInfoBuilder {
    ArtifactStepParametersBuilder builder;

    ArtifactInfo build() {
      return new ArtifactInfo(builder.build());
    }
  }
}
