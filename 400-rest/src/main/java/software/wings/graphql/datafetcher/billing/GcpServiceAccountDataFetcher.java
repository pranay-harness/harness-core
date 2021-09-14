/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.graphql.datafetcher.billing;

import static io.harness.annotations.dev.HarnessTeam.CE;

import static java.util.Collections.singletonList;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.ccm.billing.graphql.GcpServiceAccountQueryArguments;
import io.harness.ccm.config.CEGcpServiceAccountService;
import io.harness.ccm.config.GcpServiceAccount;
import io.harness.ccm.config.GcpServiceAccountDTO;

import software.wings.graphql.datafetcher.AbstractArrayDataFetcher;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.ce.CeAccountExpirationChecker;

import com.google.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@TargetModule(HarnessModule._375_CE_GRAPHQL)
@OwnedBy(CE)
public class GcpServiceAccountDataFetcher
    extends AbstractArrayDataFetcher<GcpServiceAccountDTO, GcpServiceAccountQueryArguments> {
  @Inject CEGcpServiceAccountService ceGcpServiceAccountService;
  @Inject CeAccountExpirationChecker accountChecker;

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  protected List<GcpServiceAccountDTO> fetch(GcpServiceAccountQueryArguments arguments, String accountId) {
    accountChecker.checkIsCeEnabled(accountId);
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
      log.error("Unable to get the default service account.", e);
    }
    return null;
  }

  @Override
  protected GcpServiceAccountDTO unusedReturnTypePassingDummyMethod() {
    return null;
  }
}
