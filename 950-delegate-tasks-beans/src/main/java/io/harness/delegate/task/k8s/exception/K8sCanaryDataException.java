package io.harness.delegate.task.k8s.exception;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.DataException;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@OwnedBy(CDP)
@EqualsAndHashCode(callSuper = false)
public class K8sCanaryDataException extends DataException {
  String canaryWorkload;
  boolean canaryWorkloadDeployed;

  @Builder(builderMethodName = "dataBuilder")
  public K8sCanaryDataException(String canaryWorkload, boolean canaryWorkloadDeployed, Throwable cause) {
    super(cause);
    this.canaryWorkload = canaryWorkload;
    this.canaryWorkloadDeployed = canaryWorkloadDeployed;
  }
}
