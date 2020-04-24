package software.wings.graphql.datafetcher.billing;

import com.google.inject.Inject;

import io.harness.ccm.billing.graphql.GcpServiceAccountQueryArguments;
import io.harness.ccm.config.CEGcpServiceAccountService;
import io.harness.ccm.config.GcpServiceAccount;
import software.wings.graphql.datafetcher.AbstractArrayDataFetcher;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;

import java.util.Collections;
import java.util.List;

public class GcpServiceAccountDataFetcher
    extends AbstractArrayDataFetcher<GcpServiceAccount, GcpServiceAccountQueryArguments> {
  @Inject CEGcpServiceAccountService ceGcpServiceAccountService;

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  protected List<GcpServiceAccount> fetch(GcpServiceAccountQueryArguments arguments, String accountId) {
    return Collections.singletonList(ceGcpServiceAccountService.getDefaultServiceAccount(accountId));
  }

  @Override
  protected GcpServiceAccount unusedReturnTypePassingDummyMethod() {
    return null;
  }
}
