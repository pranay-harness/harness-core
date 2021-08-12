package io.serializer.kryo;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.polling.contracts.PollingItem;
import io.harness.serializer.kryo.ProtobufKryoSerializer;

@OwnedBy(PIPELINE)
public class PollingItemKryoSerializer extends ProtobufKryoSerializer<PollingItem> {
  private static PollingItemKryoSerializer instance;

  public PollingItemKryoSerializer() {}

  public static synchronized PollingItemKryoSerializer getInstance() {
    if (instance == null) {
      instance = new PollingItemKryoSerializer();
    }
    return instance;
  }
}