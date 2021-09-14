/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.service.intfc;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.Delegate;
import io.harness.delegate.beans.DelegateEntityOwner;
import io.harness.delegate.beans.DelegateGroup;
import io.harness.delegate.beans.DelegateProfile;

@OwnedBy(HarnessTeam.DEL)
public interface DelegateCache {
  Delegate get(String accountId, String delegateId, boolean forceRefresh);

  DelegateGroup getDelegateGroup(String accountId, String delegateGroupId);

  DelegateGroup getDelegateGroupByAccountAndOwnerAndIdentifier(
      String accountId, DelegateEntityOwner owner, String delegateGroupIdentifier);

  DelegateProfile getDelegateProfile(String accountId, String delegateProfileId);

  void invalidateDelegateProfileCache(String accountId, String delegateProfileId);

  void invalidateDelegateGroupCache(String accountId, String delegateGroupId);

  void invalidateDelegateGroupCacheByIdentifier(String accountId, DelegateEntityOwner owner, String identifier);
}
