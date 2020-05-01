package io.harness.registries.adviser;

import static org.joor.Reflect.on;

import com.google.inject.Singleton;

import io.harness.adviser.Adviser;
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
public class AdviserRegistry implements Registry<AdviserType, Class<? extends Adviser>> {
  private Map<AdviserType, Class<? extends Adviser>> registry = new ConcurrentHashMap<>();

  public void register(@NonNull AdviserType adviserType, @NonNull Class<? extends Adviser> adviserClass) {
    if (registry.containsKey(adviserType)) {
      throw new DuplicateRegistryException(getType(), "Adviser Already Registered with this type: " + adviserType);
    }
    registry.put(adviserType, adviserClass);
  }

  public Adviser obtain(@Valid AdviserType adviserType) {
    if (registry.containsKey(adviserType)) {
      return on(registry.get(adviserType)).create().get();
    }
    throw new UnregisteredKeyAccessException(getType(), "No Adviser registered for type: " + adviserType);
  }

  @Override
  public Class<Adviser> getRegistrableEntityClass() {
    return Adviser.class;
  }

  @Override
  public RegistryType getType() {
    return RegistryType.ADVISER;
  }
}
