/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.stresstesting.script;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.serializer.KryoSerializer;
import io.harness.testing.DelegateTaskStressTest;

import com.google.inject.Inject;

@OwnedBy(HarnessTeam.DEL)
public abstract class StressTestGenerator {
  @Inject KryoSerializer kryoSerializer;

  public abstract DelegateTaskStressTest makeStressTest();
}
