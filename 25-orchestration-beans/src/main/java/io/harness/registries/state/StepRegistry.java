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
import io.harness.state.StateType;
import io.harness.state.Step;
import lombok.NonNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@OwnedBy(CDC)
@Redesign
@Singleton
public class StepRegistry implements Registry<StateType, Class<? extends Step>> {
  @Inject private Injector injector;

  Map<StateType, Class<? extends Step>> registry = new ConcurrentHashMap<>();

  public void register(@NonNull StateType stateType, @NonNull Class<? extends Step> state) {
    if (registry.containsKey(stateType)) {
      throw new DuplicateRegistryException(getType(), "State Already Registered with this type: " + stateType);
    }
    registry.put(stateType, state);
  }

  public Step obtain(@NonNull StateType stateType) {
    if (registry.containsKey(stateType)) {
      return injector.getInstance(registry.get(stateType));
    }
    throw new UnregisteredKeyAccessException(getType(), "No State registered for type: " + stateType);
  }

  @Override
  public RegistryType getType() {
    return RegistryType.STEP;
  }
}
