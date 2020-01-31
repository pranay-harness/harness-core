package software.wings.search.entities.environment;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import software.wings.features.AuditTrailFeature;
import software.wings.features.api.PremiumFeature;
import software.wings.search.framework.AbstractElasticsearchRequestHandler;
import software.wings.search.framework.ElasticsearchRequestHandler;
import software.wings.search.framework.SearchResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EnvironmentElasticsearchRequestHandler
    extends AbstractElasticsearchRequestHandler implements ElasticsearchRequestHandler {
  @Inject @Named(AuditTrailFeature.FEATURE_NAME) private PremiumFeature auditTrailFeature;

  @Override
  public List<SearchResult> translateHitsToSearchResults(SearchHits searchHits, String accountId) {
    boolean includeAudits = auditTrailFeature.isAvailableForAccount(accountId);
    ObjectMapper mapper = new ObjectMapper();
    List<SearchResult> searchResults = new ArrayList<>();
    for (SearchHit hit : searchHits) {
      Map<String, Object> result = hit.getSourceAsMap();
      EnvironmentView environmentView = mapper.convertValue(result, EnvironmentView.class);
      EnvironmentSearchResult environmentSearchResult =
          new EnvironmentSearchResult(environmentView, includeAudits, hit.getScore());
      searchResults.add(environmentSearchResult);
    }
    return searchResults;
  }
}
