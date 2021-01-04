package io.harness.pms.sdk.core.registries;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.refobjects.RefType;
import io.harness.pms.sdk.core.resolver.Resolver;
import io.harness.registries.Registry;
import io.harness.registries.exceptions.DuplicateRegistryException;
import io.harness.registries.exceptions.UnregisteredKeyAccessException;

import com.google.inject.Singleton;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@OwnedBy(CDC)
@Singleton
public class ResolverRegistry implements Registry<RefType, Resolver<?>> {
  private Map<RefType, Resolver<?>> registry = new ConcurrentHashMap<>();

  public void register(RefType refType, Resolver<?> resolver) {
    if (registry.containsKey(refType)) {
      throw new DuplicateRegistryException(getType(), "Resolver Already Registered with this type: " + refType);
    }
    registry.put(refType, resolver);
  }

  public Resolver<?> obtain(RefType refType) {
    if (registry.containsKey(refType)) {
      return registry.get(refType);
    }
    throw new UnregisteredKeyAccessException(getType(), "No Resolver registered for type: " + refType);
  }

  @Override
  public String getType() {
    return RegistryType.RESOLVER.name();
  }
}
