package io.harness.registries.resolver;

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
public class ResolverRegistry implements Registry {
  private Map<RefType, ResolverProducer> registry = new ConcurrentHashMap<>();

  public void register(RefType refType, ResolverProducer producer) {
    if (registry.containsKey(refType)) {
      throw new DuplicateRegistryException(getType(), "Resolver Already Registered with this type: " + refType);
    }
    registry.put(refType, producer);
  }

  public Resolver obtain(RefType refType) {
    if (registry.containsKey(refType)) {
      ResolverProducer producer = registry.get(refType);
      return producer.produceResolver();
    }
    throw new UnregisteredKeyAccessException(getType(), "No Resolver registered for type: " + refType);
  }

  @Override
  public RegistryType getType() {
    return RegistryType.RESOLVER;
  }
}
