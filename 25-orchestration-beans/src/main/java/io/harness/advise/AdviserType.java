package io.harness.advise;

import io.harness.annotations.Redesign;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@Redesign
public class AdviserType {
  // Provided From the orchestration layer system advisers
  private static final String NEXT = "ASYNC";
  private static final String ON_FAIL = "ASYNC";
  private static final String ON_SUCCESS = "ASYNC";

  String type;
}
