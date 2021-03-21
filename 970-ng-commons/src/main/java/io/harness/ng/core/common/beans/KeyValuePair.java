package io.harness.ng.core.common.beans;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

@Data
@Builder
@FieldNameConstants(innerTypeName = "KeyValuePairKeys")
public class KeyValuePair {
  @NotNull private String key;
  @NotNull private String value;
}