package io.harness.cdng.k8s.beans;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.infra.beans.InfrastructureOutcome;
import io.harness.delegate.beans.logstreaming.UnitProgressData;
import io.harness.pms.sdk.core.steps.io.PassThroughData;

import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@OwnedBy(CDP)
@TypeAlias("k8sExecutionPassThroughData")
@RecasterAlias("io.harness.cdng.k8s.beans.K8sExecutionPassThroughData")
public class K8sExecutionPassThroughData implements PassThroughData {
  InfrastructureOutcome infrastructure;
  UnitProgressData lastActiveUnitProgressData;
}
