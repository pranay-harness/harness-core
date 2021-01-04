package software.wings.search;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import software.wings.search.framework.AdvancedSearchQuery;
import software.wings.search.framework.SearchResults;

import org.hibernate.validator.constraints.NotBlank;

@OwnedBy(PL)
public interface SearchService {
  SearchResults getSearchResults(@NotBlank String query, @NotBlank String accountId);

  SearchResults getSearchResults(@NotBlank String accountId, AdvancedSearchQuery advancedSearchQuery);
}
