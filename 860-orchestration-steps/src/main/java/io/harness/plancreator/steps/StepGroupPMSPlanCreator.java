package io.harness.plancreator.steps;

import static io.harness.pms.yaml.YAMLFieldNameConstants.PARALLEL;
import static io.harness.pms.yaml.YAMLFieldNameConstants.STEP_GROUP;

import io.harness.data.structure.EmptyPredicate;
import io.harness.pms.contracts.advisers.AdviserObtainment;
import io.harness.pms.contracts.advisers.AdviserType;
import io.harness.pms.contracts.facilitators.FacilitatorObtainment;
import io.harness.pms.contracts.steps.SkipType;
import io.harness.pms.plan.creation.PlanCreatorUtils;
import io.harness.pms.sdk.core.adviser.OrchestrationAdviserTypes;
import io.harness.pms.sdk.core.adviser.success.OnSuccessAdviserParameters;
import io.harness.pms.sdk.core.facilitator.child.ChildFacilitator;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;
import io.harness.pms.sdk.core.plan.creation.creators.ChildrenPlanCreator;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;
import io.harness.serializer.KryoSerializer;
import io.harness.steps.StepOutcomeGroup;
import io.harness.steps.common.NGSectionStep;
import io.harness.steps.common.NGSectionStepParameters;
import io.harness.steps.common.steps.stepgroup.StepGroupStep;
import io.harness.steps.common.steps.stepgroup.StepGroupStepParameters;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class StepGroupPMSPlanCreator extends ChildrenPlanCreator<StepGroupElementConfig> {
  @Inject private KryoSerializer kryoSerializer;

  @Override
  public LinkedHashMap<String, PlanCreationResponse> createPlanForChildrenNodes(
      PlanCreationContext ctx, StepGroupElementConfig config) {
    List<YamlField> dependencyNodeIdsList = getDependencyNodeIdsList(ctx);

    LinkedHashMap<String, PlanCreationResponse> responseMap = new LinkedHashMap<>();
    for (YamlField yamlField : dependencyNodeIdsList) {
      Map<String, YamlField> yamlFieldMap = new HashMap<>();
      yamlFieldMap.put(yamlField.getNode().getUuid(), yamlField);
      responseMap.put(yamlField.getNode().getUuid(), PlanCreationResponse.builder().dependencies(yamlFieldMap).build());
    }

    // Add Steps Node
    if (EmptyPredicate.isNotEmpty(dependencyNodeIdsList)) {
      YamlField stepsField =
          Preconditions.checkNotNull(ctx.getCurrentField().getNode().getField(YAMLFieldNameConstants.STEPS));
      PlanNode stepsNode = getStepsPlanNode(stepsField, dependencyNodeIdsList.get(0).getNode().getUuid());
      responseMap.put(stepsNode.getUuid(), PlanCreationResponse.builder().node(stepsNode.getUuid(), stepsNode).build());
    }

    return responseMap;
  }

  @Override
  public PlanNode createPlanForParentNode(
      PlanCreationContext ctx, StepGroupElementConfig config, List<String> childrenNodeIds) {
    YamlField stepsField =
        Preconditions.checkNotNull(ctx.getCurrentField().getNode().getField(YAMLFieldNameConstants.STEPS));
    StepParameters stepParameters = StepGroupStepParameters.getStepParameters(config, stepsField.getNode().getUuid());
    return PlanNode.builder()
        .name(config.getName())
        .uuid(config.getUuid())
        .identifier(config.getIdentifier())
        .stepType(StepGroupStep.STEP_TYPE)
        .group(StepOutcomeGroup.STEP_GROUP.name())
        .stepParameters(stepParameters)
        .facilitatorObtainment(FacilitatorObtainment.newBuilder().setType(ChildFacilitator.FACILITATOR_TYPE).build())
        .adviserObtainments(getAdviserObtainmentFromMetaData(ctx.getCurrentField()))
        .skipExpressionChain(false)
        .build();
  }

  @Override
  public Class<StepGroupElementConfig> getFieldClass() {
    return StepGroupElementConfig.class;
  }

  @Override
  public Map<String, Set<String>> getSupportedTypes() {
    return Collections.singletonMap(STEP_GROUP, Collections.singleton(PlanCreatorUtils.ANY_TYPE));
  }

  private List<AdviserObtainment> getAdviserObtainmentFromMetaData(YamlField currentField) {
    List<AdviserObtainment> adviserObtainments = new ArrayList<>();
    if (currentField != null && currentField.getNode() != null) {
      if (currentField.checkIfParentIsParallel(STEP_GROUP)) {
        return adviserObtainments;
      }
      YamlField siblingField = currentField.getNode().nextSiblingFromParentArray(
          currentField.getName(), Arrays.asList(YAMLFieldNameConstants.STEP, PARALLEL, STEP_GROUP));
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

  List<YamlField> getDependencyNodeIdsList(PlanCreationContext planCreationContext) {
    List<YamlField> childYamlFields = new LinkedList<>();
    List<YamlNode> yamlNodes =
        Optional
            .of(Preconditions.checkNotNull(planCreationContext.getCurrentField().getNode().getField("steps"))
                    .getNode()
                    .asArray())
            .orElse(Collections.emptyList());
    yamlNodes.forEach(yamlNode -> {
      YamlField stepField = yamlNode.getField(YAMLFieldNameConstants.STEP);
      YamlField parallelStepField = yamlNode.getField(YAMLFieldNameConstants.PARALLEL);
      if (stepField != null) {
        childYamlFields.add(stepField);
      } else if (parallelStepField != null) {
        childYamlFields.add(parallelStepField);
      }
    });
    return childYamlFields;
  }

  PlanNode getStepsPlanNode(YamlField stepsYamlField, String childNodeId) {
    StepParameters stepParameters =
        NGSectionStepParameters.builder().childNodeId(childNodeId).logMessage("Steps Element").build();
    return PlanNode.builder()
        .uuid(stepsYamlField.getNode().getUuid())
        .identifier(YAMLFieldNameConstants.STEPS)
        .stepType(NGSectionStep.STEP_TYPE)
        .name(YAMLFieldNameConstants.STEPS)
        .stepParameters(stepParameters)
        .facilitatorObtainment(FacilitatorObtainment.newBuilder().setType(ChildFacilitator.FACILITATOR_TYPE).build())
        .skipGraphType(SkipType.SKIP_NODE)
        .build();
  }
}
