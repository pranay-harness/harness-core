package io.harness.registries.state;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.registries.Registry;
import io.harness.registries.RegistryType;
import io.harness.registries.exceptions.DuplicateRegistryException;
import io.harness.registries.exceptions.UnregisteredKeyAccessException;
import io.harness.state.Step;
import io.harness.state.StepType;
import lombok.NonNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@OwnedBy(CDC)
@Redesign
@Singleton
public class StepRegistry implements Registry<StepType, Class<? extends Step>> {
  @Inject private Injector injector;

  Map<StepType, Class<? extends Step>> registry = new ConcurrentHashMap<>();

  public void register(@NonNull StepType stepType, @NonNull Class<? extends Step> state) {
    if (registry.containsKey(stepType)) {
      throw new DuplicateRegistryException(getType(), "State Already Registered with this type: " + stepType);
    }
    registry.put(stepType, state);
  }

  public Step obtain(@NonNull StepType stepType) {
    if (registry.containsKey(stepType)) {
      return injector.getInstance(registry.get(stepType));
    }
    throw new UnregisteredKeyAccessException(getType(), "No State registered for type: " + stepType);
  }

  @Override
  public RegistryType getType() {
    return RegistryType.STEP;
  }
}
