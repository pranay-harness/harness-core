package io.harness.cdng.manifest.state;

import static io.harness.cdng.manifest.ManifestConstants.MANIFESTS;

import io.harness.cdng.manifest.yaml.ManifestAttributes;
import io.harness.cdng.manifest.yaml.ManifestConfigWrapper;
import io.harness.cdng.manifest.yaml.ManifestOutcome;
import io.harness.cdng.service.beans.ServiceConfig;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepOutcome;
import io.harness.pms.yaml.ParameterField;
import io.harness.yaml.core.intfc.WithIdentifier;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class ManifestStep {
  public StepOutcome processManifests(ServiceConfig serviceConfig) {
    List<ManifestConfigWrapper> serviceSpecManifests =
        serviceConfig.getServiceDefinition().getServiceSpec().getManifests();
    List<ManifestConfigWrapper> manifestOverrideSets = getManifestOverrideSetsApplicable(serviceConfig);

    List<ManifestConfigWrapper> stageOverrideManifests = new LinkedList<>();
    if (serviceConfig.getStageOverrides() != null) {
      stageOverrideManifests = serviceConfig.getStageOverrides().getManifests();
    }

    return processManifests(serviceSpecManifests, manifestOverrideSets, stageOverrideManifests);
  }

  private List<ManifestConfigWrapper> getManifestOverrideSetsApplicable(ServiceConfig serviceConfig) {
    List<ManifestConfigWrapper> manifestOverrideSets = new LinkedList<>();
    if (serviceConfig.getStageOverrides() != null
        && !ParameterField.isNull(serviceConfig.getStageOverrides().getUseManifestOverrideSets())) {
      serviceConfig.getStageOverrides()
          .getUseManifestOverrideSets()
          .getValue()
          .stream()
          .map(useManifestOverrideSet
              -> serviceConfig.getServiceDefinition()
                     .getServiceSpec()
                     .getManifestOverrideSets()
                     .stream()
                     .filter(o -> o.getIdentifier().equals(useManifestOverrideSet))
                     .findFirst())
          .forEachOrdered(optionalManifestOverrideSets -> {
            if (!optionalManifestOverrideSets.isPresent()) {
              throw new InvalidRequestException("Manifest Override Set is not defined.");
            }
            manifestOverrideSets.addAll(optionalManifestOverrideSets.get().getManifests());
          });
    }
    return manifestOverrideSets;
  }

  @VisibleForTesting
  StepOutcome processManifests(List<ManifestConfigWrapper> serviceSpecManifests,
      List<ManifestConfigWrapper> manifestOverrideSets, List<ManifestConfigWrapper> stageOverrideManifests) {
    Map<String, ManifestAttributes> identifierToManifestMap = new HashMap<>();

    // 1. Get Manifests belonging to KubernetesServiceSpec
    if (EmptyPredicate.isNotEmpty(serviceSpecManifests)) {
      identifierToManifestMap = serviceSpecManifests.stream().collect(
          Collectors.toMap(WithIdentifier::getIdentifier, ManifestConfigWrapper::getManifestAttributes, (a, b) -> b));
    }

    // 2. Apply Override Sets
    applyManifestOverlay(identifierToManifestMap, manifestOverrideSets);

    // 3. Get Manifests belonging to Stage Overrides
    applyManifestOverlay(identifierToManifestMap, stageOverrideManifests);

    return StepOutcome.builder()
        .name(MANIFESTS.toLowerCase())
        .outcome(
            ManifestOutcome.builder().manifestAttributes(new ArrayList<>(identifierToManifestMap.values())).build())
        .build();
  }

  private void applyManifestOverlay(
      Map<String, ManifestAttributes> identifierToManifestMap, List<ManifestConfigWrapper> stageOverrideManifests) {
    if (EmptyPredicate.isNotEmpty(stageOverrideManifests)) {
      stageOverrideManifests.forEach(stageOverrideManifest -> {
        if (identifierToManifestMap.containsKey(stageOverrideManifest.getIdentifier())) {
          identifierToManifestMap.put(stageOverrideManifest.getIdentifier(),
              identifierToManifestMap.get(stageOverrideManifest.getIdentifier())
                  .applyOverrides(stageOverrideManifest.getManifestAttributes()));
        } else {
          identifierToManifestMap.put(
              stageOverrideManifest.getIdentifier(), stageOverrideManifest.getManifestAttributes());
        }
      });
    }
  }
}
