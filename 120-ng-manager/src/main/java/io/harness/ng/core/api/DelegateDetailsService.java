package io.harness.ng.core.api;

import static io.harness.annotations.dev.HarnessTeam.DEL;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(DEL)
public interface DelegateDetailsService {
  long getDelegateGroupCount(String accountId, String orgId, String projectId);
}
