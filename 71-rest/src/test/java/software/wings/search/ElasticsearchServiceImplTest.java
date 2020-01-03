package software.wings.search;

import static io.harness.rule.OwnerRule.UJJAWAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.AccountType;
import software.wings.features.AuditTrailFeature;
import software.wings.features.api.PremiumFeature;
import software.wings.search.entities.application.ApplicationSearchEntity;
import software.wings.search.entities.deployment.DeploymentSearchEntity;
import software.wings.search.entities.environment.EnvironmentSearchEntity;
import software.wings.search.entities.pipeline.PipelineSearchEntity;
import software.wings.search.entities.service.ServiceSearchEntity;
import software.wings.search.entities.workflow.WorkflowSearchEntity;
import software.wings.search.framework.ElasticsearchClient;
import software.wings.search.framework.ElasticsearchIndexManager;
import software.wings.search.framework.SearchResults;
import software.wings.service.impl.ElasticsearchServiceImpl;

import java.io.IOException;

public class ElasticsearchServiceImplTest extends WingsBaseTest {
  @Mock private ElasticsearchIndexManager elasticsearchIndexManager;
  @Mock @Named(AuditTrailFeature.FEATURE_NAME) private PremiumFeature auditTrailFeature;
  @Mock private ElasticsearchClient elasticsearchClient;
  @Inject @InjectMocks private ElasticsearchServiceImpl elasticsearchService;

  @Before
  public void setup() throws IOException {}

  @Test
  @Owner(developers = UJJAWAL)
  @Category(UnitTests.class)
  public void testGetSearchResults() throws IOException {
    String searchString = "value";
    String accountId = getAccount(AccountType.PAID).getUuid();

    when(auditTrailFeature.isAvailableForAccount(anyString())).thenReturn(true);
    when(elasticsearchClient.search(any())).thenReturn(ElasticsearchServiceTestUtils.getSearchResponse());
    when(elasticsearchClient.multiSearch(any())).thenReturn(ElasticsearchServiceTestUtils.getMultiSearchResponse());
    when(elasticsearchIndexManager.getAliasName(DeploymentSearchEntity.TYPE)).thenReturn(DeploymentSearchEntity.TYPE);
    when(elasticsearchIndexManager.getAliasName(ApplicationSearchEntity.TYPE)).thenReturn(ApplicationSearchEntity.TYPE);
    when(elasticsearchIndexManager.getAliasName(ServiceSearchEntity.TYPE)).thenReturn(ServiceSearchEntity.TYPE);
    when(elasticsearchIndexManager.getAliasName(EnvironmentSearchEntity.TYPE)).thenReturn(EnvironmentSearchEntity.TYPE);
    when(elasticsearchIndexManager.getAliasName(WorkflowSearchEntity.TYPE)).thenReturn(WorkflowSearchEntity.TYPE);
    when(elasticsearchIndexManager.getAliasName(PipelineSearchEntity.TYPE)).thenReturn(PipelineSearchEntity.TYPE);

    SearchResults searchResults = elasticsearchService.getSearchResults(searchString, accountId);
    assertThat(searchResults).isNotNull();
    assertThat(searchResults.getSearchResults().get(DeploymentSearchEntity.TYPE).get(0).getCreatedAt()
        >= searchResults.getSearchResults().get(DeploymentSearchEntity.TYPE).get(1).getCreatedAt())
        .isTrue();
  }
}
