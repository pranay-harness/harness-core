/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.dto;

import io.harness.delegate.beans.DelegateProfileDetailsNg;
import io.harness.delegate.beans.ScopingRuleDetailsNg;
import io.harness.gitsync.beans.YamlDTO;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DelegateProfileDetailsNgDTO implements YamlDTO {
  private String accountId;
  private String orgIdentifier;
  private String projectIdentifier;

  private String name;
  private String identifier;
  private String description;
  private boolean primary;
  private boolean approvalRequired;
  private String startupScript;

  private List<ScopingRuleDetailsNg> scopingRules;
  private List<String> selectors;

  private long createdAt;
  private long lastUpdatedAt;

  private long numberOfDelegates;

  public static DelegateProfileDetailsNgDTO fromEntity(DelegateProfileDetailsNg delegateProfileDetailsNg) {
    return DelegateProfileDetailsNgDTO.builder()
        .accountId(delegateProfileDetailsNg.getAccountId())
        .orgIdentifier(delegateProfileDetailsNg.getOrgIdentifier())
        .projectIdentifier(delegateProfileDetailsNg.getProjectIdentifier())
        .name(delegateProfileDetailsNg.getName())
        .identifier(delegateProfileDetailsNg.getUuid())
        .description(delegateProfileDetailsNg.getDescription())
        .primary(delegateProfileDetailsNg.isPrimary())
        .approvalRequired(delegateProfileDetailsNg.isApprovalRequired())
        .startupScript(delegateProfileDetailsNg.getStartupScript())
        .scopingRules(delegateProfileDetailsNg.getScopingRules())
        .selectors(delegateProfileDetailsNg.getSelectors())
        .createdAt(delegateProfileDetailsNg.getCreatedAt())
        .lastUpdatedAt(delegateProfileDetailsNg.getLastUpdatedAt())
        .numberOfDelegates(delegateProfileDetailsNg.getNumberOfDelegates())
        .build();
  }
}
