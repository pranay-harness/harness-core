package io.harness.delegate.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@OwnedBy(HarnessTeam.DEL)
@FieldNameConstants(innerTypeName = "DelegateInsightsDetailsKeys")
@Value
@Builder
public class DelegateInsightsDetails {
  private List<DelegateInsightsBarDetails> insights;
}
