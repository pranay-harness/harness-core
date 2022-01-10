/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.beans;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.data.validator.EntityName;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;

@Data
@Builder
@FieldNameConstants(innerTypeName = "DelegateProfileDetailsNgKeys")
@TargetModule(HarnessModule._420_DELEGATE_SERVICE)
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DelegateProfileDetailsNg {
  private String uuid;
  @ApiModelProperty private String accountId;

  @EntityName private String name;
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

  @ApiModelProperty private String identifier;

  private long numberOfDelegates;

  @ApiModelProperty private String orgIdentifier;
  @ApiModelProperty private String projectIdentifier;
}
