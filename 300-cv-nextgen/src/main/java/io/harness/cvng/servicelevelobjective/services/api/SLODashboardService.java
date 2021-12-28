package io.harness.cvng.servicelevelobjective.services.api;

import io.harness.cvng.core.beans.params.PageParams;
import io.harness.cvng.core.beans.params.ProjectParams;
import io.harness.cvng.servicelevelobjective.SLORiskCountResponse;
import io.harness.cvng.servicelevelobjective.beans.SLODashboardApiFilter;
import io.harness.cvng.servicelevelobjective.beans.SLODashboardWidget;
import io.harness.ng.beans.PageResponse;

public interface SLODashboardService {
  PageResponse<SLODashboardWidget> getSloDashboardWidgets(
      ProjectParams projectParams, SLODashboardApiFilter filter, PageParams pageParams);

  SLORiskCountResponse getRiskCount(ProjectParams projectParams, SLODashboardApiFilter serviceLevelObjectiveFilter);
}
