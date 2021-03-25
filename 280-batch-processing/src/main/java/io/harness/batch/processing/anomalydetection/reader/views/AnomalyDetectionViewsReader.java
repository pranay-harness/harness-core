package io.harness.batch.processing.anomalydetection.reader.views;

import static io.harness.ccm.billing.preaggregated.PreAggregatedBillingDataHelper.getNumericValue;

import io.harness.batch.processing.anomalydetection.AnomalyDetectionConstants;
import io.harness.batch.processing.anomalydetection.AnomalyDetectionTimeSeries;
import io.harness.batch.processing.anomalydetection.TimeSeriesMetaData;
import io.harness.batch.processing.ccm.CCMJobConstants;
import io.harness.ccm.anomaly.entities.EntityType;
import io.harness.ccm.anomaly.entities.TimeGranularity;
import io.harness.ccm.billing.bigquery.BigQueryService;
import io.harness.ccm.views.entities.CEView;
import io.harness.ccm.views.entities.ViewField;
import io.harness.ccm.views.entities.ViewFieldIdentifier;
import io.harness.ccm.views.entities.ViewTimeGranularity;
import io.harness.ccm.views.entities.ViewVisualization;
import io.harness.ccm.views.graphql.QLCESortOrder;
import io.harness.ccm.views.graphql.QLCEView;
import io.harness.ccm.views.graphql.QLCEViewAggregateOperation;
import io.harness.ccm.views.graphql.QLCEViewAggregation;
import io.harness.ccm.views.graphql.QLCEViewFieldInput;
import io.harness.ccm.views.graphql.QLCEViewFilterWrapper;
import io.harness.ccm.views.graphql.QLCEViewMetadataFilter;
import io.harness.ccm.views.graphql.QLCEViewSortCriteria;
import io.harness.ccm.views.graphql.QLCEViewSortType;
import io.harness.ccm.views.graphql.QLCEViewTimeFilter;
import io.harness.ccm.views.graphql.QLCEViewTimeFilterOperator;
import io.harness.ccm.views.graphql.ViewsMetaDataFields;
import io.harness.ccm.views.service.CEViewService;
import io.harness.ccm.views.service.ViewsBillingService;
import io.harness.exception.InvalidArgumentsException;

import com.google.cloud.Timestamp;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.FieldList;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.Schema;
import com.google.cloud.bigquery.TableResult;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.stream.Collectors;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Autowired;

public class AnomalyDetectionViewsReader implements ItemReader<AnomalyDetectionTimeSeries>, StepExecutionListener {
  @Autowired CEViewService viewService;
  @Autowired ViewsBillingService viewsBillingService;
  JobParameters parameters;
  String accountId;
  int viewIndex;

  @Autowired BigQueryService bigQueryService;
  BigQuery bigQuery;

  String cloudProviderTableName;

  QLCEViewFilterWrapper startTimeFilter;
  QLCEViewFilterWrapper endTimeFilter;

  QLCEViewAggregation costAggregation;
  QLCEViewSortCriteria timeSortOrder;

  List<String> viewIds;
  Iterator<String> viewIdsIterator;

  TimeSeriesMetaData timeSeriesMetaData;

  Queue<AnomalyDetectionTimeSeries> queue;

  @Override
  public void beforeStep(StepExecution stepExecution) {
    parameters = stepExecution.getJobExecution().getJobParameters();
    accountId = parameters.getString(CCMJobConstants.ACCOUNT_ID);
    Instant endTime = CCMJobConstants.getFieldValueFromJobParams(parameters, CCMJobConstants.JOB_END_DATE);
    timeSeriesMetaData = TimeSeriesMetaData.builder()
                             .accountId(accountId)
                             .trainStart(endTime.minus(AnomalyDetectionConstants.DAYS_TO_CONSIDER, ChronoUnit.DAYS))
                             .trainEnd(endTime.minus(1, ChronoUnit.DAYS))
                             .testStart(endTime.minus(1, ChronoUnit.DAYS))
                             .testEnd(endTime)
                             .timeGranularity(TimeGranularity.DAILY)
                             .entityType(EntityType.VIEW)
                             .build();

    viewIds = viewService.getAllViews(accountId, false).stream().map(QLCEView::getId).collect(Collectors.toList());
    viewIdsIterator = viewIds.iterator();
    viewIndex = 0;

    startTimeFilter = QLCEViewFilterWrapper.builder()
                          .timeFilter(QLCEViewTimeFilter.builder()
                                          .field(QLCEViewFieldInput.builder()
                                                     .fieldId(ViewsMetaDataFields.START_TIME.getFieldName())
                                                     .fieldName(ViewsMetaDataFields.START_TIME.getFieldName())
                                                     .identifier(ViewFieldIdentifier.COMMON)
                                                     .build())
                                          .operator(QLCEViewTimeFilterOperator.AFTER)
                                          .value(timeSeriesMetaData.getTrainStart().toEpochMilli())
                                          .build())
                          .build();

    endTimeFilter = QLCEViewFilterWrapper.builder()
                        .timeFilter(QLCEViewTimeFilter.builder()
                                        .field(QLCEViewFieldInput.builder()
                                                   .fieldId(ViewsMetaDataFields.START_TIME.getFieldName())
                                                   .fieldName(ViewsMetaDataFields.START_TIME.getFieldName())
                                                   .identifier(ViewFieldIdentifier.COMMON)
                                                   .build())
                                        .operator(QLCEViewTimeFilterOperator.BEFORE)
                                        .value(timeSeriesMetaData.getTestEnd().toEpochMilli())
                                        .build())
                        .build();

    costAggregation = QLCEViewAggregation.builder()
                          .columnName(ViewsMetaDataFields.COST.getFieldName())
                          .operationType(QLCEViewAggregateOperation.SUM)
                          .build();

    timeSortOrder =
        QLCEViewSortCriteria.builder().sortType(QLCEViewSortType.COST).sortOrder(QLCESortOrder.ASCENDING).build();

    //    cloudProviderTableName = cloudBillingHelper.getCloudProviderTableName(accountId, unified);
    cloudProviderTableName = "ccm-play.BillingReport_kmpysmuisimorrjl6nl73w.unifiedTable";
    bigQuery = bigQueryService.get();
    queue = new PriorityQueue<>();
  }

