package io.harness.cdng.manifest.state;

import static io.harness.cdng.manifest.ManifestConstants.MANIFESTS;
import static io.harness.cdng.manifest.ManifestConstants.MANIFEST_STEP;

import io.harness.ambiance.Ambiance;
import io.harness.cdng.manifest.yaml.ManifestAttributes;
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
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ManifestStep implements Step, SyncExecutable {
  public static final StepType STEP_TYPE = StepType.builder().type(MANIFEST_STEP).build();

  @Override
  public StepResponse executeSync(Ambiance ambiance, StepParameters stepParameters, StepInputPackage inputPackage,
      PassThroughData passThroughData) {
    ManifestStepParameters parameters = (ManifestStepParameters) stepParameters;
    ManifestListConfig manifestListConfig = parameters.getManifestServiceSpec();

    // 1. Get Manifests belonging to ServiceSpec
    List<ManifestAttributes> manifestAttributesForServiceSpec = new ArrayList<>();
    if (EmptyPredicate.isNotEmpty(manifestListConfig.getManifests())) {
      manifestListConfig.getManifests().forEach(
          manifestConfigWrapper -> manifestAttributesForServiceSpec.add(manifestConfigWrapper.getManifestAttributes()));
    }

    // 2. Get Manifests belonging to Override
    manifestListConfig = parameters.getManifestStageOverride();
    List<ManifestAttributes> manifestAttributesForOverride = new ArrayList<>();
    if (EmptyPredicate.isNotEmpty(manifestListConfig.getManifests())) {
      manifestListConfig.getManifests().forEach(
          manifestConfigWrapper -> manifestAttributesForOverride.add(manifestConfigWrapper.getManifestAttributes()));
    }

    // 3. Get Manifests belonging to OverrideSets, Not done yet

    return StepResponse.builder()
        .status(Status.SUCCEEDED)
        .stepOutcome(StepResponse.StepOutcome.builder()
                         .name(MANIFESTS.toLowerCase())
                         .outcome(ManifestOutcome.builder()
                                      .manifestAttributesForServiceSpec(manifestAttributesForServiceSpec)
                                      .manifestAttributesForOverride(manifestAttributesForOverride)
                                      .build())
                         .build())
        .build();
  }
}
