package software.wings.search.framework;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import lombok.Value;

import java.util.LinkedHashMap;
import java.util.List;

@OwnedBy(PL)
@Value
public class SearchResults {
  LinkedHashMap<String, List<SearchResult>> searchResults;
}
