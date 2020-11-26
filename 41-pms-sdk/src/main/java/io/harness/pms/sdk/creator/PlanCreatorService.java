package io.harness.pms.sdk.creator;

import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.UnexpectedException;
import io.harness.pms.creator.PlanCreatorUtils;
import io.harness.pms.plan.FilterCreationBlobRequest;
import io.harness.pms.plan.FilterCreationBlobResponse;
import io.harness.pms.plan.PlanCreationBlobRequest;
import io.harness.pms.plan.PlanCreationBlobResponse;
import io.harness.pms.plan.PlanCreationServiceGrpc.PlanCreationServiceImplBase;
import io.harness.pms.plan.YamlFieldBlob;
import io.harness.pms.plan.creator.PlanCreationContext;
import io.harness.pms.plan.creator.PlanCreationResponse;
import io.harness.pms.sdk.creator.filters.FilterCreatorService;
import io.harness.pms.utils.CompletableFutures;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.validation.constraints.NotNull;

@Singleton
public class PlanCreatorService extends PlanCreationServiceImplBase {
  private final Executor executor = Executors.newFixedThreadPool(2);

  private final FilterCreatorService filterCreatorService;
  private final List<PartialPlanCreator<?>> planCreators;

  @Inject
  public PlanCreatorService(@NotNull PipelineServiceInfoProvider pipelineServiceInfoProvider,
      @NotNull FilterCreatorService filterCreatorService) {
    this.planCreators = pipelineServiceInfoProvider.getPlanCreators();
    this.filterCreatorService = filterCreatorService;
  }

  @Override
  public void createPlan(PlanCreationBlobRequest request, StreamObserver<PlanCreationBlobResponse> responseObserver) {
    Map<String, YamlFieldBlob> dependencyBlobs = request.getDependenciesMap();
    Map<String, YamlField> initialDependencies = new HashMap<>();
    if (EmptyPredicate.isNotEmpty(dependencyBlobs)) {
      try {
        for (Map.Entry<String, YamlFieldBlob> entry : dependencyBlobs.entrySet()) {
          initialDependencies.put(entry.getKey(), YamlField.fromFieldBlob(entry.getValue()));
        }
      } catch (IOException e) {
        throw new InvalidRequestException("Invalid YAML found in dependency blobs");
      }
    }

    PlanCreationResponse finalResponse = createPlanForDependenciesRecursive(initialDependencies);
    responseObserver.onNext(finalResponse.toBlobResponse());
    responseObserver.onCompleted();
  }

  private PlanCreationResponse createPlanForDependenciesRecursive(Map<String, YamlField> initialDependencies) {
    // TODO: Add patch version before sending the response back
    PlanCreationResponse finalResponse = PlanCreationResponse.builder().build();
    if (EmptyPredicate.isEmpty(planCreators) || EmptyPredicate.isEmpty(initialDependencies)) {
      return finalResponse;
    }

    PlanCreationContext ctx = new PlanCreationContext();
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
        Optional<PartialPlanCreator<?>> planCreatorOptional = findPlanCreator(planCreators, field);
        if (!planCreatorOptional.isPresent()) {
          return null;
        }

        PartialPlanCreator planCreator = planCreatorOptional.get();
        Class<?> cls = planCreator.getFieldClass();
        if (YamlField.class.isAssignableFrom(cls)) {
          return planCreator.createPlanForField(ctx, field);
        }

        try {
          Object obj = YamlUtils.read(field.getNode().toString(), cls);
          return planCreator.createPlanForField(ctx, obj);
        } catch (IOException e) {
          throw new InvalidRequestException("Invalid yaml", e);
        }
      });
    }

    try {
      List<PlanCreationResponse> planCreationBlobResponses = completableFutures.allOf().get(2, TimeUnit.MINUTES);
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

  private Optional<PartialPlanCreator<?>> findPlanCreator(List<PartialPlanCreator<?>> planCreators, YamlField field) {
    return planCreators.stream()
        .filter(planCreator -> {
          Map<String, Set<String>> supportedTypes = planCreator.getSupportedTypes();
          return PlanCreatorUtils.supportsField(supportedTypes, field);
        })
        .findFirst();
  }

  @Override
  public void createFilter(
      FilterCreationBlobRequest request, StreamObserver<FilterCreationBlobResponse> responseObserver) {
    FilterCreationBlobResponse response = filterCreatorService.createFilterBlobResponse(request);
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }
}
