package io.serializer.kryo;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.polling.contracts.service.PollingDocument;
import io.harness.serializer.kryo.ProtobufKryoSerializer;

@OwnedBy(PIPELINE)
public class PollingDocumentKryoSerializer extends ProtobufKryoSerializer<PollingDocument> {
  private static PollingDocumentKryoSerializer instance;

  public PollingDocumentKryoSerializer() {}

  public static synchronized PollingDocumentKryoSerializer getInstance() {
    if (instance == null) {
      instance = new PollingDocumentKryoSerializer();
    }
    return instance;
  }
}