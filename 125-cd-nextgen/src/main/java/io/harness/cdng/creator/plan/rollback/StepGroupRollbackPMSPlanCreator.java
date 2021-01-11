package io.harness.cdng.creator.plan.rollback;

import io.harness.plancreator.beans.PlanCreationConstants;
import io.harness.pms.contracts.facilitators.FacilitatorObtainment;
import io.harness.pms.sdk.core.facilitator.child.ChildFacilitator;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse.PlanCreationResponseBuilder;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;
import io.harness.steps.StepOutcomeGroup;
import io.harness.steps.common.NGSectionStep;
import io.harness.steps.common.NGSectionStepParameters;

import com.google.common.base.Preconditions;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class StepGroupRollbackPMSPlanCreator {
  public static PlanCreationResponse createStepGroupRollbackPlan(YamlField stepGroup) {
    YamlField rollbackStepsNode = stepGroup.getNode().getField(YAMLFieldNameConstants.ROLLBACK_STEPS);

    if (rollbackStepsNode != null) {
      PlanCreationResponseBuilder planCreationResponseBuilder = PlanCreationResponse.builder();
      List<YamlField> stepYamlFields = getStepYamlFields(rollbackStepsNode);
      for (YamlField stepYamlField : stepYamlFields) {
        Map<String, YamlField> stepYamlFieldMap = new HashMap<>();
        stepYamlFieldMap.put(stepYamlField.getNode().getUuid(), stepYamlField);
        planCreationResponseBuilder.dependencies(stepYamlFieldMap);
      }

      StepParameters stepParameters =
          NGSectionStepParameters.builder()
              .childNodeId(stepYamlFields.get(0).getNode().getUuid())
              .logMessage("Step Group rollback " + stepYamlFields.get(0).getNode().getIdentifier())
              .build();

      PlanNode stepGroupRollbackNode =
          PlanNode.builder()
              .uuid(rollbackStepsNode.getNode().getUuid())
              .name(stepGroup.getNode().getNameOrIdentifier() + "-" + PlanCreationConstants.ROLLBACK_NODE_NAME)
              .identifier(YAMLFieldNameConstants.ROLLBACK_STEPS)
              .stepType(NGSectionStep.STEP_TYPE)
              .group(StepOutcomeGroup.STEP.name())
              .stepParameters(stepParameters)
              .facilitatorObtainment(
                  FacilitatorObtainment.newBuilder().setType(ChildFacilitator.FACILITATOR_TYPE).build())
              .build();
      return planCreationResponseBuilder.node(stepGroupRollbackNode.getUuid(), stepGroupRollbackNode).build();
    }

    return PlanCreationResponse.builder().build();
  }

  private static List<YamlField> getStepYamlFields(YamlField rollbackStepsNode) {
    List<YamlNode> yamlNodes =
        Optional.of(Preconditions.checkNotNull(rollbackStepsNode).getNode().asArray()).orElse(Collections.emptyList());
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
