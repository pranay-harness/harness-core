package io.harness.pms.filter.creation;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static java.lang.String.format;

import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.UnexpectedException;
import io.harness.pms.plan.FilterCreationBlobRequest;
import io.harness.pms.plan.FilterCreationBlobResponse;
import io.harness.pms.plan.PlanCreationServiceGrpc.PlanCreationServiceBlockingStub;
import io.harness.pms.plan.YamlFieldBlob;
import io.harness.pms.plan.creation.PlanCreatorServiceInfo;
import io.harness.pms.sdk.PmsSdkInstanceService;
import io.harness.pms.utils.CompletableFutures;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlUtils;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class FilterCreatorMergeService {
  private Map<String, PlanCreationServiceBlockingStub> planCreatorServices;
  private final PmsSdkInstanceService pmsSdkInstanceService;

  public static final int MAX_DEPTH = 1;
  private final Executor executor = Executors.newFixedThreadPool(5);

  @Inject
  public FilterCreatorMergeService(
      Map<String, PlanCreationServiceBlockingStub> planCreatorServices, PmsSdkInstanceService pmsSdkInstanceService) {
    this.planCreatorServices = planCreatorServices;
    this.pmsSdkInstanceService = pmsSdkInstanceService;
  }

  public FilterCreatorMergeServiceResponse getPipelineInfo(@NotNull String yaml) throws IOException {
    Map<String, Map<String, Set<String>>> sdkInstances = pmsSdkInstanceService.getInstanceNameToSupportedTypes();
    Map<String, PlanCreatorServiceInfo> services = new HashMap<>();
    if (EmptyPredicate.isNotEmpty(planCreatorServices) && EmptyPredicate.isNotEmpty(sdkInstances)) {
      sdkInstances.forEach((k, v) -> {
        if (planCreatorServices.containsKey(k)) {
          services.put(k, new PlanCreatorServiceInfo(v, planCreatorServices.get(k)));
        }
      });
    }

    String processedYaml = YamlUtils.injectUuid(yaml);
    YamlField pipelineField = YamlUtils.extractPipelineField(processedYaml);
    Map<String, YamlFieldBlob> dependencies = new HashMap<>();
    dependencies.put(pipelineField.getNode().getUuid(), pipelineField.toFieldBlob());

    Map<String, String> filters = new HashMap<>();
    FilterCreationBlobResponse response = obtainFiltersRecursively(services, dependencies, filters);
    validateFilterCreationBlobResponse(response);

    return FilterCreatorMergeServiceResponse.builder()
        .filters(filters)
        .layoutNodeMap(response.getLayoutNodesMap())
        .stageCount(response.getStageCount())
        .startingNodeId(response.getStartingNodeId())
        .build();
  }

  @VisibleForTesting
  public void validateFilterCreationBlobResponse(FilterCreationBlobResponse response) {
    if (isNotEmpty(response.getDependenciesMap())) {
      throw new InvalidRequestException(
          format("Unable to resolve all dependencies: %s", response.getDependenciesMap().keySet().toString()));
    }
  }

  @VisibleForTesting
  public FilterCreationBlobResponse obtainFiltersRecursively(Map<String, PlanCreatorServiceInfo> services,
      Map<String, YamlFieldBlob> dependencies, Map<String, String> filters) {
    FilterCreationBlobResponse.Builder responseBuilder =
        FilterCreationBlobResponse.newBuilder().putAllDependencies(dependencies);

    if (isEmpty(services) || isEmpty(dependencies)) {
      return responseBuilder.build();
    }

    for (int i = 0; i < MAX_DEPTH && EmptyPredicate.isNotEmpty(responseBuilder.getDependenciesMap()); i++) {
      FilterCreationBlobResponse currIterResponse =
          obtainFiltersPerIteration(services, responseBuilder.getDependenciesMap(), filters);

      FilterCreationBlobResponseUtils.mergeResolvedDependencies(responseBuilder, currIterResponse);
      if (isNotEmpty(responseBuilder.getDependenciesMap())) {
        throw new InvalidRequestException(format(
            "Some YAML nodes could not be parsed: %s", responseBuilder.getDependenciesMap().keySet().toString()));
      }
      FilterCreationBlobResponseUtils.mergeDependencies(responseBuilder, currIterResponse);
      FilterCreationBlobResponseUtils.updateStageCount(responseBuilder, currIterResponse);
      FilterCreationBlobResponseUtils.mergeLayoutNodeMap(responseBuilder, currIterResponse);
      FilterCreationBlobResponseUtils.mergeStartingNodeId(responseBuilder, currIterResponse);
    }

    return responseBuilder.build();
  }

  @VisibleForTesting
  public FilterCreationBlobResponse obtainFiltersPerIteration(Map<String, PlanCreatorServiceInfo> services,
      Map<String, YamlFieldBlob> dependencies, Map<String, String> filters) {
    CompletableFutures<FilterCreationResponseWrapper> completableFutures = new CompletableFutures<>(executor);
    for (Map.Entry<String, PlanCreatorServiceInfo> entry : services.entrySet()) {
      completableFutures.supplyAsync(() -> {
        try {
          return FilterCreationResponseWrapper.builder()
              .serviceName(entry.getKey())
              .response(entry.getValue().getPlanCreationClient().createFilter(
                  FilterCreationBlobRequest.newBuilder().putAllDependencies(dependencies).build()))
              .build();
        } catch (Exception ex) {
          log.error("Error fetching filter from service " + entry.getKey(), ex);
          return null;
        }
      });
    }

    FilterCreationBlobResponse.Builder currentIteration = FilterCreationBlobResponse.newBuilder();

    try {
      List<FilterCreationResponseWrapper> filterCreationBlobResponses =
          completableFutures.allOf().get(5, TimeUnit.MINUTES);
      filterCreationBlobResponses.forEach(
          response -> FilterCreationBlobResponseUtils.mergeResponses(currentIteration, response, filters));
    } catch (Exception ex) {
      throw new UnexpectedException("Error fetching filter creation response from service", ex);
    }

    return currentIteration.build();
  }
}
