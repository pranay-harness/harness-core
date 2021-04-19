package io.harness.batch.processing.entities;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(HarnessTeam.CE)
public class ClusterDataDetails {
  int entriesCount;
  double billingAmountSum;
}
