/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
