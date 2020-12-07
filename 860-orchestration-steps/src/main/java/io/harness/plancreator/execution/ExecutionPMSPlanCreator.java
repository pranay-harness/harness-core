package io.harness.plancreator.execution;

import io.harness.plancreator.beans.PlanCreationConstants;
import io.harness.pms.facilitators.FacilitatorObtainment;
import io.harness.pms.plan.creation.PlanCreatorUtils;
import io.harness.pms.sdk.core.facilitator.child.ChildFacilitator;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;
import io.harness.pms.sdk.core.plan.creation.creators.ChildrenPlanCreator;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;
import io.harness.steps.StepOutcomeGroup;
import io.harness.steps.common.NGSectionStep;
import io.harness.steps.common.NGSectionStepParameters;

import com.google.common.base.Preconditions;
import java.util.*;

public class ExecutionPMSPlanCreator extends ChildrenPlanCreator<ExecutionElementConfig> {
  @Override
  public Map<String, PlanCreationResponse> createPlanForChildrenNodes(
      PlanCreationContext ctx, ExecutionElementConfig config) {
    Map<String, PlanCreationResponse> responseMap = new HashMap<>();
    List<YamlField> stepYamlFields = getStepYamlFields(ctx);
    for (YamlField stepYamlField : stepYamlFields) {
      Map<String, YamlField> stepYamlFieldMap = new HashMap<>();
      stepYamlFieldMap.put(stepYamlField.getNode().getUuid(), stepYamlField);
      responseMap.put(
          stepYamlField.getNode().getUuid(), PlanCreationResponse.builder().dependencies(stepYamlFieldMap).build());
    }
    YamlField rollbackStepsField = ctx.getCurrentField().getNode().getField("rollbackSteps");
    if (rollbackStepsField != null) {
      Map<String, YamlField> rollbackDependencyMap = new HashMap<>();
      rollbackDependencyMap.put(rollbackStepsField.getNode().getUuid(), rollbackStepsField);
      responseMap.put(rollbackStepsField.getNode().getUuid(),
          PlanCreationResponse.builder().dependencies(rollbackDependencyMap).build());
    }
    return responseMap;
  }

  @Override
  public PlanNode createPlanForParentNode(
      PlanCreationContext ctx, ExecutionElementConfig config, List<String> childrenNodeIds) {
    StepParameters stepParameters =
        NGSectionStepParameters.builder().childNodeId(childrenNodeIds.get(0)).logMessage("Execution Element").build();
    return PlanNode.builder()
        .uuid(ctx.getCurrentField().getNode().getUuid())
        .identifier(PlanCreationConstants.EXECUTION_NODE_IDENTIFIER)
        .stepType(NGSectionStep.STEP_TYPE)
        .group(StepOutcomeGroup.EXECUTION.name())
        .name(PlanCreationConstants.EXECUTION_NODE_NAME)
        .stepParameters(stepParameters)
        .facilitatorObtainment(FacilitatorObtainment.newBuilder().setType(ChildFacilitator.FACILITATOR_TYPE).build())
        .skipExpressionChain(false)
        .build();
  }

  @Override
  public Class<ExecutionElementConfig> getFieldClass() {
    return ExecutionElementConfig.class;
  }

  @Override
  public Map<String, Set<String>> getSupportedTypes() {
    return Collections.singletonMap("execution", Collections.singleton(PlanCreatorUtils.ANY_TYPE));
  }

  private List<YamlField> getStepYamlFields(PlanCreationContext planCreationContext) {
    List<YamlNode> yamlNodes =
        Optional
            .of(Preconditions.checkNotNull(planCreationContext.getCurrentField().getNode().getField("steps"))
                    .getNode()
                    .asArray())
            .orElse(Collections.emptyList());
    List<YamlField> stepFields = new LinkedList<>();

    yamlNodes.forEach(yamlNode -> {
      YamlField stepField = yamlNode.getField("step");
      YamlField stepGroupField = yamlNode.getField("stepGroup");
      YamlField parallelStepField = yamlNode.getField("parallel");
      if (stepField != null) {
        stepFields.add(stepField);
      } else if (stepGroupField != null) {
        stepFields.add(stepGroupField);
      } else if (parallelStepField != null) {
        stepFields.add(parallelStepField);
      }
    });
    return stepFields;
  }
}
