package io.harness.event;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@OwnedBy(HarnessTeam.PIPELINE)
@Builder
@Data
public class OverviewResponse {
  @NotNull String serviceName;
  @NotNull int totalQueriesAnalysed;
  @NotNull int flaggedQueriesCount;
}
