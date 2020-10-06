package io.harness.ccm.views.graphql;

import static io.harness.ccm.billing.preaggregated.PreAggregatedBillingDataHelper.convertTimeSeriesPointsMapToList;
import static io.harness.ccm.billing.preaggregated.PreAggregatedBillingDataHelper.fetchStringValue;
import static io.harness.ccm.billing.preaggregated.PreAggregatedBillingDataHelper.getNumericValue;
import static software.wings.graphql.datafetcher.billing.CloudBillingHelper.unified;

import com.google.cloud.Timestamp;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.FieldList;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.Schema;
import com.google.cloud.bigquery.TableResult;
import com.google.inject.Inject;

import graphql.schema.DataFetchingEnvironment;
import io.harness.ccm.billing.bigquery.BigQueryService;
import io.harness.ccm.views.service.ViewsBillingService;
import software.wings.graphql.datafetcher.AbstractStatsDataFetcherWithAggregationListAndLimit;
import software.wings.graphql.datafetcher.billing.CloudBillingHelper;
import software.wings.graphql.schema.type.aggregation.QLBillingDataPoint;
import software.wings.graphql.schema.type.aggregation.QLBillingDataPoint.QLBillingDataPointBuilder;
import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.graphql.schema.type.aggregation.QLReference;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ViewTimeSeriesStatsDataFetcher
    extends AbstractStatsDataFetcherWithAggregationListAndLimit<QLCEViewAggregation, QLCEViewFilterWrapper,
        QLCEViewGroupBy, QLCEViewSortCriteria> {
  @Inject ViewsBillingService viewsBillingService;
  @Inject CloudBillingHelper cloudBillingHelper;
  @Inject BigQueryService bigQueryService;

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  protected QLData fetch(String accountId, List<QLCEViewAggregation> aggregateFunction,
      List<QLCEViewFilterWrapper> filters, List<QLCEViewGroupBy> groupBy, List<QLCEViewSortCriteria> sort,
      Integer limit, Integer offset) {
    String cloudProviderTableName = cloudBillingHelper.getCloudProviderTableName(accountId, unified);
    BigQuery bigQuery = bigQueryService.get();

    return convertToQLViewTimeSeriesData(viewsBillingService.getTimeSeriesStats(
        bigQuery, filters, groupBy, aggregateFunction, sort, cloudProviderTableName));
  }

  public QLViewTimeSeriesData convertToQLViewTimeSeriesData(TableResult result) {
    Schema schema = result.getSchema();
    FieldList fields = schema.getFields();

    Map<Timestamp, List<QLBillingDataPoint>> timeSeriesDataPointsMap = new LinkedHashMap();
    for (FieldValueList row : result.iterateAll()) {
      QLBillingDataPointBuilder billingDataPointBuilder = QLBillingDataPoint.builder();
      Timestamp startTimeTruncatedTimestamp = null;
      Double value = Double.valueOf(0);
      for (Field field : fields) {
        switch (field.getType().getStandardType()) {
          case TIMESTAMP:
            startTimeTruncatedTimestamp = Timestamp.ofTimeMicroseconds(row.get(field.getName()).getTimestampValue());
            break;
          case STRING:
            String stringValue = fetchStringValue(row, field);
            billingDataPointBuilder.key(
                QLReference.builder().id(stringValue).name(stringValue).type(field.getName()).build());
            break;
          case FLOAT64:
            value += getNumericValue(row, field);
            break;
          default:
            break;
        }
      }

      billingDataPointBuilder.value(getRoundedDoubleValue(value));
      List<QLBillingDataPoint> dataPoints = new ArrayList<>();
      if (timeSeriesDataPointsMap.containsKey(startTimeTruncatedTimestamp)) {
        dataPoints = timeSeriesDataPointsMap.get(startTimeTruncatedTimestamp);
      }
      dataPoints.add(billingDataPointBuilder.build());
      timeSeriesDataPointsMap.put(startTimeTruncatedTimestamp, dataPoints);
    }

    return QLViewTimeSeriesData.builder().stats(convertTimeSeriesPointsMapToList(timeSeriesDataPointsMap)).build();
  }

  private double getRoundedDoubleValue(double value) {
    return Math.round(value * 100D) / 100D;
  }

  @Override
  protected QLData postFetch(String accountId, List<QLCEViewGroupBy> groupByList,
      List<QLCEViewAggregation> aggregations, List<QLCEViewSortCriteria> sort, QLData qlData, Integer limit,
      boolean includeOthers) {
    return null;
  }

  @Override
  protected QLData fetchSelectedFields(String accountId, List<QLCEViewAggregation> aggregateFunction,
      List<QLCEViewFilterWrapper> filters, List<QLCEViewGroupBy> groupBy, List<QLCEViewSortCriteria> sort,
      Integer limit, Integer offset, DataFetchingEnvironment dataFetchingEnvironment) {
    return null;
  }

  @Override
  public String getEntityType() {
    return null;
  }
}
