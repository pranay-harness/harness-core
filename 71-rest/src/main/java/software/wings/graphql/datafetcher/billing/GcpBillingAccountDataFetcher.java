package software.wings.graphql.datafetcher.billing;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.inject.Inject;

import io.harness.ccm.billing.graphql.GcpBillingAccountQueryArguments;
import io.harness.ccm.config.GcpBillingAccount;
import io.harness.ccm.config.GcpBillingAccountService;
import software.wings.graphql.datafetcher.AbstractArrayDataFetcher;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;

import java.util.ArrayList;
import java.util.List;

public class GcpBillingAccountDataFetcher
    extends AbstractArrayDataFetcher<GcpBillingAccount, GcpBillingAccountQueryArguments> {
  @Inject GcpBillingAccountService gcpBillingAccountService;

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  protected List<GcpBillingAccount> fetch(GcpBillingAccountQueryArguments arguments, String accountId) {
    String uuid = arguments.getUuid();
    String organizationSettingId = arguments.getOrganizationSettingId();

    List<GcpBillingAccount> gcpBillingAccounts = new ArrayList<>();
    if (isNotEmpty(uuid)) {
      gcpBillingAccounts.add(gcpBillingAccountService.get(uuid));
    } else {
      gcpBillingAccounts.addAll(gcpBillingAccountService.list(accountId, organizationSettingId));
    }
    return gcpBillingAccounts;
  }

  @Override
  protected GcpBillingAccount unusedReturnTypePassingDummyMethod() {
    return null;
  }
}
