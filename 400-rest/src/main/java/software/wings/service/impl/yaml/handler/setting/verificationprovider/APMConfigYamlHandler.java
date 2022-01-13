/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler.setting.verificationprovider;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import software.wings.beans.APMVerificationConfig;
import software.wings.beans.APMVerificationConfig.KeyValues;
import software.wings.beans.APMVerificationConfig.Yaml;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;

import java.util.List;
import java.util.stream.Collectors;

public class APMConfigYamlHandler extends VerificationProviderYamlHandler<Yaml, APMVerificationConfig> {
  @Override
  protected SettingAttribute toBean(
      SettingAttribute previous, ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    String uuid = previous != null ? previous.getUuid() : null;
    Yaml yaml = changeContext.getYaml();
    String accountId = changeContext.getChange().getAccountId();
    APMVerificationConfig config = new APMVerificationConfig();
    config.setAccountId(accountId);
    config.setUrl(yaml.getUrl());
    config.setValidationUrl(yaml.getValidationUrl());
    config.setValidationBody(yaml.getValidationBody());
    config.setValidationMethod(yaml.getValidationMethod());
    config.setLogVerification(yaml.isLogVerification());
    if (isNotEmpty(yaml.getHeadersList())) {
      config.setHeadersList(getKeyValuesListForBean(accountId, yaml.getHeadersList()));
    }
    if (isNotEmpty(yaml.getOptionsList())) {
      config.setOptionsList(getKeyValuesListForBean(accountId, yaml.getOptionsList()));
    }
    if (isNotEmpty(yaml.getAdditionalEncryptedFields())) {
      config.setAdditionalEncryptedFields(getKeyValuesListForBean(accountId, yaml.getAdditionalEncryptedFields()));
    }
    return buildSettingAttribute(accountId, changeContext.getChange().getFilePath(), uuid, config);
  }

  private KeyValues getKeyValueForYaml(String accountId, KeyValues keyValues) {
    String value =
        keyValues.isEncrypted() ? getSecretNameFromId(accountId, keyValues.getEncryptedValue()) : keyValues.getValue();
    return KeyValues.builder().key(keyValues.getKey()).value(value).encrypted(keyValues.isEncrypted()).build();
  }

  private List<KeyValues> getKeyValuesListForYaml(String accountId, List<KeyValues> valuesList) {
    return valuesList.stream().map(x -> getKeyValueForYaml(accountId, x)).collect(Collectors.toList());
  }

  private KeyValues getKeyValueForBean(String accountId, KeyValues keyValues) {
    String value =
        keyValues.isEncrypted() ? getSecretIdFromName(accountId, keyValues.getValue()) : keyValues.getValue();
    String encryptedValue = keyValues.isEncrypted() ? value : null;
    return KeyValues.builder()
        .key(keyValues.getKey())
        .value(value)
        .encrypted(keyValues.isEncrypted())
        .encryptedValue(encryptedValue)
        .build();
  }

  private List<KeyValues> getKeyValuesListForBean(String accountId, List<KeyValues> valuesList) {
    return valuesList.stream().map(x -> getKeyValueForBean(accountId, x)).collect(Collectors.toList());
  }

  @Override
  public Yaml toYaml(SettingAttribute bean, String appId) {
    APMVerificationConfig config = (APMVerificationConfig) bean.getValue();
    Yaml yaml = Yaml.builder()
                    .harnessApiVersion(getHarnessApiVersion())
                    .type(config.getType())
                    .url(config.getUrl())
                    .validationUrl(config.getValidationUrl())
                    .validationBody(config.getValidationBody())
                    .validationMethod(config.getValidationMethod())
                    .logVerification(config.isLogVerification())
                    .build();
    if (isNotEmpty(config.getHeadersList())) {
      yaml.setHeadersList(getKeyValuesListForYaml(config.getAccountId(), config.getHeadersList()));
    }
    if (isNotEmpty(config.getOptionsList())) {
      yaml.setOptionsList(getKeyValuesListForYaml(config.getAccountId(), config.getOptionsList()));
    }
    if (isNotEmpty(config.getAdditionalEncryptedFields())) {
      yaml.setAdditionalEncryptedFields(
          getKeyValuesListForYaml(config.getAccountId(), config.getAdditionalEncryptedFields()));
    }
    toYaml(yaml, bean, appId);
    return yaml;
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }
}
