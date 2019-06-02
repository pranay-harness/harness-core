package software.wings.graphql.datafetcher.connector;

import com.google.inject.Inject;

import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.persistence.HPersistence;
import software.wings.beans.JiraConfig;
import software.wings.beans.ServiceNowConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SlackConfig;
import software.wings.graphql.datafetcher.AbstractDataFetcher;
import software.wings.graphql.schema.query.QLConnectorQueryParameters;
import software.wings.graphql.schema.type.connector.QLConnector;
import software.wings.helpers.ext.mail.SmtpConfig;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.annotations.AuthRule;
import software.wings.settings.SettingValue;

public class ConnectorDataFetcher extends AbstractDataFetcher<QLConnector, QLConnectorQueryParameters> {
  @Inject HPersistence persistence;

  @Override
  @AuthRule(permissionType = PermissionType.LOGGED_IN)
  protected QLConnector fetch(QLConnectorQueryParameters qlQuery) {
    SettingAttribute settingAttribute = persistence.get(SettingAttribute.class, qlQuery.getConnectorId());
    if (settingAttribute == null) {
      throw new InvalidRequestException("Connector does not exist", WingsException.USER);
    }

    SettingValue settingValue = settingAttribute.getValue();
    if (settingValue instanceof JiraConfig) {
      return ConnectorsController.prepareJiraConfig(settingAttribute);
    }

    if (settingValue instanceof SlackConfig) {
      return ConnectorsController.prepareSlackConfig(settingAttribute);
    }

    if (settingValue instanceof SmtpConfig) {
      return ConnectorsController.prepareSmtpConfig(settingAttribute);
    }

    if (settingValue instanceof ServiceNowConfig) {
      return ConnectorsController.prepareServiceNowConfig(settingAttribute);
    }

    throw new WingsException("Invalid Connector Type");
  }
}