  @Override
  public AnomalyDetectionTimeSeries read() {
    while (queue.isEmpty()) {
      if (viewIdsIterator.hasNext()) {
        try {
          String viewId = viewIdsIterator.next();
          QLCEViewFilterWrapper viewFilter =
              QLCEViewFilterWrapper.builder()
                  .viewMetadataFilter(QLCEViewMetadataFilter.builder().viewId(viewId).isPreview(false).build())
                  .build();
          ViewField groupByField = getDefaultGroupBy(viewId);
          if (groupByField != null) {
            TableResult tableResult = viewsBillingService.getTimeSeriesStats(bigQuery,
                Arrays.asList(startTimeFilter, endTimeFilter, viewFilter), Collections.emptyList(),
                Arrays.asList(costAggregation), Arrays.asList(timeSortOrder), cloudProviderTableName);
            convertAndAllToQueue(tableResult, viewId, groupByField);
          } else {
            throw new InvalidArgumentsException(
                String.format("default groupby is null , skipping viewID :{} ", viewId));
          }
        } catch (Exception e) {
        }
      } else {
        return null;
      }
    }
    return queue.poll();
  }

  public ViewField getDefaultGroupBy(String viewId) {
    CEView ceView = viewService.get(viewId);
    if (ceView.getViewVisualization() != null) {
      ViewVisualization viewVisualization = ceView.getViewVisualization();
      ViewField defaultGroupByField = viewVisualization.getGroupBy();
      // TODO :: for monthly ?? what should data range be ? does algorthim fit for montly data
      ViewTimeGranularity defaultTimeGranularity = viewVisualization.getGranularity();
      if (!defaultTimeGranularity.equals(ViewTimeGranularity.DAY)) {
        throw new InvalidArgumentsException(
            String.format("default time granularity is not equal to DAY , skipping viewID :{} ", viewId));
      }
      return defaultGroupByField;
    }
    return null;
  }

  public void convertAndAllToQueue(TableResult result, String viewId, ViewField groupByField) {
    Schema schema = result.getSchema();
    FieldList fields = schema.getFields();

    Map<String, AnomalyDetectionTimeSeries> timeSeriesMap = new HashMap<>();
    for (FieldValueList row : result.iterateAll()) {
      Timestamp startTimeTruncatedTimestamp = null;
      Double value = null;
      AnomalyDetectionTimeSeries currentTimeSeries = null;
      for (Field field : fields) {
        switch (field.getType().getStandardType()) {
          case TIMESTAMP:
            startTimeTruncatedTimestamp = Timestamp.ofTimeMicroseconds(row.get(field.getName()).getTimestampValue());
            break;
          case STRING:
            String stringValue = fetchStringValue(row, field);
            if (stringValue == null) {
              continue;
            }
            currentTimeSeries = fetchTimeSeries(timeSeriesMap, stringValue, viewId, groupByField);
            break;
          case FLOAT64:
            value = getNumericValue(row, field);
            break;
          default:
            break;
        }
      }
      if (currentTimeSeries != null && value != null && startTimeTruncatedTimestamp != null) {
        currentTimeSeries.insert(startTimeTruncatedTimestamp.toSqlTimestamp().toInstant(), value);
      }
    }
    queue.addAll(timeSeriesMap.values());
  }

  public static String fetchStringValue(FieldValueList row, Field field) {
    Object value = row.get(field.getName()).getValue();
    if (value != null) {
      return value.toString();
    }
    return null; // what happens in null case ?? need to skip row ??
  }

  public AnomalyDetectionTimeSeries fetchTimeSeries(
      Map<String, AnomalyDetectionTimeSeries> timeSeriesMap, String key, String viewId, ViewField groupByField) {
    if (timeSeriesMap.containsKey(key)) {
      return timeSeriesMap.get(key);
    } else {
      AnomalyDetectionTimeSeries timeSeries = AnomalyDetectionTimeSeries.initialiseNewTimeSeries(timeSeriesMetaData);
      timeSeries.setViewId(viewId);
      addMetaInfo(timeSeries, key, groupByField);
      timeSeriesMap.put(key, timeSeries);
      return timeSeries;
    }
  }

  public void addMetaInfo(AnomalyDetectionTimeSeries timeSeries, String key, ViewField groupByField) {
    // fill meta info into anomaly detection time series
    switch (groupByField.getFieldId()) {
      case "awsServicecode":
        timeSeries.setAwsService(key);
        break;
      case "awsUsageAccountId":
        timeSeries.setAwsAccount(key);
        break;
      case "gcpProjectId":
        timeSeries.setGcpProject(key);
        break;
      case "gcpProduct":
        timeSeries.setGcpProduct(key);
        break;
      case "gcpSkuDescription":
        timeSeries.setGcpSKUDescription(key);
        break;
      case "product":
        timeSeries.setProduct(key);
        break;
      default:
        throw new InvalidArgumentsException(
            String.format("could not insert meta data into the field, Skipping viewId : {}", timeSeries.getViewId()));
    }
  }

  @Override
  public ExitStatus afterStep(StepExecution stepExecution) {
    return null;
  }
}
