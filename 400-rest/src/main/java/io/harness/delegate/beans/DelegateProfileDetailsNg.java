package io.harness.delegate.beans;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

@Data
@Builder
@FieldNameConstants(innerTypeName = "DelegateProfileDetailsNgKeys")
@TargetModule(HarnessModule._420_DELEGATE_SERVICE)
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
  private long createdAt;
  private long lastUpdatedAt;

  private String identifier;

  private long numberOfDelegates;

  private String orgIdentifier;
  private String projectIdentifier;
}
