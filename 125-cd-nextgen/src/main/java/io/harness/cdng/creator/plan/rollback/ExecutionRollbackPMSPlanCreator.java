package io.harness.cdng.creator.plan.rollback;

import io.harness.executionplan.plancreator.beans.PlanCreatorConstants;
import io.harness.pms.facilitators.FacilitatorObtainment;
import io.harness.pms.sdk.core.facilitator.child.ChildFacilitator;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse.PlanCreationResponseBuilder;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;
import io.harness.steps.common.NGSectionStep;
import io.harness.steps.common.NGSectionStepParameters;

import com.google.common.base.Preconditions;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ExecutionRollbackPMSPlanCreator {
  public static PlanCreationResponse createExecutionRollbackPlanNode(YamlField rollbackStepsField) {
    List<YamlField> stepsArrayFields = getStepYamlFields(rollbackStepsField);
    if (stepsArrayFields.isEmpty()) {
      return PlanCreationResponse.builder().build();
    }

    PlanCreationResponseBuilder planCreationResponseBuilder = PlanCreationResponse.builder();
    for (YamlField stepYamlField : stepsArrayFields) {
      Map<String, YamlField> stepYamlFieldMap = new HashMap<>();
      stepYamlFieldMap.put(stepYamlField.getNode().getUuid(), stepYamlField);
      planCreationResponseBuilder.dependencies(stepYamlFieldMap);
    }

    StepParameters stepParameters = NGSectionStepParameters.builder()
                                        .childNodeId(stepsArrayFields.get(0).getNode().getUuid())
                                        .logMessage("Execution Rollback")
                                        .build();
    PlanNode executionRollbackNode =
        PlanNode.builder()
            .uuid(rollbackStepsField.getNode().getUuid() + "_executionrollback")
            .name("Execution Rollback")
            .identifier(PlanCreatorConstants.EXECUTION_ROLLBACK_NODE_IDENTIFIER)
            .stepType(NGSectionStep.STEP_TYPE)
            .stepParameters(stepParameters)
            .facilitatorObtainment(
                FacilitatorObtainment.newBuilder().setType(ChildFacilitator.FACILITATOR_TYPE).build())
            .build();
    return planCreationResponseBuilder.node(executionRollbackNode.getUuid(), executionRollbackNode).build();
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
