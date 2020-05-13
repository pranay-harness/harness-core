package software.wings.graphql.datafetcher.billing;

import static java.util.Collections.singletonList;

import com.google.inject.Inject;

import io.harness.ccm.billing.graphql.GcpServiceAccountQueryArguments;
import io.harness.ccm.config.CEGcpServiceAccountService;
import io.harness.ccm.config.GcpServiceAccount;
import io.harness.ccm.config.GcpServiceAccountDTO;
import lombok.extern.slf4j.Slf4j;
import software.wings.graphql.datafetcher.AbstractArrayDataFetcher;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class GcpServiceAccountDataFetcher
    extends AbstractArrayDataFetcher<GcpServiceAccountDTO, GcpServiceAccountQueryArguments> {
  @Inject CEGcpServiceAccountService ceGcpServiceAccountService;

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  protected List<GcpServiceAccountDTO> fetch(GcpServiceAccountQueryArguments arguments, String accountId) {
    List<GcpServiceAccount> gcpServiceAccounts = null;
    try {
      gcpServiceAccounts = singletonList(ceGcpServiceAccountService.getDefaultServiceAccount(accountId));
      return gcpServiceAccounts.stream()
          .map(gcpServiceAccount
              -> GcpServiceAccountDTO.builder()
                     .serviceAccountId(gcpServiceAccount.getServiceAccountId())
                     .accountId(gcpServiceAccount.getAccountId())
                     .gcpUniqueId(gcpServiceAccount.getGcpUniqueId())
                     .email(gcpServiceAccount.getEmail())
                     .build())
          .collect(Collectors.toList());
    } catch (IOException e) {
      logger.error("Unable to get the default service account.", e);
    }
    return null;
  }

  @Override
  protected GcpServiceAccountDTO unusedReturnTypePassingDummyMethod() {
    return null;
  }
}
