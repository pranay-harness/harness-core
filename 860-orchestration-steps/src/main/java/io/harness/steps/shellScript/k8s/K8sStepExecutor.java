package io.harness.steps.shellScript.k8s;

import io.harness.steps.shellScript.beans.InfrastructureOutcome;
import io.harness.steps.shellScript.manifest.yaml.ManifestOutcome;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.sdk.core.steps.executables.TaskChainResponse;

import java.util.List;

public interface K8sStepExecutor {
  TaskChainResponse executeK8sTask(ManifestOutcome k8sManifestOutcome, Ambiance ambiance,
      K8sStepParameters stepParameters, List<String> valuesFileContents, InfrastructureOutcome infrastructure);
}
