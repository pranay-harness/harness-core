/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.serializer.jackson;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.yaml.ParameterField;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;

@OwnedBy(PIPELINE)
public class ParameterFieldSerializer extends StdSerializer<ParameterField<?>> {
  private static final long serialVersionUID = 1L;

  public ParameterFieldSerializer() {
    this(null);
  }

  public ParameterFieldSerializer(Class<ParameterField<?>> cls) {
    super(cls);
  }

  @Override
  public void serialize(ParameterField<?> value, JsonGenerator gen, SerializerProvider provider) throws IOException {
    gen.writeObject(value.getJsonFieldValue());
  }
}
