package io.harness.pms.serializer.kryo.serializers;

import io.harness.pms.contracts.execution.events.SdkResponseEventRequest;
import io.harness.serializer.kryo.ProtobufKryoSerializer;

public class SdkResponseEventRequestKryoSerializer extends ProtobufKryoSerializer<SdkResponseEventRequest> {
  private static SdkResponseEventRequestKryoSerializer instance;

  public SdkResponseEventRequestKryoSerializer() {}

  public static synchronized SdkResponseEventRequestKryoSerializer getInstance() {
    if (instance == null) {
      instance = new SdkResponseEventRequestKryoSerializer();
    }
    return instance;
  }
}