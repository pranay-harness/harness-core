package io.harness.timeout;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

import javax.validation.constraints.NotNull;

@OwnedBy(CDC)
@Value
@Builder
@EqualsAndHashCode
public class Dimension {
  public static final String ABSOLUTE = "ABSOLUTE";
  public static final String ACTIVE = "ACTIVE";

  @NotNull String type;
}
