package io.harness.batch.processing.anomalydetection.reader.k8s;

import io.harness.batch.processing.anomalydetection.AnomalyDetectionConstants;
import io.harness.batch.processing.anomalydetection.K8sQueryMetaData;
import io.harness.batch.processing.anomalydetection.TimeSeriesMetaData;
import io.harness.batch.processing.anomalydetection.types.EntityType;
import io.harness.batch.processing.anomalydetection.types.TimeGranularity;
import io.harness.batch.processing.ccm.CCMJobConstants;

import software.wings.graphql.datafetcher.billing.QLCCMAggregateOperation;
import software.wings.graphql.datafetcher.billing.QLCCMAggregationFunction;
import software.wings.graphql.schema.type.aggregation.QLSortOrder;
import software.wings.graphql.schema.type.aggregation.QLTimeFilter;
import software.wings.graphql.schema.type.aggregation.QLTimeOperator;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingDataFilter;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingSortCriteria;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingSortType;
import software.wings.graphql.schema.type.aggregation.billing.QLCCMEntityGroupBy;

import com.healthmarketscience.sqlbuilder.dbspec.basic.DbColumn;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepExecution;

@Slf4j
public class AnomalyDetectionNamespaceTimescaleReader extends AnomalyDetectionTimescaleReader {
  @Override
  public void beforeStep(StepExecution stepExecution) {
    parameters = stepExecution.getJobExecution().getJobParameters();
    accountId = parameters.getString(CCMJobConstants.ACCOUNT_ID);
    Instant endTime = CCMJobConstants.getFieldValueFromJobParams(parameters, CCMJobConstants.JOB_END_DATE);

    List<DbColumn> selectColumns = new ArrayList<>();
    List<QLCCMAggregationFunction> aggregationList = new ArrayList<>();
    List<QLBillingDataFilter> filtersList = new ArrayList<>();
    List<QLCCMEntityGroupBy> groupByList = new ArrayList<>();
    List<QLBillingSortCriteria> sortCriteria = new ArrayList<>();

    K8sQueryMetaData k8sQueryMetaData = K8sQueryMetaData.builder()
                                            .accountId(accountId)
                                            .filtersList(filtersList)
                                            .aggregationList(aggregationList)
                                            .sortCriteria(sortCriteria)
                                            .groupByList(groupByList)
                                            .selectColumns(selectColumns)
                                            .build();

    TimeSeriesMetaData timeSeriesMetaData =
        TimeSeriesMetaData.builder()
            .accountId(accountId)
            .trainStart(endTime.minus(AnomalyDetectionConstants.DAYS_TO_CONSIDER, ChronoUnit.DAYS))
            .trainEnd(endTime.minus(1, ChronoUnit.DAYS))
            .testStart(endTime.minus(1, ChronoUnit.DAYS))
            .testEnd(endTime)
            .timeGranularity(TimeGranularity.DAILY)
            .entityType(EntityType.NAMESPACE)
            .k8sQueryMetaData(k8sQueryMetaData)
            .build();

    // cost aggreation
    aggregationList.add(QLCCMAggregationFunction.builder()
                            .operationType(QLCCMAggregateOperation.SUM)
                            .columnName(tableSchema.getBillingAmount().toString())
                            .build());

    // filters
    // Start Time
    filtersList.add(QLBillingDataFilter.builder()
                        .startTime(QLTimeFilter.builder()
                                       .operator(QLTimeOperator.AFTER)
                                       .value(timeSeriesMetaData.getTrainStart().toEpochMilli())
                                       .build())
                        .build());

    // End Time
    filtersList.add(QLBillingDataFilter.builder()
                        .endTime(QLTimeFilter.builder()
                                     .operator(QLTimeOperator.BEFORE)
                                     .value(timeSeriesMetaData.getTestEnd().toEpochMilli())
                                     .build())
                        .build());

    // groupby
    groupByList.add(QLCCMEntityGroupBy.Cluster);
    groupByList.add(QLCCMEntityGroupBy.ClusterName);
    groupByList.add(QLCCMEntityGroupBy.Namespace);
    groupByList.add(QLCCMEntityGroupBy.StartTime);

    // sort criteria
    sortCriteria.add(
        QLBillingSortCriteria.builder().sortType(QLBillingSortType.Cluster).sortOrder(QLSortOrder.ASCENDING).build());
    sortCriteria.add(
        QLBillingSortCriteria.builder().sortType(QLBillingSortType.Namespace).sortOrder(QLSortOrder.ASCENDING).build());
    sortCriteria.add(
        QLBillingSortCriteria.builder().sortType(QLBillingSortType.Time).sortOrder(QLSortOrder.ASCENDING).build());

    log.info("Anomaly Detection batch job of type : {} , time granularity : {}, for accountId : {} , endtime : {}",
        EntityType.CLUSTER.toString(), TimeGranularity.DAILY.toString(), accountId, endTime.toString());

    listAnomalyDetectionTimeSeries = dataService.readData(timeSeriesMetaData);

    log.info("successfully read {} no of {}", listAnomalyDetectionTimeSeries.size(),
        timeSeriesMetaData.getEntityType().toString());
    timeSeriesIndex = 0;
  }
}
