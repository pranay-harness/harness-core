/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.activity.source.services.api;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.activity.entities.ActivitySource;
import io.harness.cvng.beans.activity.ActivitySourceDTO;
import io.harness.cvng.core.services.api.DeleteEntityByHandler;
import io.harness.ng.beans.PageResponse;

import javax.validation.constraints.NotNull;
@OwnedBy(HarnessTeam.CV)
public interface ActivitySourceService extends DeleteEntityByHandler<ActivitySource> {
  String create(String accountId, ActivitySourceDTO activitySourceDTO);

  String update(String accountId, String identifier, ActivitySourceDTO activitySourceDTO);

  ActivitySource getActivitySource(@NotNull String activitySourceId);

  ActivitySourceDTO getActivitySource(
      String accountId, String orgIdentifier, String projectIdentifier, String identifier);

  PageResponse<ActivitySourceDTO> listActivitySources(
      String accountId, String orgIdentifier, String projectIdentifier, int offset, int pageSize, String filter);

  boolean deleteActivitySource(String accountId, String orgIdentifier, String projectIdentifier, String identifier);

  void createDefaultCDNGActivitySource(String accountIdentifier, String orgIdentifier, String projectIdentifier);
}
