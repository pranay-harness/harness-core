/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.serializer.jackson;

import io.harness.utils.RequestField;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.Deserializers;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.type.ReferenceType;

public class HarnessDeserializers extends Deserializers.Base {
  @Override
  public JsonDeserializer<?> findReferenceDeserializer(ReferenceType refType, DeserializationConfig config,
      BeanDescription beanDesc, TypeDeserializer contentTypeDeserializer, JsonDeserializer<?> contentDeserializer) {
    if (refType.hasRawClass(RequestField.class)) {
      JavaType valueType = refType.getReferencedType();
      return new RequestFieldDeserializer(refType, valueType, contentTypeDeserializer, contentDeserializer);
    }
    return null;
  }
}
