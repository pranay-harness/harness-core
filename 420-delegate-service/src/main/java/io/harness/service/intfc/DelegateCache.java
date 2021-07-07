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
}
