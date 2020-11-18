package io.harness.pms.creator;

import com.google.protobuf.ByteString;

import io.harness.serializer.KryoSerializer;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PlanCreationContext {
  KryoSerializer kryoSerializer;

  public ByteString toByteString(Object o) {
    return ByteString.copyFrom(kryoSerializer.asBytes(o));
  }
}
