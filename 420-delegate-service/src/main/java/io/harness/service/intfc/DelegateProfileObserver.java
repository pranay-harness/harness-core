/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.service.intfc;

import io.harness.delegate.beans.DelegateProfile;

public interface DelegateProfileObserver {
  void onProfileUpdated(DelegateProfile originalProfile, DelegateProfile updatedProfile);
  void onProfileApplied(String accountId, String delegateId, String profileId);
  void onProfileSelectorsUpdated(String accountId, String profileId);
  void onProfileScopesUpdated(String accountId, String profileId);
}
