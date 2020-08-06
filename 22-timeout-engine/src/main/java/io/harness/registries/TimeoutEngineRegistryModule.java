package io.harness.registries;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import io.harness.annotations.dev.OwnedBy;
import io.harness.registries.registrar.TimeoutRegistrar;
import io.harness.registries.timeout.TimeoutRegistry;
import io.harness.timeout.Dimension;
import io.harness.timeout.TimeoutTrackerFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@OwnedBy(CDC)
@Slf4j
public class TimeoutEngineRegistryModule extends AbstractModule {
  private static TimeoutEngineRegistryModule instance;

  public static synchronized TimeoutEngineRegistryModule getInstance() {
    if (instance == null) {
      instance = new TimeoutEngineRegistryModule();
    }
    return instance;
  }

  @Provides
  @Singleton
  TimeoutRegistry providesTimeoutRegistry(Injector injector, Map<String, TimeoutRegistrar> timeoutRegistrarMap) {
    Set classes = new HashSet<>();
    timeoutRegistrarMap.values().forEach(timeoutRegistrar -> { timeoutRegistrar.register(classes); });
    TimeoutRegistry timeoutRegistry = new TimeoutRegistry();
    injector.injectMembers(timeoutRegistry);
    classes.forEach(pair -> {
      Pair<Dimension, Class<? extends TimeoutTrackerFactory>> timeoutPair =
          (Pair<Dimension, Class<? extends TimeoutTrackerFactory>>) pair;
      timeoutRegistry.register(timeoutPair.getLeft(), timeoutPair.getRight());
    });
    return timeoutRegistry;
  }
}
