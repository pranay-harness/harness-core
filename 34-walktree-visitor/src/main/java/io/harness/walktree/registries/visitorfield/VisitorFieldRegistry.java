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
  private final Map<Class<? extends VisitorFieldWrapper>, VisitorFieldType> fieldTypesRegistry =
      new ConcurrentHashMap<>();

  @Override
  public void register(
      @NonNull VisitorFieldType visitorFieldType, @NonNull Class<? extends VisitableFieldProcessor<?>> processor) {
    if (registry.containsKey(visitorFieldType)) {
      throw new DuplicateRegistryException(
          getType(), "Visitor Field Processor Already Registered with this type: " + visitorFieldType);
    }
    registry.put(visitorFieldType, processor);
  }

  public void registerFieldTypes(
      @NonNull Class<? extends VisitorFieldWrapper> fieldWrapper, @NonNull VisitorFieldType fieldType) {
    if (fieldTypesRegistry.containsKey(fieldWrapper)) {
      throw new DuplicateRegistryException(
          getType(), "Visitor Field Wrapper Already Registered with this class: " + fieldWrapper);
    }
    fieldTypesRegistry.put(fieldWrapper, fieldType);
  }

  @Override
  public VisitableFieldProcessor<?> obtain(@NonNull VisitorFieldType visitorFieldType) {
    if (registry.containsKey(visitorFieldType)) {
      return injector.getInstance(registry.get(visitorFieldType));
    }
    throw new UnregisteredKeyAccessException(
        getType(), "No Visitor Field Processor registered for type: " + visitorFieldType);
  }

  public VisitorFieldType obtainFieldType(@NonNull Class<? extends VisitorFieldWrapper> fieldWrapper) {
    if (fieldTypesRegistry.containsKey(fieldWrapper)) {
      return fieldTypesRegistry.get(fieldWrapper);
    }
    throw new UnregisteredKeyAccessException(
        getType(), "No Visitor Field Wrapper registered for class: " + fieldWrapper);
  }

  @Override
  public String getType() {
    return "VISITOR_FIELD";
  }
}
