package io.harness.ng.core.common.beans;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

@OwnedBy(PL)
@Data
@Builder
@FieldNameConstants(innerTypeName = "KeyValuePairKeys")
public class KeyValuePair {
  @NotNull private String key;
  @NotNull private String value;
}