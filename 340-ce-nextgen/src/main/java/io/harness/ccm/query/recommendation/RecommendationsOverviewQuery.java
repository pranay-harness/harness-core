package io.harness.ccm.query.recommendation;

import static io.harness.annotations.dev.HarnessTeam.CE;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.timescaledb.Tables.CE_RECOMMENDATIONS;

import static com.google.common.base.MoreObjects.firstNonNull;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.commons.beans.recommendation.RecommendationOverviewStats;
import io.harness.ccm.core.recommendation.RecommendationService;
import io.harness.ccm.dto.graphql.recommendation.RecommendationDetailsDTO;
import io.harness.ccm.dto.graphql.recommendation.RecommendationItemDTO;
import io.harness.ccm.dto.graphql.recommendation.RecommendationsDTO;
import io.harness.ccm.dto.graphql.recommendation.ResourceType;
import io.harness.ccm.utils.graphql.GraphQLApi;
import io.harness.ccm.utils.graphql.GraphQLUtils;
import io.harness.exception.InvalidRequestException;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLContext;
import io.leangen.graphql.annotations.GraphQLEnvironment;
import io.leangen.graphql.annotations.GraphQLNonNull;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.execution.ResolutionEnvironment;
import java.time.OffsetDateTime;
import java.util.List;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.jooq.Condition;
import org.jooq.impl.DSL;

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
      @GraphQLArgument(name = "offset") Long offset, @GraphQLArgument(name = "limit") Long limit,
      @GraphQLEnvironment final ResolutionEnvironment env) {
    final String accountId = graphQLUtils.getAccountIdentifier(env);

    offset = firstNonNull(offset, GraphQLUtils.DEFAULT_OFFSET);
    limit = firstNonNull(limit, GraphQLUtils.DEFAULT_LIMIT);

    Condition condition =
        applyCommonFilters(id, name, namespace, clusterName, resourceType, monthlySaving, monthlyCost);

    final List<RecommendationItemDTO> items = recommendationService.listAll(accountId, condition, offset, limit);

    return RecommendationsDTO.builder().items(items).offset(offset).limit(limit).build();
  }

  /**
   * Note: If this query becomes slow due to n+1 serial calls in future. Then,
   * we can use {@code CompletableFuture<RecommendationDetailsDTO>} to parallelize it,
   * with optional dataLoader to make 1+1 db calls instead of n+1.
   */
  @GraphQLQuery(description = "recommendation details/drillDown")
  public RecommendationDetailsDTO recommendationDetails(@GraphQLContext RecommendationItemDTO nodeDTO,
      @GraphQLArgument(name = "startTime", description = "defaults to Now().minusDays(7)") OffsetDateTime startTime,
      @GraphQLArgument(name = "endTime", description = "defaults to Now()") OffsetDateTime endTime,
      @GraphQLEnvironment final ResolutionEnvironment env) {
    final String accountIdentifier = graphQLUtils.getAccountIdentifier(env);

    return recommendationDetailsInternal(
        accountIdentifier, nodeDTO.getResourceType(), nodeDTO.getId(), startTime, endTime);
  }

  @GraphQLQuery(description = "recommendation details/drillDown")
  public RecommendationDetailsDTO recommendationDetails(@GraphQLNonNull @GraphQLArgument(name = "id") String id,
      @GraphQLNonNull @GraphQLArgument(name = "resourceType") ResourceType resourceType,
      @GraphQLArgument(name = "startTime", description = "defaults to Now().minusDays(7)") OffsetDateTime startTime,
      @GraphQLArgument(name = "endTime", description = "defaults to Now()") OffsetDateTime endTime,
      @GraphQLEnvironment final ResolutionEnvironment env) {
    final String accountIdentifier = graphQLUtils.getAccountIdentifier(env);

    return recommendationDetailsInternal(accountIdentifier, resourceType, id, startTime, endTime);
  }

  private RecommendationDetailsDTO recommendationDetailsInternal(@NotNull String accountIdentifier,
      @NotNull ResourceType resourceType, @NotNull String id, @Nullable OffsetDateTime startTime,
      @Nullable OffsetDateTime endTime) {
    startTime = firstNonNull(startTime, OffsetDateTime.now().minusDays(7));
    endTime = firstNonNull(endTime, OffsetDateTime.now());

    switch (resourceType) {
      case WORKLOAD:
        return recommendationService.getWorkloadRecommendationById(accountIdentifier, id, startTime, endTime);
      default:
        throw new InvalidRequestException("Not Implemented");
    }
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

  @NotNull
  private Condition applyCommonFilters(String id, String name, String namespace, String clusterName,
      ResourceType resourceType, Double monthlySaving, Double monthlyCost) {
    Condition condition = DSL.noCondition();

    if (!isEmpty(id)) {
      condition = condition.and(CE_RECOMMENDATIONS.ID.eq(id));
    } else {
      if (resourceType != null) {
        condition = condition.and(CE_RECOMMENDATIONS.RESOURCETYPE.eq(resourceType.name()));
      }
      if (!isEmpty(clusterName)) {
        condition = condition.and(CE_RECOMMENDATIONS.CLUSTERNAME.eq(clusterName));
      }
      if (!isEmpty(namespace)) {
        condition = condition.and(CE_RECOMMENDATIONS.NAMESPACE.eq(namespace));
      }
      if (!isEmpty(name)) {
        condition = condition.and(CE_RECOMMENDATIONS.NAME.eq(name));
      }
      if (monthlySaving != null) {
        condition = condition.and(CE_RECOMMENDATIONS.MONTHLYSAVING.greaterOrEqual(monthlySaving));
      }
      if (monthlyCost != null) {
        condition = condition.and(CE_RECOMMENDATIONS.MONTHLYCOST.greaterOrEqual(monthlyCost));
      }
    }

    return condition;
  }
}