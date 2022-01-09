/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.serializer.spring;

import com.google.inject.Inject;
import com.google.protobuf.Message;
import com.google.protobuf.Message.Builder;
import com.google.protobuf.util.JsonFormat;
import lombok.SneakyThrows;
import org.bson.Document;
import org.springframework.core.convert.converter.Converter;

@SuppressWarnings("unchecked")
public abstract class ProtoReadConverter<T extends Message> implements Converter<Document, T> {
  private final Class<T> entityClass;

  @Inject
  public ProtoReadConverter(Class<T> entityClass) {
    this.entityClass = entityClass;
  }

  @SneakyThrows
  @Override
  public T convert(Document dbObject) {
    Builder builder = (Builder) entityClass.getMethod("newBuilder").invoke(null);
    JsonFormat.parser().ignoringUnknownFields().merge(dbObject.toJson(), builder);
    return (T) builder.build();
  }
}
