package io.harness.pipeline.plan.scratch.pms.creator;

import static java.lang.String.format;

import com.google.common.base.Preconditions;

import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.UnexpectedException;
import io.harness.pipeline.plan.scratch.common.creator.PlanCreationResponse;
import io.harness.pipeline.plan.scratch.common.creator.PlanCreatorService;
import io.harness.pipeline.plan.scratch.common.utils.CompletableFutures;
import io.harness.pipeline.plan.scratch.common.yaml.YamlField;
import io.harness.pipeline.plan.scratch.common.yaml.YamlNode;
import io.harness.pipeline.plan.scratch.common.yaml.YamlUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.validation.constraints.NotNull;

public class PlanCreatorMergeService {
  private static final int MAX_DEPTH = 10;

  private final Executor executor = Executors.newFixedThreadPool(2);
  private final List<PlanCreatorService> planCreatorServices;

  public PlanCreatorMergeService(@NotNull List<PlanCreatorService> planCreatorServices) {
    this.planCreatorServices = planCreatorServices;
  }

  public PlanCreationResponse createPlan(@NotNull String content) throws IOException {
    String finalContent = preprocessYaml(content);
    YamlField rootYamlField = YamlUtils.readTree(finalContent);
    YamlField pipelineField = extractPipelineField(rootYamlField);
    Map<String, YamlField> dependencies = new HashMap<>();
    dependencies.put(pipelineField.getNode().getUuid(), pipelineField);
    PlanCreationResponse finalResponse = createPlanForDependenciesRecursive(dependencies);
    validatePlanCreationResponse(finalResponse);
    return finalResponse;
  }

  private PlanCreationResponse createPlanForDependenciesRecursive(Map<String, YamlField> initialDependencies) {
    PlanCreationResponse finalResponse = PlanCreationResponse.builder().dependencies(initialDependencies).build();
    if (EmptyPredicate.isEmpty(planCreatorServices) || EmptyPredicate.isEmpty(initialDependencies)) {
      return finalResponse;
    }

    for (int i = 0; i < MAX_DEPTH && EmptyPredicate.isNotEmpty(finalResponse.getDependencies()); i++) {
      PlanCreationResponse currIterationResponse = createPlanForDependencies(finalResponse.getDependencies());
      finalResponse.addNodes(currIterationResponse.getNodes());
      finalResponse.mergeStartingNodeId(currIterationResponse.getStartingNodeId());
      if (EmptyPredicate.isNotEmpty(finalResponse.getDependencies())) {
        throw new InvalidRequestException("Some YAML nodes could not be parsed");
      }

      finalResponse.addDependencies(currIterationResponse.getDependencies());
    }

    validatePlanCreationResponse(finalResponse);
    return finalResponse;
  }

  private PlanCreationResponse createPlanForDependencies(Map<String, YamlField> dependencies) {
    PlanCreationResponse currIterationResponse = PlanCreationResponse.builder().build();
    CompletableFutures<PlanCreationResponse> completableFutures = new CompletableFutures<>(executor);
    for (PlanCreatorService planCreatorService : planCreatorServices) {
      completableFutures.supplyAsync(() -> planCreatorService.createPlan(dependencies));
    }

    try {
      List<PlanCreationResponse> planCreationResponses = completableFutures.allOf().get(5, TimeUnit.MINUTES);
      planCreationResponses.forEach(currIterationResponse::merge);
    } catch (Exception ex) {
      throw new UnexpectedException("Error fetching plan creation response from service", ex);
    }

    return currIterationResponse;
  }

  private void validatePlanCreationResponse(PlanCreationResponse finalResponse) {
    if (EmptyPredicate.isNotEmpty(finalResponse.getDependencies())) {
      throw new InvalidRequestException(
          format("Unable to interpret nodes: %s", finalResponse.getDependencies().keySet().toString()));
    }
    if (finalResponse.getStartingNodeId() == null) {
      throw new InvalidRequestException("Unable to find out starting node");
    }
  }

  private String preprocessYaml(@NotNull String content) throws IOException {
    return YamlUtils.injectUuid(content);
  }

  private YamlField extractPipelineField(YamlField rootYamlField) {
    YamlNode rootYamlNode = rootYamlField.getNode();
    return Preconditions.checkNotNull(
        getPipelineField(rootYamlNode), "Invalid pipeline YAML: root of the yaml needs to be an object");
  }

  private YamlField getPipelineField(YamlNode rootYamlNode) {
    return (rootYamlNode == null || !rootYamlNode.isObject()) ? null : rootYamlNode.getField("pipeline");
  }
}
