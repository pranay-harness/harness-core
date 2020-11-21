package software.wings.graphql.datafetcher.connector.types;

import io.harness.exception.InvalidRequestException;
import io.harness.utils.RequestField;

import software.wings.beans.DockerConfig;
import software.wings.beans.SettingAttribute;
import software.wings.graphql.datafetcher.connector.ConnectorsController;
import software.wings.graphql.schema.mutation.connector.input.QLConnectorInput;
import software.wings.graphql.schema.mutation.connector.input.QLUpdateConnectorInput;
import software.wings.graphql.schema.mutation.connector.input.docker.QLDockerConnectorInput;
import software.wings.service.intfc.security.SecretManager;

import java.util.Optional;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@AllArgsConstructor
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

  private void handleSecrets(RequestField<String> passwordSecretId, DockerConfig dockerConfig) {
    if (passwordSecretId.isPresent()) {
      passwordSecretId.getValue().ifPresent(dockerConfig::setEncryptedPassword);
    }
  }
}
