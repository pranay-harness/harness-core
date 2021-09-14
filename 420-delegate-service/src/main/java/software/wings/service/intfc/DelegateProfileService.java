/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.intfc;

import static io.harness.annotations.dev.HarnessTeam.DEL;

import io.harness.annotations.dev.BreakDependencyOn;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.delegate.beans.DelegateEntityOwner;
import io.harness.delegate.beans.DelegateProfile;
import io.harness.delegate.beans.DelegateProfileScopingRule;

import software.wings.service.intfc.ownership.OwnedByAccount;

import java.util.List;

@OwnedBy(DEL)
@BreakDependencyOn("software.wings.service.intfc.ownership.OwnedByAccount")
public interface DelegateProfileService extends OwnedByAccount {
  PageResponse<DelegateProfile> list(PageRequest<DelegateProfile> pageRequest);
  DelegateProfile get(String accountId, String delegateProfileId);
  DelegateProfile fetchCgPrimaryProfile(String accountId);

  DelegateProfile fetchNgPrimaryProfile(String accountId, DelegateEntityOwner owner);

  DelegateProfile update(DelegateProfile delegateProfile);
  DelegateProfile updateV2(DelegateProfile delegateProfile);
  DelegateProfile updateScopingRules(
      String accountId, String delegateProfileId, List<DelegateProfileScopingRule> scopingRules);
  DelegateProfile add(DelegateProfile delegateProfile);
  void delete(String accountId, String delegateProfileId);
  DelegateProfile updateDelegateProfileSelectors(String delegateProfileId, String accountId, List<String> selectors);
  List<String> getDelegatesForProfile(String accountId, String profileId);
  DelegateProfile getProfileByIdentifier(String accountId, DelegateEntityOwner owner, String profileIdentifier);
  DelegateProfile updateScopingRules(String accountId, DelegateEntityOwner owner, String profileIdentifier,
      List<DelegateProfileScopingRule> scopingRules);
  void deleteProfileV2(String accountId, DelegateEntityOwner owner, String delegateProfileIdentifier);
  DelegateProfile updateProfileSelectorsV2(
      String accountId, DelegateEntityOwner owner, String delegateProfileIdentifier, List<String> selectors);
}
