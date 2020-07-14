package io.harness.cdng.manifest.state;

import static io.harness.cdng.manifest.ManifestConstants.MANIFESTS;
import static io.harness.cdng.manifest.ManifestConstants.MANIFEST_STEP;

import io.harness.ambiance.Ambiance;
import io.harness.cdng.manifest.yaml.ManifestAttributes;
import io.harness.cdng.manifest.yaml.ManifestConfigWrapper;
import io.harness.cdng.manifest.yaml.ManifestOutcome;
import io.harness.data.structure.EmptyPredicate;
import io.harness.execution.status.Status;
import io.harness.facilitator.PassThroughData;
import io.harness.facilitator.modes.sync.SyncExecutable;
import io.harness.state.Step;
import io.harness.state.StepType;
import io.harness.state.io.StepInputPackage;
import io.harness.state.io.StepParameters;
import io.harness.state.io.StepResponse;
import io.harness.yaml.core.intfc.WithIdentifier;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class ManifestStep implements Step, SyncExecutable {
  public static final StepType STEP_TYPE = StepType.builder().type(MANIFEST_STEP).build();

  @Override
  public StepResponse executeSync(Ambiance ambiance, StepParameters stepParameters, StepInputPackage inputPackage,
      PassThroughData passThroughData) {
    ManifestStepParameters parameters = (ManifestStepParameters) stepParameters;
    Map<String, ManifestAttributes> identifierToManifestMap = new HashMap<>();

    // 1. Get Manifests belonging to KubernetesServiceSpec
    if (EmptyPredicate.isNotEmpty(parameters.getServiceSpecManifests())) {
      identifierToManifestMap = parameters.getServiceSpecManifests().stream().collect(
          Collectors.toMap(WithIdentifier::getIdentifier, ManifestConfigWrapper::getManifestAttributes, (a, b) -> b));
    }

    // 2. Apply Override Sets
    applyManifestOverlay(identifierToManifestMap, parameters.getManifestOverrideSets());

    // 3. Get Manifests belonging to Stage Overrides
    applyManifestOverlay(identifierToManifestMap, parameters.getStageOverrideManifests());

    return StepResponse.builder()
        .status(Status.SUCCEEDED)
        .stepOutcome(StepResponse.StepOutcome.builder()
                         .name(MANIFESTS.toLowerCase())
                         .outcome(ManifestOutcome.builder()
                                      .manifestAttributes(new ArrayList<>(identifierToManifestMap.values()))
                                      .build())
                         .build())
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
