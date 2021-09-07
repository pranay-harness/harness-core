package io.serializer.jackson;

import io.harness.pms.yaml.ParameterField;

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
    if (ParameterField.isNull(value)) {
      gen.writeString("");
    } else {
      gen.writeObject(value.getJsonFieldValue());
    }
  }
}
