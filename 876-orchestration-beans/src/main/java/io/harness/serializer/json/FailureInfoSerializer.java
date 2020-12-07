package io.harness.serializer.json;

import io.harness.pms.execution.failure.FailureInfo;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.google.protobuf.util.JsonFormat;
import java.io.IOException;

public class FailureInfoSerializer extends StdSerializer<FailureInfo> {
  public FailureInfoSerializer(Class<FailureInfo> t) {
    super(t);
  }

  public FailureInfoSerializer() {
    this(null);
  }
  @Override
  public void serialize(FailureInfo failureInfo, JsonGenerator jgen, SerializerProvider serializerProvider)
      throws IOException {
    jgen.writeRawValue(JsonFormat.printer().print(failureInfo));
  }
}
