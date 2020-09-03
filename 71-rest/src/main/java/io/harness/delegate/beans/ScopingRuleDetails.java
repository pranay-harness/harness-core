package io.harness.delegate.beans;

import lombok.Builder;
import lombok.Data;

import java.util.Set;

@Data
@Builder
public class ScopingRuleDetails {
  private String description;

  private String applicationId;
  private Set<String> serviceIds;
  private Set<String> environmentIds;
}
