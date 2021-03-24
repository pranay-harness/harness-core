package io.harness.pms.yaml;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParameterFieldValueWrapper<T> {
  public static final String VALUE_FIELD = "value";

  private T value;
}
