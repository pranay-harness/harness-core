package io.harness.pipeline.plan.scratch.lib.creator;

import com.google.common.base.Preconditions;

import io.harness.pipeline.plan.scratch.common.creator.PlanCreationContext;
import io.harness.pipeline.plan.scratch.common.creator.PlanCreationResponse;
import io.harness.pipeline.plan.scratch.common.yaml.YamlField;
import io.harness.pipeline.plan.scratch.common.yaml.YamlNode;
import io.harness.pipeline.plan.scratch.lib.io.MapStepParameters;
import io.harness.pms.facilitators.FacilitatorObtainment;
import io.harness.pms.facilitators.FacilitatorType;
import io.harness.pms.plan.PlanNode;
import io.harness.pms.steps.StepType;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class PipelinePlanCreator extends ParallelChildrenPlanCreator {
  @Override
  public boolean supportsField(YamlField field) {
    return "pipeline".equals(field.getName());
  }

  @Override
  public StepType getStepType() {
    return StepType.newBuilder().setType("pipeline").build();
  }

  @Override
  public boolean isStartingNode() {
    return true;
  }

  @Override
  public Map<String, PlanCreationResponse> createPlanForChildrenNodes(PlanCreationContext ctx, YamlField field) {
    YamlNode yamlNode = field.getNode();
    Map<String, PlanCreationResponse> responseMap = new HashMap<>();
    YamlNode stagesYamlNode = Preconditions.checkNotNull(yamlNode.getField("stages")).getNode();
    if (stagesYamlNode == null) {
      return responseMap;
    }

    List<YamlField> stageYamlFields = Optional.of(stagesYamlNode.asArray())
                                          .orElse(Collections.emptyList())
                                          .stream()
                                          .map(el -> el.getField("stage"))
                                          .collect(Collectors.toList());
    String uuid = "stages-" + yamlNode.getUuid();
    PlanNode node =
        PlanNode.newBuilder()
            .setUuid(uuid)
            .setIdentifier("stages-" + yamlNode.getIdentifier())
            .setStepType(StepType.newBuilder().setType("stages").build())
            .setName("stages")
            .setStepParameters(ctx.toByteString(new MapStepParameters("childrenNodeIds",
                stageYamlFields.stream().map(el -> el.getNode().getUuid()).collect(Collectors.toList()))))
            .addFacilitatorObtainments(FacilitatorObtainment.newBuilder()
                                           .setType(FacilitatorType.newBuilder().setType("CHILDREN").build())
                                           .build())
            .setSkipExpressionChain(false)
            .build();

    Map<String, YamlField> stageYamlFieldMap = new HashMap<>();
    stageYamlFields.forEach(stepField -> stageYamlFieldMap.put(stepField.getNode().getUuid(), stepField));
    responseMap.put(node.getUuid(),
        PlanCreationResponse.builder().node(node.getUuid(), node).dependencies(stageYamlFieldMap).build());
    return responseMap;
  }
}
