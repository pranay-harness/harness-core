package io.harness.registries.facilitator;

import com.google.inject.Singleton;

import io.harness.exception.InvalidRequestException;
import io.harness.facilitate.Facilitator;
import io.harness.facilitate.FacilitatorObtainment;
import io.harness.facilitate.FacilitatorType;
import io.harness.registries.Registry;
import io.harness.registries.RegistryType;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class FacilitatorRegistry implements Registry {
  private Map<FacilitatorType, FacilitatorProducer> registry = new ConcurrentHashMap<>();

  public void register(FacilitatorType facilitatorType, FacilitatorProducer producer) {
    if (registry.containsKey(facilitatorType)) {
      throw new InvalidRequestException("Adviser Already Registered with this type: " + facilitatorType);
    }
    registry.put(facilitatorType, producer);
  }

  public Facilitator obtain(FacilitatorObtainment facilitatorObtainment) {
    if (registry.containsKey(facilitatorObtainment.getType())) {
      FacilitatorProducer producer = registry.get(facilitatorObtainment.getType());
      return producer.produce(facilitatorObtainment.getParameters());
    }
    throw new InvalidRequestException("No Facilitator registered for type: " + facilitatorObtainment.getType());
  }

  @Override
  public RegistryType getType() {
    return RegistryType.FACILITATOR;
  }
}
