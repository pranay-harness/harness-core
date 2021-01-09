package io.harness.cdng.manifest.state;

import static io.harness.cdng.manifest.ManifestConstants.MANIFESTS;

import io.harness.cdng.manifest.mappers.ManifestOutcomeMapper;
import io.harness.cdng.manifest.yaml.ManifestAttributes;
import io.harness.cdng.manifest.yaml.ManifestConfigWrapper;
import io.harness.cdng.manifest.yaml.ManifestOutcome;
import io.harness.cdng.manifest.yaml.ManifestOverrideSets;
import io.harness.cdng.manifest.yaml.ManifestsOutcome;
import io.harness.cdng.manifest.yaml.ManifestsOutcome.ManifestsOutcomeBuilder;
import io.harness.cdng.service.beans.ServiceConfig;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepOutcome;
import io.harness.pms.yaml.ParameterField;

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

    return processManifests(serviceSpecManifests, manifestOverrideSets, stageOverrideManifests,
        serviceConfig.getServiceDefinition().getServiceSpec().getManifestOverrideSets());
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
      List<ManifestConfigWrapper> applicableManifestOverrideSets, List<ManifestConfigWrapper> stageOverrideManifests,
      List<ManifestOverrideSets> allOverrideSets) {
    Map<String, ManifestAttributes> identifierToManifestMap = new HashMap<>();

    // 1. Get Manifests belonging to KubernetesServiceSpec
    if (EmptyPredicate.isNotEmpty(serviceSpecManifests)) {
      identifierToManifestMap = serviceSpecManifests.stream().collect(Collectors.toMap(serviceSpecManifest
          -> serviceSpecManifest.getManifest().getIdentifier(),
          serviceSpecManifest -> serviceSpecManifest.getManifest().getManifestAttributes(), (a, b) -> b));
    }

    ManifestsOutcomeBuilder outcomeBuilder = ManifestsOutcome.builder();
    outcomeBuilder.manifestOriginalList(
        ManifestOutcomeMapper.toManifestOutcome(new ArrayList<>(identifierToManifestMap.values())));

    // 2. Apply Override Sets
    applyManifestOverlay(identifierToManifestMap, applicableManifestOverrideSets);
    outcomeBuilder.manifestOverrideSets(getAllOverrideSets(allOverrideSets));

    // 3. Get Manifests belonging to Stage Overrides
    applyManifestOverlay(identifierToManifestMap, stageOverrideManifests);

    Map<String, ManifestAttributes> onlyStageOverridesManifests = new HashMap<>();
    applyManifestOverlay(onlyStageOverridesManifests, stageOverrideManifests);
    outcomeBuilder.manifestStageOverridesList(
        ManifestOutcomeMapper.toManifestOutcome(new ArrayList<>(onlyStageOverridesManifests.values())));

    return StepOutcome.builder()
        .name(MANIFESTS.toLowerCase())
        .outcome(outcomeBuilder
                     .manifestOutcomeList(
                         ManifestOutcomeMapper.toManifestOutcome(new ArrayList<>(identifierToManifestMap.values())))
                     .build())
        .build();
  }

  private Map<String, List<ManifestOutcome>> getAllOverrideSets(List<ManifestOverrideSets> allOverrideSets) {
    Map<String, List<ManifestOutcome>> manifestOverridesMap = new HashMap<>();
    if (EmptyPredicate.isEmpty(allOverrideSets)) {
      return manifestOverridesMap;
    }
    for (ManifestOverrideSets overrideSet : allOverrideSets) {
      manifestOverridesMap.put(overrideSet.getIdentifier(),
          ManifestOutcomeMapper.toManifestOutcome(new ArrayList<>(overrideSet.getManifests()
                                                                      .stream()
                                                                      .map(m -> m.getManifest().getManifestAttributes())
                                                                      .collect(Collectors.toList()))));
    }
    return manifestOverridesMap;
  }

  private void applyManifestOverlay(
      Map<String, ManifestAttributes> identifierToManifestMap, List<ManifestConfigWrapper> overrideManifests) {
    if (EmptyPredicate.isNotEmpty(overrideManifests)) {
      overrideManifests.forEach(stageOverrideManifest -> {
        if (identifierToManifestMap.containsKey(stageOverrideManifest.getManifest().getIdentifier())) {
          identifierToManifestMap.put(stageOverrideManifest.getManifest().getIdentifier(),
              identifierToManifestMap.get(stageOverrideManifest.getManifest().getIdentifier())
                  .applyOverrides(stageOverrideManifest.getManifest().getManifestAttributes()));
        } else {
          identifierToManifestMap.put(stageOverrideManifest.getManifest().getIdentifier(),
              stageOverrideManifest.getManifest().getManifestAttributes());
        }
      });
    }
  }
}
