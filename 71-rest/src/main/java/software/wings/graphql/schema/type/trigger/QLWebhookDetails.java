package software.wings.graphql.schema.type.trigger;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLWebhookDetailsKeys")
@Scope(PermissionAttribute.ResourceType.APPLICATION)
public class QLWebhookDetails {
  String webhookURL;
  String method;
  String header;
  String payload;
}
