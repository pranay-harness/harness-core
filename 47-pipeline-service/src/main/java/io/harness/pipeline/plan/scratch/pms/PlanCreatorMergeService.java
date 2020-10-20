package io.harness.pipeline.plan.scratch.pms;

import static java.lang.String.format;

import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.pipeline.plan.scratch.common.creator.PlanCreationResponse;
import io.harness.pipeline.plan.scratch.common.creator.PlanCreatorService;
import io.harness.pipeline.plan.scratch.common.yaml.YamlField;
import io.harness.pipeline.plan.scratch.common.yaml.YamlNode;
import io.harness.pipeline.plan.scratch.common.yaml.YamlUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;

public class PlanCreatorMergeService {
  private static final int MAX_DEPTH = 10;

  private final List<PlanCreatorService> planCreatorServices;

  public PlanCreatorMergeService(@NotNull List<PlanCreatorService> planCreatorServices) {
    this.planCreatorServices = planCreatorServices;
  }

  public PlanCreationResponse createPlan(@NotNull String content) throws IOException {
    String contentWithUuid = YamlUtils.injectUuid(content);
    YamlField rootYamlField = YamlUtils.readTree(contentWithUuid);
    YamlNode rootYamlNode = rootYamlField.getNode();
    YamlField pipelineField = getPipelineField(rootYamlNode);
    if (pipelineField == null) {
      throw new InvalidRequestException("Invalid pipeline yaml: root of the yaml needs to be an object");
    }

    Map<String, YamlField> dependencies = new HashMap<>();
    dependencies.put(pipelineField.getNode().getUuid(), pipelineField);
    PlanCreationResponse finalResponse = PlanCreationResponse.builder().dependencies(dependencies).build();
    if (EmptyPredicate.isNotEmpty(planCreatorServices)) {
      for (int i = 0; i < MAX_DEPTH && EmptyPredicate.isNotEmpty(finalResponse.getDependencies()); i++) {
        // TODO: Calling all the planCreatorServices will be done in parallel
        for (PlanCreatorService planCreatorService : planCreatorServices) {
          PlanCreationResponse newResponse = planCreatorService.createPlan(finalResponse.getDependencies());
          finalResponse.merge(newResponse);
        }
      }
    }

    return finalResponse;
  }

  public void validatePlanCreationResponse(PlanCreationResponse finalResponse) {
    if (EmptyPredicate.isNotEmpty(finalResponse.getDependencies())) {
      throw new InvalidRequestException(
          format("Unable to interpret nodes: %s", finalResponse.getDependencies().keySet().toString()));
    }
    if (finalResponse.getStartingNodeId() == null) {
      throw new InvalidRequestException("Unable to find out starting node");
    }
  }

  private YamlField getPipelineField(YamlNode rootYamlNode) {
    if (rootYamlNode == null || !rootYamlNode.isObject()) {
      return null;
    }

    return rootYamlNode.getField("pipeline");
  }
}
