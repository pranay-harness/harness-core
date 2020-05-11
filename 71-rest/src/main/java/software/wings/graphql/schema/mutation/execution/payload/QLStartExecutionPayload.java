package software.wings.graphql.schema.mutation.execution.payload;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import software.wings.graphql.schema.mutation.QLMutationPayload;
import software.wings.graphql.schema.type.QLExecution;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLTriggerExecutionPayloadKeys")
@Scope(PermissionAttribute.ResourceType.DEPLOYMENT)
public class QLStartExecutionPayload implements QLMutationPayload {
  String clientMutationId;
  QLExecution execution;
  String warningMessage;
}
