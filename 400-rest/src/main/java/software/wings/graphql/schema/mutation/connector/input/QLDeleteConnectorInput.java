package software.wings.graphql.schema.mutation.connector.input;

import software.wings.graphql.schema.mutation.QLMutationInput;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@Scope(PermissionAttribute.ResourceType.SETTING)
@JsonIgnoreProperties(ignoreUnknown = true)
public class QLDeleteConnectorInput implements QLMutationInput {
  String clientMutationId;

  String connectorId;
}
