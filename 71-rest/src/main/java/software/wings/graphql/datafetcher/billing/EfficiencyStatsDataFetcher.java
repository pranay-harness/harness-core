package software.wings.graphql.datafetcher.billing;

import static software.wings.graphql.datafetcher.billing.BillingDataQueryMetadata.BillingDataMetaDataFields.APPID;
import static software.wings.graphql.datafetcher.billing.BillingDataQueryMetadata.BillingDataMetaDataFields.CLUSTERID;
import static software.wings.graphql.datafetcher.billing.BillingDataQueryMetadata.BillingDataMetaDataFields.ENVID;
import static software.wings.graphql.datafetcher.billing.BillingDataQueryMetadata.BillingDataMetaDataFields.NAMESPACE;
import static software.wings.graphql.datafetcher.billing.BillingDataQueryMetadata.BillingDataMetaDataFields.SERVICEID;
import static software.wings.graphql.datafetcher.billing.BillingDataQueryMetadata.BillingDataMetaDataFields.WORKLOADNAME;

import com.google.inject.Inject;

import io.harness.exception.InvalidRequestException;
import io.harness.timescaledb.TimeScaleDBService;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Account;
import software.wings.graphql.datafetcher.AbstractStatsDataFetcherWithAggregationList;
import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.graphql.schema.type.aggregation.QLIdOperator;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingDataFilter;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingDataFilterType;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingSortCriteria;
import software.wings.graphql.schema.type.aggregation.billing.QLCCMGroupBy;
import software.wings.graphql.schema.type.aggregation.billing.QLContextInfo;
import software.wings.graphql.schema.type.aggregation.billing.QLEfficiencyStatsData;
import software.wings.graphql.schema.type.aggregation.billing.QLResourceStatsInfo;
import software.wings.graphql.schema.type.aggregation.billing.QLStatsBreakdownInfo;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.validation.constraints.NotNull;

