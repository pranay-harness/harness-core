package io.harness.accesscontrol.support;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.Set;

@OwnedBy(HarnessTeam.PL)
public interface SupportService {
  SupportPreference fetchSupportPreference(String accountIdentifier);
  SupportPreference syncSupportPreferenceFromRemote(String accountIdentifier);
  Set<String> fetchSupportUsers();
}
