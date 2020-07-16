package io.harness.connector;

import static io.harness.rule.OwnerRule.DEEPAK;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.connector.entities.Connector;
import io.harness.connector.entities.embedded.kubernetescluster.KubernetesClusterConfig;
import io.harness.connector.mappers.ConnectorSummaryMapper;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.PageImpl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ConnectorScopeHelperTest extends CategoryTest {
  @InjectMocks ConnectorScopeHelper connectorScopeHelper;
  @Mock OrgScopeHelper orgScopeHelper;
  @Mock ProjectScopeHelper projectScopeHelper;
  @Mock ConnectorSummaryMapper connectorSummaryMapper;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void createConnectorSummaryListForConnectors() {
    Connector connector = KubernetesClusterConfig.builder().build();
    Map<String, String> orgIdentifierOrgNameMap = new HashMap<String, String>() {
      { put("orgId", "Org 1"); }
    };
    Map<String, String> projectIdentifierProjectNameMap = new HashMap<String, String>() {
      { put("projectId", "Project 1"); }
    };
    when(orgScopeHelper.createOrgIdentifierOrgNameMap(any())).thenReturn(orgIdentifierOrgNameMap);
    when(projectScopeHelper.createProjectIdentifierProjectNameMap(any())).thenReturn(projectIdentifierProjectNameMap);
    connectorScopeHelper.createConnectorSummaryListForConnectors(new PageImpl<Connector>(Arrays.asList(connector)));
    // todo @deepak Remove this check of hard coded accountId once account support is added
    verify(connectorSummaryMapper, times(1))
        .writeConnectorSummaryDTO(
            eq(connector), eq("Test Account"), eq(orgIdentifierOrgNameMap), eq(projectIdentifierProjectNameMap));
  }
}