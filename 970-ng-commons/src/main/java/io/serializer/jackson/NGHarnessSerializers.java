/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.serializer.jackson;

import io.harness.pms.yaml.ParameterField;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.Serializers;
import com.fasterxml.jackson.databind.type.ReferenceType;

public class NGHarnessSerializers extends Serializers.Base {
  @Override
  public JsonSerializer<?> findReferenceSerializer(SerializationConfig config, ReferenceType refType,
      BeanDescription beanDesc, TypeSerializer contentTypeSerializer, JsonSerializer<Object> contentValueSerializer) {
    if (refType.hasRawClass(ParameterField.class)) {
      return new ParameterFieldSerializer();
    }
    return null;
  }
}
