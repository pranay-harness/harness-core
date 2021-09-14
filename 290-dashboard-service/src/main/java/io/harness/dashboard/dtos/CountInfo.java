package io.harness.dashboard.dtos;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@OwnedBy(HarnessTeam.PL)
public class CountInfo {
  long count;
}
