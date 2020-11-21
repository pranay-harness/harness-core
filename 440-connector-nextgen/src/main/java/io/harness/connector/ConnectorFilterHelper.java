package io.harness.connector;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.springframework.data.mongodb.core.query.Criteria.where;

import io.harness.NGCommonEntityConstants;
import io.harness.connector.entities.Connector.ConnectorKeys;
import io.harness.delegate.beans.connector.ConnectorCategory;
import io.harness.delegate.beans.connector.ConnectorType;

import com.google.inject.Singleton;
import org.springframework.data.mongodb.core.query.Criteria;

@Singleton
public class ConnectorFilterHelper {
  public Criteria createCriteriaFromConnectorFilter(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String searchTerm, ConnectorType connectorType, ConnectorCategory category) {
    Criteria criteria = new Criteria();
    criteria.and(ConnectorKeys.accountIdentifier).is(accountIdentifier);
    criteria.orOperator(where(ConnectorKeys.deleted).exists(false), where(ConnectorKeys.deleted).is(false));
    criteria.and(ConnectorKeys.orgIdentifier).is(orgIdentifier);
    criteria.and(ConnectorKeys.projectIdentifier).is(projectIdentifier);
    if (connectorType != null) {
      criteria.and(ConnectorKeys.type).is(connectorType.name());
    }

    if (category != null) {
      criteria.and(ConnectorKeys.categories).in(category);
    }

    if (isNotBlank(searchTerm)) {
      Criteria seachCriteria = new Criteria().orOperator(where(ConnectorKeys.name).regex(searchTerm, "i"),
          where(NGCommonEntityConstants.IDENTIFIER_KEY).regex(searchTerm, "i"),
          where(NGCommonEntityConstants.TAGS_KEY).regex(searchTerm, "i"));
      criteria.andOperator(seachCriteria);
    }
    return criteria;
  }
}
