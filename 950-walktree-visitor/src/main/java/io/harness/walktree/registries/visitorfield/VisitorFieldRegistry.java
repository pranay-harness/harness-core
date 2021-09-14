/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.walktree.registries.visitorfield;

import io.harness.registries.Registry;
import io.harness.registries.exceptions.DuplicateRegistryException;
import io.harness.registries.exceptions.UnregisteredKeyAccessException;

import com.google.inject.Singleton;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.NonNull;

@Singleton
public class VisitorFieldRegistry implements Registry<VisitorFieldType, VisitableFieldProcessor<?>> {
  private final Map<VisitorFieldType, VisitableFieldProcessor<?>> registry = new ConcurrentHashMap<>();
  private final Map<Class<? extends VisitorFieldWrapper>, VisitorFieldType> fieldTypesRegistry =
      new ConcurrentHashMap<>();

  @Override
  public void register(@NonNull VisitorFieldType visitorFieldType, @NonNull VisitableFieldProcessor<?> processor) {
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
      return registry.get(visitorFieldType);
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
