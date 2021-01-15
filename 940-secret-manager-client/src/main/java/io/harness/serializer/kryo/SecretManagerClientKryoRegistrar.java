package io.harness.serializer.kryo;

import io.harness.delegate.beans.SSHTaskParams;
import io.harness.secretmanagerclient.NGEncryptedDataMetadata;
import io.harness.secretmanagerclient.NGSecretManagerMetadata;
import io.harness.secretmanagerclient.dto.EncryptedDataDTO;
import io.harness.secretmanagerclient.dto.GcpKmsConfigDTO;
import io.harness.secretmanagerclient.dto.GcpKmsConfigUpdateDTO;
import io.harness.secretmanagerclient.dto.LocalConfigDTO;
import io.harness.secretmanagerclient.dto.SecretTextDTO;
import io.harness.secretmanagerclient.dto.SecretTextUpdateDTO;
import io.harness.secretmanagerclient.dto.VaultConfigDTO;
import io.harness.secretmanagerclient.dto.VaultConfigUpdateDTO;
import io.harness.serializer.KryoRegistrar;

import com.esotericsoftware.kryo.Kryo;

public class SecretManagerClientKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(NGSecretManagerMetadata.class, 543210);
    kryo.register(NGEncryptedDataMetadata.class, 543211);
    kryo.register(SecretTextDTO.class, 543212);
    kryo.register(SecretTextUpdateDTO.class, 543213);

    kryo.register(EncryptedDataDTO.class, 543216);
    kryo.register(VaultConfigDTO.class, 543217);
    kryo.register(LocalConfigDTO.class, 543218);
    kryo.register(VaultConfigUpdateDTO.class, 543219);
    kryo.register(GcpKmsConfigDTO.class, 543220);
    kryo.register(GcpKmsConfigUpdateDTO.class, 543221);

    kryo.register(SSHTaskParams.class, 543225);
  }
}
