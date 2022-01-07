/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.serializer.jackson;

import io.harness.pms.yaml.ParameterField;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.Deserializers;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.type.ReferenceType;

public class NGHarnessDeserializers extends Deserializers.Base {
  @Override
  public JsonDeserializer<?> findReferenceDeserializer(ReferenceType refType, DeserializationConfig config,
      BeanDescription beanDesc, TypeDeserializer contentTypeDeserializer, JsonDeserializer<?> contentDeserializer) {
    if (refType.hasRawClass(ParameterField.class)) {
      JavaType valueType = refType.getReferencedType();
      return new ParameterFieldDeserializer(refType, valueType, contentTypeDeserializer, contentDeserializer);
    }
    return null;
  }
}
