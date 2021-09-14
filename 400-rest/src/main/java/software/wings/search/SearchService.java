/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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
