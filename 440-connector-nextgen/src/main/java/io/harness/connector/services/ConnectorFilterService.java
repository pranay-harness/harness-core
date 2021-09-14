/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.connector.services;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.ConnectorCategory;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.filter.dto.FilterPropertiesDTO;

import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(DX)
public interface ConnectorFilterService {
  Criteria createCriteriaFromConnectorFilter(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      String searchTerm, ConnectorType type, ConnectorCategory category, ConnectorCategory sourceCategory);

  Criteria createCriteriaFromConnectorListQueryParams(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String filterIdentifier, String searchTerm, FilterPropertiesDTO filterProperties,
      Boolean includeAllConnectorsAccessibleAtScope);
}
