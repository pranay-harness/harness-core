package software.wings.graphql.datafetcher.secretManager;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.inject.Inject;

import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.persistence.HIterator;
import io.harness.persistence.HPersistence;
import software.wings.beans.SecretManagerConfig;
import software.wings.beans.SettingAttribute.SettingAttributeKeys;
import software.wings.graphql.datafetcher.AbstractObjectDataFetcher;
import software.wings.graphql.schema.query.QLSecretManagerQueryParameters;
import software.wings.graphql.schema.type.secretManagers.QLSecretManager;
import software.wings.graphql.schema.type.secretManagers.QLSecretManager.QLSecretManagerBuilder;
import software.wings.graphql.schema.type.secretManagers.QLSecretManager.QLSecretManagerKeys;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;

public class SecretManagerDataFetcher
    extends AbstractObjectDataFetcher<QLSecretManager, QLSecretManagerQueryParameters> {
  @Inject HPersistence persistence;
  public static final String SECURITY_MANAGER_DOES_NOT_EXIST_MSG = "Secret Manager does not exist";

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  protected QLSecretManager fetch(QLSecretManagerQueryParameters qlQuery, String accountId) {
    SecretManagerConfig secretManager = null;

    if (isNotBlank(qlQuery.getSecretManagerId())) {
      secretManager = getById(qlQuery.getSecretManagerId().trim(), accountId);
    }

    if (isNotBlank(qlQuery.getName())) {
      secretManager = getByName(qlQuery.getName().trim(), accountId);
    }

    if (secretManager == null) {
      throw new InvalidRequestException(SECURITY_MANAGER_DOES_NOT_EXIST_MSG, WingsException.USER);
    }

    final QLSecretManagerBuilder builder = QLSecretManager.builder();
    SecretManagerController.populateSecretManager(secretManager, builder);
    return builder.build();
  }

  private SecretManagerConfig getByName(String name, String accountId) {
    SecretManagerConfig secretManager = null;
    try (
        HIterator<SecretManagerConfig> iterator = new HIterator<>(persistence.createQuery(SecretManagerConfig.class)
                                                                      .disableValidation()
                                                                      .filter(QLSecretManagerKeys.name, name)
                                                                      .filter(SettingAttributeKeys.accountId, accountId)
                                                                      .fetch())) {
      if (iterator.hasNext()) {
        secretManager = iterator.next();
      }
    }
    return secretManager;
  }

  private SecretManagerConfig getById(String id, String accountId) {
    SecretManagerConfig secretManagerConfig = persistence.get(SecretManagerConfig.class, id);
    if (!secretManagerConfig.getAccountId().equals(accountId)) {
      return null;
    }
    return secretManagerConfig;
  }
}
