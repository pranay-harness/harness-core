package io.harness.delegate.service.intfc;

import io.harness.delegate.beans.DelegateConfiguration;

public interface DelegateRingService {
  String getDelegateImageTag(String accountId);

  String getUpgraderImageTag(String accountId);

  DelegateConfiguration getDelegateConfiguration(String accountId);
}
