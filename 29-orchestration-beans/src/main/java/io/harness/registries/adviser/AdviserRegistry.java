package io.harness.registries.adviser;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import io.harness.adviser.Adviser;
import io.harness.adviser.AdviserType;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.registries.Registry;
import io.harness.registries.RegistryType;
import io.harness.registries.exceptions.DuplicateRegistryException;
import io.harness.registries.exceptions.UnregisteredKeyAccessException;
import lombok.NonNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.validation.Valid;

@OwnedBy(CDC)
@Redesign
@Singleton
public class AdviserRegistry implements Registry<AdviserType, Class<? extends Adviser>> {
  @Inject private Injector injector;

  private Map<AdviserType, Class<? extends Adviser>> registry = new ConcurrentHashMap<>();

  public void register(@NonNull AdviserType adviserType, @NonNull Class<? extends Adviser> adviser) {
    if (registry.containsKey(adviserType)) {
      throw new DuplicateRegistryException(getType(), "Adviser Already Registered with this type: " + adviserType);
    }
    registry.put(adviserType, adviser);
  }

  public Adviser obtain(@Valid AdviserType adviserType) {
    if (registry.containsKey(adviserType)) {
      return injector.getInstance(registry.get(adviserType));
    }
    throw new UnregisteredKeyAccessException(getType(), "No Adviser registered for type: " + adviserType);
  }

  @Override
  public String getType() {
    return RegistryType.ADVISER.name();
  }
}
