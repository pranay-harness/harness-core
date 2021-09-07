package io.harness.delegate.beans;

import java.util.Set;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

@Data
@Builder
@FieldNameConstants(innerTypeName = "ScopingRuleDetailsNGKeys")
public class ScopingRuleDetailsNg {
  private String description;

  private String environmentTypeId;
  private Set<String> environmentIds;
}
