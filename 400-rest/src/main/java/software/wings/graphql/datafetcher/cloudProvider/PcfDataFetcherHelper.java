/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.cloudProvider;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.exception.WingsException.USER;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.exception.InvalidRequestException;
import io.harness.utils.RequestField;

import software.wings.beans.PcfConfig;
import software.wings.beans.PcfConfig.PcfConfigBuilder;
import software.wings.beans.SettingAttribute;
import software.wings.graphql.datafetcher.secrets.UsageScopeController;
import software.wings.graphql.schema.mutation.cloudProvider.QLPcfCloudProviderInput;
import software.wings.graphql.schema.mutation.cloudProvider.QLUpdatePcfCloudProviderInput;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
@TargetModule(HarnessModule._380_CG_GRAPHQL)
@OwnedBy(CDP)
public class PcfDataFetcherHelper {
  @Inject private UsageScopeController usageScopeController;

  public SettingAttribute toSettingAttribute(QLPcfCloudProviderInput input, String accountId) {
    PcfConfigBuilder pcfConfigBuilder = PcfConfig.builder().accountId(accountId);

    validateUsernameFields(input.getUserName(), input.getUserNameSecretId(), false);

    if (input.getEndpointUrl().isPresent()) {
      input.getEndpointUrl().getValue().ifPresent(pcfConfigBuilder::endpointUrl);
    }
    if (input.getUserName().isPresent()) {
      input.getUserName().getValue().map(String::toCharArray).ifPresent(username -> {
        pcfConfigBuilder.username(username);
        pcfConfigBuilder.useEncryptedUsername(false);
      });
    }
    if (input.getUserNameSecretId().isPresent()) {
      input.getUserNameSecretId().getValue().ifPresent(usernameSecretId -> {
        pcfConfigBuilder.encryptedUsername(usernameSecretId);
        pcfConfigBuilder.useEncryptedUsername(true);
      });
    }
    if (input.getPasswordSecretId().isPresent()) {
      input.getPasswordSecretId().getValue().ifPresent(pcfConfigBuilder::encryptedPassword);
    }
    if (input.getSkipValidation().isPresent()) {
      input.getSkipValidation().getValue().ifPresent(pcfConfigBuilder::skipValidation);
    }

    SettingAttribute.Builder settingAttributeBuilder = SettingAttribute.Builder.aSettingAttribute()
                                                           .withValue(pcfConfigBuilder.build())
                                                           .withAccountId(accountId)
                                                           .withCategory(SettingAttribute.SettingCategory.SETTING);

    if (input.getName().isPresent()) {
      input.getName().getValue().ifPresent(settingAttributeBuilder::withName);
    }

    return settingAttributeBuilder.build();
  }

  public void updateSettingAttribute(
      SettingAttribute settingAttribute, QLUpdatePcfCloudProviderInput input, String accountId) {
    PcfConfig pcfConfig = (PcfConfig) settingAttribute.getValue();
    validateUsernameFields(input.getUserName(), input.getUserNameSecretId(), true);

    if (input.getEndpointUrl().isPresent()) {
      input.getEndpointUrl().getValue().ifPresent(pcfConfig::setEndpointUrl);
    }
    if (input.getUserName().isPresent()) {
      input.getUserName().getValue().ifPresent(username -> {
        pcfConfig.setUsername(username.toCharArray());
        pcfConfig.setEncryptedUsername(null);
        pcfConfig.setUseEncryptedUsername(false);
      });
    }
    if (input.getUserNameSecretId().isPresent()) {
      input.getUserNameSecretId().getValue().ifPresent(usernameSecretId -> {
        pcfConfig.setUsername(null);
        pcfConfig.setEncryptedUsername(usernameSecretId);
        pcfConfig.setUseEncryptedUsername(true);
      });
    }
    if (input.getPasswordSecretId().isPresent()) {
      input.getPasswordSecretId().getValue().ifPresent(pcfConfig::setEncryptedPassword);
    }
    if (input.getSkipValidation().isPresent()) {
      input.getSkipValidation().getValue().ifPresent(pcfConfig::setSkipValidation);
    }
    settingAttribute.setValue(pcfConfig);

    if (input.getName().isPresent()) {
      input.getName().getValue().ifPresent(settingAttribute::setName);
    }
  }

  private void validateUsernameFields(
      RequestField<String> userName, RequestField<String> userNameSecretId, boolean isUpdate) {
    if (isFieldValuePresent(userName) && isFieldValuePresent(userNameSecretId)) {
      throw new InvalidRequestException("Cannot set both value and secret reference for username field", USER);
    }

    if (!isUpdate && !isFieldValuePresent(userName) && !isFieldValuePresent(userNameSecretId)) {
      throw new InvalidRequestException("One of fields 'userName' or 'userNameSecretId' is required", USER);
    }
  }

  private <T> boolean isFieldValuePresent(RequestField<T> field) {
    return field.isPresent() && field.getValue().isPresent();
  }
}
