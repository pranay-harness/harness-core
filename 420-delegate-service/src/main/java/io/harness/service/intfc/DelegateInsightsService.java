package io.harness.service.intfc;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateInsightsDetails;

@OwnedBy(HarnessTeam.DEL)
public interface DelegateInsightsService {
  DelegateInsightsDetails retrieveDelegateInsightsDetails(
      String accountId, String delegateGroupId, long startTimestamp);
}
