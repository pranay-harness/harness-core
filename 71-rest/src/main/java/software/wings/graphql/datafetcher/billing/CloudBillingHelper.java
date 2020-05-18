package software.wings.graphql.datafetcher.billing;

import static java.lang.String.format;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.harness.ccm.billing.graphql.CloudBillingFilter;
import io.harness.ccm.cluster.dao.BillingDataPipelineRecordDao;
import io.harness.ccm.setup.config.CESetUpConfig;
import io.harness.exception.InvalidRequestException;
import software.wings.app.MainConfiguration;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Singleton
public class CloudBillingHelper {
  @Inject private MainConfiguration mainConfiguration;
  @Inject private BillingDataPipelineRecordDao billingDataPipelineRecordDao;

  private Cache<String, String> billingDataPipelineRecordCache =
      Caffeine.newBuilder().expireAfterWrite(24, TimeUnit.HOURS).build();

  public String geDataPipelineMetadata(String accountId) {
    return billingDataPipelineRecordCache.get(accountId, key -> getDataSetId(key));
  }

  public String getDataSetId(String accountId) {
    return billingDataPipelineRecordDao.fetchBillingPipelineMetaDataFromAccountId(accountId).getDataSetId();
  }

  public String getCloudProviderTableName(String accountId) {
    CESetUpConfig ceSetUpConfig = mainConfiguration.getCeSetUpConfig();
    String projectId = ceSetUpConfig.getGcpProjectId();
    String dataSetId = geDataPipelineMetadata(accountId);
    String tableName = "preAggregated";
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
}
