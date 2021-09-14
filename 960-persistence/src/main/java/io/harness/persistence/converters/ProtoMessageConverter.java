/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.persistence.converters;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;

import com.google.inject.Singleton;
import com.google.protobuf.Message;
import com.google.protobuf.Message.Builder;
import com.google.protobuf.util.JsonFormat;
import com.mongodb.BasicDBObject;
import lombok.SneakyThrows;
import org.mongodb.morphia.converters.SimpleValueConverter;
import org.mongodb.morphia.converters.TypeConverter;
import org.mongodb.morphia.mapping.MappedField;

@SuppressWarnings("unchecked")
@Singleton
@OwnedBy(PIPELINE)
public abstract class ProtoMessageConverter<T extends Message> extends TypeConverter implements SimpleValueConverter {
  public ProtoMessageConverter(Class<T> entityClass) {
    super(entityClass);
  }

  @SneakyThrows
  @Override
  public Object encode(Object value, MappedField optionalExtraInfo) {
    if (value == null) {
      return null;
    }
    Message message = (Message) value;
    String entityJson = JsonFormat.printer().includingDefaultValueFields().print(message);
    return BasicDBObject.parse(entityJson);
  }

  @SneakyThrows
  @Override
  public Object decode(Class<?> targetClass, Object fromDBObject, MappedField optionalExtraInfo) {
    if (fromDBObject == null) {
      return null;
    }
    Builder builder = null;
    builder = (Builder) targetClass.getMethod("newBuilder").invoke(null);
    JsonFormat.parser().ignoringUnknownFields().merge(fromDBObject.toString(), builder);
    return (T) builder.build();
  }
}
