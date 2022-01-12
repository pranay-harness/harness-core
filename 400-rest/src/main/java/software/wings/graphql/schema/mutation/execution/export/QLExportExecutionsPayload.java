/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.schema.mutation.execution.export;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.execution.export.request.ExportExecutionsRequestSummary;

import software.wings.graphql.schema.mutation.QLMutationPayload;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@OwnedBy(CDC)
@Value
@Builder
@FieldNameConstants(innerTypeName = "QLExportExecutionsPayloadKeys")
@Scope(ResourceType.APPLICATION)
@TargetModule(HarnessModule._380_CG_GRAPHQL)
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
