package io.harness.cdng.k8s.beans;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.logstreaming.UnitProgressData;
import io.harness.pms.sdk.core.steps.io.PassThroughData;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(CDP)
@RecasterAlias("io.harness.cdng.k8s.beans.StepExceptionPassThroughData")
public class StepExceptionPassThroughData implements PassThroughData {
  String errorMessage;
  UnitProgressData unitProgressData;
}
