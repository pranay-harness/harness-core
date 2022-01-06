/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.search;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.exception.WingsException.USER;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.GeneralException;
import io.harness.exception.InvalidArgumentsException;

import software.wings.search.framework.AdvancedSearchQuery;
import software.wings.search.framework.ElasticsearchClient;
import software.wings.search.framework.ElasticsearchIndexManager;
import software.wings.search.framework.ElasticsearchRequestHandler;
import software.wings.search.framework.SearchEntity;
import software.wings.search.framework.SearchResult;
import software.wings.search.framework.SearchResults;

import com.google.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.hibernate.validator.constraints.NotBlank;

@OwnedBy(PL)
@Slf4j
public class ElasticsearchServiceImpl implements SearchService {
  @Inject private Set<SearchEntity<?>> searchEntities;
  @Inject private ElasticsearchClient elasticsearchClient;
  @Inject private ElasticsearchIndexManager elasticsearchIndexManager;
  private static final int GLOBAL_MAX_RESULTS_TO_RETURN_PER_ENTITY = 40;
  private static final int DEFAULT_RESULTS_TO_RETURN_PER_ENTITY = 20;
  private static final int MAX_RESULT_WINDOW = 10000;
  public static final int MAX_NO_OF_ATTEMPTS = 20;

  @Builder
  private static class SearchInternalResponse {
    private List<SearchResult> searchResults;
    private boolean areResultsExhausted;
  }

  @Override
  public SearchResults getSearchResults(@NotBlank String searchString, @NotBlank String accountId) {
    List<String> searchEntityKeys = searchEntities.stream().map(SearchEntity::getType).collect(Collectors.toList());
    AdvancedSearchQuery advancedSearchQuery = AdvancedSearchQuery.builder()
                                                  .searchQuery(searchString)
                                                  .numResults(DEFAULT_RESULTS_TO_RETURN_PER_ENTITY)
                                                  .offset(0)
                                                  .entities(searchEntityKeys)
                                                  .build();
    return getSearchResults(accountId, advancedSearchQuery);
  }

  @Override
  public SearchResults getSearchResults(@NotBlank String acccountId, AdvancedSearchQuery advancedSearchQuery) {
    String searchString = advancedSearchQuery.getSearchQuery();
    int offset = advancedSearchQuery.getOffset();
    int numResults = advancedSearchQuery.getNumResults();
    Set<String> searchEntityKeys = new LinkedHashSet<>(advancedSearchQuery.getEntities());

    LinkedHashMap<String, List<SearchResult>> searchResult = new LinkedHashMap<>();
    boolean areResultsAvailable = areResultsAvailable(offset, numResults, searchEntityKeys.size());
    if (!areResultsAvailable) {
      return new SearchResults(searchResult);
    }
    int numberOfResultsToReturn = getNumberOfResultstoReturn(offset, numResults);

    Map<String, SearchEntity<?>> searchEntitiesMap = new HashMap<>();
    searchEntities.forEach(searchEntity -> searchEntitiesMap.put(searchEntity.getType(), searchEntity));
    searchEntityKeys.stream()
        .filter(key -> searchEntitiesMap.get(key) != null)
        .map(searchEntitiesMap::get)
        .forEach(searchEntity -> {
          List<SearchResult> searchResults =
              fetchSearchResults(searchEntity, searchString, acccountId, numberOfResultsToReturn, offset);
          searchResult.put(searchEntity.getType(), searchResults);
        });
    return new SearchResults(searchResult);
  }

  private boolean areResultsAvailable(int offset, int numResults, int searchEntityKeysLength) {
    if (offset < 0) {
      throw new InvalidArgumentsException("The offset cannot be negative", USER);
    }
    if (numResults <= 0) {
      throw new InvalidArgumentsException("The requested number of results cannot be negative or zero", USER);
    }
    if (searchEntityKeysLength > searchEntities.size()) {
      throw new InvalidArgumentsException("The requested entities is more than the max searchable entities", USER);
    }
    return offset < MAX_RESULT_WINDOW;
  }

  private int getNumberOfResultstoReturn(int offset, int numResults) {
    return Math.max(
        Math.min(Math.min(MAX_RESULT_WINDOW - offset, GLOBAL_MAX_RESULTS_TO_RETURN_PER_ENTITY), numResults), 0);
  }

  private List<SearchResult> fetchSearchResults(
      SearchEntity<?> searchEntity, String searchString, String accountId, int numResultsToReturn, int offset) {
    List<SearchResult> searchResults = new ArrayList<>();
    int attempts = 0;
    boolean areResultsExhausted = false;
    while (attempts < MAX_NO_OF_ATTEMPTS && searchResults.size() < numResultsToReturn && !areResultsExhausted) {
      int pageOffsetForFetch = offset + attempts * GLOBAL_MAX_RESULTS_TO_RETURN_PER_ENTITY;
      int numberOfResultsToFetch =
          getNumberOfResultstoReturn(pageOffsetForFetch, GLOBAL_MAX_RESULTS_TO_RETURN_PER_ENTITY);
      if (numberOfResultsToFetch != 0) {
        SearchInternalResponse response = fetchSearchResultsInternal(
            searchEntity, searchString, accountId, numberOfResultsToFetch, pageOffsetForFetch);
        areResultsExhausted = response.areResultsExhausted;
        searchResults.addAll(response.searchResults);
      } else {
        areResultsExhausted = true;
      }
      attempts++;
    }
    ElasticsearchRequestHandler elasticsearchRequestHandler = searchEntity.getElasticsearchRequestHandler();
    elasticsearchRequestHandler.processSearchResults(searchResults);
    return searchResults;
  }

  private SearchInternalResponse fetchSearchResultsInternal(
      SearchEntity<?> searchEntity, String searchString, String accountId, int numResultsToReturn, int offset) {
    SearchRequest searchRequest = getSearchRequest(searchEntity, searchString, accountId, numResultsToReturn, offset);
    try {
      SearchResponse searchResponse = elasticsearchClient.search(searchRequest);
      if (searchResponse.getHits() == null) {
        return new SearchInternalResponse(Collections.emptyList(), true);
      }
      ElasticsearchRequestHandler elasticsearchRequestHandler = searchEntity.getElasticsearchRequestHandler();
      List<SearchResult> searchResults =
          elasticsearchRequestHandler.translateHitsToSearchResults(searchResponse.getHits(), accountId);
      boolean areSearchResultsExhausted = searchResults.size() < numResultsToReturn;
      return new SearchInternalResponse(
          elasticsearchRequestHandler.filterSearchResults(searchResults), areSearchResultsExhausted);
    } catch (IOException e) {
      throw new GeneralException("Search request failed. Could not connect to elasticsearch", e, USER);
    }
  }

  private SearchRequest getSearchRequest(
      SearchEntity<?> searchEntity, String searchString, String accountId, int size, int offset) {
    String indexName = elasticsearchIndexManager.getAliasName(searchEntity.getType());
    SearchRequest searchRequest = new SearchRequest(indexName);
    BoolQueryBuilder boolQueryBuilder =
        searchEntity.getElasticsearchRequestHandler().createQuery(searchString, accountId);
    SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder().query(boolQueryBuilder).size(size).from(offset);
    searchRequest.source(searchSourceBuilder);
    searchRequest.indicesOptions(IndicesOptions.LENIENT_EXPAND_OPEN);
    return searchRequest;
  }
}
