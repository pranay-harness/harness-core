/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.api;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.delegate.beans.DelegateProfileDetailsNg;
import io.harness.delegate.beans.ScopingRuleDetailsNg;
import io.harness.delegate.filter.DelegateProfileFilterPropertiesDTO;

import java.util.List;

@OwnedBy(HarnessTeam.DEL)
public interface DelegateProfileManagerNgService {
  PageResponse<DelegateProfileDetailsNg> list(
      String accountId, PageRequest<DelegateProfileDetailsNg> pageRequest, String orgId, String projectId);

  PageResponse<DelegateProfileDetailsNg> listV2(String accountId, String orgId, String projectId,
      String filterIdentifier, String searchTerm, DelegateProfileFilterPropertiesDTO filterProperties,
      PageRequest<DelegateProfileDetailsNg> pageRequest);

  DelegateProfileDetailsNg get(String accountId, String delegateProfileId);

  DelegateProfileDetailsNg update(DelegateProfileDetailsNg delegateProfile);

  DelegateProfileDetailsNg updateScopingRules(
      String accountId, String delegateProfileId, List<ScopingRuleDetailsNg> scopingRules);

  DelegateProfileDetailsNg updateSelectors(String accountId, String delegateProfileId, List<String> selectors);

  DelegateProfileDetailsNg add(DelegateProfileDetailsNg delegateProfile);

  void delete(String accountId, String delegateProfileId);

  DelegateProfileDetailsNg get(String accountId, String orgId, String projectId, String delegateProfileIdentifier);

  DelegateProfileDetailsNg updateScopingRules(String accountId, String orgId, String projectId,
      String delegateProfileIdentifier, List<ScopingRuleDetailsNg> scopingRules);

  boolean delete(String accountId, String orgId, String projectId, String delegateProfileIdentifier);

  DelegateProfileDetailsNg updateSelectors(
      String accountId, String orgId, String projectId, String delegateProfileIdentifier, List<String> selectors);

  DelegateProfileDetailsNg updateV2(String accountId, String orgIdentifier, String projectIdentifier,
      String profileIdentifier, DelegateProfileDetailsNg updatedProfileDetails);
}
