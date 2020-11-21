package software.wings.graphql.schema.mutation.connector.payload;

import software.wings.graphql.schema.mutation.QLMutationPayload;
import software.wings.graphql.schema.type.connector.QLConnector;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@Scope(PermissionAttribute.ResourceType.SETTING)
public class QLUpdateConnectorPayload implements QLMutationPayload {
  String clientMutationId;

  QLConnector connector;
}
