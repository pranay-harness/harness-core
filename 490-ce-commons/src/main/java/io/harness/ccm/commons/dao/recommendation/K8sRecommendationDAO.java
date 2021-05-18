package io.harness.ccm.commons.dao.recommendation;

import static io.harness.annotations.dev.HarnessTeam.CE;
import static io.harness.ccm.commons.Constants.ZONE_OFFSET;
import static io.harness.timescaledb.Tables.CE_RECOMMENDATIONS;
import static io.harness.timescaledb.Tables.KUBERNETES_UTILIZATION_DATA;
import static io.harness.timescaledb.Tables.NODE_INFO;
import static io.harness.timescaledb.Tables.NODE_POOL_AGGREGATED;
import static io.harness.timescaledb.Tables.POD_INFO;

import static com.google.common.base.MoreObjects.firstNonNull;
import static org.jooq.impl.DSL.concat;
import static org.jooq.impl.DSL.greatest;
import static org.jooq.impl.DSL.max;
import static org.jooq.impl.DSL.select;
import static org.jooq.impl.DSL.sum;
import static org.jooq.impl.DSL.val;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.retry.RetryOnException;
import io.harness.ccm.commons.beans.JobConstants;
import io.harness.ccm.commons.beans.recommendation.NodePoolId;
import io.harness.ccm.commons.beans.recommendation.RecommendationOverviewStats;
import io.harness.ccm.commons.beans.recommendation.ResourceId;
import io.harness.ccm.commons.beans.recommendation.TotalResourceUsage;
import io.harness.persistence.HPersistence;
import io.harness.timescaledb.Keys;
import io.harness.timescaledb.Routines;
import io.harness.timescaledb.tables.pojos.CeRecommendations;

import software.wings.graphql.datafetcher.ce.recommendation.entity.K8sWorkloadRecommendation;
import software.wings.graphql.datafetcher.ce.recommendation.entity.K8sWorkloadRecommendation.K8sWorkloadRecommendationKeys;
import software.wings.graphql.datafetcher.ce.recommendation.entity.PartialRecommendationHistogram;
import software.wings.graphql.datafetcher.ce.recommendation.entity.PartialRecommendationHistogram.PartialRecommendationHistogramKeys;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.InsertFinalStep;
import org.jooq.Record;
import org.jooq.SelectFinalStep;
import org.jooq.SelectSelectStep;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.jooq.types.YearToSecond;

@Slf4j
@Singleton
@OwnedBy(CE)
public class K8sRecommendationDAO {
  private static final int RETRY_COUNT = 3;
  private static final int SLEEP_DURATION = 200;
  private static final String MAXCPU = "maxcpu";
  private static final String MAXMEMORY = "maxmemory";
  private static final String SUMCPU = "sumcpu";
  private static final String SUMMEMORY = "summemory";
  private static final String TIME = "time";

  @Inject private HPersistence hPersistence;
  @Inject private DSLContext dslContext;

  @NonNull
  public Optional<K8sWorkloadRecommendation> fetchK8sWorkloadRecommendationById(
      @NonNull String accountIdentifier, @NonNull String id) {
    return Optional.ofNullable(hPersistence.createQuery(K8sWorkloadRecommendation.class)
                                   .filter(K8sWorkloadRecommendationKeys.accountId, accountIdentifier)
                                   .filter(K8sWorkloadRecommendationKeys.uuid, id)
                                   .get());
  }

