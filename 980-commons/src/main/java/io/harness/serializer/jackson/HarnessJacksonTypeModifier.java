/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.serializer.jackson;

import io.harness.utils.RequestField;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.ReferenceType;
import com.fasterxml.jackson.databind.type.TypeBindings;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.type.TypeModifier;
import java.lang.reflect.Type;

public class HarnessJacksonTypeModifier extends TypeModifier {
  @Override
  public JavaType modifyType(JavaType type, Type jdkType, TypeBindings bindings, TypeFactory typeFactory) {
    if (type.isReferenceType() || type.isContainerType()) {
      return type;
    }
    final Class<?> raw = type.getRawClass();

    if (raw == RequestField.class) {
      JavaType refType = bindings.isEmpty() ? TypeFactory.unknownType() : bindings.getBoundType(0);
      return ReferenceType.upgradeFrom(type, refType);
    }
    return type;
  }
}
