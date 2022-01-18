package io.harness.ccm.commons.entities;

import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CCMStringFilter {
  CCMField field;
  CCMOperator operator;
  List<String> values;
}
