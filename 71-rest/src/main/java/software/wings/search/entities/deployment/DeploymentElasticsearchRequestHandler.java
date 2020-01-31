package software.wings.search.entities.deployment;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import software.wings.search.entities.deployment.DeploymentView.DeploymentViewKeys;
import software.wings.search.framework.AbstractElasticsearchRequestHandler;
import software.wings.search.framework.ElasticsearchRequestHandler;
import software.wings.search.framework.SearchResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class DeploymentElasticsearchRequestHandler
    extends AbstractElasticsearchRequestHandler implements ElasticsearchRequestHandler {
  @Override
  public BoolQueryBuilder createQuery(String searchString, String accountId) {
    return super.createQuery(searchString, accountId)
        .filter(QueryBuilders.termQuery(DeploymentViewKeys.workflowInPipeline, false));
  }

  @Override
  public List<SearchResult> processSearchResults(List<SearchResult> searchResults) {
    int start = 0;
    while (start < searchResults.size()) {
      int end = getLastIndexWithSameDeploymentName(searchResults, start);
      if (end > start) {
        sortInterval(searchResults.subList(start, end));
      }
      start = end;
    }
    return searchResults;
  }

  @Override
  public List<SearchResult> translateHitsToSearchResults(SearchHits searchHits, String accountId) {
    ObjectMapper mapper = new ObjectMapper();
    List<SearchResult> searchResults = new ArrayList<>();
    for (SearchHit hit : searchHits) {
      Map<String, Object> result = hit.getSourceAsMap();
      DeploymentView deploymentView = mapper.convertValue(result, DeploymentView.class);
      DeploymentSearchResult deploymentSearchResult = new DeploymentSearchResult(deploymentView, hit.getScore());
      searchResults.add(deploymentSearchResult);
    }
    return searchResults;
  }

  private static int getLastIndexWithSameDeploymentName(List<SearchResult> searchResults, int startIndex) {
    int endIndex = startIndex + 1;

    while (endIndex < searchResults.size()
        && searchResults.get(endIndex).getName().equals(searchResults.get(startIndex).getName())) {
      endIndex++;
    }
    return endIndex;
  }

  private static void sortInterval(List<SearchResult> searchResults) {
    searchResults.sort(Comparator.comparingLong(SearchResult::getCreatedAt).reversed());
  }
}
