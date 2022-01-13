/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.billing;

import static io.harness.annotations.dev.HarnessTeam.CE;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.ccm.billing.graphql.GcpBillingAccountQueryArguments;
import io.harness.ccm.config.GcpBillingAccount;
import io.harness.ccm.config.GcpBillingAccountDTO;
import io.harness.ccm.config.GcpBillingAccountService;

import software.wings.graphql.datafetcher.AbstractArrayDataFetcher;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.ce.CeAccountExpirationChecker;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@TargetModule(HarnessModule._375_CE_GRAPHQL)
@OwnedBy(CE)
public class GcpBillingAccountDataFetcher
    extends AbstractArrayDataFetcher<GcpBillingAccountDTO, GcpBillingAccountQueryArguments> {
  @Inject GcpBillingAccountService gcpBillingAccountService;
  @Inject CeAccountExpirationChecker accountChecker;

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  protected List<GcpBillingAccountDTO> fetch(GcpBillingAccountQueryArguments arguments, String accountId) {
    accountChecker.checkIsCeEnabled(accountId);
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
