package software.wings.search.framework;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import java.util.LinkedHashMap;
import java.util.List;
import lombok.Value;

@OwnedBy(PL)
@Value
public class SearchResults {
  LinkedHashMap<String, List<SearchResult>> searchResults;
}
