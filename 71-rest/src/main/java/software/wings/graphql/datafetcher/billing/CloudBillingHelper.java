package software.wings.graphql.datafetcher.billing;

import static java.lang.String.format;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.ccm.billing.graphql.CloudBillingFilter;
import io.harness.ccm.setup.config.CESetUpConfig;
import io.harness.exception.InvalidRequestException;
import software.wings.app.MainConfiguration;

import java.util.List;
import java.util.Optional;

@Singleton
public class CloudBillingHelper {
  @Inject private MainConfiguration mainConfiguration;

  public String getCloudProviderTableName(List<CloudBillingFilter> filters) {
    CESetUpConfig ceSetUpConfig = mainConfiguration.getCeSetUpConfig();
    String projectId = ceSetUpConfig.getGcpProjectId();
    String datasetId = "BillingReport_830767422336"; // billing
    String tableName = "preAggregation"; // gcp_preaggregate
    return format("%s.%s.%s", projectId, datasetId, tableName);
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
