package io.harness.facilitate;

import io.harness.annotations.Redesign;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@Redesign
public class FacilitatorType {
  // Provided From the orchestration layer system facilitators
  private static final String SYNC = "ASYNC";
  private static final String ASYNC = "ASYNC";

  String type;
}
