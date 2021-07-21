package io.harness.pms.sdk.core.pipeline.filters;

import static io.harness.pms.plan.creation.PlanCreatorUtils.supportsField;

import static java.lang.String.format;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidYamlException;
import io.harness.pms.contracts.plan.FilterCreationBlobRequest;
import io.harness.pms.contracts.plan.FilterCreationBlobResponse;
import io.harness.pms.contracts.plan.SetupMetadata;
import io.harness.pms.contracts.plan.YamlFieldBlob;
import io.harness.pms.filter.creation.FilterCreationResponse;
import io.harness.pms.gitsync.PmsGitSyncBranchContextGuard;
import io.harness.pms.gitsync.PmsGitSyncHelper;
import io.harness.pms.sdk.core.filter.creation.beans.FilterCreationContext;
import io.harness.pms.sdk.core.pipeline.creators.BaseCreatorService;
import io.harness.pms.sdk.core.plan.creation.creators.PipelineServiceInfoProvider;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.PIPELINE)
@Slf4j
@Singleton
public class FilterCreatorService extends BaseCreatorService<FilterCreationResponse, SetupMetadata> {
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
    Map<String, YamlField> initialDependencies = getInitialDependencies(dependencyBlobs);

    SetupMetadata setupMetadata = request.getSetupMetadata();
    try (PmsGitSyncBranchContextGuard ignore =
             pmsGitSyncHelper.createGitSyncBranchContextGuardFromBytes(setupMetadata.getGitSyncBranchContext(), true)) {
      FilterCreationResponse finalResponse =
          processNodesRecursively(initialDependencies, setupMetadata, FilterCreationResponse.builder().build());
      return finalResponse.toBlobResponse();
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

  @Override
  public FilterCreationResponse processNodeInternal(SetupMetadata setupMetadata, YamlField yamlField) {
    Optional<FilterJsonCreator> filterCreatorOptional =
        findFilterCreator(pipelineServiceInfoProvider.getFilterJsonCreators(), yamlField);

    if (!filterCreatorOptional.isPresent()) {
      return null;
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
    return response;
  }

  @Override
  public void mergeResponses(FilterCreationResponse finalResponse, FilterCreationResponse response) {
    finalResponse.setStageCount(finalResponse.getStageCount() + response.getStageCount());
    finalResponse.addReferredEntities(response.getReferredEntities());
    finalResponse.addStageNames(response.getStageNames());
    filterCreationResponseMerger.mergeFilterCreationResponse(finalResponse, response);
  }
}
