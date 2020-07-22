package io.harness.serializer.kryo;

import com.esotericsoftware.kryo.Kryo;
import io.harness.secretmanagerclient.NGSecretMetadata;
import io.harness.serializer.KryoRegistrar;

public class SecretManagerClientKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(NGSecretMetadata.class, 543210);
  }
}
