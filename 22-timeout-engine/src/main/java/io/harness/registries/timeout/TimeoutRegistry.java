package io.harness.registries.timeout;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import io.harness.annotations.dev.OwnedBy;
import io.harness.registries.Registry;
import io.harness.registries.exceptions.DuplicateRegistryException;
import io.harness.registries.exceptions.UnregisteredKeyAccessException;
import io.harness.timeout.Dimension;
import io.harness.timeout.TimeoutTrackerFactory;
import lombok.NonNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.validation.Valid;

@OwnedBy(CDC)
@Singleton
public class TimeoutRegistry implements Registry<Dimension, Class<? extends TimeoutTrackerFactory>> {
  @Inject private Injector injector;

  private final Map<Dimension, Class<? extends TimeoutTrackerFactory>> registry = new ConcurrentHashMap<>();

  public void register(
      @NonNull Dimension dimension, @NonNull Class<? extends TimeoutTrackerFactory> timeoutTrackerFactory) {
    if (registry.containsKey(dimension)) {
      throw new DuplicateRegistryException(getType(), "Timeout Already Registered with this type: " + dimension);
    }
    registry.put(dimension, timeoutTrackerFactory);
  }

  public TimeoutTrackerFactory obtain(@Valid Dimension dimension) {
    if (registry.containsKey(dimension)) {
      return injector.getInstance(registry.get(dimension));
    }
    throw new UnregisteredKeyAccessException(getType(), "No Timeout registered for type: " + dimension);
  }

  @Override
  public String getType() {
    return "TIMEOUT";
  }
}
