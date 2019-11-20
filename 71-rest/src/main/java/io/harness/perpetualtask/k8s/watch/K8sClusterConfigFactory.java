package io.harness.perpetualtask.k8s.watch;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.security.encryption.EncryptedDataDetail;
import software.wings.annotation.EncryptableSetting;
import software.wings.beans.SettingAttribute;
import software.wings.helpers.ext.k8s.request.K8sClusterConfig;
import software.wings.service.intfc.security.SecretManager;

import java.util.List;

@Singleton
public class K8sClusterConfigFactory {
  private final SecretManager secretManager;

  @Inject
  K8sClusterConfigFactory(SecretManager secretManager) {
    this.secretManager = secretManager;
  }

  public K8sClusterConfig getK8sClusterConfig(SettingAttribute settingAttribute) {
    List<EncryptedDataDetail> encryptionDetails =
        secretManager.getEncryptionDetails((EncryptableSetting) settingAttribute.getValue());

    return K8sClusterConfig.builder()
        .cloudProvider(settingAttribute.getValue())
        .cloudProviderEncryptionDetails(encryptionDetails)
        .build();
  }
}
