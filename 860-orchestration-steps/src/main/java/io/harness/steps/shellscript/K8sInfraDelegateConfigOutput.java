package io.harness.steps.shellscript;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.task.k8s.K8sInfraDelegateConfig;
import io.harness.pms.sdk.core.data.ExecutionSweepingOutput;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(CDP)
public class K8sInfraDelegateConfigOutput implements ExecutionSweepingOutput {
  K8sInfraDelegateConfig k8sInfraDelegateConfig;
}