@Slf4j
public class EfficiencyStatsDataFetcher extends AbstractStatsDataFetcherWithAggregationList<QLCCMAggregationFunction,
    QLBillingDataFilter, QLCCMGroupBy, QLBillingSortCriteria> {
  @Inject IdleCostTrendStatsDataFetcher idleCostTrendStatsDataFetcher;
  @Inject private TimeScaleDBService timeScaleDBService;
  @Inject BillingDataHelper billingDataHelper;
  @Inject QLBillingStatsHelper billingStatsHelper;

  private static final String CPU_CONSTANT = "CPU";
  private static final String MEMORY_CONSTANT = "MEMORY";
  private static final String TOTAL_COST_DESCRIPTION = "Total Cost between %s - %s";
  private static int idleCostBaseline = 30;
  private static int unallocatedCostBaseline = 5;

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  protected QLData fetch(String accountId, List<QLCCMAggregationFunction> aggregateFunction,
      List<QLBillingDataFilter> filters, List<QLCCMGroupBy> groupBy, List<QLBillingSortCriteria> sort) {
    try {
      if (timeScaleDBService.isValid()) {
        return getData(accountId, aggregateFunction, filters);
      } else {
        throw new InvalidRequestException("Cannot process request");
      }
    } catch (Exception e) {
      throw new InvalidRequestException(
          "Error while connecting to the TimeScale DB in EfficiencyStats Data Fetcher", e);
    }
  }

  protected QLEfficiencyStatsData getData(
      @NotNull String accountId, List<QLCCMAggregationFunction> aggregateFunction, List<QLBillingDataFilter> filters) {
    boolean isClusterView = checkIfClusterFilterIsPresent(filters);
    QLIdleCostData costData = idleCostTrendStatsDataFetcher.getIdleCostData(accountId, aggregateFunction, filters);
    QLUnallocatedCost unallocatedCost =
        idleCostTrendStatsDataFetcher.getUnallocatedCostData(accountId, aggregateFunction, filters);
    QLStatsBreakdownInfo costStats =
        getCostBreakdown(costData, unallocatedCost, checkIfClusterFilterIsPresent(filters));
    List<QLResourceStatsInfo> resourceStatsInfo = getResourceStats(costData, unallocatedCost, isClusterView);
    QLContextInfo contextInfo = getContextInfo(costStats, filters, accountId);

    return QLEfficiencyStatsData.builder()
        .context(contextInfo)
        .efficiencyBreakdown(costStats)
        .resourceBreakdown(resourceStatsInfo)
        .build();
  }

  private boolean checkIfClusterFilterIsPresent(List<QLBillingDataFilter> filters) {
    boolean isClusterFilter = false;
    boolean isNamespaceOrWorkloadNameOrTaskIdOrCloudServiceNameFilter = false;
    for (QLBillingDataFilter filter : filters) {
      if (filter.getCluster() != null) {
        isClusterFilter = true;
      }
      if (filter.getWorkloadName() != null || filter.getNamespace() != null || filter.getTaskId() != null
          || filter.getCloudServiceName() != null) {
        isNamespaceOrWorkloadNameOrTaskIdOrCloudServiceNameFilter = true;
      }
    }
    return isClusterFilter && !isNamespaceOrWorkloadNameOrTaskIdOrCloudServiceNameFilter;
  }

  private QLStatsBreakdownInfo getCostBreakdown(
      QLIdleCostData costData, QLUnallocatedCost unallocatedCostData, boolean isClusterView) {
    double totalCost = billingDataHelper.getRoundedDoubleValue(checkForNullValues(costData.getTotalCost()));
    double idleCost = billingDataHelper.getRoundedDoubleValue(checkForNullValues(costData.getIdleCost()));
    double unallocatedCost = validateUnallocatedCost(unallocatedCostData);
    if (isClusterView) {
      idleCost -= unallocatedCost;
    }
    double utilizedCost = billingDataHelper.getRoundedDoubleValue(totalCost - idleCost - unallocatedCost);
    return QLStatsBreakdownInfo.builder()
        .total(totalCost)
        .idle(idleCost)
        .unallocated(unallocatedCost)
        .utilized(utilizedCost)
        .build();
  }

  private List<QLResourceStatsInfo> getResourceStats(
      QLIdleCostData costData, QLUnallocatedCost unallocatedCostData, boolean isClusterView) {
    double cpuTotal = billingDataHelper.getRoundedDoubleValue(checkForNullValues(costData.getTotalCpuCost()));
    double cpuIdleCost = billingDataHelper.getRoundedDoubleValue(checkForNullValues(costData.getCpuIdleCost()));
    double cpuUnallocated = validateCpuUnallocatedCost(unallocatedCostData);
    if (isClusterView) {
      cpuIdleCost -= cpuUnallocated;
    }
    double cpuUtilizedCost = cpuTotal - cpuIdleCost - cpuUnallocated;

    double memoryTotal = billingDataHelper.getRoundedDoubleValue(checkForNullValues(costData.getTotalMemoryCost()));
    double memoryIdleCost = billingDataHelper.getRoundedDoubleValue(checkForNullValues(costData.getMemoryIdleCost()));
    double memoryUnallocated = validateMemoryUnallocatedCost(unallocatedCostData);
    if (isClusterView) {
      memoryIdleCost -= memoryUnallocated;
    }
    double memoryUtilizedCost = memoryTotal - memoryIdleCost - memoryUnallocated;

    QLStatsBreakdownInfo cpuInfo = QLStatsBreakdownInfo.builder()
                                       .idle(billingDataHelper.getRoundedDoubleValue(cpuIdleCost / cpuTotal))
                                       .unallocated(billingDataHelper.getRoundedDoubleValue(cpuUnallocated / cpuTotal))
                                       .utilized(billingDataHelper.getRoundedDoubleValue(cpuUtilizedCost / cpuTotal))
                                       .build();
    QLStatsBreakdownInfo memInfo =
        QLStatsBreakdownInfo.builder()
            .idle(billingDataHelper.getRoundedDoubleValue(memoryIdleCost / memoryTotal))
            .unallocated(billingDataHelper.getRoundedDoubleValue(memoryUnallocated / memoryTotal))
            .utilized(billingDataHelper.getRoundedDoubleValue(memoryUtilizedCost / memoryTotal))
            .build();

    List<QLResourceStatsInfo> resourceStatsInfoList = new ArrayList<>();
    resourceStatsInfoList.add(QLResourceStatsInfo.builder().type(CPU_CONSTANT).info(cpuInfo).build());
    resourceStatsInfoList.add(QLResourceStatsInfo.builder().type(MEMORY_CONSTANT).info(memInfo).build());
    return resourceStatsInfoList;
  }

  private BigDecimal checkForNullValues(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value;
  }

  private double validateUnallocatedCost(QLUnallocatedCost unallocatedCostData) {
    if (unallocatedCostData.getUnallocatedCost() != null) {
      return billingDataHelper.getRoundedDoubleValue(unallocatedCostData.getUnallocatedCost());
    }
    return 0.0;
  }

  private double validateCpuUnallocatedCost(QLUnallocatedCost unallocatedCostData) {
    if (unallocatedCostData.getCpuUnallocatedCost() != null) {
      return billingDataHelper.getRoundedDoubleValue(unallocatedCostData.getCpuUnallocatedCost());
    }
    return 0.0;
  }

  private double validateMemoryUnallocatedCost(QLUnallocatedCost unallocatedCostData) {
    if (unallocatedCostData.getMemoryUnallocatedCost() != null) {
      return billingDataHelper.getRoundedDoubleValue(unallocatedCostData.getMemoryUnallocatedCost());
    }
    return 0.0;
  }

  private QLContextInfo getContextInfo(
      QLStatsBreakdownInfo costStats, List<QLBillingDataFilter> filters, String accountId) {
    String startTime = billingDataHelper.getTotalCostFormattedDate(
        Instant.ofEpochMilli(billingDataHelper.getStartTimeFilter(filters).getValue().longValue()));
    String endTime = billingDataHelper.getTotalCostFormattedDate(
        Instant.ofEpochMilli(billingDataHelper.getEndTimeFilter(filters).getValue().longValue()));

    String totalCostDescription = String.format(TOTAL_COST_DESCRIPTION, startTime, endTime);

    int efficiencyScore = calculateEfficiencyScore(costStats);

    return QLContextInfo.builder()
        .contextName(getContextNameFromFilter(filters, accountId))
        .efficiencyScore(efficiencyScore < 100 ? efficiencyScore : 100)
        .totalCost(costStats.getTotal())
        .totalCostDescription(totalCostDescription)
        .build();
  }

  private int calculateEfficiencyScore(QLStatsBreakdownInfo costStats) {
    int utilizedBaseline = 100 - idleCostBaseline - unallocatedCostBaseline;
    double utilized = costStats.getUtilized().doubleValue();
    double total = costStats.getTotal().doubleValue();
    double utilizedPercentage = utilized / total * 100;
    return (int) ((1 - ((utilizedBaseline - utilizedPercentage) / utilizedBaseline)) * 100);
  }

  private String getContextNameFromFilter(List<QLBillingDataFilter> filters, String accountId) {
    for (QLBillingDataFilter filter : filters) {
      Set<QLBillingDataFilterType> filterTypes = QLBillingDataFilter.getFilterTypes(filter);
      for (QLBillingDataFilterType type : filterTypes) {
        switch (type) {
          case Cluster:
            if (filter.getCluster().getOperator() == QLIdOperator.NOT_NULL) {
              return getAccountName(accountId);
            }
            return billingStatsHelper.getEntityName(CLUSTERID, filter.getCluster().getValues()[0]);
          case Namespace:
            return billingStatsHelper.getEntityName(NAMESPACE, filter.getNamespace().getValues()[0]);
          case WorkloadName:
            return billingStatsHelper.getEntityName(WORKLOADNAME, filter.getWorkloadName().getValues()[0]);
          case Application:
            if (filter.getApplication().getOperator() == QLIdOperator.NOT_NULL) {
              return getAccountName(accountId);
            }
            return billingStatsHelper.getEntityName(APPID, filter.getApplication().getValues()[0]);
          case Service:
            return billingStatsHelper.getEntityName(SERVICEID, filter.getService().getValues()[0]);
          case Environment:
            return billingStatsHelper.getEntityName(ENVID, filter.getEnvironment().getValues()[0]);
          default:
            continue;
        }
      }
    }
    return null;
  }

  private String getAccountName(String accountId) {
    Account account = wingsPersistence.get(Account.class, accountId);
    return account.getAccountName();
  }

  @Override
  protected QLData postFetch(String accountId, List<QLCCMGroupBy> groupByList,
      List<QLCCMAggregationFunction> aggregationFunctions, QLData qlData) {
    return null;
  }

  @Override
  public String getEntityType() {
    return null;
  }
}
