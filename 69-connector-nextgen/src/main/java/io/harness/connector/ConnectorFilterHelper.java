package io.harness.connector;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.inject.Singleton;

import io.harness.connector.apis.dto.ConnectorFilter;
import io.harness.connector.entities.Connector.ConnectorKeys;
import org.springframework.data.mongodb.core.query.Criteria;

@Singleton
public class ConnectorFilterHelper {
  public Criteria createCriteriaFromConnectorFilter(ConnectorFilter connectorFilter, String accountIdentifier) {
    Criteria criteria = new Criteria();
    if (connectorFilter == null) {
      return criteria;
    }
    criteria.and(ConnectorKeys.accountIdentifier).is(connectorFilter.getAccountId());
    if (isNotBlank(connectorFilter.getOrgId())) {
      criteria.and(ConnectorKeys.orgIdentifier).is(connectorFilter.getOrgId());
    }
    if (isNotBlank(connectorFilter.getProjectId())) {
      criteria.and(ConnectorKeys.projectIdentifier).is(connectorFilter.getProjectId());
    }
    if (connectorFilter.getType() != null) {
      criteria.and(ConnectorKeys.type).is(connectorFilter.getType().name());
    }
    if (isNotEmpty(connectorFilter.getName())) {
      criteria.and(ConnectorKeys.name).regex(connectorFilter.getName());
    }
    return criteria;
  }
}
