package io.harness.delegate.beans;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DelegateProfileDetails {
  private String uuid;
  private String accountId;

  private String name;
  private String description;
  private boolean primary;
  private boolean approvalRequired;
  private String startupScript;

  private List<ScopingRuleDetails> scopingRules;
}
