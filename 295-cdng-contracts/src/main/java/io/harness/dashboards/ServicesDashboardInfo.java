package io.harness.dashboards;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;

import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@OwnedBy(PIPELINE)
@Value
@Builder
public class ServicesDashboardInfo {
  @Builder.Default List<ServiceDashboardInfo> serviceDashboardInfoList = new ArrayList<>();
}
