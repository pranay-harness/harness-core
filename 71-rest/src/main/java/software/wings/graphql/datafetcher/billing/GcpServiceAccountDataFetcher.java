package software.wings.graphql.datafetcher.billing;

import com.google.inject.Inject;

import io.harness.ccm.billing.graphql.GcpServiceAccountQueryArguments;
import io.harness.ccm.config.CEGcpServiceAccountService;
import io.harness.ccm.config.GcpServiceAccount;
import io.harness.ccm.config.GcpServiceAccountDTO;
import software.wings.graphql.datafetcher.AbstractArrayDataFetcher;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class GcpServiceAccountDataFetcher
    extends AbstractArrayDataFetcher<GcpServiceAccountDTO, GcpServiceAccountQueryArguments> {
  @Inject CEGcpServiceAccountService ceGcpServiceAccountService;

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  protected List<GcpServiceAccountDTO> fetch(GcpServiceAccountQueryArguments arguments, String accountId) {
    List<GcpServiceAccount> gcpServiceAccounts =
        Collections.singletonList(ceGcpServiceAccountService.getDefaultServiceAccount(accountId));
    return gcpServiceAccounts.stream()
        .map(gcpServiceAccount
            -> GcpServiceAccountDTO.builder()
                   .serviceAccountId(gcpServiceAccount.getServiceAccountId())
                   .accountId(gcpServiceAccount.getAccountId())
                   .gcpUniqueId(gcpServiceAccount.getGcpUniqueId())
                   .email(gcpServiceAccount.getEmail())
                   .build())
        .collect(Collectors.toList());
  }

  @Override
  protected GcpServiceAccountDTO unusedReturnTypePassingDummyMethod() {
    return null;
  }
}
