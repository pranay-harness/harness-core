package software.wings.search.entities.application;

import static io.harness.rule.OwnerRule.UTKARSH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.elasticsearch.action.search.SearchResponse;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.beans.AccountType;
import software.wings.beans.EntityType;
import software.wings.features.AuditTrailFeature;
import software.wings.features.api.PremiumFeature;
import software.wings.search.SearchRequestHandlerTestUtils;
import software.wings.search.framework.SearchResult;

import java.util.List;

public class ApplicationElasticsearchRequestHandlerTest extends WingsBaseTest {
  @Mock @Named(AuditTrailFeature.FEATURE_NAME) PremiumFeature auditTrailFeature;
  @Inject @InjectMocks ApplicationElasticsearchRequestHandler applicationSearchRequestHandler;

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void translateHitsToSearchResultsTest() {
    Account account = getAccount(AccountType.PAID);
    String accountId = wingsPersistence.save(account);
    SearchResponse searchResponse = SearchRequestHandlerTestUtils.getSearchResponse(ApplicationSearchEntity.TYPE);
    when(auditTrailFeature.isAvailableForAccount(accountId)).thenReturn(true);

    List<SearchResult> searchResults =
        applicationSearchRequestHandler.translateHitsToSearchResults(searchResponse.getHits(), accountId);
    assertThat(searchResults).isNotNull();
    assertThat(searchResults.size()).isEqualTo(1);
    assertThat(searchResults.get(0).getType()).isEqualTo(EntityType.APPLICATION);
  }
}
