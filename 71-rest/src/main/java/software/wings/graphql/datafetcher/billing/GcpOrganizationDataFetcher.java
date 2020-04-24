package software.wings.graphql.datafetcher.billing;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.inject.Inject;

import io.harness.ccm.billing.graphql.GcpOrganizationQueryArguments;
import io.harness.ccm.config.GcpOrganization;
import io.harness.ccm.config.GcpOrganizationService;
import software.wings.graphql.datafetcher.AbstractArrayDataFetcher;

import java.util.Collections;
import java.util.List;

public class GcpOrganizationDataFetcher
    extends AbstractArrayDataFetcher<GcpOrganization, GcpOrganizationQueryArguments> {
  @Inject GcpOrganizationService gcpOrganizationService;

  @Override
  protected List<GcpOrganization> fetch(GcpOrganizationQueryArguments arguments, String accountId) {
    String uuid = arguments.getUuid();
    List<GcpOrganization> gcpOrganizations;
    if (isNotEmpty(uuid)) {
      gcpOrganizations = Collections.singletonList(gcpOrganizationService.get(uuid));
    } else {
      gcpOrganizations = gcpOrganizationService.list(accountId);
    }
    return gcpOrganizations;
  }

  @Override
  protected GcpOrganization unusedReturnTypePassingDummyMethod() {
    return null;
  }
}