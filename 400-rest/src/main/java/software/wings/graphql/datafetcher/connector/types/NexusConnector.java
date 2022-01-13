/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.connector.types;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.exception.InvalidRequestException;
import io.harness.utils.RequestField;

import software.wings.beans.SettingAttribute;
import software.wings.beans.config.NexusConfig;
import software.wings.graphql.datafetcher.connector.ConnectorsController;
import software.wings.graphql.schema.mutation.connector.input.QLConnectorInput;
import software.wings.graphql.schema.mutation.connector.input.QLUpdateConnectorInput;
import software.wings.graphql.schema.mutation.connector.input.nexus.QLNexusConnectorInput;
import software.wings.service.intfc.security.SecretManager;

import java.util.Optional;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@AllArgsConstructor
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class NexusConnector extends Connector {
  private SecretManager secretManager;
  private ConnectorsController connectorsController;

  @Override
  public SettingAttribute getSettingAttribute(QLConnectorInput input, String accountId) {
    QLNexusConnectorInput nexusConnectorInput = input.getNexusConnector();
    NexusConfig nexusConfig = new NexusConfig();
    nexusConfig.setAccountId(accountId);

    setUsernameAndPassword(nexusConnectorInput, nexusConfig);
    checkUrlExists(nexusConnectorInput, nexusConfig);
    setNexusVersion(nexusConnectorInput, nexusConfig);
    setDelegateSelectors(nexusConnectorInput, nexusConfig);

    SettingAttribute.Builder settingAttributeBuilder = SettingAttribute.Builder.aSettingAttribute()
                                                           .withValue(nexusConfig)
                                                           .withAccountId(accountId)
                                                           .withCategory(SettingAttribute.SettingCategory.SETTING);

    if (nexusConnectorInput.getName().isPresent()) {
      nexusConnectorInput.getName().getValue().ifPresent(name -> settingAttributeBuilder.withName(name.trim()));
    }

    return settingAttributeBuilder.build();
  }

  @Override
  public void updateSettingAttribute(SettingAttribute settingAttribute, QLUpdateConnectorInput input) {
    QLNexusConnectorInput nexusConnectorInput = input.getNexusConnector();
    NexusConfig nexusConfig = (NexusConfig) settingAttribute.getValue();

    setUsernameAndPassword(nexusConnectorInput, nexusConfig);
    checkUrlExists(nexusConnectorInput, nexusConfig);
    setNexusVersion(nexusConnectorInput, nexusConfig);
    setDelegateSelectors(nexusConnectorInput, nexusConfig);

    settingAttribute.setValue(nexusConfig);

    if (nexusConnectorInput.getName().isPresent()) {
      nexusConnectorInput.getName().getValue().ifPresent(name -> settingAttribute.setName(name.trim()));
    }
  }

  @Override
  public void checkSecrets(QLConnectorInput input, String accountId) {
    QLNexusConnectorInput nexusConnectorInput = input.getNexusConnector();
    checkNexusConnectorSecrets(accountId, nexusConnectorInput);
  }

  private void checkNexusConnectorSecrets(String accountId, QLNexusConnectorInput nexusConnectorInput) {
    checkUserNameExists(nexusConnectorInput);

    nexusConnectorInput.getPasswordSecretId().getValue().ifPresent(
        secretId -> checkSecretExists(secretManager, accountId, secretId));
  }

  @Override
  public void checkSecrets(QLUpdateConnectorInput input, SettingAttribute settingAttribute) {
    QLNexusConnectorInput nexusConnectorInput = input.getNexusConnector();
    checkNexusConnectorSecrets(settingAttribute.getAccountId(), nexusConnectorInput);
  }

  @Override
  public void checkInputExists(QLConnectorInput input) {
    connectorsController.checkInputExists(input.getConnectorType(), input.getNexusConnector());
  }

  @Override
  public void checkInputExists(QLUpdateConnectorInput input) {
    connectorsController.checkInputExists(input.getConnectorType(), input.getNexusConnector());
  }

  private void checkUserNameExists(QLNexusConnectorInput nexusConnectorInput) {
    RequestField<String> passwordSecretId = nexusConnectorInput.getPasswordSecretId();
    RequestField<String> userName = nexusConnectorInput.getUserName();
    Optional<String> userNameValue;

    if (passwordSecretId.isPresent() && passwordSecretId.getValue().isPresent()) {
      if (userName.isPresent()) {
        userNameValue = userName.getValue();
        if (!userNameValue.isPresent() || StringUtils.isBlank(userNameValue.get())) {
          throw new InvalidRequestException("userName should be specified");
        }
      } else {
        throw new InvalidRequestException("userName should be specified");
      }
    }
  }

  private void checkUrlExists(QLNexusConnectorInput nexusConnectorInput, NexusConfig nexusConfig) {
    if (nexusConnectorInput.getURL().isPresent()) {
      String url;
      Optional<String> urlValue = nexusConnectorInput.getURL().getValue();
      if (urlValue.isPresent() && StringUtils.isNotBlank(urlValue.get())) {
        url = urlValue.get().trim();
      } else {
        throw new InvalidRequestException("URL should be specified");
      }
      nexusConfig.setNexusUrl(url);
    }
  }

  private void setUsernameAndPassword(QLNexusConnectorInput nexusConnectorInput, NexusConfig nexusConfig) {
    if (nexusConnectorInput.getPasswordSecretId().isPresent()) {
      nexusConnectorInput.getPasswordSecretId().getValue().ifPresent(nexusConfig::setEncryptedPassword);
    }
    if (nexusConnectorInput.getUserName().isPresent()) {
      nexusConnectorInput.getUserName().getValue().ifPresent(userName -> nexusConfig.setUsername(userName.trim()));
    }
  }

  private void setNexusVersion(QLNexusConnectorInput nexusConnectorInput, NexusConfig nexusConfig) {
    if (nexusConnectorInput.getVersion().isPresent()) {
      nexusConnectorInput.getVersion().getValue().ifPresent(nexusVersion -> nexusConfig.setVersion(nexusVersion.value));
    }
  }

  private void setDelegateSelectors(QLNexusConnectorInput nexusConnectorInput, NexusConfig nexusConfig) {
    if (nexusConnectorInput.getDelegateSelectors().isPresent()) {
      nexusConnectorInput.getDelegateSelectors().getValue().ifPresent(nexusConfig::setDelegateSelectors);
    }
  }
}
