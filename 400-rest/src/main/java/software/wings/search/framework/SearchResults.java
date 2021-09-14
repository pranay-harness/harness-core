/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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
