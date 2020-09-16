package software.wings.graphql.datafetcher.connector.types;

import io.harness.exception.InvalidRequestException;
import software.wings.graphql.datafetcher.connector.ConnectorsController;
import software.wings.graphql.schema.mutation.connector.input.QLConnectorInput;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;

public class ConnectorFactory {
  private ConnectorFactory() {
    throw new IllegalStateException("Utility class");
  }

  public static Connector getConnector(QLConnectorInput input, ConnectorsController connectorsController,
      SecretManager secretManager, SettingsService settingsService) {
    switch (input.getConnectorType()) {
      case GIT:
        return new GitConnector(secretManager, settingsService, connectorsController);
      case DOCKER:
        return new DockerConnector(secretManager, connectorsController);
      default:
        throw new InvalidRequestException("Invalid connector Type");
    }
  }
}
