package software.wings.search.framework;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.search.SearchHits;

import java.util.List;

public interface ElasticsearchRequestHandler {
  BoolQueryBuilder createQuery(String searchString, String accountId);
  List<SearchResult> processSearchResults(List<SearchResult> searchResults);
  List<SearchResult> translateHitsToSearchResults(SearchHits searchHits, String accountId);
  List<SearchResult> filterSearchResults(List<SearchResult> searchResults);
}
