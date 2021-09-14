/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.serializer.jackson;

import io.harness.pms.yaml.ParameterField;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.ReferenceType;
import com.fasterxml.jackson.databind.type.TypeBindings;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.type.TypeModifier;
import java.lang.reflect.Type;

public class NGHarnessJacksonTypeModifier extends TypeModifier {
  @Override
  public JavaType modifyType(JavaType type, Type jdkType, TypeBindings bindings, TypeFactory typeFactory) {
    if (type.isReferenceType() || type.isContainerType()) {
      return type;
    }
    final Class<?> raw = type.getRawClass();

    if (raw == ParameterField.class) {
      JavaType refType = bindings.isEmpty() ? TypeFactory.unknownType() : bindings.getBoundType(0);
      return ReferenceType.upgradeFrom(type, refType);
    }
    return type;
  }
}
