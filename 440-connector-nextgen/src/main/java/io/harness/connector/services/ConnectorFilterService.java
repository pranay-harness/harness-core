package io.harness.connector.services;

import io.harness.connector.apis.dto.ConnectorFilterDTO;
import io.harness.connector.apis.dto.ConnectorListFilter;
import io.harness.delegate.beans.connector.ConnectorCategory;
import io.harness.delegate.beans.connector.ConnectorType;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.mongodb.core.query.Criteria;

public interface ConnectorFilterService {
  ConnectorFilterDTO create(String accountId, ConnectorFilterDTO filter);
  ConnectorFilterDTO update(String accountId, ConnectorFilterDTO filter);
  boolean delete(String accountId, String orgIdentifier, String projectIdentifier, String identifier);
  ConnectorFilterDTO get(String accountId, String orgIdentifier, String projectIdentifier, String identifier);
  Page<ConnectorFilterDTO> list(
      int page, int size, String accountId, String orgIdentifier, String projectIdentifier, List<String> filterIds);
  Criteria createCriteriaFromConnectorListQueryParams(String accountIdentifier, ConnectorListFilter connectorFilter);

  Criteria createCriteriaFromConnectorFilter(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      String searchTerm, ConnectorType type, ConnectorCategory category);
}
