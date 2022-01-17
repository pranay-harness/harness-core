/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cdng.creator.plan.artifact;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.artifact.bean.yaml.SidecarsListWrapper;
import io.harness.cdng.artifact.steps.SidecarsStep;
import io.harness.cdng.creator.plan.PlanCreatorConstants;
import io.harness.cdng.utilities.SideCarsListArtifactsUtility;
import io.harness.cdng.visitor.YamlTypes;
import io.harness.data.structure.UUIDGenerator;
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
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.yaml.DependenciesUtils;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;
import io.harness.serializer.KryoSerializer;
import io.harness.steps.fork.ForkStepParameters;

import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import java.util.*;
import java.util.stream.Collectors;

@OwnedBy(HarnessTeam.CDC)
public class SideCarListPlanCreator extends ChildrenPlanCreator<SidecarsListWrapper> {
  @Inject KryoSerializer kryoSerializer;

  //  private PlanCreationResponse createPlanForSidecarsNode(String sideCarsPlanNodeId, ArtifactList artifactList) {
  //    Map<String, PlanNode> planNodes = new HashMap<>();
  //    for (Map.Entry<String, ArtifactInfo> entry : artifactList.getSidecars().entrySet()) {
  //      PlanNode sideCarPlanNode = createPlanForArtifactNode(entry.getKey(), entry.getValue());
  //      planNodes.put(sideCarPlanNode.getUuid(), sideCarPlanNode);
  //    }
  //
  //    ForkStepParameters stepParameters =
  //        ForkStepParameters.builder()
  //            .parallelNodeIds(planNodes.values().stream().map(PlanNode::getUuid).collect(Collectors.toList()))
  //            .build();
  //    PlanNode sidecarsNode =
  //        PlanNode.builder()
  //            .uuid(sideCarsPlanNodeId)
  //            .stepType(SidecarsStep.STEP_TYPE)
  //            .name(PlanCreatorConstants.SIDECARS_NODE_NAME)
  //            .identifier(YamlTypes.SIDECARS_ARTIFACT_CONFIG)
  //            .stepParameters(stepParameters)
  //            .facilitatorObtainment(
  //                FacilitatorObtainment.newBuilder()
  //                    .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.CHILDREN).build())
  //                    .build())
  //            .skipExpressionChain(false)
  //            .build();
  //    planNodes.put(sidecarsNode.getUuid(), sidecarsNode);
  //    return PlanCreationResponse.builder().nodes(planNodes).build();
  //  }

  @Override
  public Class<SidecarsListWrapper> getFieldClass() {
    return SidecarsListWrapper.class;
  }

  @Override
  public Map<String, Set<String>> getSupportedTypes() {
    return Collections.singletonMap(
        YamlTypes.SIDECARS_ARTIFACT_CONFIG, Collections.singleton(PlanCreatorUtils.ANY_TYPE));
  }

  @Override
  public LinkedHashMap<String, PlanCreationResponse> createPlanForChildrenNodes(
      PlanCreationContext ctx, SidecarsListWrapper sidecarsListWrapper) {
    LinkedHashMap<String, PlanCreationResponse> planCreationResponseMap = new LinkedHashMap<>();
    YamlField sideCarsYamlField = ctx.getCurrentField();

    Map<String, StepParameters> sideCarsParametersMap = (Map<String, StepParameters>) kryoSerializer.asInflatedObject(
        ctx.getDependency().getMetadataMap().get(PlanCreatorConstants.SIDECARS_PARAMETERS_MAP).toByteArray());

    List<YamlNode> yamlNodes = Optional.of(sideCarsYamlField.getNode().asArray()).orElse(Collections.emptyList());
    Map<String, YamlNode> mapIdentifierWithYamlNode =
        yamlNodes.stream().collect(Collectors.toMap(e -> e.getField("sidecar").getNode().getIdentifier(), k -> k));

    for (Map.Entry<String, StepParameters> entry : sideCarsParametersMap.entrySet()) {
      addDependenciesForIndividualSideCar(
          sideCarsYamlField, entry.getKey(), entry.getValue(), mapIdentifierWithYamlNode, planCreationResponseMap);
    }

    return planCreationResponseMap;
  }

  private void addDependenciesForIndividualSideCar(YamlField sideCarsYamlField, String identifier,
      StepParameters stepParameters, Map<String, YamlNode> mapIdentifierWithYamlNode,
      LinkedHashMap<String, PlanCreationResponse> planCreationResponseMap) {
    YamlUpdates.Builder yamlUpdates = YamlUpdates.newBuilder();
    YamlField individualSideCar =
        SideCarsListArtifactsUtility.createIndividualSideCarsArtifactYamlFieldAndSetYamlUpdate(
            sideCarsYamlField, identifier, mapIdentifierWithYamlNode, yamlUpdates);

    String individualSideCarPlanNodeId = UUIDGenerator.generateUuid();
    Map<String, ByteString> metadataDependency =
        prepareMetadataForIndividualSideCarListArtifactPlanCreator(individualSideCarPlanNodeId, stepParameters,identifier);

    Map<String, YamlField> dependenciesMap = new HashMap<>();
    dependenciesMap.put(individualSideCarPlanNodeId, individualSideCar);
    PlanCreationResponse.PlanCreationResponseBuilder individualSideCarPlanResponse =
        PlanCreationResponse.builder().dependencies(
            DependenciesUtils.toDependenciesProto(dependenciesMap)
                .toBuilder()
                .putDependencyMetadata(
                    individualSideCarPlanNodeId, Dependency.newBuilder().putAllMetadata(metadataDependency).build())
                .build());
    if (yamlUpdates.getFqnToYamlCount() > 0) {
      individualSideCarPlanResponse.yamlUpdates(yamlUpdates.build());
    }
    planCreationResponseMap.put(individualSideCarPlanNodeId, individualSideCarPlanResponse.build());
  }

  private Map<String, ByteString> prepareMetadataForIndividualSideCarListArtifactPlanCreator(
          String individualSideCarPlanNodeId, StepParameters stepParameters, String identifier) {
    Map<String, ByteString> metadataDependency = new HashMap<>();
    metadataDependency.put(
        YamlTypes.UUID, ByteString.copyFrom(kryoSerializer.asDeflatedBytes(individualSideCarPlanNodeId)));
    metadataDependency.put(PlanCreatorConstants.SIDECARS_PARAMETERS_MAP,
        ByteString.copyFrom(kryoSerializer.asDeflatedBytes(stepParameters)));
    metadataDependency.put(PlanCreatorConstants.IDENTIFIER,
            ByteString.copyFrom(kryoSerializer.asDeflatedBytes(identifier)));
    return metadataDependency;
  }

  @Override
  public PlanNode createPlanForParentNode(
      PlanCreationContext ctx, SidecarsListWrapper config, List<String> childrenNodeIds) {
    String sideCarsPlanNodeId = (String) kryoSerializer.asInflatedObject(
        ctx.getDependency().getMetadataMap().get(YamlTypes.UUID).toByteArray());

    ForkStepParameters stepParameters = ForkStepParameters.builder().parallelNodeIds(childrenNodeIds).build();
    return PlanNode.builder()
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
  }
}