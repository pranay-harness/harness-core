package io.harness.delegate.beans;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

import java.util.List;

@Data
@Builder
@FieldNameConstants(innerTypeName = "DelegateProfileDetailsKeys")
@TargetModule(Module._420_DELEGATE_SERVICE)
public class DelegateProfileDetails {
  private String uuid;
  private String accountId;

  private String name;
  private String description;
  private boolean primary;
  private boolean approvalRequired;
  private String startupScript;

  private List<ScopingRuleDetails> scopingRules;
  private List<String> selectors;

  private EmbeddedUserDetails createdBy;
  private EmbeddedUserDetails lastUpdatedBy;

  private String identifier;
}
