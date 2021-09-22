package io.harness.pms.sdk.core.registries;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.sdk.core.execution.expression.SdkFunctor;
import io.harness.registries.Registry;
import io.harness.registries.exceptions.DuplicateRegistryException;
import io.harness.registries.exceptions.UnregisteredKeyAccessException;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@OwnedBy(HarnessTeam.PIPELINE)
public class FunctorRegistry implements Registry<String, SdkFunctor> {
  Map<String, SdkFunctor> registry = new ConcurrentHashMap<>();

  public Map<String, SdkFunctor> getRegistry() {
    return new HashMap<>(registry);
  }

  @Override
  public void register(String functorKey, SdkFunctor functorEntity) {
    if (registry.containsKey(functorKey)) {
      throw new DuplicateRegistryException(getType(), "Functor Already Registered with this key: " + functorKey);
    }
    registry.put(functorKey, functorEntity);
  }

  @Override
  public SdkFunctor obtain(String s) {
    if (registry.containsKey(s)) {
      return registry.get(s);
    }
    throw new UnregisteredKeyAccessException(getType(), "No Functor registered for key: " + s);
  }

  @Override
  public String getType() {
    return RegistryType.SDK_FUNCTOR.name();
  }
}
