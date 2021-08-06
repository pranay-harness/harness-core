package io.serializer.kryo;

import io.harness.polling.contracts.PollingItem;
import io.harness.polling.contracts.service.PollingDocument;
import io.harness.serializer.KryoRegistrar;

import com.esotericsoftware.kryo.Kryo;

public class PollingKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(PollingItem.class, PollingItemKryoSerializer.getInstance(), 410001);
    kryo.register(PollingDocument.class, PollingDocumentKryoSerializer.getInstance(), 410002);
  }
}
