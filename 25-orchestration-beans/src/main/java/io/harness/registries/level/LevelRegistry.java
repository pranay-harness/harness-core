package io.harness.registries.level;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import io.harness.ambiance.Level;
import io.harness.ambiance.LevelType;
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
public class LevelRegistry implements Registry<LevelType, Class<? extends Level>> {
  @Inject private Injector injector;

  Map<LevelType, Class<? extends Level>> registry = new ConcurrentHashMap<>();

  public void register(@NonNull LevelType levelType, @Valid Class<? extends Level> level) {
    if (registry.containsKey(levelType)) {
      throw new DuplicateRegistryException(getType(), "Level Already Registered with this name: " + levelType);
    }
    registry.put(levelType, level);
  }

  public Level obtain(@NonNull LevelType levelType) {
    if (registry.containsKey(levelType)) {
      return injector.getInstance(registry.get(levelType));
    }
    throw new UnregisteredKeyAccessException(getType(), "No Level registered for name: " + levelType);
  }

  @Override
  public RegistryType getType() {
    return RegistryType.LEVEL;
  }
}