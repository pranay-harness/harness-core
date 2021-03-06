package io.harness.delegate.beans;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.EmbeddedUser;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

@Data
@Builder
@FieldNameConstants(innerTypeName = "DelegateProfileDetailsNgKeys")
@TargetModule(Module._420_DELEGATE_SERVICE)
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

  private EmbeddedUser createdBy;
  private EmbeddedUser lastUpdatedBy;

  private String identifier;

  private long numberOfDelegates;
}
