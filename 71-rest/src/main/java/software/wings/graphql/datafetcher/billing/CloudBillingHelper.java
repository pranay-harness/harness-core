package software.wings.graphql.datafetcher.billing;

import static io.harness.ccm.billing.graphql.CloudEntityGroupBy.cloudProvider;
import static io.harness.ccm.billing.graphql.CloudEntityGroupBy.labelsKey;
import static io.harness.ccm.billing.graphql.CloudEntityGroupBy.labelsValue;
import static io.harness.ccm.billing.graphql.CloudEntityGroupBy.tagsKey;
import static io.harness.ccm.billing.graphql.CloudEntityGroupBy.tagsValue;
import static java.lang.String.format;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.healthmarketscience.sqlbuilder.Condition;
import com.healthmarketscience.sqlbuilder.CustomSql;
import com.healthmarketscience.sqlbuilder.SqlObject;
import io.harness.ccm.billing.dao.BillingDataPipelineRecordDao;
import io.harness.ccm.billing.entities.BillingDataPipelineRecord;
import io.harness.ccm.billing.graphql.CloudBillingAggregate;
import io.harness.ccm.billing.graphql.CloudBillingFilter;
import io.harness.ccm.billing.graphql.CloudBillingGroupBy;
import io.harness.ccm.billing.graphql.CloudBillingIdFilter;
import io.harness.ccm.setup.config.CESetUpConfig;
import io.harness.exception.InvalidRequestException;
import org.jetbrains.annotations.NotNull;
import software.wings.app.MainConfiguration;
import software.wings.graphql.schema.type.aggregation.QLIdOperator;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Singleton
public class CloudBillingHelper {
  @Inject private MainConfiguration mainConfiguration;
  @Inject private BillingDataPipelineRecordDao billingDataPipelineRecordDao;

  private static final String preAggregated = "preAggregated";
  private static final String awsRawTable = "awscur*";
  private static final String gcpRawTable = "gcp_billing_export*";
  private static final String leftJoinTemplate = " LEFT JOIN UNNEST(%s) as %s";
  private static final String tags = "tags";
  private static final String labels = "labels";
  private static final String credits = "credits";

  private Cache<String, BillingDataPipelineCacheObject> billingDataPipelineRecordCache =
      Caffeine.newBuilder().expireAfterWrite(24, TimeUnit.HOURS).build();

  public BillingDataPipelineCacheObject getDataPipelineMetadata(String accountId) {
    return billingDataPipelineRecordCache.get(accountId, key -> getBillingDataPipelineCacheObject(key));
  }

  public BillingDataPipelineCacheObject getBillingDataPipelineCacheObject(String accountId) {
    BillingDataPipelineRecord billingDataPipelineRecord =
        billingDataPipelineRecordDao.fetchBillingPipelineMetaDataFromAccountId(accountId);
    return BillingDataPipelineCacheObject.builder()
        .dataSetId(billingDataPipelineRecord.getDataSetId())
        .awsLinkedAccountsToExclude(billingDataPipelineRecord.getAwsLinkedAccountsToExclude())
        .build();
  }

  public String getCloudProviderTableName(String accountId) {
    return getCloudProviderTableName(accountId, preAggregated);
  }

  public String getCloudProviderTableName(String accountId, String tableName) {
    CESetUpConfig ceSetUpConfig = mainConfiguration.getCeSetUpConfig();
    String projectId = ceSetUpConfig.getGcpProjectId();
    String dataSetId = getDataPipelineMetadata(accountId).getDataSetId();
    return format("%s.%s.%s", projectId, dataSetId, tableName);
  }

  public String getCloudProvider(List<CloudBillingFilter> filters) {
    Optional<CloudBillingFilter> cloudProviderFilter =
        filters.stream().filter(billingFilter -> billingFilter.getCloudProvider() != null).findFirst();

    if (cloudProviderFilter.isPresent()) {
      return (String) cloudProviderFilter.get().getCloudProvider().getValues()[0];
    } else {
      throw new InvalidRequestException("Cloud Provider cannot be null");
    }
  }

  public String getTableName(String cloudProvider) {
    switch (cloudProvider) {
      case "AWS":
        return awsRawTable;
      case "GCP":
        return gcpRawTable;
      default:
        throw new InvalidRequestException("Invalid Cloud Provider");
    }
  }

  public SqlObject getLeftJoin(String cloudProvider) {
    switch (cloudProvider) {
      case "AWS":
        return new CustomSql(String.format(leftJoinTemplate, tags, tags));
      case "GCP":
        return new CustomSql(String.format(leftJoinTemplate, labels, labels));
      default:
        throw new InvalidRequestException("Invalid Cloud Provider");
    }
  }

