package io.harness.registries.state;

import com.google.inject.Singleton;

import io.harness.annotations.Redesign;
import io.harness.registries.DuplicateRegistryException;
import io.harness.registries.Registry;
import io.harness.registries.RegistryType;
import io.harness.registries.UnregisteredKeyAccess;
import io.harness.state.State;
import lombok.NonNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Redesign
@Singleton
public class StateRegistry implements Registry {
  Map<String, StateProducer> registry = new ConcurrentHashMap<>();

  public void register(@NonNull String stateType, @NonNull StateProducer producer) {
    if (registry.containsKey(stateType)) {
      throw new DuplicateRegistryException(getType(), "State Already Registered with this type: " + stateType);
    }
    registry.put(stateType, producer);
  }

  public State obtain(@NonNull String stateType) {
    if (registry.containsKey(stateType)) {
      StateProducer producer = registry.get(stateType);
      return producer.produce();
    }
    throw new UnregisteredKeyAccess(getType(), "No State registered for type: " + stateType);
  }

  @Override
  public RegistryType getType() {
    return RegistryType.STATE;
  }
}
