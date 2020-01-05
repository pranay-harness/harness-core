package software.wings.graphql.datafetcher.connector;

import io.harness.exception.WingsException;
import lombok.experimental.UtilityClass;
import software.wings.beans.SettingAttribute;
import software.wings.graphql.datafetcher.user.UserController;
import software.wings.graphql.schema.type.connector.QLAmazonS3RepoConnector;
import software.wings.graphql.schema.type.connector.QLApmVerificationConnector;
import software.wings.graphql.schema.type.connector.QLAppDynamicsConnector;
import software.wings.graphql.schema.type.connector.QLArtifactoryConnector;
import software.wings.graphql.schema.type.connector.QLBambooConnector;
import software.wings.graphql.schema.type.connector.QLBugSnagConnector;
import software.wings.graphql.schema.type.connector.QLConnectorBuilder;
import software.wings.graphql.schema.type.connector.QLCustomConnector;
import software.wings.graphql.schema.type.connector.QLDataDogConnector;
import software.wings.graphql.schema.type.connector.QLDockerConnector;
import software.wings.graphql.schema.type.connector.QLDynaTraceConnector;
import software.wings.graphql.schema.type.connector.QLECRConnector;
import software.wings.graphql.schema.type.connector.QLElbConnector;
import software.wings.graphql.schema.type.connector.QLElkConnector;
import software.wings.graphql.schema.type.connector.QLGCRConnector;
import software.wings.graphql.schema.type.connector.QLGCSConnector;
import software.wings.graphql.schema.type.connector.QLGCSHelmRepoConnector;
import software.wings.graphql.schema.type.connector.QLGitConnector;
import software.wings.graphql.schema.type.connector.QLHttpHelmRepoConnector;
import software.wings.graphql.schema.type.connector.QLJenkinsConnector;
import software.wings.graphql.schema.type.connector.QLJiraConnector;
import software.wings.graphql.schema.type.connector.QLLogzConnector;
import software.wings.graphql.schema.type.connector.QLNewRelicConnector;
import software.wings.graphql.schema.type.connector.QLNexusConnector;
import software.wings.graphql.schema.type.connector.QLPrometheusConnector;
import software.wings.graphql.schema.type.connector.QLServiceNowConnector;
import software.wings.graphql.schema.type.connector.QLSftpConnector;
import software.wings.graphql.schema.type.connector.QLSlackConnector;
import software.wings.graphql.schema.type.connector.QLSmtpConnector;
import software.wings.graphql.schema.type.connector.QLSplunkConnector;
import software.wings.graphql.schema.type.connector.QLSumoConnector;
import software.wings.settings.SettingValue.SettingVariableTypes;

@UtilityClass
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
      case JENKINS:
        return QLJenkinsConnector.builder();
      case BAMBOO:
        return QLBambooConnector.builder();
      case SPLUNK:
        return QLSplunkConnector.builder();
      case ELK:
        return QLElkConnector.builder();
      case LOGZ:
        return QLLogzConnector.builder();
      case SUMO:
        return QLSumoConnector.builder();
      case APP_DYNAMICS:
        return QLAppDynamicsConnector.builder();
      case NEW_RELIC:
        return QLNewRelicConnector.builder();
      case DYNA_TRACE:
        return QLDynaTraceConnector.builder();
      case BUG_SNAG:
        return QLBugSnagConnector.builder();
      case DATA_DOG:
        return QLDataDogConnector.builder();
      case APM_VERIFICATION:
        return QLApmVerificationConnector.builder();
      case PROMETHEUS:
        return QLPrometheusConnector.builder();
      case ELB:
        return QLElbConnector.builder();
      case ECR:
        return QLECRConnector.builder();
      case GCR:
        return QLGCRConnector.builder();
      case NEXUS:
        return QLNexusConnector.builder();
      case ARTIFACTORY:
        return QLArtifactoryConnector.builder();
      case AMAZON_S3:
        return QLAmazonS3RepoConnector.builder();
      case GCS:
        return QLGCSConnector.builder();
      case GIT:
        return QLGitConnector.builder();
      case SMB:
        return QLSmtpConnector.builder();
      case SFTP:
        return QLSftpConnector.builder();
      case HTTP_HELM_REPO:
        return QLHttpHelmRepoConnector.builder();
      case AMAZON_S3_HELM_REPO:
        return QLAmazonS3RepoConnector.builder();
      case GCS_HELM_REPO:
        return QLGCSHelmRepoConnector.builder();
      case CUSTOM:
        return QLCustomConnector.builder();
      default:
        throw new WingsException("Unsupported Connector " + settingType);
    }
  }
}
