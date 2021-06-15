package io.harness.pms.sdk.core.pipeline.filters;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.pms.plan.creation.PlanCreatorUtils.supportsField;

import static java.lang.String.format;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.InvalidYamlException;
import io.harness.pms.contracts.plan.FilterCreationBlobRequest;
import io.harness.pms.contracts.plan.FilterCreationBlobResponse;
import io.harness.pms.contracts.plan.SetupMetadata;
import io.harness.pms.contracts.plan.YamlFieldBlob;
import io.harness.pms.filter.creation.FilterCreationResponse;
import io.harness.pms.gitsync.PmsGitSyncBranchContextGuard;
import io.harness.pms.gitsync.PmsGitSyncHelper;
import io.harness.pms.sdk.core.filter.creation.beans.FilterCreationContext;
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
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.PIPELINE)
@Slf4j
@Singleton
public class FilterCreatorService {
  private final PipelineServiceInfoProvider pipelineServiceInfoProvider;
  private final FilterCreationResponseMerger filterCreationResponseMerger;
  private final PmsGitSyncHelper pmsGitSyncHelper;

  @Inject
  public FilterCreatorService(@NotNull PipelineServiceInfoProvider pipelineServiceInfoProvider,
      @NotNull FilterCreationResponseMerger filterCreationResponseMerger, PmsGitSyncHelper pmsGitSyncHelper) {
    this.pipelineServiceInfoProvider = pipelineServiceInfoProvider;
    this.filterCreationResponseMerger = filterCreationResponseMerger;
    this.pmsGitSyncHelper = pmsGitSyncHelper;
  }

  public FilterCreationBlobResponse createFilterBlobResponse(FilterCreationBlobRequest request) {
    Map<String, YamlFieldBlob> dependencyBlobs = request.getDependenciesMap();
    Map<String, YamlField> initialDependencies = new HashMap<>();

    if (isNotEmpty(dependencyBlobs)) {
      try {
        for (Map.Entry<String, YamlFieldBlob> entry : dependencyBlobs.entrySet()) {
          initialDependencies.put(entry.getKey(), YamlField.fromFieldBlob(entry.getValue()));
        }
      } catch (Exception e) {
        log.error("Invalid YAML found in dependency blobs", e);
        throw new InvalidRequestException("Invalid YAML found in dependency blobs", e);
      }
    }

    SetupMetadata setupMetadata = request.getSetupMetadata();
    try (PmsGitSyncBranchContextGuard ignore =
             pmsGitSyncHelper.createGitSyncBranchContextGuardFromBytes(setupMetadata.getGitSyncBranchContext(), true)) {
      FilterCreationResponse finalResponse = processNodesRecursively(initialDependencies, setupMetadata);
      return finalResponse.toBlobResponse();
    }
  }

  private FilterCreationResponse processNodesRecursively(
      Map<String, YamlField> initialDependencies, SetupMetadata setupMetadata) {
    FilterCreationResponse finalResponse = FilterCreationResponse.builder().build();
    if (isEmpty(initialDependencies)) {
      return finalResponse;
    }

    Map<String, YamlField> dependencies = new HashMap<>(initialDependencies);
    while (!dependencies.isEmpty()) {
      processNodes(dependencies, finalResponse, setupMetadata);
      initialDependencies.keySet().forEach(dependencies::remove);
    }

    if (EmptyPredicate.isNotEmpty(finalResponse.getDependencies())) {
      initialDependencies.keySet().forEach(k -> finalResponse.getDependencies().remove(k));
    }

    return finalResponse;
  }

  private void processNodes(
      Map<String, YamlField> dependencies, FilterCreationResponse finalResponse, SetupMetadata setupMetadata) {
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
        response = filterJsonCreator.handleNode(
            FilterCreationContext.builder().currentField(yamlField).setupMetadata(setupMetadata).build(), yamlField);
      } else {
        try {
          Object obj = YamlUtils.read(yamlField.getNode().toString(), clazz);
          response = filterJsonCreator.handleNode(
              FilterCreationContext.builder().currentField(yamlField).setupMetadata(setupMetadata).build(), obj);
        } catch (IOException e) {
          // YamlUtils.getErrorNodePartialFQN() uses exception path to build FQN
          log.error(format("Invalid yaml in node [%s]", YamlUtils.getErrorNodePartialFQN(yamlField.getNode(), e)), e);
          throw new InvalidYamlException(
              format("Invalid yaml in node [%s]", YamlUtils.getErrorNodePartialFQN(yamlField.getNode(), e)), e);
        }
      }

      if (response == null) {
        finalResponse.addDependency(yamlField);
        continue;
      }
      finalResponse.setStageCount(finalResponse.getStageCount() + response.getStageCount());
      finalResponse.addReferredEntities(response.getReferredEntities());
      finalResponse.addStageNames(response.getStageNames());
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
