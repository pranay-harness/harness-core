/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler.setting.cloudprovider;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;

import io.harness.ccm.config.CCMConfig;
import io.harness.ccm.config.CCMConfigYamlHandler;
import io.harness.ccm.config.CCMSettingService;
import io.harness.exception.InvalidRequestException;

import software.wings.beans.KubernetesClusterConfig;
import software.wings.beans.KubernetesClusterConfig.Yaml;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

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

    if (isNotEmpty(kubernetesClusterConfig.getDelegateName())
        && isEmpty(kubernetesClusterConfig.getDelegateSelectors())) {
      yaml.setDelegateSelectors(Collections.singletonList(kubernetesClusterConfig.getDelegateName()));
    } else if (isNotEmpty(kubernetesClusterConfig.getDelegateSelectors())) {
      yaml.setDelegateSelectors(new ArrayList<>(kubernetesClusterConfig.getDelegateSelectors()));
    }

    yaml.setType(kubernetesClusterConfig.getType());
    yaml.setMasterUrl(kubernetesClusterConfig.getMasterUrl());
    yaml.setUsername(
        kubernetesClusterConfig.getUsername() != null ? String.valueOf(kubernetesClusterConfig.getUsername()) : null);
    yaml.setSkipValidation(kubernetesClusterConfig.isSkipValidation());

    String encryptedYamlRef;
    if (kubernetesClusterConfig.isUseEncryptedUsername()) {
      encryptedYamlRef = secretManager.getEncryptedYamlRef(
          kubernetesClusterConfig.getAccountId(), kubernetesClusterConfig.getEncryptedUsername());
      yaml.setUsernameSecretId(encryptedYamlRef);
    }

    if (kubernetesClusterConfig.getEncryptedPassword() != null) {
      encryptedYamlRef = secretManager.getEncryptedYamlRef(
          kubernetesClusterConfig.getAccountId(), kubernetesClusterConfig.getEncryptedPassword());
      yaml.setPassword(encryptedYamlRef);
    }

    if (kubernetesClusterConfig.getEncryptedCaCert() != null) {
      encryptedYamlRef = secretManager.getEncryptedYamlRef(
          kubernetesClusterConfig.getAccountId(), kubernetesClusterConfig.getEncryptedCaCert());
      yaml.setCaCert(encryptedYamlRef);
    }

    if (kubernetesClusterConfig.getEncryptedClientCert() != null) {
      encryptedYamlRef = secretManager.getEncryptedYamlRef(
          kubernetesClusterConfig.getAccountId(), kubernetesClusterConfig.getEncryptedClientCert());
      yaml.setClientCert(encryptedYamlRef);
    }

    if (kubernetesClusterConfig.getEncryptedClientKey() != null) {
      encryptedYamlRef = secretManager.getEncryptedYamlRef(
          kubernetesClusterConfig.getAccountId(), kubernetesClusterConfig.getEncryptedClientKey());
      yaml.setClientKey(encryptedYamlRef);
    }

    if (kubernetesClusterConfig.getEncryptedClientKeyPassphrase() != null) {
      encryptedYamlRef = secretManager.getEncryptedYamlRef(
          kubernetesClusterConfig.getAccountId(), kubernetesClusterConfig.getEncryptedClientKeyPassphrase());
      yaml.setClientKeyPassphrase(encryptedYamlRef);
    }

    if (kubernetesClusterConfig.getEncryptedServiceAccountToken() != null) {
      encryptedYamlRef = secretManager.getEncryptedYamlRef(
          kubernetesClusterConfig.getAccountId(), kubernetesClusterConfig.getEncryptedServiceAccountToken());
      yaml.setServiceAccountToken(encryptedYamlRef);
    }

    if (kubernetesClusterConfig.getEncryptedOidcSecret() != null) {
      encryptedYamlRef = secretManager.getEncryptedYamlRef(
          kubernetesClusterConfig.getAccountId(), kubernetesClusterConfig.getEncryptedOidcSecret());
      yaml.setOidcSecret(encryptedYamlRef);
    }

    if (kubernetesClusterConfig.getEncryptedOidcClientId() != null) {
      encryptedYamlRef = secretManager.getEncryptedYamlRef(
          kubernetesClusterConfig.getAccountId(), kubernetesClusterConfig.getEncryptedOidcClientId());
      yaml.setOidcClientId(encryptedYamlRef);
    }

    if (kubernetesClusterConfig.getEncryptedOidcPassword() != null) {
      encryptedYamlRef = secretManager.getEncryptedYamlRef(
          kubernetesClusterConfig.getAccountId(), kubernetesClusterConfig.getEncryptedOidcPassword());
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

    if (isNotEmpty(yaml.getUsername()) && isNotEmpty(yaml.getUsernameSecretId())) {
      throw new InvalidRequestException("Cannot set both value and secret reference for username field", USER);
    }

    KubernetesClusterConfig kubernetesClusterConfig = KubernetesClusterConfig.builder().accountId(accountId).build();

    kubernetesClusterConfig.setUseKubernetesDelegate(yaml.isUseKubernetesDelegate());
    kubernetesClusterConfig.setDelegateName(yaml.getDelegateName());

    if (isNotEmpty(yaml.getDelegateName()) && isEmpty(yaml.getDelegateSelectors())) {
      kubernetesClusterConfig.setDelegateSelectors(Collections.singleton(yaml.getDelegateName()));
    } else if (isNotEmpty(yaml.getDelegateSelectors())) {
      kubernetesClusterConfig.setDelegateSelectors(new HashSet<>(yaml.getDelegateSelectors()));
    }

    kubernetesClusterConfig.setMasterUrl(yaml.getMasterUrl());
    kubernetesClusterConfig.setUsername(yaml.getUsername() != null ? yaml.getUsername().toCharArray() : null);
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
      if (null != ccmConfig) {
        kubernetesClusterConfig.setCcmConfig(ccmConfig);
      }
    }

    String encryptedRef = yaml.getUsernameSecretId();
    if (encryptedRef != null) {
      kubernetesClusterConfig.setUsername(null);
      kubernetesClusterConfig.setEncryptedUsername(encryptedRef);
      kubernetesClusterConfig.setUseEncryptedUsername(true);
    }

    encryptedRef = yaml.getPassword();
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
