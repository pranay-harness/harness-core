package io.harness.cvng.servicelevelobjective.services.api;

import io.harness.cvng.core.beans.params.PageParams;
import io.harness.cvng.core.beans.params.ProjectParams;
import io.harness.cvng.servicelevelobjective.beans.SLODashboardApiFilter;
import io.harness.cvng.servicelevelobjective.beans.ServiceLevelObjectiveDTO;
import io.harness.cvng.servicelevelobjective.beans.ServiceLevelObjectiveFilter;
import io.harness.cvng.servicelevelobjective.beans.ServiceLevelObjectiveResponse;
import io.harness.cvng.servicelevelobjective.entities.ServiceLevelObjective;
import io.harness.ng.beans.PageResponse;

public interface ServiceLevelObjectiveService {
  ServiceLevelObjectiveResponse create(ProjectParams projectParams, ServiceLevelObjectiveDTO serviceLevelObjectiveDTO);

  ServiceLevelObjectiveResponse update(
      ProjectParams projectParams, String identifier, ServiceLevelObjectiveDTO serviceLevelObjectiveDTO);

  boolean delete(ProjectParams accountId, String identifier);

  PageResponse<ServiceLevelObjectiveResponse> get(ProjectParams projectParams, Integer offset, Integer pageSize,
      ServiceLevelObjectiveFilter serviceLevelObjectiveFilter);

  ServiceLevelObjectiveResponse get(ProjectParams projectParams, String identifier);
  ServiceLevelObjective getEntity(ProjectParams projectParams, String identifier);

  PageResponse<ServiceLevelObjectiveResponse> getSLOForDashboard(
      ProjectParams projectParams, SLODashboardApiFilter filter, PageParams pageParams);
}
