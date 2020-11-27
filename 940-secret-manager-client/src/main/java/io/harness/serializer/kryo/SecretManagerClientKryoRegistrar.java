package io.harness.serializer.kryo;

import io.harness.delegate.beans.SSHTaskParams;
import io.harness.ng.core.dto.secrets.KerberosConfigDTO;
import io.harness.ng.core.dto.secrets.SSHAuthDTO;
import io.harness.ng.core.dto.secrets.SSHConfigDTO;
import io.harness.ng.core.dto.secrets.SSHCredentialType;
import io.harness.ng.core.dto.secrets.SSHKeyPathCredentialDTO;
import io.harness.ng.core.dto.secrets.SSHKeyReferenceCredentialDTO;
import io.harness.ng.core.dto.secrets.SSHKeySpecDTO;
import io.harness.ng.core.dto.secrets.SSHPasswordCredentialDTO;
import io.harness.ng.core.dto.secrets.TGTGenerationMethod;
import io.harness.ng.core.dto.secrets.TGTKeyTabFilePathSpecDTO;
import io.harness.ng.core.dto.secrets.TGTPasswordSpecDTO;
import io.harness.secretmanagerclient.NGEncryptedDataMetadata;
import io.harness.secretmanagerclient.NGSecretManagerMetadata;
import io.harness.secretmanagerclient.SSHAuthScheme;
import io.harness.secretmanagerclient.SecretType;
import io.harness.secretmanagerclient.ValueType;
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
    kryo.register(SecretType.class, 543214);
    kryo.register(ValueType.class, 543215);
    kryo.register(EncryptedDataDTO.class, 543216);
    kryo.register(VaultConfigDTO.class, 543217);
    kryo.register(LocalConfigDTO.class, 543218);
    kryo.register(VaultConfigUpdateDTO.class, 543219);
    kryo.register(GcpKmsConfigDTO.class, 543220);
    kryo.register(GcpKmsConfigUpdateDTO.class, 543221);
    kryo.register(SSHKeySpecDTO.class, 543222);
    kryo.register(SSHAuthScheme.class, 543223);
    kryo.register(SSHConfigDTO.class, 543224);
    kryo.register(SSHTaskParams.class, 543225);
    kryo.register(TGTGenerationMethod.class, 543226);
    kryo.register(TGTPasswordSpecDTO.class, 543227);
    kryo.register(SSHCredentialType.class, 543228);
    kryo.register(TGTKeyTabFilePathSpecDTO.class, 543229);
    kryo.register(SSHKeyReferenceCredentialDTO.class, 543230);
    kryo.register(SSHPasswordCredentialDTO.class, 543231);
    kryo.register(SSHKeyPathCredentialDTO.class, 543232);
    kryo.register(KerberosConfigDTO.class, 543233);
    kryo.register(SSHAuthDTO.class, 543234);
  }
}
