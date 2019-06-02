package software.wings.graphql.datafetcher.connector;

import software.wings.beans.SettingAttribute;
import software.wings.graphql.datafetcher.user.UserController;
import software.wings.graphql.scalar.GraphQLDateTimeScalar;
import software.wings.graphql.schema.type.connector.QLConnectorBuilder;
import software.wings.graphql.schema.type.connector.QLJiraConnector;
import software.wings.graphql.schema.type.connector.QLJiraConnector.QLJiraConnectorBuilder;
import software.wings.graphql.schema.type.connector.QLServiceNowConnector;
import software.wings.graphql.schema.type.connector.QLServiceNowConnector.QLServiceNowConnectorBuilder;
import software.wings.graphql.schema.type.connector.QLSlackConnector;
import software.wings.graphql.schema.type.connector.QLSlackConnector.QLSlackConnectorBuilder;
import software.wings.graphql.schema.type.connector.QLSmtpConnector;
import software.wings.graphql.schema.type.connector.QLSmtpConnector.QLSmtpConnectorBuilder;

public class ConnectorsController {
  public static void populateConnector(SettingAttribute settingAttribute, QLConnectorBuilder builder) {
    builder.id(settingAttribute.getUuid())
        .name(settingAttribute.getName())
        .createdAt(GraphQLDateTimeScalar.convert(settingAttribute.getCreatedAt()))
        .createdBy(UserController.populateUser(settingAttribute.getCreatedBy()));
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
