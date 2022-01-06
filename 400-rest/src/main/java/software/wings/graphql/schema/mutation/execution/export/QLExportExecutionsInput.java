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
@TargetModule(HarnessModule._380_CG_GRAPHQL)
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
