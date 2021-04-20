package io.harness.service.intfc;

import software.wings.beans.DelegateInsightsDetails;

public interface DelegateInsightsService {
  DelegateInsightsDetails retrieveDelegateInsightsDetails(
      String accountId, String delegateGroupId, long startTimestamp);
}
