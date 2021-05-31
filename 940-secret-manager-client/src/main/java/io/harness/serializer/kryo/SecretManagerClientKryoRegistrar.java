package io.harness.serializer.kryo;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.SSHTaskParams;
import io.harness.ng.core.NGAccessWithEncryptionConsumer;
import io.harness.ng.core.entities.SampleEncryptableSettingImplementation;
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
import io.harness.secretmanagerclient.dto.awskms.AwsKmsConfigDTO;
import io.harness.secretmanagerclient.dto.awskms.AwsKmsConfigUpdateDTO;
import io.harness.secretmanagerclient.dto.awskms.AwsKmsCredentialSpecConfig;
import io.harness.secretmanagerclient.dto.awskms.AwsKmsIamCredentialConfig;
import io.harness.secretmanagerclient.dto.awskms.AwsKmsManualCredentialConfig;
import io.harness.secretmanagerclient.dto.awskms.AwsKmsStsCredentialConfig;
import io.harness.secretmanagerclient.dto.awskms.BaseAwsKmsConfigDTO;
import io.harness.secretmanagerclient.dto.azurekeyvault.AzureKeyVaultConfigDTO;
import io.harness.secretmanagerclient.dto.azurekeyvault.AzureKeyVaultConfigUpdateDTO;
import io.harness.serializer.KryoRegistrar;

import com.esotericsoftware.kryo.Kryo;

@OwnedBy(PL)
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

    kryo.register(SampleEncryptableSettingImplementation.class, 54322);
    kryo.register(NGAccessWithEncryptionConsumer.class, 54323);

    kryo.register(AwsKmsConfigDTO.class, 643283);
    kryo.register(AwsKmsConfigUpdateDTO.class, 643284);
    kryo.register(BaseAwsKmsConfigDTO.class, 543287);

    kryo.register(AwsKmsCredentialSpecConfig.class, 543295);
    kryo.register(AwsKmsIamCredentialConfig.class, 543296);
    kryo.register(AwsKmsManualCredentialConfig.class, 543297);
    kryo.register(AwsKmsStsCredentialConfig.class, 543298);

    kryo.register(AzureKeyVaultConfigDTO.class, 543299);
    kryo.register(AzureKeyVaultConfigUpdateDTO.class, 543300);
  }
}
