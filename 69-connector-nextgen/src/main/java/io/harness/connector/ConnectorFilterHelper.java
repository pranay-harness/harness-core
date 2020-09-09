package io.harness.connector;

import static io.harness.ng.NGConstants.IDENTIFIER_KEY;
import static io.harness.ng.NGConstants.TAGS_KEY;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.springframework.data.mongodb.core.query.Criteria.where;

import com.google.inject.Singleton;

import io.harness.connector.entities.Connector.ConnectorKeys;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.beans.connector.ConnectorCategory;
import io.harness.delegate.beans.connector.ConnectorType;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.List;

@Singleton
public class ConnectorFilterHelper {
  public Criteria createCriteriaFromConnectorFilter(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String searchTerm, ConnectorType connectorType, List<ConnectorCategory> categories) {
    Criteria criteria = new Criteria();
    criteria.and(ConnectorKeys.accountIdentifier).is(accountIdentifier);
    // todo @deepak: The fist query is for returning old records where the deleted field was not there
    criteria.orOperator(where(ConnectorKeys.deleted).exists(false), where(ConnectorKeys.deleted).is(false));
    if (isNotBlank(orgIdentifier)) {
      criteria.and(ConnectorKeys.orgIdentifier).is(orgIdentifier);
    }
    if (isNotBlank(projectIdentifier)) {
      criteria.and(ConnectorKeys.projectIdentifier).is(projectIdentifier);
    }
    if (connectorType != null) {
      criteria.and(ConnectorKeys.type).is(connectorType.name());
    }

    if (EmptyPredicate.isNotEmpty(categories)) {
      criteria.and(ConnectorKeys.categories).in(categories);
    }

    if (isNotBlank(searchTerm)) {
      Criteria seachCriteria = new Criteria().orOperator(where(ConnectorKeys.name).regex(searchTerm),
          where(IDENTIFIER_KEY).regex(searchTerm), where(TAGS_KEY).regex(searchTerm));
      criteria.andOperator(seachCriteria);
    }
    return criteria;
  }
}
