package io.harness.ccm.commons.entities;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CCMNumberFilter {
  CCMField field;
  CCMOperator operator;
  Number value;
}
