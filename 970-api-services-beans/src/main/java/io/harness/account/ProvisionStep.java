package io.harness.account;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

@Data
@Builder
@FieldNameConstants(innerTypeName = "ProvisionStepKeys")
public class ProvisionStep {
  private String step;
  private boolean done;
}
