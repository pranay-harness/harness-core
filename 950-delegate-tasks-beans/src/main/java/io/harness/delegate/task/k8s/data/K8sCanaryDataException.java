package io.harness.delegate.task.k8s.data;

import io.harness.exception.DataException;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@EqualsAndHashCode(callSuper = true)
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
