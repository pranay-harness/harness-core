package io.harness.engine.executions.retry;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@OwnedBy(HarnessTeam.PIPELINE)
@Data
@Builder
@Schema(name = "RetryInfo",
    description = "This is the view of the Stages from where the User can resume a Failed Pipeline.")
public class RetryInfo {
  private boolean isResumable;
  private String errorMessage;
  private List<RetryGroup> groups;
}
