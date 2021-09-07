package io.harness.ng.cdOverview.dto;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.AllArgsConstructor;
import lombok.Getter;

@OwnedBy(HarnessTeam.DX)
@Getter
@AllArgsConstructor
public class TimeValuePair<T> {
  long timestamp;
  T value;
}
