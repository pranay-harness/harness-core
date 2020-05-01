package io.harness.registries.resolver;

import static org.joor.Reflect.on;

import com.google.inject.Singleton;

import io.harness.annotations.Redesign;
import io.harness.references.RefType;
import io.harness.registries.Registry;
import io.harness.registries.RegistryType;
import io.harness.registries.exceptions.DuplicateRegistryException;
import io.harness.registries.exceptions.UnregisteredKeyAccessException;
import io.harness.resolvers.Resolver;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Redesign
@Singleton
public class ResolverRegistry implements Registry<RefType, Class<? extends Resolver>> {
  private Map<RefType, Class<? extends Resolver>> registry = new ConcurrentHashMap<>();

  public void register(RefType refType, Class<? extends Resolver> resolverClass) {
    if (registry.containsKey(refType)) {
      throw new DuplicateRegistryException(getType(), "Resolver Already Registered with this type: " + refType);
    }
    registry.put(refType, resolverClass);
  }

  public Resolver obtain(RefType refType) {
    if (registry.containsKey(refType)) {
      return on(registry.get(refType)).create().get();
    }
    throw new UnregisteredKeyAccessException(getType(), "No Resolver registered for type: " + refType);
  }

  @Override
  public RegistryType getType() {
    return RegistryType.RESOLVER;
  }

  @Override
  public Class<Resolver> getRegistrableEntityClass() {
    return Resolver.class;
  }
}
