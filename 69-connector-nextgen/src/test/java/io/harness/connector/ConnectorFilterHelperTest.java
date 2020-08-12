package io.harness.connector;

import static io.harness.delegate.beans.connector.ConnectorType.KUBERNETES_CLUSTER;
import static io.harness.rule.OwnerRule.DEEPAK;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.connector.apis.dto.ConnectorFilter;
import io.harness.connector.entities.Connector.ConnectorKeys;
import io.harness.rule.Owner;
import org.bson.Document;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.regex.Pattern;

public class ConnectorFilterHelperTest extends CategoryTest {
  @InjectMocks ConnectorFilterHelper connectorFilterHelper;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void createCriteriaFromConnectorFilter() {
    String accountId = "accountId";
    String name = "name";
    String orgId = "orgId";
    String projectId = "projectId";
    ConnectorFilter connectorFilter = ConnectorFilter.builder().name(name).type(KUBERNETES_CLUSTER).build();
    Criteria criteria =
        connectorFilterHelper.createCriteriaFromConnectorFilter(connectorFilter, accountId, orgId, projectId);
    Document criteriaObject = criteria.getCriteriaObject();
    assertThat(criteriaObject.size()).isEqualTo(5);
    assertThat(criteriaObject.get(ConnectorKeys.accountIdentifier)).isEqualTo(accountId);
    assertThat(criteriaObject.get(ConnectorKeys.orgIdentifier)).isEqualTo(orgId);
    assertThat(criteriaObject.get(ConnectorKeys.projectIdentifier)).isEqualTo(projectId);
    assertThat(criteriaObject.get(ConnectorKeys.type)).isEqualTo(KUBERNETES_CLUSTER.name());
    // regex check for name
    assertThat(((Pattern) (criteriaObject.get(ConnectorKeys.name))).pattern()).isEqualTo(name);
  }
}