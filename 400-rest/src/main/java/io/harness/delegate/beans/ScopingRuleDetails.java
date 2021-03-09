package io.harness.delegate.beans;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

import java.util.Set;

@Data
@Builder
@FieldNameConstants(innerTypeName = "ScopingRuleDetailsKeys")
public class ScopingRuleDetails {
  private String description;

  private String applicationId;
  private Set<String> serviceIds;
  private String environmentTypeId;
  private Set<String> environmentIds;
}
