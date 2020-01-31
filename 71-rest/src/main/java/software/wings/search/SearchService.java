package software.wings.search;

import org.hibernate.validator.constraints.NotBlank;
import software.wings.search.framework.AdvancedSearchQuery;
import software.wings.search.framework.SearchResults;

public interface SearchService {
  SearchResults getSearchResults(@NotBlank String query, @NotBlank String accountId);

  SearchResults getSearchResults(@NotBlank String accountId, AdvancedSearchQuery advancedSearchQuery);
}