/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.serializer.json;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import java.io.IOException;

@OwnedBy(PIPELINE)
public class ProtoJsonSerializer<T extends Message> extends StdSerializer<T> {
  public ProtoJsonSerializer(Class<T> t) {
    super(t);
  }
  public ProtoJsonSerializer() {
    this(null);
  }
  @Override
  public void serialize(T entity, JsonGenerator jgen, SerializerProvider serializerProvider) throws IOException {
    jgen.writeRawValue(JsonFormat.printer().includingDefaultValueFields().print(entity));
  }
}
