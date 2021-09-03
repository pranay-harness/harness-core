package io.harness.cvng.core.beans;

import io.harness.cvng.core.types.ChangeCategory;

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
