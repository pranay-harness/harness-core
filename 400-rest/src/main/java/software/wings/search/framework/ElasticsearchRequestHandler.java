package software.wings.search.framework;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.search.SearchHits;

@OwnedBy(PL)
public interface ElasticsearchRequestHandler {
  BoolQueryBuilder createQuery(String searchString, String accountId);
  List<SearchResult> processSearchResults(List<SearchResult> searchResults);
  List<SearchResult> translateHitsToSearchResults(SearchHits searchHits, String accountId);
  List<SearchResult> filterSearchResults(List<SearchResult> searchResults);
}
