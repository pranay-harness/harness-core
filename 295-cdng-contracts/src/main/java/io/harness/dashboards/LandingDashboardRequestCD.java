package io.harness.dashboards;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.OrgProjectIdentifier;
import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@Builder
@OwnedBy(HarnessTeam.PIPELINE)
public class LandingDashboardRequestCD {
    @NotNull List<OrgProjectIdentifier> orgProjectIdentifiers;
}
