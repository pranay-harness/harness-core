package io.harness.cdng.k8s;

import io.harness.cdng.infra.beans.InfrastructureOutcome;
import io.harness.cdng.manifest.yaml.kinds.K8sManifest;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.sdk.core.steps.executables.TaskChainResponse;

import java.util.List;

public interface K8sStepExecutor {
  TaskChainResponse executeK8sTask(K8sManifest k8sManifest, Ambiance ambiance, K8sStepParameters stepParameters,
      List<String> valuesFileContents, InfrastructureOutcome infrastructure);
}
