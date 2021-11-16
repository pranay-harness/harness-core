package io.harness.pms.sdk.core.plan.creation.creators;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.pms.contracts.plan.Dependencies;
import io.harness.pms.plan.creation.PlanCreationBlobResponseUtils;
import io.harness.pms.plan.creation.PlanCreatorUtils;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.PIPELINE)
@UtilityClass
public class PlanCreatorServiceHelper {
  public Optional<PartialPlanCreator<?>> findPlanCreator(List<PartialPlanCreator<?>> planCreators, YamlField field) {
    return planCreators.stream()
        .filter(planCreator -> {
          Map<String, Set<String>> supportedTypes = planCreator.getSupportedTypes();
          return PlanCreatorUtils.supportsField(supportedTypes, field);
        })
        .findFirst();
  }

  public Dependencies handlePlanCreationResponses(List<PlanCreationResponse> planCreationResponses,
      PlanCreationResponse finalResponse, String currentYaml, Dependencies dependencies,
      List<Map.Entry<String, String>> dependenciesList) {
    String updatedYaml = currentYaml;
    List<String> errorMessages = planCreationResponses.stream()
                                     .filter(resp -> resp != null && EmptyPredicate.isNotEmpty(resp.getErrorMessages()))
                                     .flatMap(resp -> resp.getErrorMessages().stream())
                                     .collect(Collectors.toList());
    if (EmptyPredicate.isNotEmpty(errorMessages)) {
      finalResponse.setErrorMessages(errorMessages);
      return dependencies.toBuilder().clearDependencies().build();
    }

    Map<String, String> newDependencies = new HashMap<>();
    for (int i = 0; i < dependenciesList.size(); i++) {
      Map.Entry<String, String> entry = dependenciesList.get(i);
      String fieldYamlPath = entry.getValue();
      PlanCreationResponse response = planCreationResponses.get(i);
      if (response == null) {
        finalResponse.addDependency(currentYaml, entry.getKey(), fieldYamlPath);
        continue;
      }

      mergeCurrentResponseWithFinalResponse(response, finalResponse);
      if (response.getDependencies() != null
          && EmptyPredicate.isNotEmpty(response.getDependencies().getDependenciesMap())) {
        newDependencies.putAll(response.getDependencies().getDependenciesMap());
      }
      if (response.getYamlUpdates() != null && EmptyPredicate.isNotEmpty(response.getYamlUpdates().getFqnToYamlMap())) {
        updatedYaml = PlanCreationBlobResponseUtils.mergeYamlUpdates(
            currentYaml, finalResponse.getYamlUpdates().getFqnToYamlMap());
        finalResponse.updateYamlInDependencies(updatedYaml);
      }
    }
    return dependencies.toBuilder()
        .setYaml(updatedYaml)
        .clearDependencies()
        .putAllDependencies(newDependencies)
        .build();
  }

  public void decorateNodesWithStageFqn(YamlField field, PlanCreationResponse planForField) {
    String stageFqn = YamlUtils.getStageFqnPath(field.getNode());
    if (!EmptyPredicate.isEmpty(stageFqn)) {
      planForField.getNodes().forEach((k, v) -> v.setStageFqn(stageFqn));
    }
  }

  public Dependencies removeInitialDependencies(Dependencies dependencies, Dependencies initialDependencies) {
    if (isEmptyDependencies(initialDependencies) || isEmptyDependencies(dependencies)) {
      return dependencies;
    }

    Dependencies.Builder builder = dependencies.toBuilder();
    initialDependencies.getDependenciesMap().keySet().forEach(builder::removeDependencies);
    return builder.build();
  }

  protected boolean isEmptyDependencies(Dependencies dependencies) {
    return dependencies == null || EmptyPredicate.isEmpty(dependencies.getDependenciesMap());
  }

  protected void mergeCurrentResponseWithFinalResponse(
      PlanCreationResponse response, PlanCreationResponse finalResponse) {
    finalResponse.addNodes(response.getNodes());
    finalResponse.mergeContext(response.getContextMap());
    finalResponse.mergeLayoutNodeInfo(response.getGraphLayoutResponse());
    finalResponse.mergeStartingNodeId(response.getStartingNodeId());
    if (response.getYamlUpdates() != null && EmptyPredicate.isNotEmpty(response.getYamlUpdates().getFqnToYamlMap())) {
      finalResponse.addYamlUpdates(response.getYamlUpdates());
    }
  }
}
