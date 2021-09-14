/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.message;
import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

@TargetModule(HarnessModule._920_DELEGATE_AGENT_BEANS)
public interface ManagerMessageConstants {
  // Messages sent from manager to delegate
  String SELF_DESTRUCT = "[SELF_DESTRUCT]";
  String MIGRATE = "[MIGRATE]";
  String USE_CDN = "[USE_CDN]";
  String USE_STORAGE_PROXY = "[USE_STORAGE_PROXY]";
  String JRE_VERSION = "[JRE_VERSION]";
  String UPDATE_PERPETUAL_TASK = "[UPDATE_PERPETUAL_TASK]";
}
