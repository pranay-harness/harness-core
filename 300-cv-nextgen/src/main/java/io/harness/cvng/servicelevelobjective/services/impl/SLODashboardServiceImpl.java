package io.harness.cvng.servicelevelobjective.services.impl;

import io.harness.cvng.core.beans.params.PageParams;
import io.harness.cvng.core.beans.params.ProjectParams;
import io.harness.cvng.servicelevelobjective.beans.SLODashboardApiFilter;
import io.harness.cvng.servicelevelobjective.beans.SLODashboardWidget;
import io.harness.cvng.servicelevelobjective.beans.ServiceLevelObjectiveDTO;
import io.harness.cvng.servicelevelobjective.beans.ServiceLevelObjectiveResponse;
import io.harness.cvng.servicelevelobjective.services.api.SLODashboardService;
import io.harness.cvng.servicelevelobjective.services.api.ServiceLevelObjectiveService;
import io.harness.ng.beans.PageResponse;

import com.google.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

public class SLODashboardServiceImpl implements SLODashboardService {
  @Inject private ServiceLevelObjectiveService serviceLevelObjectiveService;
  @Override
  public PageResponse<SLODashboardWidget> getSloDashboardWidgets(
      ProjectParams projectParams, SLODashboardApiFilter filter, PageParams pageParams) {
    PageResponse<ServiceLevelObjectiveResponse> sloPageResponse =
        serviceLevelObjectiveService.getSLOForDashboard(projectParams, filter, pageParams);
    List<SLODashboardWidget> sloDashboardWidgets = sloPageResponse.getContent()
                                                       .stream()
                                                       .map(sloResponse -> {
                                                         ServiceLevelObjectiveDTO slo =
                                                             sloResponse.getServiceLevelObjectiveDTO();
                                                         return SLODashboardWidget.builder()
                                                             .title(slo.getName())
                                                             .monitoredServiceIdentifier(slo.getMonitoredServiceRef())
                                                             .healthSourceIdentifier(slo.getHealthSourceRef())
                                                             .build();
                                                       })
                                                       .collect(Collectors.toList());
    return PageResponse.<SLODashboardWidget>builder()
        .pageSize(sloPageResponse.getPageSize())
        .pageIndex(sloPageResponse.getPageIndex())
        .totalPages(sloPageResponse.getTotalPages())
        .totalItems(sloPageResponse.getTotalItems())
        .pageItemCount(sloPageResponse.getPageItemCount())
        .content(sloDashboardWidgets)
        .build();
  }
}
