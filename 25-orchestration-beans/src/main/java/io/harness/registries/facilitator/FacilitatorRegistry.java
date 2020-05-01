package io.harness.registries.facilitator;

import static org.joor.Reflect.on;

import com.google.inject.Singleton;

import io.harness.annotations.Redesign;
import io.harness.facilitate.Facilitator;
import io.harness.facilitate.FacilitatorType;
import io.harness.registries.Registry;
import io.harness.registries.RegistryType;
import io.harness.registries.exceptions.DuplicateRegistryException;
import io.harness.registries.exceptions.UnregisteredKeyAccessException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.validation.Valid;

@Redesign
@Singleton
public class FacilitatorRegistry implements Registry<FacilitatorType, Class<? extends Facilitator>> {
  private Map<FacilitatorType, Class<? extends Facilitator>> registry = new ConcurrentHashMap<>();

  public void register(FacilitatorType facilitatorType, Class<? extends Facilitator> facilitatorClass) {
    if (registry.containsKey(facilitatorType)) {
      throw new DuplicateRegistryException(getType(), "Facilitator Already Registered with type: " + facilitatorType);
    }
    registry.put(facilitatorType, facilitatorClass);
  }

  public Facilitator obtain(@Valid FacilitatorType facilitatorType) {
    if (registry.containsKey(facilitatorType)) {
      return on(registry.get(facilitatorType)).create().get();
    }
    throw new UnregisteredKeyAccessException(getType(), "No Facilitator registered for type: " + facilitatorType);
  }

  @Override
  public RegistryType getType() {
    return RegistryType.FACILITATOR;
  }
}
