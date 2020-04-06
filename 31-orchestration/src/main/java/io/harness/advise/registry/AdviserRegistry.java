package io.harness.advise.registry;

import com.google.inject.Singleton;

import io.harness.advise.Adviser;
import io.harness.advise.AdviserObtainment;
import io.harness.advise.AdviserType;
import io.harness.exception.InvalidRequestException;
import lombok.Value;

import java.util.Map;

@Value
@Singleton
public class AdviserRegistry {
  Map<AdviserType, AdviserProducer> registry;

  public void register(AdviserType adviserType, AdviserProducer producer) {
    if (registry.containsKey(adviserType)) {
      throw new InvalidRequestException("Adviser Already Registered with this type: " + adviserType);
    }
    registry.put(adviserType, producer);
  }

  public Adviser obtain(AdviserObtainment adviserObtainment) {
    if (registry.containsKey(adviserObtainment.getType())) {
      AdviserProducer producer = registry.get(adviserObtainment.getType());
      return producer.produce(adviserObtainment.getParameters());
    }
    throw new InvalidRequestException("No Adviser registered for type: " + adviserObtainment.getType());
  }
}
