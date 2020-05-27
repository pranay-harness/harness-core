package io.harness.execution.export.request;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.CreatedByType;
import io.harness.execution.export.request.ExportExecutionsRequest.OutputFormat;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@OwnedBy(CDC)
@Data
@Builder
public class ExportExecutionsUserParams {
  @Builder.Default private OutputFormat outputFormat = OutputFormat.JSON;
  private boolean notifyOnlyTriggeringUser;
  private List<String> userGroupIds;
  private CreatedByType createdByType;
}
