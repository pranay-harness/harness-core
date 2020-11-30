package io.harness.pms.sdk.core.pipeline.filters;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.pms.plan.creation.PlanCreatorUtils.supportsField;

import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.pipeline.filter.FilterCreationResponse;
import io.harness.pms.plan.FilterCreationBlobRequest;
import io.harness.pms.plan.FilterCreationBlobResponse;
import io.harness.pms.plan.YamlFieldBlob;
import io.harness.pms.sdk.core.plan.creation.creators.PipelineServiceInfoProvider;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.validation.constraints.NotNull;

@Singleton
public class FilterCreatorService {
  private final PipelineServiceInfoProvider pipelineServiceInfoProvider;
  private final FilterCreationResponseMerger filterCreationResponseMerger;

  @Inject
  public FilterCreatorService(@NotNull PipelineServiceInfoProvider pipelineServiceInfoProvider,
      @NotNull FilterCreationResponseMerger filterCreationResponseMerger) {
    this.pipelineServiceInfoProvider = pipelineServiceInfoProvider;
    this.filterCreationResponseMerger = filterCreationResponseMerger;
  }

  public FilterCreationBlobResponse createFilterBlobResponse(FilterCreationBlobRequest request) {
    Map<String, YamlFieldBlob> dependencyBlobs = request.getDependenciesMap();
    Map<String, YamlField> initialDependencies = new HashMap<>();

    if (isNotEmpty(dependencyBlobs)) {
      try {
        for (Map.Entry<String, YamlFieldBlob> entry : dependencyBlobs.entrySet()) {
          initialDependencies.put(entry.getKey(), YamlField.fromFieldBlob(entry.getValue()));
        }
      } catch (IOException e) {
        throw new InvalidRequestException("Invalid YAML found in dependency blobs");
      }
    }

    FilterCreationResponse finalResponse = processNodesRecursively(initialDependencies);
    return finalResponse.toBlobResponse();
  }

  private FilterCreationResponse processNodesRecursively(Map<String, YamlField> initialDependencies) {
    FilterCreationResponse finalResponse = FilterCreationResponse.builder().build();
    if (isEmpty(initialDependencies)) {
      return finalResponse;
    }

    Map<String, YamlField> dependencies = new HashMap<>(initialDependencies);
    while (!dependencies.isEmpty()) {
      processNodes(dependencies, finalResponse);
      initialDependencies.keySet().forEach(dependencies::remove);
    }

    if (EmptyPredicate.isNotEmpty(finalResponse.getDependencies())) {
      initialDependencies.keySet().forEach(k -> finalResponse.getDependencies().remove(k));
    }

    return finalResponse;
  }

  private void processNodes(Map<String, YamlField> dependencies, FilterCreationResponse finalResponse) {
    List<YamlField> dependenciesList = new ArrayList<>(dependencies.values());
    dependencies.clear();

    for (YamlField yamlField : dependenciesList) {
      Optional<FilterJsonCreator> filterCreatorOptional =
          findFilterCreator(pipelineServiceInfoProvider.getFilterJsonCreators(), yamlField);

      if (!filterCreatorOptional.isPresent()) {
        finalResponse.addDependency(yamlField);
        continue;
      }

      FilterCreationResponse response;
      FilterJsonCreator filterJsonCreator = filterCreatorOptional.get();
      Class<?> clazz = filterJsonCreator.getFieldClass();
      if (YamlField.class.isAssignableFrom(clazz)) {
        response = filterJsonCreator.handleNode(yamlField);
      } else {
        try {
          Object obj = YamlUtils.read(yamlField.getNode().toString(), clazz);
          response = filterJsonCreator.handleNode(obj);
        } catch (IOException e) {
          throw new InvalidRequestException("Invalid yaml", e);
        }
      }

      if (response == null) {
        finalResponse.addDependency(yamlField);
        continue;
      }
      finalResponse.setStageCount(finalResponse.getStageCount() + response.getStageCount());
      filterCreationResponseMerger.mergeFilterCreationResponse(finalResponse, response);
      finalResponse.addResolvedDependency(yamlField);
      if (isNotEmpty(response.getDependencies())) {
        response.getDependencies().values().forEach(field -> dependencies.put(field.getNode().getUuid(), field));
      }
    }
  }

  private Optional<FilterJsonCreator> findFilterCreator(
      List<FilterJsonCreator> filterJsonCreators, YamlField yamlField) {
    return filterJsonCreators.stream()
        .filter(filterJsonCreator -> {
          Map<String, Set<String>> supportedTypes = filterJsonCreator.getSupportedTypes();
          return supportsField(supportedTypes, yamlField);
        })
        .findFirst();
  }
}
