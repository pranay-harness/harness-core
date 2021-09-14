/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.search.framework;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import software.wings.search.framework.EntityBaseView.EntityBaseViewKeys;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.hibernate.validator.constraints.NotBlank;

@OwnedBy(PL)
@Slf4j
public abstract class AbstractElasticsearchRequestHandler implements ElasticsearchRequestHandler {
  private static final int BOOST_VALUE = 15;
  private static final int SLOP_DISTANCE_VALUE = 10;

  public BoolQueryBuilder createQuery(@NotBlank String searchString, @NotBlank String accountId) {
    BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
    QueryBuilder queryBuilder =
        QueryBuilders.disMaxQuery()
            .add(QueryBuilders.matchQuery(EntityBaseViewKeys.name, searchString)
                     .operator(Operator.AND)
                     .boost(BOOST_VALUE))
            .add(QueryBuilders.matchPhrasePrefixQuery(EntityBaseViewKeys.description, searchString)
                     .slop(SLOP_DISTANCE_VALUE))
            .tieBreaker(0.0f);
    boolQueryBuilder.must(queryBuilder).filter(QueryBuilders.termQuery(EntityBaseViewKeys.accountId, accountId));
    return boolQueryBuilder;
  }

  public List<SearchResult> processSearchResults(List<SearchResult> searchResults) {
    return searchResults;
  }

  public List<SearchResult> filterSearchResults(List<SearchResult> searchResults) {
    return searchResults;
  }
}
