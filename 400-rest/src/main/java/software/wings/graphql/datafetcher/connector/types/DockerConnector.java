/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.connector.types;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.exception.InvalidRequestException;
import io.harness.utils.RequestField;

import software.wings.beans.DockerConfig;
import software.wings.beans.SettingAttribute;
import software.wings.graphql.datafetcher.connector.ConnectorsController;
import software.wings.graphql.schema.mutation.connector.input.QLConnectorInput;
import software.wings.graphql.schema.mutation.connector.input.QLUpdateConnectorInput;
import software.wings.graphql.schema.mutation.connector.input.docker.QLDockerConnectorInput;
import software.wings.service.intfc.security.SecretManager;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@AllArgsConstructor
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class DockerConnector extends Connector {
  private SecretManager secretManager;
  private ConnectorsController connectorsController;

  @Override
  public SettingAttribute getSettingAttribute(QLConnectorInput input, String accountId) {
    QLDockerConnectorInput dockerConnectorInput = input.getDockerConnector();
    DockerConfig dockerConfig = new DockerConfig();

    dockerConfig.setAccountId(accountId);
    handleSecrets(dockerConnectorInput.getPasswordSecretId(), dockerConfig);

    if (dockerConnectorInput.getUserName().isPresent()) {
      dockerConnectorInput.getUserName().getValue().ifPresent(userName -> dockerConfig.setUsername(userName.trim()));
    }

    handleDelegateSelectors(dockerConnectorInput.getDelegateSelectors(), dockerConfig);

    checkUrlExists(dockerConnectorInput, dockerConfig);

    SettingAttribute.Builder settingAttributeBuilder = SettingAttribute.Builder.aSettingAttribute()
                                                           .withValue(dockerConfig)
                                                           .withAccountId(accountId)
                                                           .withCategory(SettingAttribute.SettingCategory.SETTING);

    if (dockerConnectorInput.getName().isPresent()) {
      dockerConnectorInput.getName().getValue().ifPresent(name -> settingAttributeBuilder.withName(name.trim()));
    }

    return settingAttributeBuilder.build();
  }

  @Override
  public void updateSettingAttribute(SettingAttribute settingAttribute, QLUpdateConnectorInput input) {
    QLDockerConnectorInput dockerConnectorInput = input.getDockerConnector();
    DockerConfig dockerConfig = (DockerConfig) settingAttribute.getValue();

    handleSecrets(dockerConnectorInput.getPasswordSecretId(), dockerConfig);

    if (dockerConnectorInput.getUserName().isPresent()) {
      dockerConnectorInput.getUserName().getValue().ifPresent(userName -> dockerConfig.setUsername(userName.trim()));
    }

    handleDelegateSelectors(dockerConnectorInput.getDelegateSelectors(), dockerConfig);

    checkUrlExists(dockerConnectorInput, dockerConfig);

    settingAttribute.setValue(dockerConfig);

    if (dockerConnectorInput.getName().isPresent()) {
      dockerConnectorInput.getName().getValue().ifPresent(name -> settingAttribute.setName(name.trim()));
    }
  }

  @Override
  public void checkSecrets(QLConnectorInput input, String accountId) {
    checkDockerConnectorSecrets(input.getDockerConnector(), accountId);
  }

  private void checkDockerConnectorSecrets(QLDockerConnectorInput dockerConnectorInput, String accountId) {
    checkUserNameExists(dockerConnectorInput);

    dockerConnectorInput.getPasswordSecretId().getValue().ifPresent(
        secretId -> checkSecretExists(secretManager, accountId, secretId));
  }

  @Override
  public void checkSecrets(QLUpdateConnectorInput input, SettingAttribute settingAttribute) {
    checkDockerConnectorSecrets(input.getDockerConnector(), settingAttribute.getAccountId());
  }

  @Override
  public void checkInputExists(QLConnectorInput input) {
    connectorsController.checkInputExists(input.getConnectorType(), input.getDockerConnector());
  }

  @Override
  public void checkInputExists(QLUpdateConnectorInput input) {
    connectorsController.checkInputExists(input.getConnectorType(), input.getDockerConnector());
  }

  private void checkUserNameExists(QLDockerConnectorInput dockerConnectorInput) {
    RequestField<String> passwordSecretId = dockerConnectorInput.getPasswordSecretId();
    RequestField<String> userName = dockerConnectorInput.getUserName();
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

  private void checkUrlExists(QLDockerConnectorInput dockerConnectorInput, DockerConfig dockerConfig) {
    if (dockerConnectorInput.getURL().isPresent()) {
      String url;
      Optional<String> urlValue = dockerConnectorInput.getURL().getValue();
      if (urlValue.isPresent() && StringUtils.isNotBlank(urlValue.get())) {
        url = urlValue.get().trim();
      } else {
        throw new InvalidRequestException("URL should be specified");
      }
      dockerConfig.setDockerRegistryUrl(url);
    }
  }

  private void handleDelegateSelectors(RequestField<List<String>> delegateSelectors, DockerConfig dockerConfig) {
    List<String> selectors = null;
    if (delegateSelectors.isPresent()) {
      selectors = delegateSelectors.getValue().orElse(null);
    }
    if (isNotEmpty(selectors)) {
      selectors = selectors.stream().filter(StringUtils::isNotBlank).collect(Collectors.toList());
      dockerConfig.setDelegateSelectors(selectors);
    }
  }

  private void handleSecrets(RequestField<String> passwordSecretId, DockerConfig dockerConfig) {
    if (passwordSecretId.isPresent()) {
      passwordSecretId.getValue().ifPresent(dockerConfig::setEncryptedPassword);
    }
  }
}
