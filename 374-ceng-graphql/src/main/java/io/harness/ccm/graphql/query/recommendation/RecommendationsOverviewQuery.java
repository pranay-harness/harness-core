/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ccm.graphql.query.recommendation;

import static io.harness.annotations.dev.HarnessTeam.CE;
import static io.harness.ccm.commons.utils.TimeUtils.offsetDateTimeNow;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.timescaledb.Tables.CE_RECOMMENDATIONS;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.commons.beans.recommendation.RecommendationOverviewStats;
import io.harness.ccm.commons.beans.recommendation.ResourceType;
import io.harness.ccm.graphql.core.recommendation.RecommendationService;
import io.harness.ccm.graphql.dto.recommendation.FilterStatsDTO;
import io.harness.ccm.graphql.dto.recommendation.RecommendationItemDTO;
import io.harness.ccm.graphql.dto.recommendation.RecommendationsDTO;
import io.harness.ccm.graphql.utils.GraphQLUtils;
import io.harness.ccm.graphql.utils.annotations.GraphQLApi;
import io.harness.timescaledb.tables.records.CeRecommendationsRecord;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLEnvironment;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.execution.ResolutionEnvironment;
import java.time.temporal.ChronoUnit;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.jooq.Condition;
import org.jooq.TableField;

@Slf4j
@Singleton
@GraphQLApi
@OwnedBy(CE)
public class RecommendationsOverviewQuery {
  @Inject private GraphQLUtils graphQLUtils;
  @Inject private RecommendationService recommendationService;

  @GraphQLQuery(name = "recommendations", description = "the list of all types of recommendations for overview page")
  public RecommendationsDTO recommendations(@GraphQLArgument(name = "id") String id,
      @GraphQLArgument(name = "name") String name, @GraphQLArgument(name = "namespace") String namespace,
      @GraphQLArgument(name = "clusterName") String clusterName,
      @GraphQLArgument(name = "resourceType") ResourceType resourceType,
      @GraphQLArgument(name = "minSaving") Double monthlySaving, @GraphQLArgument(name = "minCost") Double monthlyCost,
      @GraphQLArgument(name = "offset", defaultValue = "0") Long offset,
      @GraphQLArgument(name = "limit", defaultValue = "10") Long limit,
      @GraphQLEnvironment final ResolutionEnvironment env) {
    final String accountId = graphQLUtils.getAccountIdentifier(env);

    Condition condition =
        applyCommonFilters(id, name, namespace, clusterName, resourceType, monthlySaving, monthlyCost);

    final List<RecommendationItemDTO> items = recommendationService.listAll(accountId, condition, offset, limit);

    return RecommendationsDTO.builder().items(items).offset(offset).limit(limit).build();
  }

  @GraphQLQuery(description = "top panel stats API")
  public RecommendationOverviewStats recommendationStats(@GraphQLArgument(name = "id") String id,
      @GraphQLArgument(name = "name") String name, @GraphQLArgument(name = "namespace") String namespace,
      @GraphQLArgument(name = "clusterName") String clusterName,
      @GraphQLArgument(name = "resourceType") ResourceType resourceType,
      @GraphQLArgument(name = "minSaving") Double monthlySaving, @GraphQLArgument(name = "minCost") Double monthlyCost,
      @GraphQLEnvironment final ResolutionEnvironment env) {
    final String accountId = graphQLUtils.getAccountIdentifier(env);

    Condition condition =
        applyCommonFilters(id, name, namespace, clusterName, resourceType, monthlySaving, monthlyCost);
    return recommendationService.getStats(accountId, condition);
  }

  @GraphQLQuery(description = "possible filter values for each column")
  public List<FilterStatsDTO> recommendationFilterStats(
      @GraphQLArgument(name = "keys", defaultValue = "[]") List<String> columns,
      @GraphQLArgument(name = "name") String name, @GraphQLArgument(name = "namespace") String namespace,
      @GraphQLArgument(name = "clusterName") String clusterName,
      @GraphQLArgument(name = "resourceType") ResourceType resourceType,
      @GraphQLArgument(name = "minSaving") Double monthlySaving, @GraphQLArgument(name = "minCost") Double monthlyCost,
      @GraphQLEnvironment final ResolutionEnvironment env) {
    final String accountId = graphQLUtils.getAccountIdentifier(env);

    Condition condition =
        applyCommonFilters(null, name, namespace, clusterName, resourceType, monthlySaving, monthlyCost);

    return recommendationService.getFilterStats(accountId, condition, columns, CE_RECOMMENDATIONS);
  }

  @NotNull
  private Condition applyCommonFilters(String id, String name, String namespace, String clusterName,
      ResourceType resourceType, Double monthlySaving, Double monthlyCost) {
    Condition condition = getValidRecommendationFilter();

    if (!isEmpty(id)) {
      condition = condition.and(CE_RECOMMENDATIONS.ID.eq(id));
    } else {
      if (resourceType != null) {
        condition = condition.and(CE_RECOMMENDATIONS.RESOURCETYPE.eq(resourceType.name()));
      }

      condition = appendStringFilter(condition, CE_RECOMMENDATIONS.CLUSTERNAME, clusterName);
      condition = appendStringFilter(condition, CE_RECOMMENDATIONS.NAMESPACE, namespace);
      condition = appendStringFilter(condition, CE_RECOMMENDATIONS.NAME, name);
      condition = appendGreaterOrEqualFilter(condition, CE_RECOMMENDATIONS.MONTHLYSAVING, monthlySaving);
      condition = appendGreaterOrEqualFilter(condition, CE_RECOMMENDATIONS.MONTHLYCOST, monthlyCost);
    }

    return condition;
  }

  private static Condition appendStringFilter(
      Condition condition, TableField<CeRecommendationsRecord, String> field, String value) {
    if (!isEmpty(value)) {
      return condition.and(field.eq(value));
    }
    return condition;
  }

  private static Condition appendGreaterOrEqualFilter(
      Condition condition, TableField<CeRecommendationsRecord, Double> field, Double value) {
    if (value != null) {
      return condition.and(field.greaterOrEqual(value));
    }
    return condition;
  }

  private static Condition getValidRecommendationFilter() {
    return CE_RECOMMENDATIONS.ISVALID
        .eq(true)
        // based on current-gen workload recommendation dataFetcher
        .and(CE_RECOMMENDATIONS.LASTPROCESSEDAT.greaterOrEqual(
            offsetDateTimeNow().truncatedTo(ChronoUnit.DAYS).minusDays(2)));
  }
}
