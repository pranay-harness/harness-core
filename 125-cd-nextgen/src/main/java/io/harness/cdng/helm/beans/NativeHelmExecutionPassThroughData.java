package io.harness.cdng.helm.beans;

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
@TypeAlias("NativeHelmExecutionPassThroughData")
@RecasterAlias("io.harness.cdng.helm.beans.NativeHelmExecutionPassThroughData")
public class NativeHelmExecutionPassThroughData implements PassThroughData {
  InfrastructureOutcome infrastructure;
  UnitProgressData lastActiveUnitProgressData;
}
