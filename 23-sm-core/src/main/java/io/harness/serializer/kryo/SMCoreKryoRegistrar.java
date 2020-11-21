package io.harness.serializer.kryo;
import io.harness.beans.AzureEnvironmentType;
import io.harness.beans.EncryptedData;
import io.harness.beans.EncryptedDataParent;
import io.harness.beans.SecretChangeLog;
import io.harness.delegatetasks.DeleteSecretTaskParameters;
import io.harness.delegatetasks.DeleteSecretTaskResponse;
import io.harness.delegatetasks.EncryptSecretTaskParameters;
import io.harness.delegatetasks.EncryptSecretTaskResponse;
import io.harness.delegatetasks.FetchSecretTaskParameters;
import io.harness.delegatetasks.FetchSecretTaskResponse;
import io.harness.delegatetasks.UpsertSecretTaskParameters;
import io.harness.delegatetasks.UpsertSecretTaskResponse;
import io.harness.delegatetasks.UpsertSecretTaskType;
import io.harness.delegatetasks.ValidateSecretReferenceTaskParameters;
import io.harness.delegatetasks.ValidateSecretReferenceTaskResponse;
import io.harness.exception.SecretManagementDelegateException;
import io.harness.exception.SecretManagementException;
import io.harness.helpers.ext.vault.SecretEngineSummary;
import io.harness.helpers.ext.vault.VaultAppRoleLoginResult;
import io.harness.serializer.KryoRegistrar;

import software.wings.beans.AwsSecretsManagerConfig;
import software.wings.beans.AzureVaultConfig;
import software.wings.beans.CyberArkConfig;
import software.wings.beans.GcpKmsConfig;
import software.wings.beans.KmsConfig;
import software.wings.beans.LocalEncryptionConfig;
import software.wings.beans.VaultConfig;

import com.amazonaws.services.secretsmanager.model.AWSSecretsManagerException;
import com.esotericsoftware.kryo.Kryo;

public class SMCoreKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(AwsSecretsManagerConfig.class, 7178);
    kryo.register(AWSSecretsManagerException.class, 7184);
    kryo.register(AzureEnvironmentType.class, 7365);
    kryo.register(AzureVaultConfig.class, 7205);
    kryo.register(CyberArkConfig.class, 7228);
    kryo.register(EncryptedData.class, 5124);
    kryo.register(EncryptedDataParent.class, 7335);
    kryo.register(GcpKmsConfig.class, 7290);
    kryo.register(KmsConfig.class, 5183);
    kryo.register(LocalEncryptionConfig.class, 7180);
    kryo.register(SecretChangeLog.class, 5598);
    kryo.register(SecretEngineSummary.class, 7239);
    kryo.register(SecretManagementDelegateException.class, 5585);
    kryo.register(SecretManagementException.class, 5517);
    kryo.register(VaultAppRoleLoginResult.class, 7240);
    kryo.register(VaultConfig.class, 5214);

    kryo.register(ValidateSecretReferenceTaskParameters.class, 150001);
    kryo.register(UpsertSecretTaskParameters.class, 150002);
    kryo.register(FetchSecretTaskParameters.class, 150003);
    kryo.register(EncryptSecretTaskParameters.class, 150004);
    kryo.register(DeleteSecretTaskParameters.class, 150005);
    kryo.register(UpsertSecretTaskResponse.class, 150006);
    kryo.register(FetchSecretTaskResponse.class, 150007);
    kryo.register(EncryptSecretTaskResponse.class, 150008);
    kryo.register(DeleteSecretTaskResponse.class, 150009);
    kryo.register(ValidateSecretReferenceTaskResponse.class, 150010);
    kryo.register(UpsertSecretTaskType.class, 15011);
  }
}
