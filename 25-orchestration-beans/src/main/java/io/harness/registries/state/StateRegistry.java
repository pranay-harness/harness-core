package io.harness.registries.state;

import com.google.inject.Singleton;

import io.harness.annotations.Redesign;
import io.harness.registries.Registry;
import io.harness.registries.RegistryType;
import io.harness.registries.exceptions.DuplicateRegistryException;
import io.harness.registries.exceptions.UnregisteredKeyAccessException;
import io.harness.state.State;
import io.harness.state.StateType;
import lombok.NonNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Redesign
@Singleton
public class StateRegistry implements Registry<StateType, State> {
  Map<StateType, State> registry = new ConcurrentHashMap<>();

  public void register(@NonNull StateType stateType, @NonNull State state) {
    if (registry.containsKey(stateType)) {
      throw new DuplicateRegistryException(getType(), "State Already Registered with this type: " + stateType);
    }
    registry.put(stateType, state);
  }

  public State obtain(@NonNull StateType stateType) {
    if (registry.containsKey(stateType)) {
      return registry.get(stateType);
    }
    throw new UnregisteredKeyAccessException(getType(), "No State registered for type: " + stateType);
  }

  @Override
  public RegistryType getType() {
    return RegistryType.STATE;
  }
}
