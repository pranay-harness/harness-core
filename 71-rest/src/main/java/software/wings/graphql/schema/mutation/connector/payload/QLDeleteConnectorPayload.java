package software.wings.graphql.schema.mutation.connector.payload;

import lombok.Builder;
import lombok.Value;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

@Value
@Builder
@Scope(PermissionAttribute.ResourceType.SETTING)
public class QLDeleteConnectorPayload {
  String clientMutationId;
}
