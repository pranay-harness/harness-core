package io.harness.service.intfc;

import io.harness.delegate.beans.DelegateProfile;

public interface DelegateProfileObserver {
  void onProfileUpdated(DelegateProfile originalProfile, DelegateProfile updatedProfile);
  void onProfileApplied(String accountId, String delegateId, String profileId);
}
