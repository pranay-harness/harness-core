package io.harness.registries.level;

import com.google.inject.Singleton;

import io.harness.ambiance.Level;
import io.harness.ambiance.LevelType;
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
public class LevelRegistry implements Registry<LevelType, Level> {
  Map<LevelType, Level> registry = new ConcurrentHashMap<>();

  public void register(@NonNull LevelType levelType, @Valid Level level) {
    if (registry.containsKey(levelType)) {
      throw new DuplicateRegistryException(getType(), "Level Already Registered with this name: " + levelType);
    }
    registry.put(levelType, level);
  }

  public Level obtain(@NonNull LevelType levelType) {
    if (registry.containsKey(levelType)) {
      return registry.get(levelType);
    }
    throw new UnregisteredKeyAccessException(getType(), "No Level registered for name: " + levelType);
  }

  @Override
  public RegistryType getType() {
    return RegistryType.LEVEL;
  }
}