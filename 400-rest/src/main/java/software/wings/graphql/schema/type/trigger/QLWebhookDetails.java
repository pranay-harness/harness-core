package software.wings.graphql.schema.type.trigger;

import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

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
