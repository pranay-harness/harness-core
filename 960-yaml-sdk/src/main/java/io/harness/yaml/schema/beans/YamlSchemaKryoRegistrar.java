package io.harness.yaml.schema.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.serializer.KryoRegistrar;

import com.esotericsoftware.kryo.Kryo;

@OwnedBy(HarnessTeam.PIPELINE)
public class YamlSchemaKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(YamlSchemaDetailsWrapper.class, 800100);
    kryo.register(YamlSchemaMetadata.class, 800101);
    kryo.register(YamlGroup.class, 800102);
  }
}
