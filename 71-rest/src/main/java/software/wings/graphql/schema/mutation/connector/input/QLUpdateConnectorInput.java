package software.wings.graphql.schema.mutation.connector.input;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.mutation.QLMutationInput;
import software.wings.graphql.schema.mutation.connector.input.docker.QLDockerConnectorInput;
import software.wings.graphql.schema.mutation.connector.input.git.QLUpdateGitConnectorInput;
import software.wings.graphql.schema.mutation.connector.input.helm.QLHelmConnectorInput;
import software.wings.graphql.schema.mutation.connector.input.nexus.QLNexusConnectorInput;
import software.wings.graphql.schema.type.QLConnectorType;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

@Value
@Builder
@Scope(PermissionAttribute.ResourceType.SETTING)
@JsonIgnoreProperties(ignoreUnknown = true)
public class QLUpdateConnectorInput implements QLMutationInput {
  private String clientMutationId;
  private String connectorId;
  private QLConnectorType connectorType;
  private QLUpdateGitConnectorInput gitConnector;
  private QLDockerConnectorInput dockerConnector;
  private QLNexusConnectorInput nexusConnector;
  private QLHelmConnectorInput helmConnector;
}
