package software.wings.graphql.datafetcher.connector.types;

import io.harness.exception.InvalidRequestException;

import software.wings.graphql.datafetcher.connector.ConnectorsController;
import software.wings.graphql.schema.type.QLConnectorType;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;

public class ConnectorFactory {
  private ConnectorFactory() {
    throw new IllegalStateException("Utility class");
  }

  public static Connector getConnector(QLConnectorType qlConnectorType, ConnectorsController connectorsController,
      SecretManager secretManager, SettingsService settingsService) {
    switch (qlConnectorType) {
      case GIT:
        return new GitConnector(secretManager, settingsService, connectorsController);
      case DOCKER:
        return new DockerConnector(secretManager, connectorsController);
      case NEXUS:
        return new NexusConnector(secretManager, connectorsController);
      case AMAZON_S3_HELM_REPO:
      case GCS_HELM_REPO:
      case HTTP_HELM_REPO:
        return new HelmConnector(secretManager, connectorsController, settingsService);
      default:
        throw new InvalidRequestException("Invalid connector Type");
    }
  }
}
