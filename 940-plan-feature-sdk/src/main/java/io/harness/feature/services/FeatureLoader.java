/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.feature.services;

import com.google.inject.Injector;

public interface FeatureLoader {
  void run(Injector injector);
}
