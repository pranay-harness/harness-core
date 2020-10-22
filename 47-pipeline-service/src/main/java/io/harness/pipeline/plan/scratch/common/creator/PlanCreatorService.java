package io.harness.pipeline.plan.scratch.common.creator;

import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.UnexpectedException;
import io.harness.pipeline.plan.scratch.common.utils.CompletableFutures;
import io.harness.pipeline.plan.scratch.common.yaml.YamlField;
import io.harness.pms.plan.PlanCreationBlobResponse;
import io.harness.pms.plan.YamlFieldBlob;
import io.harness.serializer.KryoSerializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.validation.constraints.NotNull;

public abstract class PlanCreatorService {
  private final Executor executor = Executors.newFixedThreadPool(2);

  private final KryoSerializer kryoSerializer;
  private final List<PartialPlanCreator> planCreators;

  protected PlanCreatorService(
      @NotNull KryoSerializer kryoSerializer, @NotNull PlanCreatorProvider planCreatorProvider) {
    this.kryoSerializer = kryoSerializer;
    this.planCreators = planCreatorProvider.getPlanCreators();
  }

  public PlanCreationBlobResponse createPlan(@NotNull Map<String, YamlFieldBlob> dependencyBlobs) {
    Map<String, YamlField> initialDependencies = new HashMap<>();
    if (EmptyPredicate.isNotEmpty(dependencyBlobs)) {
      try {
        for (Map.Entry<String, YamlFieldBlob> entry : dependencyBlobs.entrySet()) {
          initialDependencies.put(entry.getKey(), YamlField.fromFieldFieldBlob(entry.getValue()));
        }
      } catch (IOException e) {
        throw new InvalidRequestException("Invalid YAML found in dependency blobs");
      }
    }

    PlanCreationResponse finalResponse = createPlanForDependenciesRecursive(initialDependencies);
    return finalResponse.toBlobResponse();
  }

  private PlanCreationResponse createPlanForDependenciesRecursive(Map<String, YamlField> initialDependencies) {
    // TODO: Add patch version before sending the response back
    PlanCreationResponse finalResponse = PlanCreationResponse.builder().build();
    if (EmptyPredicate.isEmpty(planCreators) || EmptyPredicate.isEmpty(initialDependencies)) {
      return finalResponse;
    }

    PlanCreationContext ctx = PlanCreationContext.builder().kryoSerializer(kryoSerializer).build();
    Map<String, YamlField> dependencies = new HashMap<>(initialDependencies);
    while (!dependencies.isEmpty()) {
      createPlanForDependencies(ctx, finalResponse, dependencies);
      initialDependencies.keySet().forEach(dependencies::remove);
    }

    if (EmptyPredicate.isNotEmpty(finalResponse.getDependencies())) {
      initialDependencies.keySet().forEach(k -> finalResponse.getDependencies().remove(k));
    }
    return finalResponse;
  }

  private void createPlanForDependencies(
      PlanCreationContext ctx, PlanCreationResponse finalResponse, Map<String, YamlField> dependencies) {
    if (EmptyPredicate.isEmpty(dependencies)) {
      return;
    }

    List<YamlField> dependenciesList = new ArrayList<>(dependencies.values());
    dependencies.clear();
    CompletableFutures<PlanCreationResponse> completableFutures = new CompletableFutures<>(executor);
    for (YamlField field : dependenciesList) {
      completableFutures.supplyAsync(() -> {
        Optional<PartialPlanCreator> planCreatorOptional = findPlanCreator(planCreators, field);
        return planCreatorOptional.map(partialPlanCreator -> partialPlanCreator.createPlanForField(ctx, field))
            .orElse(null);
      });
    }

    try {
      List<PlanCreationResponse> planCreationBlobResponses = completableFutures.allOf().get(1, TimeUnit.MINUTES);
      for (int i = 0; i < dependenciesList.size(); i++) {
        YamlField field = dependenciesList.get(i);
        PlanCreationResponse response = planCreationBlobResponses.get(i);
        if (response == null) {
          finalResponse.addDependency(field);
          continue;
        }

        finalResponse.addNodes(response.getNodes());
        finalResponse.mergeStartingNodeId(response.getStartingNodeId());
        if (EmptyPredicate.isNotEmpty(response.getDependencies())) {
          for (YamlField childField : response.getDependencies().values()) {
            dependencies.put(childField.getNode().getUuid(), childField);
          }
        }
      }
    } catch (Exception ex) {
      throw new UnexpectedException("Error fetching plan creation response from service", ex);
    }
  }

  private Optional<PartialPlanCreator> findPlanCreator(List<PartialPlanCreator> planCreators, YamlField field) {
    return planCreators.stream().filter(planCreator -> planCreator.supportsField(field)).findFirst();
  }
}
