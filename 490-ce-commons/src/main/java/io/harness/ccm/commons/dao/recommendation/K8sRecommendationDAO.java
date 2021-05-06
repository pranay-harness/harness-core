package io.harness.ccm.commons.dao.recommendation;

import static io.harness.ccm.commons.Constants.ZONE_OFFSET;
import static io.harness.timescaledb.Tables.CE_RECOMMENDATIONS;

import static com.google.common.base.MoreObjects.firstNonNull;
import static org.jooq.impl.DSL.sum;

import io.harness.annotations.retry.RetryOnException;
import io.harness.ccm.commons.beans.recommendation.RecommendationOverviewStats;
import io.harness.ccm.commons.beans.recommendation.ResourceId;
import io.harness.persistence.HPersistence;
import io.harness.timescaledb.tables.pojos.CeRecommendations;

import software.wings.graphql.datafetcher.ce.recommendation.entity.K8sWorkloadRecommendation;
import software.wings.graphql.datafetcher.ce.recommendation.entity.K8sWorkloadRecommendation.K8sWorkloadRecommendationKeys;
import software.wings.graphql.datafetcher.ce.recommendation.entity.PartialRecommendationHistogram;
import software.wings.graphql.datafetcher.ce.recommendation.entity.PartialRecommendationHistogram.PartialRecommendationHistogramKeys;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.InsertFinalStep;
import org.jooq.Record;
import org.jooq.impl.DSL;

@Singleton
public class K8sRecommendationDAO {
  private static final int RETRY_COUNT = 3;
  private static final int SLEEP_DURATION = 100;

  @Inject private HPersistence hPersistence;
  @Inject private DSLContext dslContext;

  public Optional<K8sWorkloadRecommendation> fetchK8sWorkloadRecommendationById(
      @NotNull String accountIdentifier, @NotNull String id) {
    return Optional.ofNullable(hPersistence.createQuery(K8sWorkloadRecommendation.class)
                                   .filter(K8sWorkloadRecommendationKeys.accountId, accountIdentifier)
                                   .filter(K8sWorkloadRecommendationKeys.uuid, id)
                                   .get());
  }

  public List<PartialRecommendationHistogram> fetchPartialRecommendationHistograms(
      @NotNull String accountId, @NotNull ResourceId resourceId, @NotNull Instant startTime, @NotNull Instant endTime) {
    return hPersistence.createQuery(PartialRecommendationHistogram.class)
        .filter(PartialRecommendationHistogramKeys.accountId, accountId)
        .filter(PartialRecommendationHistogramKeys.clusterId, resourceId.getClusterId())
        .filter(PartialRecommendationHistogramKeys.namespace, resourceId.getNamespace())
        .filter(PartialRecommendationHistogramKeys.workloadName, resourceId.getName())
        .filter(PartialRecommendationHistogramKeys.workloadType, resourceId.getKind())
        .field(PartialRecommendationHistogramKeys.date)
        .greaterThanOrEq(startTime)
        .field(PartialRecommendationHistogramKeys.date)
        .lessThanOrEq(endTime)
        .asList();
  }

  @RetryOnException(retryCount = RETRY_COUNT)
  public List<CeRecommendations> fetchRecommendationsOverview(
      @NotNull String accountId, @Nullable Condition condition, @NotNull Long offset, @NotNull Long limit) {
    return dslContext.selectFrom(CE_RECOMMENDATIONS)
        .where(CE_RECOMMENDATIONS.ACCOUNTID.eq(accountId).and(firstNonNull(condition, DSL.noCondition())))
        .orderBy(CE_RECOMMENDATIONS.MONTHLYSAVING.desc().nullsLast())
        .offset(offset)
        .limit(limit)
        .fetchInto(CeRecommendations.class);
  }

  @RetryOnException(retryCount = RETRY_COUNT)
  public RecommendationOverviewStats fetchRecommendationsOverviewStats(
      @NotNull String accountId, @Nullable Condition condition) {
    return dslContext
        .select(sum(CE_RECOMMENDATIONS.MONTHLYCOST).as("totalMonthlyCost"),
            sum(CE_RECOMMENDATIONS.MONTHLYSAVING).as("totalMonthlySaving"))
        .from(CE_RECOMMENDATIONS)
        .where(CE_RECOMMENDATIONS.ACCOUNTID.eq(accountId).and(firstNonNull(condition, DSL.noCondition())))
        .fetchOneInto(RecommendationOverviewStats.class);
  }

  public void insertIntoCeRecommendation(@NotNull String uuid, @NotNull ResourceId workloadId,
      @Nullable Double monthlyCost, @Nullable Double monthlySaving, boolean shouldShowRecommendation,
      @NotNull Instant lastReceivedUntilAt) {
    insertOne(dslContext.insertInto(CE_RECOMMENDATIONS)
                  .set(CE_RECOMMENDATIONS.ACCOUNTID, workloadId.getAccountId())
                  .set(CE_RECOMMENDATIONS.ID, uuid)
                  // insert cluster-name instead after ClusterRecord from 40-rest is demoted to ce-commons
                  .set(CE_RECOMMENDATIONS.CLUSTERNAME, workloadId.getClusterId())
                  .set(CE_RECOMMENDATIONS.NAME, workloadId.getName())
                  .set(CE_RECOMMENDATIONS.NAMESPACE, workloadId.getNamespace())
                  .set(CE_RECOMMENDATIONS.RESOURCETYPE, "WORKLOAD")
                  .set(CE_RECOMMENDATIONS.MONTHLYCOST, monthlyCost)
                  .set(CE_RECOMMENDATIONS.MONTHLYSAVING, monthlySaving)
                  .set(CE_RECOMMENDATIONS.ISVALID, shouldShowRecommendation)
                  .set(CE_RECOMMENDATIONS.LASTPROCESSEDAT, lastReceivedUntilAt.atOffset(ZONE_OFFSET))
                  .set(CE_RECOMMENDATIONS.UPDATEDAT, OffsetDateTime.now(ZONE_OFFSET))
                  .onConflictOnConstraint(CE_RECOMMENDATIONS.getPrimaryKey())
                  .doUpdate()
                  .set(CE_RECOMMENDATIONS.MONTHLYCOST, monthlyCost)
                  .set(CE_RECOMMENDATIONS.MONTHLYSAVING, monthlySaving)
                  .set(CE_RECOMMENDATIONS.ISVALID, shouldShowRecommendation)
                  .set(CE_RECOMMENDATIONS.LASTPROCESSEDAT, lastReceivedUntilAt.atOffset(ZONE_OFFSET))
                  .set(CE_RECOMMENDATIONS.UPDATEDAT, OffsetDateTime.now(ZONE_OFFSET)));
  }

  @RetryOnException(retryCount = RETRY_COUNT, sleepDurationInMilliseconds = SLEEP_DURATION)
  private static void insertOne(@NotNull InsertFinalStep<? extends Record> finalStep) {
    finalStep.execute();
  }
}
