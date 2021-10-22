package io.harness.cvng.servicelevelobjective.services;

import io.harness.cvng.core.beans.params.ProjectParams;
import io.harness.cvng.servicelevelobjective.beans.ServiceLevelObjectiveDTO;
import io.harness.cvng.servicelevelobjective.beans.ServiceLevelObjectiveFilter;
import io.harness.cvng.servicelevelobjective.beans.ServiceLevelObjectiveResponse;
import io.harness.ng.beans.PageResponse;

public interface ServiceLevelObjectiveService {
  ServiceLevelObjectiveResponse create(ProjectParams projectParams, ServiceLevelObjectiveDTO serviceLevelObjectiveDTO);

  ServiceLevelObjectiveResponse update(
      ProjectParams projectParams, String identifier, ServiceLevelObjectiveDTO serviceLevelObjectiveDTO);

  boolean delete(ProjectParams accountId, String identifier);

  PageResponse<ServiceLevelObjectiveResponse> get(ProjectParams projectParams, Integer offset, Integer pageSize,
      ServiceLevelObjectiveFilter serviceLevelObjectiveFilter);
}
