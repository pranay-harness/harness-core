package software.wings.graphql.datafetcher.billing;

import io.harness.ccm.billing.graphql.CloudBillingFilter;
import io.harness.exception.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

@Slf4j
public class CloudHelper {
  public String getCloudProviderTableName(List<CloudBillingFilter> filters) {
    return "ccm-play.BillingReport_830767422336.preAggregation_" + getCloudProvider(filters);
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
