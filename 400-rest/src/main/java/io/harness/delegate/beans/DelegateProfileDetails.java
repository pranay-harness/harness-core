/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.beans;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

@Data
@Builder
@FieldNameConstants(innerTypeName = "DelegateProfileDetailsKeys")
@TargetModule(HarnessModule._420_DELEGATE_SERVICE)
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
