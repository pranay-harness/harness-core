package software.wings.service.intfc;

import org.hibernate.validator.constraints.NotBlank;
import software.wings.search.framework.SearchResults;

import java.io.IOException;

public interface SearchService {
  SearchResults getSearchResults(@NotBlank String query, @NotBlank String accountId) throws IOException;
}