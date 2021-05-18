package io.harness.cdng.manifest.steps;

import static java.lang.String.format;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.manifest.mappers.ManifestOutcomeMapper;
import io.harness.cdng.manifest.yaml.ManifestAttributes;
import io.harness.cdng.service.steps.ServiceStepsHelper;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.UnexpectedTypeException;
import io.harness.executions.steps.ExecutionNodeType;
import io.harness.logstreaming.NGLogCallback;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.steps.executables.SyncExecutable;
import io.harness.pms.sdk.core.steps.io.PassThroughData;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepOutcome;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.LinkedList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.CDC)
@Singleton
@Slf4j
public class ManifestStep implements SyncExecutable<ManifestStepParameters> {
  public static final StepType STEP_TYPE = StepType.newBuilder().setType(ExecutionNodeType.MANIFEST.getName()).build();

  @Inject private ServiceStepsHelper serviceStepsHelper;

  @Override
  public Class<ManifestStepParameters> getStepParametersClass() {
    return ManifestStepParameters.class;
  }

  @Override
  public StepResponse executeSync(Ambiance ambiance, ManifestStepParameters stepParameters,
      StepInputPackage inputPackage, PassThroughData passThroughData) {
    NGLogCallback logCallback = serviceStepsHelper.getServiceLogCallback(ambiance);
    logCallback.saveExecutionLog(format("Processing manifest [%s]...", stepParameters.getIdentifier()));
    ManifestAttributes finalManifest = applyManifestsOverlay(stepParameters);
    logCallback.saveExecutionLog(format("Processed manifest [%s]", stepParameters.getIdentifier()));
    return StepResponse.builder()
        .status(Status.SUCCEEDED)
        .stepOutcome(StepOutcome.builder()
                         .name("output")
                         .outcome(ManifestOutcomeMapper.toManifestOutcome(finalManifest))
                         .build())
        .build();
  }

  private ManifestAttributes applyManifestsOverlay(ManifestStepParameters stepParameters) {
    List<ManifestAttributes> manifestList = new LinkedList<>();
    // 1. Original manifests
    if (stepParameters.getSpec() != null) {
      manifestList.add(stepParameters.getSpec());
    }
    // 2. Override sets
    if (stepParameters.getOverrideSets() != null) {
      manifestList.addAll(stepParameters.getOverrideSets());
    }
    // 3. Stage Overrides
    if (stepParameters.getStageOverride() != null) {
      manifestList.add(stepParameters.getStageOverride());
    }
    if (EmptyPredicate.isEmpty(manifestList)) {
      throw new InvalidArgumentsException("No manifests defined");
    }
    ManifestAttributes resultantManifest = manifestList.get(0);
    for (ManifestAttributes manifest : manifestList.subList(1, manifestList.size())) {
      if (!manifest.getKind().equals(resultantManifest.getKind())) {
        throw new UnexpectedTypeException(
            format("Unable to apply manifest override of type '%s' to manifest of type '%s' with identifier '%s'",
                manifest.getKind(), resultantManifest.getKind(), resultantManifest.getIdentifier()));
      }

      resultantManifest = resultantManifest.applyOverrides(manifest);
    }
    return resultantManifest;
  }
}
