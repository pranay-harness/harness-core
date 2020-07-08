package software.wings.graphql.datafetcher.billing;

import com.google.inject.Inject;

import com.healthmarketscience.sqlbuilder.CustomSql;
import com.healthmarketscience.sqlbuilder.SqlObject;
import io.harness.ccm.billing.graphql.CloudBillingAggregate;
import io.harness.ccm.billing.graphql.CloudBillingFilter;
import io.harness.ccm.billing.graphql.CloudBillingGroupBy;
import io.harness.ccm.billing.graphql.CloudBillingSortCriteria;
import io.harness.ccm.billing.preaggregated.PreAggregateBillingService;
import software.wings.graphql.datafetcher.AbstractStatsDataFetcher;
import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CloudFilterValuesDataFetcher extends AbstractStatsDataFetcher<CloudBillingAggregate, CloudBillingFilter,
    CloudBillingGroupBy, CloudBillingSortCriteria> {
  @Inject PreAggregateBillingService preAggregateBillingService;
  @Inject CloudBillingHelper cloudBillingHelper;

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  protected QLData fetch(String accountId, CloudBillingAggregate aggregateFunction, List<CloudBillingFilter> filters,
      List<CloudBillingGroupBy> groupByList, List<CloudBillingSortCriteria> sort) {
    boolean isQueryRawTableRequired = cloudBillingHelper.fetchIfRawTableQueryRequired(filters, groupByList);
    SqlObject leftJoin = null;
    String queryTableName;
    if (isQueryRawTableRequired) {
      String tableName = cloudBillingHelper.getTableName(cloudBillingHelper.getCloudProvider(filters));
      leftJoin = new CustomSql(" LEFT JOIN UNNEST(labels) as labels");
      queryTableName = cloudBillingHelper.getCloudProviderTableName(accountId, tableName);
      filters = cloudBillingHelper.removeAndReturnCloudProviderFilter(filters);
      groupByList = cloudBillingHelper.removeAndReturnCloudProviderGroupBy(groupByList);
    } else {
      queryTableName = cloudBillingHelper.getCloudProviderTableName(accountId);
    }
    return preAggregateBillingService.getPreAggregateFilterValueStats(accountId,
        Optional.ofNullable(groupByList)
            .map(Collection::stream)
            .orElseGet(Stream::empty)
            .map(isQueryRawTableRequired ? CloudBillingGroupBy::toRawTableGroupbyObject
                                         : CloudBillingGroupBy::toGroupbyObject)
            .collect(Collectors.toList()),
        Optional.ofNullable(filters)
            .map(Collection::stream)
            .orElseGet(Stream::empty)
            .map(isQueryRawTableRequired ? CloudBillingFilter::toRawTableCondition : CloudBillingFilter::toCondition)
            .collect(Collectors.toList()),
        queryTableName, leftJoin);
  }

  @Override
  protected QLData postFetch(String accountId, List<CloudBillingGroupBy> groupByList, QLData qlData) {
    return null;
  }

  @Override
  public String getEntityType() {
    return null;
  }
}
