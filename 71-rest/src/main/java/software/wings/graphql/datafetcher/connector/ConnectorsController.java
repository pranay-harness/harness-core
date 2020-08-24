package software.wings.graphql.datafetcher.connector;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import software.wings.beans.GitConfig;
import software.wings.beans.SettingAttribute;
import software.wings.graphql.datafetcher.user.UserController;
import software.wings.graphql.schema.type.QLConnectorType;
import software.wings.graphql.schema.type.QLCustomCommitDetails;
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
import software.wings.graphql.schema.type.connector.QLGitConnector.QLGitConnectorBuilder;
import software.wings.graphql.schema.type.connector.QLHttpHelmRepoConnector;
import software.wings.graphql.schema.type.connector.QLInstanaConnector;
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
import software.wings.helpers.ext.url.SubdomainUrlHelper;
import software.wings.settings.SettingVariableTypes;

@Singleton
public class ConnectorsController {
  public static final String WEBHOOK_URL_PATH = "api/setup-as-code/yaml/webhook/";
  @Inject private SubdomainUrlHelper subdomainUrlHelper;
  public QLConnectorBuilder populateConnector(SettingAttribute settingAttribute, QLConnectorBuilder builder) {
    return builder.id(settingAttribute.getUuid())
        .name(settingAttribute.getName())
        .createdAt(settingAttribute.getCreatedAt())
        .createdBy(UserController.populateUser(settingAttribute.getCreatedBy()));
  }

  public QLConnectorBuilder getConnectorBuilder(SettingAttribute settingAttribute) {
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
      case INSTANA:
        return QLInstanaConnector.builder();
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
        return getPrePopulatedGitConnectorBuilder(settingAttribute);
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

  public QLGitConnectorBuilder getPrePopulatedGitConnectorBuilder(SettingAttribute settingAttribute) {
    QLGitConnectorBuilder builder = QLGitConnector.builder();
    GitConfig gitConfig = (GitConfig) settingAttribute.getValue();
    builder.userName(gitConfig.getUsername())
        .URL(gitConfig.getRepoUrl())
        .urlType(gitConfig.getUrlType())
        .branch(gitConfig.getBranch())
        .sshSettingId(gitConfig.getSshSettingId())
        .webhookUrl(generateWebhookUrl(gitConfig.getWebhookToken(), settingAttribute.getAccountId()))
        .generateWebhookUrl(gitConfig.isGenerateWebhookUrl())
        .customCommitDetails(QLCustomCommitDetails.builder()
                                 .authorName(gitConfig.getAuthorName())
                                 .authorEmailId(gitConfig.getAuthorEmailId())
                                 .commitMessage(gitConfig.getCommitMessage())
                                 .build());
    if (null != gitConfig.getEncryptedPassword()) {
      builder.passwordSecretId(gitConfig.getEncryptedPassword());
    } else if (null != gitConfig.getPassword()) {
      builder.passwordSecretId(String.copyValueOf(gitConfig.getPassword()));
    }
    return builder;
  }

  private String generateWebhookUrl(String webHookToken, String accountId) {
    if (null != webHookToken) {
      StringBuilder webhookURL = new StringBuilder(subdomainUrlHelper.getApiBaseUrl(accountId));
      webhookURL.append(WEBHOOK_URL_PATH).append(webHookToken).append("?accountId=").append(accountId);
      return webhookURL.toString();
    }
    return null;
  }

  public void checkIfInputIsNotPresent(QLConnectorType type, Object input) {
    if (input == null) {
      throw new InvalidRequestException(
          String.format("No input provided with the request for %s connector", type.getStringValue()));
    }
  }
}
