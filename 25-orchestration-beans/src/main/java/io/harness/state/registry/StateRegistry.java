package io.harness.state.registry;

import com.google.inject.Singleton;

import io.harness.exception.InvalidRequestException;
import io.harness.state.State;
import io.harness.state.metadata.StateMetadata;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class StateRegistry {
  Map<String, StateProducer> registry = new ConcurrentHashMap<>();

  public void registerState(String stateType, StateProducer producer) {
    if (registry.containsKey(stateType)) {
      throw new InvalidRequestException("State Already Registered with this type: " + stateType);
    }
    registry.put(stateType, producer);
  }

  public State obtainState(String stateType) {
    if (registry.containsKey(stateType)) {
      StateProducer producer = registry.get(stateType);
      return producer.produce();
    }
    throw new InvalidRequestException("No State registered for type: " + stateType);
  }

  public StateMetadata obtainMetadata(String stateType) {
    if (registry.containsKey(stateType)) {
      StateProducer producer = registry.get(stateType);
      return producer.produceMetadata();
    }
    throw new InvalidRequestException("No State registered for type: " + stateType);
  }
}
