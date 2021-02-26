package io.harness.connector.services;

import io.harness.connector.ConnectorCategory;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.filter.dto.FilterPropertiesDTO;

import org.springframework.data.mongodb.core.query.Criteria;

public interface ConnectorFilterService {
  Criteria createCriteriaFromConnectorFilter(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      String searchTerm, ConnectorType type, ConnectorCategory category);

  Criteria createCriteriaFromConnectorListQueryParams(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String filterIdentifier, String searchTerm, FilterPropertiesDTO filterProperties,
      Boolean includeAllConnectorsAccessibleAtScope);
}
