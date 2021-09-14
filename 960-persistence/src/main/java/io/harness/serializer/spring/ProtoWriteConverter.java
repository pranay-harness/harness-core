/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.serializer.spring;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;

import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import lombok.SneakyThrows;
import org.bson.Document;
import org.springframework.core.convert.converter.Converter;

@OwnedBy(PIPELINE)
public abstract class ProtoWriteConverter<T extends Message> implements Converter<T, Document> {
  @SneakyThrows
  @Override
  public Document convert(T entity) {
    String entityJson = JsonFormat.printer().includingDefaultValueFields().print(entity);
    return Document.parse(entityJson);
  }
}
