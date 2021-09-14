package io.harness.dashboard.dtos;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(HarnessTeam.PL)
public class MostActiveServicesList {
  List<ServiceDashboardInfo> activeServices;
}
