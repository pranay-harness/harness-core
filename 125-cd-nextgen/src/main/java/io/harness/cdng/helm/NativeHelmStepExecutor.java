package io.harness.cdng.helm;

import io.harness.cdng.helm.beans.NativeHelmExecutionPassThroughData;
import io.harness.cdng.manifest.yaml.ManifestOutcome;
import io.harness.delegate.beans.logstreaming.UnitProgressData;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.sdk.core.steps.executables.TaskChainResponse;

import java.util.List;

public interface NativeHelmStepExecutor {
  TaskChainResponse executeHelmTask(ManifestOutcome HelmChartManifestOutcome, Ambiance ambiance,
      StepElementParameters stepParameters, List<String> valuesFileContents,
      NativeHelmExecutionPassThroughData executionPassThroughData, boolean shouldOpenFetchFilesLogStream,
      UnitProgressData unitProgressData);
}
