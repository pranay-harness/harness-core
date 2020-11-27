package io.harness.registries.timeout;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.registries.Registry;
import io.harness.registries.exceptions.DuplicateRegistryException;
import io.harness.registries.exceptions.UnregisteredKeyAccessException;
import io.harness.timeout.Dimension;
import io.harness.timeout.TimeoutTrackerFactory;

import com.google.inject.Singleton;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.validation.Valid;
import lombok.NonNull;

@OwnedBy(CDC)
@Singleton
public class TimeoutRegistry implements Registry<Dimension, TimeoutTrackerFactory<?>> {
  private final Map<Dimension, TimeoutTrackerFactory<?>> registry = new ConcurrentHashMap<>();

  public void register(@NonNull Dimension dimension, @NonNull TimeoutTrackerFactory<?> timeoutTrackerFactory) {
    if (registry.containsKey(dimension)) {
      throw new DuplicateRegistryException(getType(), "Timeout Already Registered with this type: " + dimension);
    }
    registry.put(dimension, timeoutTrackerFactory);
  }

  public TimeoutTrackerFactory<?> obtain(@Valid Dimension dimension) {
    if (registry.containsKey(dimension)) {
      return registry.get(dimension);
    }
    throw new UnregisteredKeyAccessException(getType(), "No Timeout registered for type: " + dimension);
  }

  @Override
  public String getType() {
    return "TIMEOUT";
  }
}
