/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.core.managerConfiguration;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import java.util.List;

@TargetModule(HarnessModule._960_PERSISTENCE)
public interface ConfigChangeListener {
  void onConfigChange(List<ConfigChangeEvent> events);
}
