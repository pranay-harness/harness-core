package software.wings.service.intfc;

import org.hibernate.validator.constraints.NotBlank;
import software.wings.search.framework.SearchResponse;

import java.io.IOException;

public interface SearchService {
  SearchResponse getSearchResults(@NotBlank String query, @NotBlank String accountId) throws IOException;
}