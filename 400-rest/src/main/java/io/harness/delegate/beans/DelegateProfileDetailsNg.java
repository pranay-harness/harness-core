package io.harness.delegate.beans;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

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
}
