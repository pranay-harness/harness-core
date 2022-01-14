package io.harness.seeddata;

import software.wings.beans.Account;

public interface SampleDataProviderService {
  void createHarnessSampleApp(Account account);
  void createK8sV2SampleApp(Account account);
}
