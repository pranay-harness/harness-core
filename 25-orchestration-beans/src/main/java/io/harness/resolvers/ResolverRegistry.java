package io.harness.resolvers;

import io.harness.exception.InvalidRequestException;
import io.harness.refrences.RefType;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ResolverRegistry {
  private Map<RefType, ResolverProducer> registry = new ConcurrentHashMap<>();

  public void registerResolver(RefType refType, ResolverProducer producer) {
    if (registry.containsKey(refType)) {
      throw new InvalidRequestException("Resolver Already Registered with this type: " + refType);
    }
    registry.put(refType, producer);
  }

  public Resolver obtainResolver(RefType refType) {
    if (registry.containsKey(refType)) {
      ResolverProducer producer = registry.get(refType);
      return producer.produceResolver(refType);
    }
    throw new InvalidRequestException("No Resolver registered for type: " + refType);
  }
}
