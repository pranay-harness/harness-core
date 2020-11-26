package io.harness.serializer.jackson;

import io.harness.beans.ParameterField;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;

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
