/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.search.framework;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.search.SearchHits;

@OwnedBy(PL)
public interface ElasticsearchRequestHandler {
  BoolQueryBuilder createQuery(String searchString, String accountId);
  List<SearchResult> processSearchResults(List<SearchResult> searchResults);
  List<SearchResult> translateHitsToSearchResults(SearchHits searchHits, String accountId);
  List<SearchResult> filterSearchResults(List<SearchResult> searchResults);
}
