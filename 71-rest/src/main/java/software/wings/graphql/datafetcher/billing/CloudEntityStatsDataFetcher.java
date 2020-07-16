package software.wings.graphql.datafetcher.billing;

import com.google.inject.Inject;

import com.healthmarketscience.sqlbuilder.SqlObject;
import graphql.schema.DataFetchingEnvironment;
import io.harness.ccm.billing.graphql.CloudBillingAggregate;
import io.harness.ccm.billing.graphql.CloudBillingFilter;
import io.harness.ccm.billing.graphql.CloudBillingGroupBy;
import io.harness.ccm.billing.graphql.CloudBillingSortCriteria;
import io.harness.ccm.billing.preaggregated.PreAggregateBillingService;
import software.wings.graphql.datafetcher.AbstractStatsDataFetcherWithAggregationListAndLimit;
import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CloudEntityStatsDataFetcher
    extends AbstractStatsDataFetcherWithAggregationListAndLimit<CloudBillingAggregate, CloudBillingFilter,
        CloudBillingGroupBy, CloudBillingSortCriteria> {
  @Inject PreAggregateBillingService preAggregateBillingService;
  @Inject CloudBillingHelper cloudBillingHelper;

  private static final String startTimeColumnNameConst = "startTime";

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  protected QLData fetch(String accountId, List<CloudBillingAggregate> aggregateFunction,
      List<CloudBillingFilter> filters, List<CloudBillingGroupBy> groupByList, List<CloudBillingSortCriteria> sort,
      Integer limit, Integer offset) {
    String cloudProvider = cloudBillingHelper.getCloudProvider(filters);
    boolean isAWSCloudProvider = cloudProvider.equals("AWS");
    boolean isQueryRawTableRequired = cloudBillingHelper.fetchIfRawTableQueryRequired(filters, groupByList);
    SqlObject leftJoin = null;
    String queryTableName;
    if (isQueryRawTableRequired) {
      String tableName = cloudBillingHelper.getTableName(cloudBillingHelper.getCloudProvider(filters));
      queryTableName = cloudBillingHelper.getCloudProviderTableName(accountId, tableName);
      filters = cloudBillingHelper.removeAndReturnCloudProviderFilter(filters);
      groupByList = cloudBillingHelper.removeAndReturnCloudProviderGroupBy(groupByList);
      leftJoin = cloudBillingHelper.getLeftJoin(cloudProvider);
    } else {
      queryTableName = cloudBillingHelper.getCloudProviderTableName(accountId);
    }
    aggregateFunction.add(CloudBillingAggregate.builder()
                              .operationType(QLCCMAggregateOperation.MIN)
                              .columnName(startTimeColumnNameConst)
                              .build());
    aggregateFunction.add(CloudBillingAggregate.builder()
                              .operationType(QLCCMAggregateOperation.MAX)
                              .columnName(startTimeColumnNameConst)
                              .build());
    return preAggregateBillingService.getPreAggregateBillingEntityStats(accountId,
        Optional.ofNullable(aggregateFunction)
            .map(Collection::stream)
            .orElseGet(Stream::empty)
            .map(cloudBillingHelper.getAggregationMapper(isAWSCloudProvider, isQueryRawTableRequired))
            .collect(Collectors.toList()),
        Optional.ofNullable(groupByList)
            .map(Collection::stream)
            .orElseGet(Stream::empty)
            .map(cloudBillingHelper.getGroupByMapper(isAWSCloudProvider, isQueryRawTableRequired))
            .collect(Collectors.toList()),
        Optional.ofNullable(filters)
            .map(Collection::stream)
            .orElseGet(Stream::empty)
            .map(cloudBillingHelper.getFiltersMapper(isAWSCloudProvider, isQueryRawTableRequired))
            .collect(Collectors.toList()),
        Optional.ofNullable(sort)
            .map(Collection::stream)
            .orElseGet(Stream::empty)
            .map(CloudBillingSortCriteria::toOrderObject)
            .collect(Collectors.toList()),
        queryTableName, filters, leftJoin);
  }

  @Override
  protected QLData postFetch(String accountId, List<CloudBillingGroupBy> groupByList,
      List<CloudBillingAggregate> aggregations, List<CloudBillingSortCriteria> sort, QLData qlData, Integer limit,
      boolean includeOthers) {
    return null;
  }

  @Override
  protected QLData fetchSelectedFields(String accountId, List<CloudBillingAggregate> aggregateFunction,
      List<CloudBillingFilter> filters, List<CloudBillingGroupBy> groupBy, List<CloudBillingSortCriteria> sort,
      Integer limit, Integer offset, DataFetchingEnvironment dataFetchingEnvironment) {
    return null;
  }

  @Override
  public String getEntityType() {
    return null;
  }
}
