package software.wings.graphql.datafetcher.connector;

import io.harness.exception.WingsException;
import software.wings.beans.SettingAttribute;
import software.wings.graphql.datafetcher.user.UserController;
import software.wings.graphql.schema.type.connector.QLConnectorBuilder;
import software.wings.graphql.schema.type.connector.QLDockerConnector;
import software.wings.graphql.schema.type.connector.QLJiraConnector;
import software.wings.graphql.schema.type.connector.QLJiraConnector.QLJiraConnectorBuilder;
import software.wings.graphql.schema.type.connector.QLServiceNowConnector;
import software.wings.graphql.schema.type.connector.QLServiceNowConnector.QLServiceNowConnectorBuilder;
import software.wings.graphql.schema.type.connector.QLSlackConnector;
import software.wings.graphql.schema.type.connector.QLSlackConnector.QLSlackConnectorBuilder;
import software.wings.graphql.schema.type.connector.QLSmtpConnector;
import software.wings.graphql.schema.type.connector.QLSmtpConnector.QLSmtpConnectorBuilder;
import software.wings.settings.SettingValue.SettingVariableTypes;

public class ConnectorsController {
  public static QLConnectorBuilder populateConnector(SettingAttribute settingAttribute, QLConnectorBuilder builder) {
    return builder.id(settingAttribute.getUuid())
        .name(settingAttribute.getName())
        .createdAt(settingAttribute.getCreatedAt())
        .createdBy(UserController.populateUser(settingAttribute.getCreatedBy()));
  }

  public static QLConnectorBuilder getConnectorBuilder(SettingAttribute settingAttribute) {
    QLConnectorBuilder builder;
    final SettingVariableTypes settingType = settingAttribute.getValue().getSettingType();
    switch (settingType) {
      case JIRA:
        return QLJiraConnector.builder();
      case SERVICENOW:
        return QLServiceNowConnector.builder();
      case SMTP:
        return QLSmtpConnector.builder();
      case SLACK:
        return QLSlackConnector.builder();
      case DOCKER:
        return QLDockerConnector.builder();
      default:
        throw new WingsException("Unsupported Connector " + settingType);
    }
  }

  public static QLJiraConnector prepareJiraConfig(SettingAttribute settingAttribute) {
    QLJiraConnectorBuilder qlJiraConnectorBuilder = QLJiraConnector.builder();
    ConnectorsController.populateConnector(settingAttribute, qlJiraConnectorBuilder);

    return qlJiraConnectorBuilder.build();
  }
  public static QLServiceNowConnector prepareServiceNowConfig(SettingAttribute settingAttribute) {
    QLServiceNowConnectorBuilder qlServiceNowConnectorBuilder = QLServiceNowConnector.builder();
    ConnectorsController.populateConnector(settingAttribute, qlServiceNowConnectorBuilder);

    return qlServiceNowConnectorBuilder.build();
  }
  public static QLSmtpConnector prepareSmtpConfig(SettingAttribute settingAttribute) {
    QLSmtpConnectorBuilder qlSmtpConnectorBuilder = QLSmtpConnector.builder();
    ConnectorsController.populateConnector(settingAttribute, qlSmtpConnectorBuilder);

    return qlSmtpConnectorBuilder.build();
  }
  public static QLSlackConnector prepareSlackConfig(SettingAttribute settingAttribute) {
    QLSlackConnectorBuilder qlSlackConnectorBuilder = QLSlackConnector.builder();
    ConnectorsController.populateConnector(settingAttribute, qlSlackConnectorBuilder);

    return qlSlackConnectorBuilder.build();
  }
}
