package io.harness.pms.dashboards;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.OrgProjectIdentifier;

import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(HarnessTeam.PIPELINE)
public class LandingDashboardRequest {
  @NotNull List<OrgProjectIdentifier> orgProjectIdentifiers;
}
