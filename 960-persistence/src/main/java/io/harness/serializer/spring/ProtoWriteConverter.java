/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
