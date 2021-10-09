package io.harness.ng.cdOverview.dto;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Data;

@OwnedBy(HarnessTeam.DX)
@Data
@Builder
public class DeploymentChangeRates {
  double failureRate;
  double failureRateChangeRate;
  double frequency;
  double frequencyChangeRate;
}
