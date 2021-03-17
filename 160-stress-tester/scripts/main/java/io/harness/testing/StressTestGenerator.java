package io.harness.testing;

import io.harness.serializer.KryoSerializer;

import com.google.inject.Inject;

public abstract class StressTestGenerator {
  @Inject KryoSerializer kryoSerializer;

  abstract public DelegateTaskStressTest makeStressTest();
}
