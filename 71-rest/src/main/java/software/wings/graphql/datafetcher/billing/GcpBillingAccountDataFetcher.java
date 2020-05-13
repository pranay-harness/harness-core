package software.wings.graphql.datafetcher.billing;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.inject.Inject;

import io.harness.ccm.billing.graphql.GcpBillingAccountQueryArguments;
import io.harness.ccm.config.GcpBillingAccount;
import io.harness.ccm.config.GcpBillingAccountDTO;
import io.harness.ccm.config.GcpBillingAccountService;
import lombok.extern.slf4j.Slf4j;
import software.wings.graphql.datafetcher.AbstractArrayDataFetcher;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class GcpBillingAccountDataFetcher
    extends AbstractArrayDataFetcher<GcpBillingAccountDTO, GcpBillingAccountQueryArguments> {
  @Inject GcpBillingAccountService gcpBillingAccountService;

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  protected List<GcpBillingAccountDTO> fetch(GcpBillingAccountQueryArguments arguments, String accountId) {
    String uuid = arguments.getUuid();
    String organizationSettingId = arguments.getOrganizationSettingId();

    List<GcpBillingAccount> gcpBillingAccounts = new ArrayList<>();
    if (isNotEmpty(uuid)) {
      gcpBillingAccounts.add(gcpBillingAccountService.get(uuid));
    } else {
      gcpBillingAccounts.addAll(gcpBillingAccountService.list(accountId, organizationSettingId));
    }
    return gcpBillingAccounts.stream()
        .map(gcpBillingAccount
            -> GcpBillingAccountDTO.builder()
                   .uuid(gcpBillingAccount.getUuid())
                   .accountId(gcpBillingAccount.getAccountId())
                   .organizationSettingId(gcpBillingAccount.getOrganizationSettingId())
                   .gcpBillingAccountId(gcpBillingAccount.getGcpBillingAccountId())
                   .gcpBillingAccountName(gcpBillingAccount.getGcpBillingAccountName())
                   .exportEnabled(gcpBillingAccount.isExportEnabled())
                   .bqProjectId(gcpBillingAccount.getBqProjectId())
                   .bqDatasetId(gcpBillingAccount.getBqDatasetId())
                   .build())
        .collect(Collectors.toList());
  }

  @Override
  protected GcpBillingAccountDTO unusedReturnTypePassingDummyMethod() {
    return null;
  }
}