  public List<PartialRecommendationHistogram> fetchPartialRecommendationHistograms(
      @NonNull String accountId, @NonNull ResourceId resourceId, @NonNull Instant startTime, @NonNull Instant endTime) {
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

  @RetryOnException(retryCount = RETRY_COUNT, sleepDurationInMilliseconds = SLEEP_DURATION)
  public List<CeRecommendations> fetchRecommendationsOverview(
      @NonNull String accountId, @Nullable Condition condition, @NonNull Long offset, @NonNull Long limit) {
    return dslContext.selectFrom(CE_RECOMMENDATIONS)
        .where(CE_RECOMMENDATIONS.ACCOUNTID.eq(accountId).and(firstNonNull(condition, DSL.noCondition())))
        .orderBy(CE_RECOMMENDATIONS.MONTHLYSAVING.desc().nullsLast())
        .offset(offset)
        .limit(limit)
        .fetchInto(CeRecommendations.class);
  }

  @RetryOnException(retryCount = RETRY_COUNT, sleepDurationInMilliseconds = SLEEP_DURATION)
  public RecommendationOverviewStats fetchRecommendationsOverviewStats(
      @NonNull String accountId, @Nullable Condition condition) {
    return dslContext
        .select(sum(CE_RECOMMENDATIONS.MONTHLYCOST).as("totalMonthlyCost"),
            sum(CE_RECOMMENDATIONS.MONTHLYSAVING).as("totalMonthlySaving"))
        .from(CE_RECOMMENDATIONS)
        .where(CE_RECOMMENDATIONS.ACCOUNTID.eq(accountId).and(firstNonNull(condition, DSL.noCondition())))
        .fetchOneInto(RecommendationOverviewStats.class);
  }

  public void insertIntoCeRecommendation(@NonNull String uuid, @NonNull ResourceId workloadId,
      @Nullable Double monthlyCost, @Nullable Double monthlySaving, boolean shouldShowRecommendation,
      @NonNull Instant lastReceivedUntilAt) {
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
  public List<NodePoolId> getUniqueNodePools(@NonNull String accountId) {
    // TODO(UTSAV): We want to fetch only node pools which has at least one node running in jobStartTime and jobEndTime
    // window. will enforce this once the recommendation algo is stable.
    return dslContext.select(NODE_INFO.NODEPOOLNAME, NODE_INFO.CLUSTERID)
        .from(NODE_INFO)
        .where(NODE_INFO.ACCOUNTID.eq(accountId))
        .groupBy(NODE_INFO.CLUSTERID, NODE_INFO.NODEPOOLNAME)
        .fetchInto(NodePoolId.class);
  }

  @RetryOnException(retryCount = RETRY_COUNT, sleepDurationInMilliseconds = SLEEP_DURATION)
  public TotalResourceUsage maxResourceOfAllTimeBucketsForANodePool(
      @NonNull JobConstants jobConstants, @NonNull NodePoolId nodePoolId) {
    Table<? extends Record> t2 = sumResourceForEachTimeBucket(jobConstants, nodePoolId);

    // T3
    SelectSelectStep<? extends Record> selectStepT3 =
        select(max(t2.field(SUMCPU)).as(SUMCPU), max(t2.field(SUMMEMORY)).as(SUMMEMORY),
            max(t2.field(MAXCPU)).as(MAXCPU), max(t2.field(MAXMEMORY)).as(MAXMEMORY));

    SelectFinalStep<? extends Record> finalStepT3 = dslContext.select(selectStepT3.getSelect()).from(t2);
    log.info("maxResourceOfAllTimeBucketsForANodePool, final query: {}", finalStepT3.toString());

    return finalStepT3.fetchOneInto(TotalResourceUsage.class);
  }

  private Table<? extends Record> sumResourceForEachTimeBucket(JobConstants jobConstants, NodePoolId nodePoolId) {
    Table<? extends Record> t1 = groupByInstanceIdForEachTimeBucket(jobConstants, nodePoolId);

    SelectSelectStep<? extends Record> selectStep = select(sum(t1.field(KUBERNETES_UTILIZATION_DATA.CPU)).as(SUMCPU),
        sum(t1.field(KUBERNETES_UTILIZATION_DATA.MEMORY)).as(SUMMEMORY),
        max(t1.field(KUBERNETES_UTILIZATION_DATA.MAXCPU)).as(MAXCPU),
        max(t1.field(KUBERNETES_UTILIZATION_DATA.MAXMEMORY)).as(MAXMEMORY));

    return dslContext.select(selectStep.getSelect()).from(t1).groupBy(t1.field(TIME)).asTable();
  }

  private Table<? extends Record> groupByInstanceIdForEachTimeBucket(JobConstants jobConstants, NodePoolId nodePoolId) {
    Table<? extends Record> t0 = getPodRequest(jobConstants, nodePoolId);

    Field<OffsetDateTime> timeBucket =
        Routines.timeBucket2(val(YearToSecond.valueOf(Duration.ofMinutes(20))), KUBERNETES_UTILIZATION_DATA.STARTTIME)
            .as(TIME);

    Condition kubernetesUtilizationData_with_t0 =
        KUBERNETES_UTILIZATION_DATA.ACTUALINSTANCEID.eq(t0.field(KUBERNETES_UTILIZATION_DATA.ACTUALINSTANCEID))
            .and(KUBERNETES_UTILIZATION_DATA.ACCOUNTID.eq(jobConstants.getAccountId())
                     .and(KUBERNETES_UTILIZATION_DATA.CLUSTERID.eq(nodePoolId.getClusterid())
                              .and(isAlive(KUBERNETES_UTILIZATION_DATA.STARTTIME, KUBERNETES_UTILIZATION_DATA.ENDTIME,
                                  jobConstants.getJobStartTime(), jobConstants.getJobEndTime()))));

    SelectSelectStep<? extends Record> selectStepT1 = select(timeBucket,
        greatest(max(KUBERNETES_UTILIZATION_DATA.CPU), max(t0.field(POD_INFO.CPUREQUEST)))
            .as(KUBERNETES_UTILIZATION_DATA.CPU),
        greatest(max(KUBERNETES_UTILIZATION_DATA.MEMORY), max(t0.field(POD_INFO.MEMORYREQUEST)))
            .as(KUBERNETES_UTILIZATION_DATA.MEMORY),
        greatest(max(KUBERNETES_UTILIZATION_DATA.MAXCPU), max(t0.field(POD_INFO.CPUREQUEST)))
            .as(KUBERNETES_UTILIZATION_DATA.MAXCPU),
        greatest(max(KUBERNETES_UTILIZATION_DATA.MAXMEMORY), max(t0.field(POD_INFO.MEMORYREQUEST)))
            .as(KUBERNETES_UTILIZATION_DATA.MAXMEMORY));

    return dslContext.select(selectStepT1.getSelect())
        .from(KUBERNETES_UTILIZATION_DATA.innerJoin(t0).on(kubernetesUtilizationData_with_t0))
        .groupBy(timeBucket, KUBERNETES_UTILIZATION_DATA.ACTUALINSTANCEID)
        .asTable();
  }

  private Table<? extends Record> getPodRequest(JobConstants jobConstants, NodePoolId nodePoolId) {
    final String kube_system_namespace = "kube-system";
    final String namespace_name_separator = "/";

    return dslContext
        .select(concat(POD_INFO.NAMESPACE, val(namespace_name_separator), POD_INFO.NAME)
                    .as(KUBERNETES_UTILIZATION_DATA.ACTUALINSTANCEID),
            POD_INFO.CPUREQUEST, POD_INFO.MEMORYREQUEST)
        .from(POD_INFO.innerJoin(NODE_INFO).on(NODE_INFO.INSTANCEID.eq(POD_INFO.PARENTNODEID),
            NODE_INFO.ACCOUNTID.eq(jobConstants.getAccountId()), NODE_INFO.CLUSTERID.eq(nodePoolId.getClusterid()),
            NODE_INFO.NODEPOOLNAME.eq(nodePoolId.getNodepoolname()),
            isAlive(
                NODE_INFO.STARTTIME, NODE_INFO.STOPTIME, jobConstants.getJobStartTime(), jobConstants.getJobEndTime()),
            POD_INFO.NAMESPACE.notEqual(kube_system_namespace),
            isAlive(
                POD_INFO.STARTTIME, POD_INFO.STOPTIME, jobConstants.getJobStartTime(), jobConstants.getJobEndTime())))
        .asTable();
  }

  private static Condition isAlive(
      Field<OffsetDateTime> STARTTIME, Field<OffsetDateTime> STOPTIME, long jobStartTime, long jobEndTime) {
    return Routines.isAlive(STARTTIME, STOPTIME, val(fromEpochMilli(jobStartTime)), val(fromEpochMilli(jobEndTime)))
        .eq(true);
  }

  public void insertNodePoolAggregated(@NonNull JobConstants jobConstants, @NonNull NodePoolId nodePoolId,
      @NonNull TotalResourceUsage totalResourceUsage) {
    insertOne(dslContext.insertInto(NODE_POOL_AGGREGATED)
                  .set(NODE_POOL_AGGREGATED.ACCOUNTID, jobConstants.getAccountId())
                  .set(NODE_POOL_AGGREGATED.CLUSTERID, nodePoolId.getClusterid())
                  .set(NODE_POOL_AGGREGATED.NAME, nodePoolId.getNodepoolname())
                  .set(NODE_POOL_AGGREGATED.STARTTIME, fromEpochMilli(jobConstants.getJobStartTime()))
                  .set(NODE_POOL_AGGREGATED.ENDTIME, fromEpochMilli(jobConstants.getJobEndTime()))
                  .set(NODE_POOL_AGGREGATED.SUMCPU, totalResourceUsage.getSumcpu())
                  .set(NODE_POOL_AGGREGATED.SUMMEMORY, totalResourceUsage.getSummemory())
                  .set(NODE_POOL_AGGREGATED.MAXCPU, totalResourceUsage.getMaxcpu())
                  .set(NODE_POOL_AGGREGATED.MAXMEMORY, totalResourceUsage.getMaxmemory())
                  .onConflictOnConstraint(Keys.NODE_POOL_AGGREGATED_UNIQUE_RECORD_INDEX)
                  .doUpdate()
                  .set(NODE_POOL_AGGREGATED.SUMCPU, totalResourceUsage.getSumcpu())
                  .set(NODE_POOL_AGGREGATED.SUMMEMORY, totalResourceUsage.getSummemory())
                  .set(NODE_POOL_AGGREGATED.MAXCPU, totalResourceUsage.getMaxcpu())
                  .set(NODE_POOL_AGGREGATED.MAXMEMORY, totalResourceUsage.getMaxmemory())
                  .set(NODE_POOL_AGGREGATED.UPDATEDAT, OffsetDateTime.now(ZONE_OFFSET)));
  }

  private static OffsetDateTime fromEpochMilli(long epochMilli) {
    return OffsetDateTime.ofInstant(Instant.ofEpochMilli(epochMilli), ZONE_OFFSET);
  }

  @RetryOnException(retryCount = RETRY_COUNT, sleepDurationInMilliseconds = SLEEP_DURATION)
  private static void insertOne(@NonNull InsertFinalStep<? extends Record> finalStep) {
    finalStep.execute();
  }
}
