package software.wings.graphql.schema.mutation.execution.payload;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import software.wings.graphql.schema.mutation.QLMutationPayload;
import software.wings.graphql.schema.type.QLExecution;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

@OwnedBy(CDC)
@Value
@Builder
@FieldNameConstants(innerTypeName = "QLTriggerExecutionPayloadKeys")
@Scope(PermissionAttribute.ResourceType.DEPLOYMENT)
public class QLStartExecutionPayload implements QLMutationPayload {
  String clientMutationId;
  QLExecution execution;
  String warningMessage;
}
