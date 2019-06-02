package software.wings.graphql.datafetcher.connector;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.inject.Inject;

import software.wings.beans.JiraConfig;
import software.wings.beans.ServiceNowConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingAttributeKeys;
import software.wings.beans.SettingAttribute.SettingCategory;
import software.wings.beans.SlackConfig;
import software.wings.graphql.datafetcher.AbstractConnectionDataFetcher;
import software.wings.graphql.schema.query.QLConnectorsQueryParameters;
import software.wings.graphql.schema.type.QLPageInfo;
import software.wings.graphql.schema.type.QLPageInfo.QLPageInfoBuilder;
import software.wings.graphql.schema.type.connector.QLConnectorsConnection;
import software.wings.graphql.schema.type.connector.QLConnectorsConnection.QLConnectorsConnectionBuilder;
import software.wings.helpers.ext.mail.SmtpConfig;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.SettingsService;
import software.wings.settings.SettingValue;

import java.util.List;

public class ConnectorConnectionDataFetcher
    extends AbstractConnectionDataFetcher<QLConnectorsConnection, QLConnectorsQueryParameters> {
  @Inject private SettingsService settingsService;

  @Override
  @AuthRule(permissionType = PermissionType.LOGGED_IN)
  protected QLConnectorsConnection fetchConnection(QLConnectorsQueryParameters parameters) {
    final List<SettingAttribute> settingAttributes =
        persistence.createAuthorizedQuery(SettingAttribute.class)
            .filter(SettingAttributeKeys.accountId, parameters.getAccountId())
            .filter(SettingAttributeKeys.category, SettingCategory.CONNECTOR)
            .asList();

    final List<SettingAttribute> filteredSettingAttributes =
        settingsService.getFilteredSettingAttributes(settingAttributes, null, null);

    QLConnectorsConnectionBuilder qlConnectorsConnectionBuilder = QLConnectorsConnection.builder();

    QLPageInfoBuilder pageInfoBuilder = QLPageInfo.builder().hasMore(false).offset(0).limit(0).total(0);

    if (isNotEmpty(filteredSettingAttributes)) {
      pageInfoBuilder.total(filteredSettingAttributes.size()).limit(filteredSettingAttributes.size());

      for (SettingAttribute settingAttribute : filteredSettingAttributes) {
        SettingValue settingValue = settingAttribute.getValue();
        if (settingValue instanceof JiraConfig) {
          qlConnectorsConnectionBuilder.node(ConnectorsController.prepareJiraConfig(settingAttribute));
        } else if (settingValue instanceof SlackConfig) {
          qlConnectorsConnectionBuilder.node(ConnectorsController.prepareSlackConfig(settingAttribute));
        } else if (settingValue instanceof ServiceNowConfig) {
          qlConnectorsConnectionBuilder.node(ConnectorsController.prepareServiceNowConfig(settingAttribute));
        } else if (settingValue instanceof SmtpConfig) {
          qlConnectorsConnectionBuilder.node(ConnectorsController.prepareSmtpConfig(settingAttribute));
        }
      }
    }

    return qlConnectorsConnectionBuilder.build();
  }
}
