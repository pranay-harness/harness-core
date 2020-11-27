package io.harness.serializer.morphia;

import io.harness.beans.EncryptedData;
import io.harness.beans.MigrateSecretTask;
import io.harness.beans.SecretChangeLog;
import io.harness.beans.SecretManagerConfig;
import io.harness.beans.SecretUsageLog;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.morphia.MorphiaRegistrarHelperPut;

import software.wings.beans.AwsSecretsManagerConfig;
import software.wings.beans.AzureVaultConfig;
import software.wings.beans.CyberArkConfig;
import software.wings.beans.GcpKmsConfig;
import software.wings.beans.KmsConfig;
import software.wings.beans.LocalEncryptionConfig;
import software.wings.beans.VaultConfig;

import java.util.Set;

public class SMCoreMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(AwsSecretsManagerConfig.class);
    set.add(AzureVaultConfig.class);
    set.add(CyberArkConfig.class);
    set.add(EncryptedData.class);
    set.add(GcpKmsConfig.class);
    set.add(KmsConfig.class);
    set.add(LocalEncryptionConfig.class);
    set.add(SecretChangeLog.class);
    set.add(SecretManagerConfig.class);
    set.add(SecretUsageLog.class);
    set.add(VaultConfig.class);
    set.add(MigrateSecretTask.class);
  }

  @Override
  public void registerImplementationClasses(MorphiaRegistrarHelperPut h, MorphiaRegistrarHelperPut w) {
    w.put("beans.AwsSecretsManagerConfig", AwsSecretsManagerConfig.class);
    w.put("beans.AzureVaultConfig", AzureVaultConfig.class);
    w.put("beans.CyberArkConfig", CyberArkConfig.class);
    w.put("beans.GcpKmsConfig", GcpKmsConfig.class);
    w.put("beans.KmsConfig", KmsConfig.class);
    w.put("beans.LocalEncryptionConfig", LocalEncryptionConfig.class);
    w.put("beans.SecretManagerConfig", SecretManagerConfig.class);
    w.put("beans.VaultConfig", VaultConfig.class);
  }
}
