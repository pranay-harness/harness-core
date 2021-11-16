package io.harness.overviewdashboard.dtos;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@OwnedBy(HarnessTeam.PL)
public class CountChangeDetails extends CountInfo {
  CountChangeAndCountChangeRateInfo countChangeAndCountChangeRateInfo;
}
