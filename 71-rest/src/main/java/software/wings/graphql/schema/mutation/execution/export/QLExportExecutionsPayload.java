package software.wings.graphql.schema.mutation.execution.export;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.execution.export.request.ExportExecutionsRequestSummary;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import software.wings.graphql.schema.mutation.QLMutationPayload;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;

@OwnedBy(CDC)
@Value
@Builder
@FieldNameConstants(innerTypeName = "QLExportExecutionsPayloadKeys")
@Scope(ResourceType.APPLICATION)
public class QLExportExecutionsPayload implements QLMutationPayload {
  String clientMutationId;
  String requestId;
  QLExportExecutionsStatus status;
  Long totalExecutions;
  Long triggeredAt;

  // For status = QUEUED or status = READY.
  String statusLink;
  String downloadLink;
  Long expiresAt;

  // For status = FAILED.
  String errorMessage;

  public static QLExportExecutionsPayload fromExportExecutionsRequestSummary(
      ExportExecutionsRequestSummary summary, QLExportExecutionsInput exportExecutionsInput) {
    return QLExportExecutionsPayload.builder()
        .clientMutationId(exportExecutionsInput.getClientMutationId())
        .requestId(summary.getRequestId())
        .status(QLExportExecutionsStatus.fromStatus(summary.getStatus()))
        .totalExecutions(summary.getTotalExecutions())
        .triggeredAt(summary.getTriggeredAt() == null ? null : summary.getTriggeredAt().toInstant().toEpochMilli())
        .statusLink(summary.getStatusLink())
        .downloadLink(summary.getDownloadLink())
        .expiresAt(summary.getExpiresAt() == null ? null : summary.getExpiresAt().toInstant().toEpochMilli())
        .errorMessage(summary.getErrorMessage())
        .build();
  }
}
