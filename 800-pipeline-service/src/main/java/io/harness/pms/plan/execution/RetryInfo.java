package io.harness.pms.plan.execution;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@OwnedBy(HarnessTeam.PIPELINE)
@Data
@Builder
public class RetryInfo {
  private boolean isResumable;
  private String errorMessage;
  private List<RetryGroup> groups;
}
