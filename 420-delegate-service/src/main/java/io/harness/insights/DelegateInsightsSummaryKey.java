package io.harness.insights;

import static io.harness.annotations.dev.HarnessTeam.DEL;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.DelegateInsightsType;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@Builder
@EqualsAndHashCode
@OwnedBy(DEL)
public class DelegateInsightsSummaryKey {
  private String accountId;
  private DelegateInsightsType insightsType;
  private long periodStartTime;
  private String delegateGroupId;
}
