package io.harness.ng.core.event;

import static io.harness.NGConstants.DEFAULT_ORG_IDENTIFIER;
import static io.harness.annotations.dev.HarnessTeam.PL;

import static java.util.Collections.emptyMap;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.AccountOrgProjectValidator;
import io.harness.ng.core.dto.OrganizationDTO;
import io.harness.ng.core.services.OrganizationService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PL)
@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class DefaultOrganizationManager {
  private final OrganizationService organizationService;
  private final AccountOrgProjectValidator accountOrgProjectValidator;

  public void createDefaultOrganization(String accountIdentifier) {
    if (accountOrgProjectValidator.isPresent(accountIdentifier, DEFAULT_ORG_IDENTIFIER, null)) {
      log.info(String.format("Default Organization for account %s already present", accountIdentifier));
      return;
    }
    if (!accountOrgProjectValidator.isPresent(accountIdentifier, null, null)) {
      log.info(String.format(
          "Account with accountIdentifier %s not found, skipping creation of Default Organization", accountIdentifier));
      return;
    }
    OrganizationDTO createOrganizationDTO = OrganizationDTO.builder().build();
    createOrganizationDTO.setIdentifier(DEFAULT_ORG_IDENTIFIER);
    createOrganizationDTO.setName("Default");
    createOrganizationDTO.setTags(emptyMap());
    createOrganizationDTO.setDescription("Default Organization");
    createOrganizationDTO.setHarnessManaged(true);
    organizationService.create(accountIdentifier, createOrganizationDTO);
  }

  public boolean deleteDefaultOrganization(String accountIdentifier) {
    return organizationService.delete(accountIdentifier, DEFAULT_ORG_IDENTIFIER, null);
  }
}
