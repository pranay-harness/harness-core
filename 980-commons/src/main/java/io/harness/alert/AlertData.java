/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.alert;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

@TargetModule(HarnessModule._955_ALERT_BEANS)
public interface AlertData {
  boolean matches(AlertData alertData);

  String buildTitle();

  default String buildResolutionTitle() {
    return null;
  }
}
