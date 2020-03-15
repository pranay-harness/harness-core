package software.wings.service.impl.yaml.handler.setting.cloudprovider;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.ccm.CCMConfig;
import io.harness.ccm.CCMConfigYamlHandler;
import io.harness.ccm.CCMSettingService;
import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.KubernetesClusterConfig;
import software.wings.beans.KubernetesClusterConfig.Yaml;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;

import java.util.List;
import java.util.Optional;

@Singleton
@Slf4j
public class KubernetesClusterConfigYamlHandler extends CloudProviderYamlHandler<Yaml, KubernetesClusterConfig> {
  @Inject CCMConfigYamlHandler ccmConfigYamlHandler;
  @Inject CCMSettingService ccmSettingService;

  @Override
  public Yaml toYaml(SettingAttribute settingAttribute, String appId) {
    KubernetesClusterConfig kubernetesClusterConfig = (KubernetesClusterConfig) settingAttribute.getValue();
    KubernetesClusterConfig.Yaml yaml =
        KubernetesClusterConfig.Yaml.builder().harnessApiVersion(getHarnessApiVersion()).build();

    yaml.setUseKubernetesDelegate(kubernetesClusterConfig.isUseKubernetesDelegate());
    yaml.setDelegateName(kubernetesClusterConfig.getDelegateName());
    yaml.setType(kubernetesClusterConfig.getType());
    yaml.setMasterUrl(kubernetesClusterConfig.getMasterUrl());
    yaml.setUsername(kubernetesClusterConfig.getUsername());
    yaml.setSkipValidation(kubernetesClusterConfig.isSkipValidation());

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

      if (kubernetesClusterConfig.getEncryptedOidcSecret() != null) {
        fieldName = "oidcSecret";
        encryptedYamlRef = secretManager.getEncryptedYamlRef(kubernetesClusterConfig, fieldName);
        yaml.setOidcSecret(encryptedYamlRef);
      }

      if (kubernetesClusterConfig.getEncryptedOidcClientId() != null) {
        fieldName = "oidcClientId";
        encryptedYamlRef = secretManager.getEncryptedYamlRef(kubernetesClusterConfig, fieldName);
        yaml.setOidcClientId(encryptedYamlRef);
      }

      if (kubernetesClusterConfig.getEncryptedOidcPassword() != null) {
        fieldName = "oidcPassword";
        encryptedYamlRef = secretManager.getEncryptedYamlRef(kubernetesClusterConfig, fieldName);
        yaml.setOidcPassword(encryptedYamlRef);
      }

      yaml.setOidcIdentityProviderUrl(kubernetesClusterConfig.getOidcIdentityProviderUrl());
      yaml.setOidcUsername(kubernetesClusterConfig.getOidcUsername());
      yaml.setOidcGrantType(kubernetesClusterConfig.getOidcGrantType());
      yaml.setOidcScopes(kubernetesClusterConfig.getOidcScopes());
      yaml.setAuthType(kubernetesClusterConfig.getAuthType());

      yaml.setClientKeyAlgo(kubernetesClusterConfig.getClientKeyAlgo());

      if (ccmSettingService.isCloudCostEnabled(settingAttribute.getAccountId())) {
        CCMConfig.Yaml ccmConfigYaml = ccmConfigYamlHandler.toYaml(kubernetesClusterConfig.getCcmConfig(), "");
        yaml.setContinuousEfficiencyConfig(ccmConfigYaml);
      }
      toYaml(yaml, settingAttribute, appId);

    } catch (IllegalAccessException e) {
      logger.warn("Invalid " + fieldName + ". Should be a valid url to a secret");
      throw new WingsException(e);
    }
    return yaml;
  }

  @VisibleForTesting
  @Override
  public SettingAttribute toBean(
      SettingAttribute previous, ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    Optional<SettingAttribute> optionalPrevious = Optional.ofNullable(previous);
    String uuid = null;
    if (optionalPrevious.isPresent()) {
      uuid = previous.getUuid();
    }

    Yaml yaml = changeContext.getYaml();
    String accountId = changeContext.getChange().getAccountId();

    KubernetesClusterConfig kubernetesClusterConfig = KubernetesClusterConfig.builder().accountId(accountId).build();

    kubernetesClusterConfig.setUseKubernetesDelegate(yaml.isUseKubernetesDelegate());
    kubernetesClusterConfig.setDelegateName(yaml.getDelegateName());
    kubernetesClusterConfig.setMasterUrl(yaml.getMasterUrl());
    kubernetesClusterConfig.setUsername(yaml.getUsername());
    kubernetesClusterConfig.setClientKeyAlgo(yaml.getClientKeyAlgo());

    kubernetesClusterConfig.setEncryptedServiceAccountToken(yaml.getServiceAccountToken());
    kubernetesClusterConfig.setEncryptedPassword(yaml.getPassword());
    kubernetesClusterConfig.setEncryptedCaCert(yaml.getCaCert());
    kubernetesClusterConfig.setEncryptedClientCert(yaml.getClientCert());
    kubernetesClusterConfig.setEncryptedClientKey(yaml.getClientKey());
    kubernetesClusterConfig.setEncryptedClientKeyPassphrase(yaml.getClientKeyPassphrase());
    kubernetesClusterConfig.setSkipValidation(yaml.isSkipValidation());
    kubernetesClusterConfig.setOidcIdentityProviderUrl(yaml.getOidcIdentityProviderUrl());
    kubernetesClusterConfig.setAuthType(yaml.getAuthType());
    kubernetesClusterConfig.setOidcUsername(yaml.getOidcUsername());
    kubernetesClusterConfig.setOidcGrantType(yaml.getOidcGrantType());
    kubernetesClusterConfig.setOidcScopes(yaml.getOidcScopes());
    kubernetesClusterConfig.setEncryptedOidcClientId(yaml.getOidcClientId());
    kubernetesClusterConfig.setEncryptedOidcSecret(yaml.getOidcSecret());
    kubernetesClusterConfig.setEncryptedOidcPassword(yaml.getOidcPassword());

    ChangeContext.Builder clonedContextBuilder =
        cloneFileChangeContext(changeContext, changeContext.getYaml().getContinuousEfficiencyConfig());
    ChangeContext clonedContext = clonedContextBuilder.build();

    if (optionalPrevious.isPresent() && ccmSettingService.isCloudCostEnabled(previous.getAccountId())) {
      CCMConfig ccmConfig = ccmConfigYamlHandler.upsertFromYaml(clonedContext, changeSetContext);
      kubernetesClusterConfig.setCcmConfig(ccmConfig);
    }

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
