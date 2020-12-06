package io.harness.delegate.beans;

import java.util.Set;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

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
