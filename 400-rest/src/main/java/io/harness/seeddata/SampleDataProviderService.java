/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.seeddata;

import software.wings.beans.Account;

public interface SampleDataProviderService {
  void createHarnessSampleApp(Account account);
  void createK8sV2SampleApp(Account account);
}
