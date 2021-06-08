package io.harness.event;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class OverviewResponse {
  @NotNull String serviceName;
  @NotNull int totalQueriesAnalysed;
  @NotNull int flaggedQueriesCount;
}
