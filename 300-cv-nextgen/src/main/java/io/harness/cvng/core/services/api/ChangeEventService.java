/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.core.services.api;

import io.harness.cvng.beans.change.ChangeCategory;
import io.harness.cvng.beans.change.ChangeEventDTO;
import io.harness.cvng.beans.change.ChangeSourceType;
import io.harness.cvng.core.beans.change.ChangeSummaryDTO;
import io.harness.cvng.core.beans.change.ChangeTimeline;
import io.harness.cvng.core.beans.params.ProjectParams;
import io.harness.cvng.core.beans.params.ServiceEnvironmentParams;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;

import java.time.Instant;
import java.util.List;

public interface ChangeEventService {
  Boolean register(ChangeEventDTO changeEventDTO);
  List<ChangeEventDTO> get(ServiceEnvironmentParams serviceEnvironmentParams, List<String> changeSourceIdentifiers,
      Instant startTime, Instant endTime, List<ChangeCategory> changeCategories);

  ChangeEventDTO get(String activityId);

  ChangeSummaryDTO getChangeSummary(ServiceEnvironmentParams serviceEnvironmentParams,
      List<String> changeSourceIdentifiers, Instant startTime, Instant endTime);
  PageResponse<ChangeEventDTO> getChangeEvents(ProjectParams projectParams, List<String> serviceIdentifiers,
      List<String> environmentIdentifier, String searchText, List<ChangeCategory> changeCategories,
      List<ChangeSourceType> changeSourceTypes, Instant startTime, Instant endTime, PageRequest pageRequest);
  ChangeTimeline getTimeline(ProjectParams projectParams, List<String> serviceIdentifiers,
      List<String> environmentIdentifier, String searchText, List<ChangeCategory> changeCategories,
      List<ChangeSourceType> changeSourceTypes, Instant startTime, Instant endTime, Integer pointCount);
  ChangeSummaryDTO getChangeSummary(ProjectParams projectParams, List<String> serviceIdentifiers,
      List<String> environmentIdentifier, List<ChangeCategory> changeCategories,
      List<ChangeSourceType> changeSourceTypes, Instant startTime, Instant endTime);
}
