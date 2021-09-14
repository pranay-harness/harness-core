/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.core.activityhistory.service;

import io.harness.EntityType;
import io.harness.ng.core.activityhistory.dto.NGActivitySummaryDTO;
import io.harness.ng.core.activityhistory.dto.TimeGroupType;

import org.springframework.data.domain.Page;

public interface NGActivitySummaryService {
  Page<NGActivitySummaryDTO> listActivitySummary(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String referredEntityIdentifier, TimeGroupType timeGroupType, long start, long end,
      EntityType referredEntityType, EntityType referredByEntityType);
}
