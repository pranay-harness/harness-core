package io.harness.serializer.kryo;

import com.esotericsoftware.kryo.Kryo;
import io.harness.secretmanagerclient.NGEncryptedDataMetadata;
import io.harness.secretmanagerclient.NGSecretManagerMetadata;
import io.harness.secretmanagerclient.SecretType;
import io.harness.secretmanagerclient.ValueType;
import io.harness.secretmanagerclient.dto.EncryptedDataDTO;
import io.harness.secretmanagerclient.dto.LocalConfigDTO;
import io.harness.secretmanagerclient.dto.SecretTextDTO;
import io.harness.secretmanagerclient.dto.SecretTextUpdateDTO;
import io.harness.secretmanagerclient.dto.VaultConfigDTO;
import io.harness.secretmanagerclient.dto.VaultConfigUpdateDTO;
import io.harness.serializer.KryoRegistrar;

public class SecretManagerClientKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(NGSecretManagerMetadata.class, 543210);
    kryo.register(NGEncryptedDataMetadata.class, 543211);
    kryo.register(SecretTextDTO.class, 543212);
    kryo.register(SecretTextUpdateDTO.class, 543213);
    kryo.register(SecretType.class, 543214);
    kryo.register(ValueType.class, 543215);
    kryo.register(EncryptedDataDTO.class, 543216);
    kryo.register(VaultConfigDTO.class, 543217);
    kryo.register(LocalConfigDTO.class, 543218);
    kryo.register(VaultConfigUpdateDTO.class, 543219);
  }
}
