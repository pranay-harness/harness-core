package io.harness.registries.facilitator;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.facilitator.Facilitator;
import io.harness.facilitator.FacilitatorType;
import io.harness.registries.Registry;
import io.harness.registries.RegistryType;
import io.harness.registries.exceptions.DuplicateRegistryException;
import io.harness.registries.exceptions.UnregisteredKeyAccessException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.validation.Valid;

@OwnedBy(CDC)
@Redesign
@Singleton
public class FacilitatorRegistry implements Registry<FacilitatorType, Class<? extends Facilitator>> {
  @Inject private Injector injector;

  private Map<FacilitatorType, Class<? extends Facilitator>> registry = new ConcurrentHashMap<>();

  public void register(FacilitatorType facilitatorType, Class<? extends Facilitator> facilitator) {
    if (registry.containsKey(facilitatorType)) {
      throw new DuplicateRegistryException(getType(), "Facilitator Already Registered with type: " + facilitatorType);
    }
    registry.put(facilitatorType, facilitator);
  }

  public Facilitator obtain(@Valid FacilitatorType facilitatorType) {
    if (registry.containsKey(facilitatorType)) {
      return injector.getInstance(registry.get(facilitatorType));
    }
    throw new UnregisteredKeyAccessException(getType(), "No Facilitator registered for type: " + facilitatorType);
  }

  @Override
  public RegistryType getType() {
    return RegistryType.FACILITATOR;
  }
}
