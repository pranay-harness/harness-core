package io.harness.pms.yaml;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ParameterFieldValueWrapper<T> {
  public static final String VALUE_FIELD = "value";

  private T value;
}