  public List<CloudBillingFilter> removeAndReturnCloudProviderFilter(List<CloudBillingFilter> filters) {
    return filters.stream()
        .filter(billingFilter -> billingFilter.getCloudProvider() == null)
        .collect(Collectors.toList());
  }

  public List<CloudBillingGroupBy> removeAndReturnCloudProviderGroupBy(List<CloudBillingGroupBy> groupByList) {
    return groupByList.stream()
        .filter(billingGroupBy -> billingGroupBy.getEntityGroupBy() != cloudProvider)
        .collect(Collectors.toList());
  }

  public Boolean fetchIfRawTableQueryRequired(List<CloudBillingFilter> filters, List<CloudBillingGroupBy> groupByList) {
    boolean labelsFilterPresent = filters.stream().anyMatch(billingFilter
        -> billingFilter.getLabelsValue() != null || billingFilter.getLabelsKey() != null
            || billingFilter.getLabels() != null || billingFilter.getTagsValue() != null
            || billingFilter.getTagsKey() != null || billingFilter.getTags() != null);

    boolean labelsGroupByPresent = groupByList.stream().anyMatch(billingGroupBy
        -> billingGroupBy.getEntityGroupBy() == labelsKey || billingGroupBy.getEntityGroupBy() == labelsValue
            || billingGroupBy.getEntityGroupBy() == tagsKey || billingGroupBy.getEntityGroupBy() == tagsValue);

    return labelsFilterPresent || labelsGroupByPresent;
  }

  public Boolean fetchIfDiscountsAggregationPresent(List<CloudBillingAggregate> aggregateFunction) {
    return aggregateFunction.stream().anyMatch(
        aggregation -> aggregation.getColumnName().equals(CloudBillingAggregate.BILLING_GCP_CREDITS));
  }

  @NotNull
  public Function<CloudBillingFilter, Condition> getFiltersMapper(
      boolean isAWSCloudProvider, boolean isQueryRawTableRequired) {
    if (isQueryRawTableRequired) {
      return isAWSCloudProvider ? CloudBillingFilter::toAwsRawTableCondition : CloudBillingFilter::toRawTableCondition;
    } else {
      return CloudBillingFilter::toCondition;
    }
  }

  @NotNull
  protected Function<CloudBillingGroupBy, Object> getGroupByMapper(
      boolean isAWSCloudProvider, boolean isQueryRawTableRequired) {
    if (isQueryRawTableRequired) {
      return isAWSCloudProvider ? CloudBillingGroupBy::toAwsRawTableGroupbyObject
                                : CloudBillingGroupBy::toRawTableGroupbyObject;
    } else {
      return CloudBillingGroupBy::toGroupbyObject;
    }
  }

  @NotNull
  protected Function<CloudBillingAggregate, SqlObject> getAggregationMapper(
      boolean isAWSCloudProvider, boolean isQueryRawTableRequired) {
    if (isQueryRawTableRequired) {
      return isAWSCloudProvider ? CloudBillingAggregate::toAwsRawTableFunctionCall
                                : CloudBillingAggregate::toRawTableFunctionCall;

    } else {
      return CloudBillingAggregate::toFunctionCall;
    }
  }

  public SqlObject getCreditsLeftJoin() {
    return new CustomSql(String.format(leftJoinTemplate, credits, credits));
  }

  public void processAndAddLinkedAccountsFilter(String accountId, List<CloudBillingFilter> filters) {
    String[] linkedAccounts = getLinkedAccounts(accountId);
    if (linkedAccounts != null) {
      CloudBillingFilter cloudBillingFilter = new CloudBillingFilter();
      cloudBillingFilter.setAwsLinkedAccount(
          CloudBillingIdFilter.builder().operator(QLIdOperator.NOT_IN).values(linkedAccounts).build());
      filters.add(cloudBillingFilter);
    }
  }

  private String[] getLinkedAccounts(String accountId) {
    BillingDataPipelineCacheObject dataPipelineMetadata = getDataPipelineMetadata(accountId);
    List<String> awsLinkedAccountsToExclude = dataPipelineMetadata.getAwsLinkedAccountsToExclude();

    if (awsLinkedAccountsToExclude != null && awsLinkedAccountsToExclude.size() > 0) {
      String[] linkedAccounts = new String[awsLinkedAccountsToExclude.size()];
      return awsLinkedAccountsToExclude.toArray(linkedAccounts);
    }
    return null;
  }
}
