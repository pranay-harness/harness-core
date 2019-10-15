package software.wings.search.framework;

import lombok.Value;

import java.util.LinkedHashMap;
import java.util.List;

@Value
public class SearchResults {
  LinkedHashMap<String, List<SearchResult>> searchResults;
}
