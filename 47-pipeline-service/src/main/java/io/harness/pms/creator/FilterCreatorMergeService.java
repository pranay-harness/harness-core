package io.harness.pms.creator;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static java.lang.String.format;

import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.UnexpectedException;
import io.harness.pms.beans.filters.FilterCreationResponseWrapper;
import io.harness.pms.plan.FilterCreationBlobRequest;
import io.harness.pms.plan.FilterCreationBlobResponse;
import io.harness.pms.plan.PlanCreationServiceGrpc.PlanCreationServiceBlockingStub;
import io.harness.pms.plan.YamlFieldBlob;
import io.harness.pms.service.PmsSdkInstanceService;
import io.harness.pms.utils.CompletableFutures;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;
import io.harness.pms.yaml.YamlUtils;

import com.google.common.base.Preconditions;
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

  private static final int MAX_DEPTH = 1;
  private final Executor executor = Executors.newFixedThreadPool(5);

  @Inject
  public FilterCreatorMergeService(
      Map<String, PlanCreationServiceBlockingStub> planCreatorServices, PmsSdkInstanceService pmsSdkInstanceService) {
    this.planCreatorServices = planCreatorServices;
    this.pmsSdkInstanceService = pmsSdkInstanceService;
  }

  public Map<String, String> getFilters(@NotNull String yaml) throws IOException {
    Map<String, Map<String, Set<String>>> sdkInstances = pmsSdkInstanceService.getSdkInstancesMap();
    Map<String, PlanCreatorServiceInfo> services = new HashMap<>();
    if (EmptyPredicate.isNotEmpty(planCreatorServices) && EmptyPredicate.isNotEmpty(sdkInstances)) {
      sdkInstances.forEach((k, v) -> {
        if (planCreatorServices.containsKey(k)) {
          services.put(k, new PlanCreatorServiceInfo(v, planCreatorServices.get(k)));
        }
      });
    }

    String processedYaml = YamlUtils.injectUuid(yaml);
    YamlField rootYamlField = YamlUtils.readTree(processedYaml);

    YamlField pipelineField = extractPipelineField(rootYamlField);
    Map<String, YamlFieldBlob> dependencies = new HashMap<>();
    dependencies.put(pipelineField.getNode().getUuid(), pipelineField.toFieldBlob());

    Map<String, String> filters = new HashMap<>();
    FilterCreationBlobResponse response = obtainFiltersRecursively(services, dependencies, filters);
    validateFilterCreationBlobResponse(response);

    return filters;
  }

  private void validateFilterCreationBlobResponse(FilterCreationBlobResponse response) {
    if (isNotEmpty(response.getDependenciesMap())) {
      throw new InvalidRequestException(
          format("Unable to resolve all dependencies: %s", response.getDependenciesMap().keySet().toString()));
    }
  }

  private FilterCreationBlobResponse obtainFiltersRecursively(Map<String, PlanCreatorServiceInfo> services,
      Map<String, YamlFieldBlob> dependencies, Map<String, String> filters) {
    FilterCreationBlobResponse.Builder responseBuilder =
        FilterCreationBlobResponse.newBuilder().putAllDependencies(dependencies);

    if (isEmpty(services) || isEmpty(dependencies)) {
      return responseBuilder.build();
    }

    for (int i = 0; i < MAX_DEPTH && EmptyPredicate.isNotEmpty(responseBuilder.getDependenciesMap()); i++) {
      FilterCreationBlobResponse currIterResponse =
          obtainFiltersPerIteration(services, responseBuilder.getDependenciesMap(), filters);

      mergeResolvedDependencies(responseBuilder, currIterResponse);
      if (isNotEmpty(responseBuilder.getDependenciesMap())) {
        throw new InvalidRequestException(format(
            "Some YAML nodes could not be parsed: %s", responseBuilder.getDependenciesMap().keySet().toString()));
      }
      mergeDependencies(responseBuilder, currIterResponse);
    }

    return responseBuilder.build();
  }

  private FilterCreationBlobResponse obtainFiltersPerIteration(Map<String, PlanCreatorServiceInfo> services,
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
      filterCreationBlobResponses.forEach(response -> mergeResponses(currentIteration, response, filters));
    } catch (Exception ex) {
      throw new UnexpectedException("Error fetching filter creation response from service", ex);
    }

    return currentIteration.build();
  }

  private void mergeResponses(
      FilterCreationBlobResponse.Builder builder, FilterCreationResponseWrapper response, Map<String, String> filters) {
    if (response == null) {
      return;
    }
    mergeResolvedDependencies(builder, response.getResponse());
    mergeDependencies(builder, response.getResponse());
    mergeFilters(response, filters);
  }

  private void mergeFilters(FilterCreationResponseWrapper response, Map<String, String> filters) {
    if (isNotEmpty(response.getResponse().getFilter())) {
      filters.put(response.getServiceName(), response.getResponse().getFilter());
    }
  }

  private void mergeResolvedDependencies(
      FilterCreationBlobResponse.Builder builder, FilterCreationBlobResponse response) {
    if (isNotEmpty(response.getResolvedDependenciesMap())) {
      response.getResolvedDependenciesMap().forEach((key, value) -> {
        builder.putResolvedDependencies(key, value);
        builder.removeDependencies(key);
      });
    }
  }

  private void mergeDependencies(FilterCreationBlobResponse.Builder builder, FilterCreationBlobResponse response) {
    if (isNotEmpty(response.getDependenciesMap())) {
      response.getDependenciesMap().forEach((key, value) -> {
        if (!builder.containsResolvedDependencies(key)) {
          builder.putDependencies(key, value);
        }
      });
    }
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
