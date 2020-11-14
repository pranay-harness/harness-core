package io.harness.references;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder
@Redesign
@EqualsAndHashCode
public class RefType {
  public static final String SWEEPING_OUTPUT = "SWEEPING_OUTPUT";
  public static final String OUTCOME = "OUTCOME";
  String type;
}
