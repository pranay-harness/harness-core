package io.harness.delegate.beans;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

import java.util.List;

@Data
@Builder
@FieldNameConstants(innerTypeName = "DelegateProfileDetailsNgKeys")
public class DelegateProfileDetailsNg {
  private String uuid;
  private String accountId;

  private String name;
  private String description;
  private boolean primary;
  private boolean approvalRequired;
  private String startupScript;

  private List<ScopingRuleDetailsNg> scopingRules;
  private List<String> selectors;

  private EmbeddedUserDetails createdBy;
  private EmbeddedUserDetails lastUpdatedBy;

  private String identifier;

  private long numberOfDelegates;
}
