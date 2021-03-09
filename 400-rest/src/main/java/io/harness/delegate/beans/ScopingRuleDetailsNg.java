package io.harness.delegate.beans;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

import java.util.Set;

@Data
@Builder
@FieldNameConstants(innerTypeName = "ScopingRuleDetailsNGKeys")
public class ScopingRuleDetailsNg {
  private String description;

  private String environmentTypeId;
  private Set<String> environmentIds;
}
