/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.core;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.remote.client.RestClientUtils.getResponse;

import io.harness.account.AccountClient;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.services.OrganizationService;
import io.harness.ng.core.services.ProjectService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PL)
@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class AccountOrgProjectValidator {
  private final OrganizationService organizationService;
  private final ProjectService projectService;
  private final AccountClient accountClient;

  @DefaultOrganization
  public boolean isPresent(
      String accountIdentifier, @OrgIdentifier String orgIdentifier, @ProjectIdentifier String projectIdentifier) {
    if (isEmpty(accountIdentifier)) {
      return true;
    } else if (isEmpty(orgIdentifier)) {
      try {
        return getResponse(accountClient.getAccountDTO(accountIdentifier)) != null;
      } catch (InvalidRequestException exception) {
        log.error(String.format("Account with accountIdentifier %s not found", accountIdentifier));
        return false;
      }
    } else if (isEmpty(projectIdentifier)) {
      return organizationService.get(accountIdentifier, orgIdentifier).isPresent();
    } else {
      return projectService.get(accountIdentifier, orgIdentifier, projectIdentifier).isPresent();
    }
  }
}
