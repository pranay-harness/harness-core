package io.harness.delegate.beans.executioncapability;

import io.harness.delegate.beans.executioncapability.CapabilityType;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.k8s.model.HelmVersion;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class HelmInstallationCapability implements ExecutionCapability {
  HelmVersion version;
  String criteria;
  CapabilityType capabilityType = CapabilityType.HELM_INSTALL;

  @Override
  public EvaluationMode evaluationMode() {
    return EvaluationMode.AGENT;
  }

  @Override
  public String fetchCapabilityBasis() {
    return criteria;
  }
}
