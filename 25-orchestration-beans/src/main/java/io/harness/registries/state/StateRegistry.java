package io.harness.registries.state;

import com.google.inject.Singleton;

import io.harness.annotations.Redesign;
import io.harness.exception.InvalidRequestException;
import io.harness.registries.Registry;
import io.harness.registries.RegistryType;
import io.harness.state.State;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Redesign
@Singleton
public class StateRegistry implements Registry {
  Map<String, StateProducer> registry = new ConcurrentHashMap<>();

  public void register(String stateType, StateProducer producer) {
    if (registry.containsKey(stateType)) {
      throw new InvalidRequestException("State Already Registered with this type: " + stateType);
    }
    registry.put(stateType, producer);
  }

  public State obtain(String stateType) {
    if (registry.containsKey(stateType)) {
      StateProducer producer = registry.get(stateType);
      return producer.produce();
    }
    throw new InvalidRequestException("No State registered for type: " + stateType);
  }

  @Override
  public RegistryType getType() {
    return RegistryType.STATE;
  }
}
