package io.harness.cdng.variables;

import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotNull;

@Value
@Builder
public class Variable {
  @NotNull private String name;
  @NotNull private String value;
  @NotNull private String type;
}
