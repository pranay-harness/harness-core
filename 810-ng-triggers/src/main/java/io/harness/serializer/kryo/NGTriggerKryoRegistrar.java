package io.harness.serializer.kryo;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ngtriggers.beans.config.NGTriggerConfig;
import io.harness.ngtriggers.beans.config.NGTriggerConfigV2;
import io.harness.polling.contracts.PollingItem;
import io.harness.polling.contracts.service.PollingDocument;
import io.harness.serializer.KryoRegistrar;

import com.esotericsoftware.kryo.Kryo;
import io.serializer.kryo.PollingDocumentKryoSerializer;
import io.serializer.kryo.PollingItemKryoSerializer;

@OwnedBy(PIPELINE)
public class NGTriggerKryoRegistrar implements KryoRegistrar {
  // Next ID: 400_002
  @Override
  public void register(Kryo kryo) {
    kryo.register(NGTriggerConfig.class, 400001);
    kryo.register(NGTriggerConfigV2.class, 400002);
    //    kryo.register(PollingItem.class, PollingItemKryoSerializer.getInstance(), 400003);
    //    kryo.register(PollingDocument.class, PollingDocumentKryoSerializer.getInstance(), 400004);
  }
}
