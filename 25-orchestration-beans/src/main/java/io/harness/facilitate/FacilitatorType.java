package io.harness.facilitate;

import io.harness.annotations.Redesign;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@Redesign
public class FacilitatorType {
  // Provided From the orchestration layer system facilitators
  public static final String SYNC = "SYNC";
  public static final String ASYNC = "ASYNC";

  String type;
}
