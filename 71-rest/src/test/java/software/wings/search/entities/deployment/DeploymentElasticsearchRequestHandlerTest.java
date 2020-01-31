package software.wings.search.entities.deployment;

import static io.harness.rule.OwnerRule.UTKARSH;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.beans.AccountType;
import software.wings.beans.EntityType;
import software.wings.search.SearchRequestHandlerTestUtils;
import software.wings.search.framework.SearchResult;

import java.util.List;

public class DeploymentElasticsearchRequestHandlerTest extends WingsBaseTest {
  @Inject @InjectMocks DeploymentElasticsearchRequestHandler deploymentSearchRequestHandler;

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void translateHitsToSearchResultsTest() {
    Account account = getAccount(AccountType.PAID);
    String accountId = wingsPersistence.save(account);
    SearchResponse searchResponse = SearchRequestHandlerTestUtils.getSearchResponse(DeploymentSearchEntity.TYPE);

    List<SearchResult> searchResults =
        deploymentSearchRequestHandler.translateHitsToSearchResults(searchResponse.getHits(), accountId);
    searchResults = deploymentSearchRequestHandler.processSearchResults(searchResults);
    assertThat(searchResults).isNotNull();
    assertThat(searchResults.size()).isEqualTo(2);
    assertThat(searchResults.get(0).getType()).isEqualTo(EntityType.DEPLOYMENT);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testCreateQuery() {
    String searchString = "value";
    Account account = getAccount(AccountType.PAID);
    String accountId = wingsPersistence.save(account);
    account.setUuid(accountId);

    BoolQueryBuilder boolQueryBuilder = deploymentSearchRequestHandler.createQuery(searchString, accountId);
    assertThat(boolQueryBuilder).isNotNull();
    assertThat(boolQueryBuilder.filter().size()).isEqualTo(2);
    assertThat(boolQueryBuilder.must().size()).isEqualTo(1);
  }
}
