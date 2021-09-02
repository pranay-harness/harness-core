package io.harness.pms.yaml;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@RecasterAlias("io.harness.pms.yaml.ParameterFieldValueWrapper")
@OwnedBy(HarnessTeam.PIPELINE)
public class ParameterFieldValueWrapper<T> {
  public static final String VALUE_FIELD = "value";

  private final T value;
}
