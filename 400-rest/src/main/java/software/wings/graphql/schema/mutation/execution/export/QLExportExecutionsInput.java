package software.wings.graphql.schema.mutation.execution.export;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.CreatedByType;
import io.harness.execution.export.request.ExportExecutionsRequest.OutputFormat;
import io.harness.execution.export.request.ExportExecutionsUserParams;

import software.wings.graphql.datafetcher.execution.QLBaseExecutionFilter;
import software.wings.graphql.schema.mutation.QLMutationInput;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import java.util.List;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@OwnedBy(CDC)
@Value
@Builder
@FieldNameConstants(innerTypeName = "QLExportExecutionsInputKeys")
@Scope(PermissionAttribute.ResourceType.SETTING)
public class QLExportExecutionsInput implements QLMutationInput {
  String clientMutationId;
  Boolean notifyOnlyTriggeringUser;
  List<String> userGroupIds;
  List<QLBaseExecutionFilter> filters;

  public static ExportExecutionsUserParams toUserParams(QLExportExecutionsInput input) {
    return ExportExecutionsUserParams.builder()
        .outputFormat(OutputFormat.JSON)
        .notifyOnlyTriggeringUser(input.getNotifyOnlyTriggeringUser() != null && input.getNotifyOnlyTriggeringUser())
        .userGroupIds(input.getUserGroupIds())
        .createdByType(CreatedByType.API_KEY)
        .build();
  }
}
