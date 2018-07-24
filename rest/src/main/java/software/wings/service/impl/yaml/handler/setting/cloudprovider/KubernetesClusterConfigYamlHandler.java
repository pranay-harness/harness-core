package software.wings.service.impl.yaml.handler.setting.cloudprovider;

import com.google.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.KubernetesClusterConfig;
import software.wings.beans.KubernetesClusterConfig.Yaml;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;
import software.wings.exception.WingsException;
import software.wings.service.impl.yaml.handler.inframapping.DirectKubernetesInfraMappingYamlHandler;

import java.util.List;

@Singleton
public class KubernetesClusterConfigYamlHandler extends CloudProviderYamlHandler<Yaml, KubernetesClusterConfig> {
  private static final Logger logger = LoggerFactory.getLogger(DirectKubernetesInfraMappingYamlHandler.class);

  @Override
  public Yaml toYaml(SettingAttribute settingAttribute, String appId) {
    KubernetesClusterConfig kubernetesClusterConfig = (KubernetesClusterConfig) settingAttribute.getValue();
    KubernetesClusterConfig.Yaml yaml = KubernetesClusterConfig.Yaml.builder().build();

    yaml.setUseKubernetesDelegate(kubernetesClusterConfig.isUseKubernetesDelegate());
    yaml.setDelegateName(kubernetesClusterConfig.getDelegateName());
    yaml.setType(kubernetesClusterConfig.getType());
    yaml.setMasterUrl(kubernetesClusterConfig.getMasterUrl());
    yaml.setUsername(kubernetesClusterConfig.getUsername());

    String fieldName = null;
    String encryptedYamlRef;
    try {
      if (kubernetesClusterConfig.getEncryptedPassword() != null) {
        fieldName = "password";
        encryptedYamlRef = secretManager.getEncryptedYamlRef(kubernetesClusterConfig, fieldName);
        yaml.setPassword(encryptedYamlRef);
      }

      if (kubernetesClusterConfig.getEncryptedCaCert() != null) {
        fieldName = "caCert";
        encryptedYamlRef = secretManager.getEncryptedYamlRef(kubernetesClusterConfig, fieldName);
        yaml.setCaCert(encryptedYamlRef);
      }

      if (kubernetesClusterConfig.getEncryptedClientCert() != null) {
        fieldName = "clientCert";
        encryptedYamlRef = secretManager.getEncryptedYamlRef(kubernetesClusterConfig, fieldName);
        yaml.setClientCert(encryptedYamlRef);
      }

      if (kubernetesClusterConfig.getEncryptedClientKey() != null) {
        fieldName = "clientKey";
        encryptedYamlRef = secretManager.getEncryptedYamlRef(kubernetesClusterConfig, fieldName);
        yaml.setClientKey(encryptedYamlRef);
      }

      if (kubernetesClusterConfig.getEncryptedClientKeyPassphrase() != null) {
        fieldName = "clientKeyPassphrase";
        encryptedYamlRef = secretManager.getEncryptedYamlRef(kubernetesClusterConfig, fieldName);
        yaml.setClientKeyPassphrase(encryptedYamlRef);
      }

      if (kubernetesClusterConfig.getEncryptedServiceAccountToken() != null) {
        fieldName = "serviceAccountToken";
        encryptedYamlRef = secretManager.getEncryptedYamlRef(kubernetesClusterConfig, fieldName);
        yaml.setServiceAccountToken(encryptedYamlRef);
      }

      yaml.setClientKeyAlgo(kubernetesClusterConfig.getClientKeyAlgo());

    } catch (IllegalAccessException e) {
      logger.warn("Invalid " + fieldName + ". Should be a valid url to a secret");
      throw new WingsException(e);
    }
    return yaml;
  }

  protected SettingAttribute toBean(SettingAttribute previous, ChangeContext<Yaml> changeContext,
      List<ChangeContext> changeSetContext) throws HarnessException {
    String uuid = previous != null ? previous.getUuid() : null;
    Yaml yaml = changeContext.getYaml();
    String accountId = changeContext.getChange().getAccountId();

    KubernetesClusterConfig kubernetesClusterConfig = KubernetesClusterConfig.builder().accountId(accountId).build();

    kubernetesClusterConfig.setUseKubernetesDelegate(yaml.isUseKubernetesDelegate());
    kubernetesClusterConfig.setDelegateName(yaml.getDelegateName());
    kubernetesClusterConfig.setMasterUrl(yaml.getMasterUrl());
    kubernetesClusterConfig.setUsername(yaml.getUsername());
    kubernetesClusterConfig.setClientKeyAlgo(yaml.getClientKeyAlgo());

    kubernetesClusterConfig.setEncryptedPassword(yaml.getPassword());
    kubernetesClusterConfig.setEncryptedCaCert(yaml.getCaCert());
    kubernetesClusterConfig.setEncryptedClientCert(yaml.getClientCert());
    kubernetesClusterConfig.setEncryptedClientKey(yaml.getClientKey());
    kubernetesClusterConfig.setEncryptedClientKeyPassphrase(yaml.getClientKeyPassphrase());

    String encryptedRef = yaml.getPassword();
    if (encryptedRef != null) {
      kubernetesClusterConfig.setPassword(null);
      kubernetesClusterConfig.setEncryptedPassword(encryptedRef);
    }

    encryptedRef = yaml.getCaCert();
    if (encryptedRef != null) {
      kubernetesClusterConfig.setCaCert(null);
      kubernetesClusterConfig.setEncryptedCaCert(encryptedRef);
    }

    encryptedRef = yaml.getClientCert();
    if (encryptedRef != null) {
      kubernetesClusterConfig.setClientCert(null);
      kubernetesClusterConfig.setEncryptedClientCert(encryptedRef);
    }

    encryptedRef = yaml.getClientKey();
    if (encryptedRef != null) {
      kubernetesClusterConfig.setClientKey(null);
      kubernetesClusterConfig.setEncryptedClientKey(encryptedRef);
    }

    encryptedRef = yaml.getClientKeyPassphrase();
    if (encryptedRef != null) {
      kubernetesClusterConfig.setClientKeyPassphrase(null);
      kubernetesClusterConfig.setEncryptedClientKeyPassphrase(encryptedRef);
    }

    return buildSettingAttribute(accountId, changeContext.getChange().getFilePath(), uuid, kubernetesClusterConfig);
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }
}
