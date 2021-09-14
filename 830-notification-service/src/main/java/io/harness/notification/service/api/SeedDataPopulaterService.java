/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.notification.service.api;

import io.harness.notification.SeedDataConfiguration;

public interface SeedDataPopulaterService {
  void populateSeedData(SeedDataConfiguration seedDataConfiguration);
}
