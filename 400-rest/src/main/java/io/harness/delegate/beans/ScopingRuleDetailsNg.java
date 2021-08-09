package io.harness.delegate.beans;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import java.util.Set;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

@Data
@Builder
@FieldNameConstants(innerTypeName = "ScopingRuleDetailsNGKeys")
@TargetModule(HarnessModule._420_DELEGATE_SERVICE)
public class ScopingRuleDetailsNg {
  private String description;

  private String environmentTypeId;
  private Set<String> environmentIds;
}
