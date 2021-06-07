package io.harness.ccm.graphql.query.recommendation;

import static io.harness.annotations.dev.HarnessTeam.CE;
import static io.harness.ccm.commons.constants.Constants.ZONE_OFFSET;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.timescaledb.Tables.CE_RECOMMENDATIONS;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.commons.beans.recommendation.RecommendationOverviewStats;
import io.harness.ccm.graphql.core.recommendation.RecommendationService;
import io.harness.ccm.graphql.dto.recommendation.FilterStatsDTO;
import io.harness.ccm.graphql.dto.recommendation.K8sRecommendationFilterDTO;
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
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import org.jooq.Condition;
import org.jooq.TableField;

@Singleton
@GraphQLApi
@OwnedBy(CE)
public class RecommendationsOverviewQueryV2 {
  @Inject private GraphQLUtils graphQLUtils;
  @Inject private RecommendationService recommendationService;

  @GraphQLQuery(name = "recommendationsV2", description = "The list of all types of recommendations for overview page")
  public RecommendationsDTO recommendations(
      @GraphQLArgument(name = "filter", defaultValue = "{\"offset\":0,\"limit\":10}") K8sRecommendationFilterDTO filter,
      @GraphQLEnvironment final ResolutionEnvironment env) {
    final String accountId = graphQLUtils.getAccountIdentifier(env);

    Condition condition = applyCommonFilters(filter);

    final List<RecommendationItemDTO> items =
        recommendationService.listAll(accountId, condition, filter.getOffset(), filter.getLimit());
    return RecommendationsDTO.builder().items(items).offset(filter.getOffset()).limit(filter.getLimit()).build();
  }

  @GraphQLQuery(name = "recommendationStatsV2", description = "Top panel stats API, aggregated")
  public RecommendationOverviewStats recommendationStats(
      @GraphQLArgument(name = "filter", defaultValue = "{}") K8sRecommendationFilterDTO filter,
      @GraphQLEnvironment final ResolutionEnvironment env) {
    final String accountId = graphQLUtils.getAccountIdentifier(env);

    Condition condition = applyCommonFilters(filter);

    return recommendationService.getStats(accountId, condition);
  }

  @GraphQLQuery(name = "recommendationFilterStatsV2", description = "Possible filter values for each key")
  public List<FilterStatsDTO> recommendationFilterStats(
      @GraphQLArgument(name = "keys", defaultValue = "[]") List<String> columns,
      @GraphQLArgument(name = "filter", defaultValue = "{}") K8sRecommendationFilterDTO filter,
      @GraphQLEnvironment final ResolutionEnvironment env) {
    final String accountId = graphQLUtils.getAccountIdentifier(env);

    Condition condition = applyCommonFilters(filter);

    return recommendationService.getFilterStats(accountId, condition, columns, CE_RECOMMENDATIONS);
  }

  @NotNull
  private Condition applyCommonFilters(K8sRecommendationFilterDTO filter) {
    Condition condition = getValidRecommendationFilter();

    if (!isEmpty(filter.getIds())) {
      condition = condition.and(CE_RECOMMENDATIONS.ID.in(filter.getIds()));
    } else {
      if (!isEmpty(filter.getResourceTypes())) {
        condition = condition.and(CE_RECOMMENDATIONS.RESOURCETYPE.in(enumToString(filter.getResourceTypes())));
      }

      condition = appendStringFilter(condition, CE_RECOMMENDATIONS.CLUSTERNAME, filter.getClusterNames());
      condition = appendStringFilter(condition, CE_RECOMMENDATIONS.NAMESPACE, filter.getNamespaces());
      condition = appendStringFilter(condition, CE_RECOMMENDATIONS.NAME, filter.getNames());
      condition = appendGreaterOrEqualFilter(condition, CE_RECOMMENDATIONS.MONTHLYSAVING, filter.getMinSaving());
      condition = appendGreaterOrEqualFilter(condition, CE_RECOMMENDATIONS.MONTHLYCOST, filter.getMinCost());
    }

    return condition;
  }

  private static List<String> enumToString(List<? extends Enum> list) {
    return list.stream().map(Enum::name).collect(Collectors.toList());
  }

  private static Condition appendStringFilter(
      Condition condition, TableField<CeRecommendationsRecord, String> field, List<String> value) {
    if (!isEmpty(value)) {
      return condition.and(field.in(value));
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
            OffsetDateTime.now(ZONE_OFFSET).truncatedTo(ChronoUnit.DAYS).minusDays(2)));
  }
}
