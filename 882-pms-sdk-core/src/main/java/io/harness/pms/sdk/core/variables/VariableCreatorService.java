package io.harness.pms.sdk.core.variables;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.pms.plan.creation.PlanCreatorUtils.supportsField;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.contracts.plan.VariablesCreationBlobRequest;
import io.harness.pms.contracts.plan.VariablesCreationBlobResponse;
import io.harness.pms.contracts.plan.YamlFieldBlob;
import io.harness.pms.gitsync.PmsGitSyncBranchContextGuard;
import io.harness.pms.gitsync.PmsGitSyncHelper;
import io.harness.pms.sdk.core.plan.creation.creators.PipelineServiceInfoProvider;
import io.harness.pms.sdk.core.variables.beans.VariableCreationContext;
import io.harness.pms.sdk.core.variables.beans.VariableCreationResponse;
import io.harness.pms.yaml.YamlField;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@OwnedBy(HarnessTeam.PIPELINE)
@Singleton
public class VariableCreatorService {
  private final PipelineServiceInfoProvider pipelineServiceInfoProvider;
  private final PmsGitSyncHelper pmsGitSyncHelper;

  @Inject
  public VariableCreatorService(
      PipelineServiceInfoProvider pipelineServiceInfoProvider, PmsGitSyncHelper pmsGitSyncHelper) {
    this.pipelineServiceInfoProvider = pipelineServiceInfoProvider;
    this.pmsGitSyncHelper = pmsGitSyncHelper;
  }

  public VariablesCreationBlobResponse createVariablesResponse(VariablesCreationBlobRequest request) {
    Map<String, YamlFieldBlob> dependencyBlobs = request.getDependenciesMap();
    Map<String, YamlField> initialDependencies = new HashMap<>();

    if (isNotEmpty(dependencyBlobs)) {
      try {
        for (Map.Entry<String, YamlFieldBlob> entry : dependencyBlobs.entrySet()) {
          initialDependencies.put(entry.getKey(), YamlField.fromFieldBlob(entry.getValue()));
        }
      } catch (Exception e) {
        throw new InvalidRequestException("Invalid YAML found in dependency blobs");
      }
    }

    try (PmsGitSyncBranchContextGuard ignore = pmsGitSyncHelper.createGitSyncBranchContextGuardFromBytes(
             request.getMetadata().getGitSyncBranchContext(), true)) {
      VariableCreationResponse response = processNodesRecursively(initialDependencies);
      return response.toBlobResponse();
    }
  }

  private VariableCreationResponse processNodesRecursively(Map<String, YamlField> initialDependencies) {
    VariableCreationResponse finalResponse = VariableCreationResponse.builder().build();
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

  private void processNodes(Map<String, YamlField> dependencies, VariableCreationResponse finalResponse) {
    List<YamlField> dependenciesList = new ArrayList<>(dependencies.values());
    dependencies.clear();

    for (YamlField yamlField : dependenciesList) {
      Optional<VariableCreator> variableCreatorOptional =
          findVariableCreator(pipelineServiceInfoProvider.getVariableCreators(), yamlField);

      if (!variableCreatorOptional.isPresent()) {
        finalResponse.addDependency(yamlField);
        continue;
      }

      VariableCreationResponse response;
      VariableCreator variableCreator = variableCreatorOptional.get();

      response = variableCreator.createVariablesForField(
          VariableCreationContext.builder().currentField(yamlField).build(), yamlField);

      if (response == null) {
        finalResponse.addDependency(yamlField);
        continue;
      }
      finalResponse.addYamlProperties(response.getYamlProperties());
      finalResponse.addResolvedDependency(yamlField);
      if (isNotEmpty(response.getDependencies())) {
        response.getDependencies().values().forEach(field -> dependencies.put(field.getNode().getUuid(), field));
      }
    }
  }

  private Optional<VariableCreator> findVariableCreator(List<VariableCreator> variableCreators, YamlField yamlField) {
    if (EmptyPredicate.isEmpty(variableCreators)) {
      return Optional.empty();
    }
    return variableCreators.stream()
        .filter(variableCreator -> {
          Map<String, Set<String>> supportedTypes = variableCreator.getSupportedTypes();
          return supportsField(supportedTypes, yamlField);
        })
        .findFirst();
  }
}
