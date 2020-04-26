package io.harness.registries.level;

import com.google.inject.Singleton;

import io.harness.annotations.Redesign;
import io.harness.registries.Registry;
import io.harness.registries.RegistryType;
import io.harness.registries.exceptions.DuplicateRegistryException;
import io.harness.registries.exceptions.UnregisteredKeyAccessException;
import io.harness.state.io.ambiance.Level;
import lombok.NonNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.validation.Valid;

@Redesign
@Singleton
public class LevelRegistry implements Registry {
  Map<String, Level> registry = new ConcurrentHashMap<>();

  public void register(@Valid Level level) {
    if (registry.containsKey(level.getName())) {
      throw new DuplicateRegistryException(getType(), "Level Already Registered with this name: " + level.getName());
    }
    registry.put(level.getName(), level);
  }

  public Level obtain(@NonNull String levelName) {
    if (registry.containsKey(levelName)) {
      return registry.get(levelName);
    }
    throw new UnregisteredKeyAccessException(getType(), "No Level registered for name: " + levelName);
  }

  @Override
  public RegistryType getType() {
    return RegistryType.LEVEL;
  }
}