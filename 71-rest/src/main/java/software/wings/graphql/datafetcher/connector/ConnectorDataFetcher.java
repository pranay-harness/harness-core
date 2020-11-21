package software.wings.graphql.datafetcher.connector;

import static software.wings.security.PermissionAttribute.PermissionType.LOGGED_IN;

import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.persistence.HPersistence;

import software.wings.beans.SettingAttribute;
import software.wings.graphql.datafetcher.AbstractObjectDataFetcher;
import software.wings.graphql.schema.query.QLConnectorQueryParameters;
import software.wings.graphql.schema.type.connector.QLConnector;
import software.wings.security.annotations.AuthRule;

import com.google.inject.Inject;

public class ConnectorDataFetcher extends AbstractObjectDataFetcher<QLConnector, QLConnectorQueryParameters> {
  @Inject HPersistence persistence;
  @Inject ConnectorsController connectorsController;

  @Override
  @AuthRule(permissionType = LOGGED_IN)
  protected QLConnector fetch(QLConnectorQueryParameters qlQuery, String accountId) {
    SettingAttribute settingAttribute = persistence.get(SettingAttribute.class, qlQuery.getConnectorId());
    if (settingAttribute == null) {
      throw new InvalidRequestException("Connector does not exist", WingsException.USER);
    }

    if (!settingAttribute.getAccountId().equals(accountId)) {
      throw new InvalidRequestException("Connector does not exist", WingsException.USER);
    }

    return connectorsController
        .populateConnector(settingAttribute, connectorsController.getConnectorBuilder(settingAttribute))
        .build();
  }
}
