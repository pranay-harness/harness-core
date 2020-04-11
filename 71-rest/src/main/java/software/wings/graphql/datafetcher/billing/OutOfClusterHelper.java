package software.wings.graphql.datafetcher.billing;

import io.harness.ccm.billing.graphql.OutOfClusterBillingFilter;
import io.harness.exception.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

@Slf4j
public class OutOfClusterHelper {
  public String getCloudProviderTableName(List<OutOfClusterBillingFilter> filters) {
    return "ccm-play.BillingReport_830767422336.preAggregation_" + getCloudProvider(filters);
  }

  public String getCloudProvider(List<OutOfClusterBillingFilter> filters) {
    Optional<OutOfClusterBillingFilter> cloudProviderFilter =
        filters.stream().filter(billingFilter -> billingFilter.getCloudProvider() != null).findFirst();

    if (cloudProviderFilter.isPresent()) {
      return (String) cloudProviderFilter.get().getCloudProvider().getValues()[0];
    } else {
      throw new InvalidRequestException("Cloud Provider cannot be null");
    }
  }
}
