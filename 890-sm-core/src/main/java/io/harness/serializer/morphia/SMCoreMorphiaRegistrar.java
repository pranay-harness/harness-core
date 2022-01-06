/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.serializer.morphia;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EncryptedData;
import io.harness.beans.MigrateSecretTask;
import io.harness.beans.SecretChangeLog;
import io.harness.beans.SecretKey;
import io.harness.beans.SecretManagerConfig;
import io.harness.beans.SecretUsageLog;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.morphia.MorphiaRegistrarHelperPut;
import io.harness.ng.core.entities.NGEncryptedData;

import software.wings.beans.AwsSecretsManagerConfig;
import software.wings.beans.AzureVaultConfig;
import software.wings.beans.BaseVaultConfig;
import software.wings.beans.CyberArkConfig;
import software.wings.beans.GcpKmsConfig;
import software.wings.beans.GcpSecretsManagerConfig;
import software.wings.beans.KmsConfig;
import software.wings.beans.LocalEncryptionConfig;
import software.wings.beans.SSHVaultConfig;
import software.wings.beans.SecretManagerRuntimeParameters;
import software.wings.beans.VaultConfig;

import java.util.Set;

@OwnedBy(PL)
public class SMCoreMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(AwsSecretsManagerConfig.class);
    set.add(AzureVaultConfig.class);
    set.add(CyberArkConfig.class);
    set.add(EncryptedData.class);
    set.add(GcpKmsConfig.class);
    set.add(GcpSecretsManagerConfig.class);
    set.add(KmsConfig.class);
    set.add(LocalEncryptionConfig.class);
    set.add(SecretChangeLog.class);
    set.add(SecretManagerConfig.class);
    set.add(SecretUsageLog.class);
    set.add(VaultConfig.class);
    set.add(SSHVaultConfig.class);
    set.add(BaseVaultConfig.class);
    set.add(MigrateSecretTask.class);
    set.add(NGEncryptedData.class);
    set.add(SecretManagerRuntimeParameters.class);
    set.add(SecretKey.class);
  }
  @Override
  public void registerImplementationClasses(MorphiaRegistrarHelperPut h, MorphiaRegistrarHelperPut w) {
    w.put("beans.AwsSecretsManagerConfig", AwsSecretsManagerConfig.class);
    w.put("beans.AzureVaultConfig", AzureVaultConfig.class);
    w.put("beans.CyberArkConfig", CyberArkConfig.class);
    w.put("beans.GcpKmsConfig", GcpKmsConfig.class);
    w.put("beans.GcpSecretsManagerConfig", GcpSecretsManagerConfig.class);
    w.put("beans.KmsConfig", KmsConfig.class);
    w.put("beans.LocalEncryptionConfig", LocalEncryptionConfig.class);
    w.put("beans.SecretManagerConfig", SecretManagerConfig.class);
    w.put("beans.VaultConfig", VaultConfig.class);
    w.put("beans.SSHVaultConfig", SSHVaultConfig.class);
    w.put("beans.BaseVaultConfig", BaseVaultConfig.class);
  }
}
