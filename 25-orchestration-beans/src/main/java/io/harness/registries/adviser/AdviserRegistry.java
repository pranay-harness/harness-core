package io.harness.registries.adviser;

import com.google.inject.Singleton;

import io.harness.adviser.Adviser;
import io.harness.adviser.AdviserObtainment;
import io.harness.adviser.AdviserType;
import io.harness.annotations.Redesign;
import io.harness.registries.Registry;
import io.harness.registries.RegistryType;
import io.harness.registries.exceptions.DuplicateRegistryException;
import io.harness.registries.exceptions.UnregisteredKeyAccessException;
import lombok.NonNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.validation.Valid;

@Redesign
@Singleton
public class AdviserRegistry implements Registry {
  private Map<AdviserType, AdviserProducer> registry = new ConcurrentHashMap<>();

  public void register(@NonNull AdviserType adviserType, @NonNull AdviserProducer producer) {
    if (registry.containsKey(adviserType)) {
      throw new DuplicateRegistryException(getType(), "Adviser Already Registered with this type: " + adviserType);
    }
    registry.put(adviserType, producer);
  }

  public Adviser obtain(@Valid AdviserObtainment adviserObtainment) {
    if (registry.containsKey(adviserObtainment.getType())) {
      AdviserProducer producer = registry.get(adviserObtainment.getType());
      return producer.produce(adviserObtainment.getParameters());
    }
    throw new UnregisteredKeyAccessException(
        getType(), "No Adviser registered for type: " + adviserObtainment.getType());
  }

  @Override
  public RegistryType getType() {
    return RegistryType.ADVISER;
  }
}
