/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.core.beans;

import io.harness.cvng.beans.change.ChangeCategory;

import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ChangeSummaryDTO {
  Map<ChangeCategory, CategoryCountDetails> categoryCountMap;

  @Value
  @Builder
  public static class CategoryCountDetails {
    long count;
    long countInPrecedingWindow;
  }
}
