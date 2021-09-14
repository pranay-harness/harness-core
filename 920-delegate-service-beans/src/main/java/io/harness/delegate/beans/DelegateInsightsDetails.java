/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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
