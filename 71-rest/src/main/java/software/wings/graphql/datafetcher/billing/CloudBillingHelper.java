package software.wings.graphql.datafetcher.billing;

import static io.harness.ccm.billing.graphql.CloudEntityGroupBy.cloudProvider;
import static io.harness.ccm.billing.graphql.CloudEntityGroupBy.labelsKey;
import static io.harness.ccm.billing.graphql.CloudEntityGroupBy.labelsValue;
import static java.lang.String.format;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.harness.ccm.billing.dao.BillingDataPipelineRecordDao;
import io.harness.ccm.billing.graphql.CloudBillingFilter;
import io.harness.ccm.billing.graphql.CloudBillingGroupBy;
import io.harness.ccm.setup.config.CESetUpConfig;
import io.harness.exception.InvalidRequestException;
import software.wings.app.MainConfiguration;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Singleton
public class CloudBillingHelper {
  @Inject private MainConfiguration mainConfiguration;
  @Inject private BillingDataPipelineRecordDao billingDataPipelineRecordDao;

  private static final String preAggregated = "preAggregated";
  private static final String awsRawTable = "awscur*";
  private static final String gcpRawTable = "gcp_billing_export*";

  private Cache<String, String> billingDataPipelineRecordCache =
      Caffeine.newBuilder().expireAfterWrite(24, TimeUnit.HOURS).build();

  public String geDataPipelineMetadata(String accountId) {
    return billingDataPipelineRecordCache.get(accountId, key -> getDataSetId(key));
  }

  public String getDataSetId(String accountId) {
    return billingDataPipelineRecordDao.fetchBillingPipelineMetaDataFromAccountId(accountId).getDataSetId();
  }

  public String getCloudProviderTableName(String accountId) {
    return getCloudProviderTableName(accountId, preAggregated);
  }

  public String getCloudProviderTableName(String accountId, String tableName) {
    CESetUpConfig ceSetUpConfig = mainConfiguration.getCeSetUpConfig();
    String projectId = ceSetUpConfig.getGcpProjectId();
    String dataSetId = geDataPipelineMetadata(accountId);
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
    boolean labelsFilterPresent = filters.stream().anyMatch(
        billingFilter -> billingFilter.getLabelsValue() != null || billingFilter.getLabelsKey() != null);

    boolean labelsGroupByPresent = groupByList.stream().anyMatch(billingGroupBy
        -> billingGroupBy.getEntityGroupBy() == labelsKey || billingGroupBy.getEntityGroupBy() == labelsValue);

    return labelsFilterPresent || labelsGroupByPresent;
  }
}
