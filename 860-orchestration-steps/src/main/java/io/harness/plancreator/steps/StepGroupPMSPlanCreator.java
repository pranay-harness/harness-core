package io.harness.plancreator.steps;

import io.harness.pms.contracts.advisers.AdviserObtainment;
import io.harness.pms.contracts.advisers.AdviserType;
import io.harness.pms.contracts.facilitators.FacilitatorObtainment;
import io.harness.pms.plan.creation.PlanCreatorUtils;
import io.harness.pms.sdk.core.adviser.OrchestrationAdviserTypes;
import io.harness.pms.sdk.core.adviser.success.OnSuccessAdviserParameters;
import io.harness.pms.sdk.core.facilitator.child.ChildFacilitator;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;
import io.harness.pms.sdk.core.plan.creation.creators.ChildrenPlanCreator;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;
import io.harness.pms.yaml.YamlUtils;
import io.harness.serializer.KryoSerializer;
import io.harness.steps.StepOutcomeGroup;
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
    return responseMap;
  }

  @Override
  public PlanNode createPlanForParentNode(
      PlanCreationContext ctx, StepGroupElementConfig config, List<String> childrenNodeIds) {
    StepParameters stepParameters = StepGroupStepParameters.getStepParameters(config, childrenNodeIds.get(0));
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
    return Collections.singletonMap("stepGroup", Collections.singleton(PlanCreatorUtils.ANY_TYPE));
  }

  private List<AdviserObtainment> getAdviserObtainmentFromMetaData(YamlField currentField) {
    List<AdviserObtainment> adviserObtainments = new ArrayList<>();
    if (currentField != null && currentField.getNode() != null) {
      if (checkIfParentIsParallel(currentField)) {
        return adviserObtainments;
      }
      YamlField siblingField = currentField.getNode().nextSiblingFromParentArray(
          currentField.getName(), Arrays.asList("step", "parallel", "stepGroup"));
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
      YamlField stepField = yamlNode.getField("step");
      YamlField parallelStepField = yamlNode.getField("parallel");
      if (stepField != null) {
        childYamlFields.add(stepField);
      } else if (parallelStepField != null) {
        childYamlFields.add(parallelStepField);
      }
    });
    return childYamlFields;
  }

  private boolean checkIfParentIsParallel(YamlField currentField) {
    return YamlUtils.getGivenYamlNodeFromParentPath(currentField.getNode(), "parallel") != null;
  }
}
