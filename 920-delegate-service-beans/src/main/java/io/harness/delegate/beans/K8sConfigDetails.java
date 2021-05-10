package io.harness.delegate.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(HarnessTeam.DEL)
public class K8sConfigDetails {
  private K8sPermissionType k8sPermissionType;
  private String namespace;
}
