package io.harness.walktree.registries.visitorfield;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import io.harness.registries.Registry;
import io.harness.registries.exceptions.DuplicateRegistryException;
import io.harness.registries.exceptions.UnregisteredKeyAccessException;
import lombok.NonNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class VisitorFieldRegistry implements Registry<VisitorFieldType, Class<? extends VisitableFieldProcessor<?>>> {
  @Inject private Injector injector;

  private final Map<VisitorFieldType, Class<? extends VisitableFieldProcessor<?>>> registry = new ConcurrentHashMap<>();

  @Override
  public void register(
      @NonNull VisitorFieldType visitorFieldType, @NonNull Class<? extends VisitableFieldProcessor<?>> processor) {
    if (registry.containsKey(visitorFieldType)) {
      throw new DuplicateRegistryException(
          getType(), "Visitor Field Processor Already Registered with this type: " + visitorFieldType);
    }
    registry.put(visitorFieldType, processor);
  }

  @Override
  public VisitableFieldProcessor<?> obtain(VisitorFieldType visitorFieldType) {
    if (registry.containsKey(visitorFieldType)) {
      return injector.getInstance(registry.get(visitorFieldType));
    }
    throw new UnregisteredKeyAccessException(
        getType(), "No Visitor Field Processor registered for type: " + visitorFieldType);
  }

  @Override
  public String getType() {
    return "VISITOR_FIELD";
  }
}
