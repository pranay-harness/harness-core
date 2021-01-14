package io.harness.cvng.activity.source.services.api;

import io.harness.cvng.activity.entities.ActivitySource;
import io.harness.cvng.beans.activity.ActivitySourceDTO;
import io.harness.cvng.core.services.api.DeleteEntityByProjectHandler;
import io.harness.ng.beans.PageResponse;

import javax.validation.constraints.NotNull;

public interface ActivitySourceService extends DeleteEntityByProjectHandler<ActivitySource> {
  String saveActivitySource(
      String accountId, String orgIdentifier, String projectIdentifier, ActivitySourceDTO activitySourceDTO);

  ActivitySource getActivitySource(@NotNull String activitySourceId);

  ActivitySourceDTO getActivitySource(
      String accountId, String orgIdentifier, String projectIdentifier, String identifier);

  PageResponse<ActivitySourceDTO> listActivitySources(
      String accountId, String orgIdentifier, String projectIdentifier, int offset, int pageSize, String filter);

  boolean deleteActivitySource(String accountId, String orgIdentifier, String projectIdentifier, String identifier);
}
