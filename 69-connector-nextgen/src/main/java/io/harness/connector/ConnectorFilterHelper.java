package io.harness.connector;

import static io.harness.secretmanagerclient.NGConstants.IDENTIFIER_KEY;
import static io.harness.secretmanagerclient.NGConstants.TAGS_KEY;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.inject.Singleton;

import io.harness.connector.entities.Connector.ConnectorKeys;
import io.harness.delegate.beans.connector.ConnectorType;
import org.springframework.data.mongodb.core.query.Criteria;

@Singleton
public class ConnectorFilterHelper {
  public Criteria createCriteriaFromConnectorFilter(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String searchTerm, String type) {
    Criteria criteria = new Criteria();
    criteria.and(ConnectorKeys.accountIdentifier).is(accountIdentifier);
    if (isNotBlank(orgIdentifier)) {
      criteria.and(ConnectorKeys.orgIdentifier).is(orgIdentifier);
    }
    if (isNotBlank(projectIdentifier)) {
      criteria.and(ConnectorKeys.projectIdentifier).is(projectIdentifier);
    }
    if (type != null) {
      ConnectorType connectorType = ConnectorType.getConnectorType(type);
      criteria.and(ConnectorKeys.type).is(connectorType);
    }
    if (isNotBlank(searchTerm)) {
      criteria.orOperator(criteria.where(ConnectorKeys.name).regex(searchTerm),
          criteria.where(IDENTIFIER_KEY).regex(searchTerm), criteria.where(TAGS_KEY).regex(searchTerm));
    }
    return criteria;
  }
}
