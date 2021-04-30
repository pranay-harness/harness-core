package io.harness.ng.core.event;

import static io.harness.NGConstants.DEFAULT_ORG_IDENTIFIER;
import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.ng.core.user.UserMembershipUpdateSource.SYSTEM;

import static java.util.Collections.emptyMap;

import io.harness.account.AccountClient;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.Scope;
import io.harness.ng.core.AccountOrgProjectValidator;
import io.harness.ng.core.dto.OrganizationDTO;
import io.harness.ng.core.entities.Organization;
import io.harness.ng.core.services.OrganizationService;
import io.harness.ng.core.user.service.NgUserService;
import io.harness.remote.client.RestClientUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PL)
@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class DefaultOrganizationManager {
  public static final String ORGANIZATION_ADMIN = "_organization_admin";
  private final OrganizationService organizationService;
  private final AccountOrgProjectValidator accountOrgProjectValidator;
  private final AccountClient accountClient;
  private final NgUserService ngUserService;

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
    Organization createdOrg = organizationService.create(accountIdentifier, createOrganizationDTO);
    assignAdmins(createdOrg);
  }

  private void assignAdmins(Organization org) {
    List<String> admins = RestClientUtils.getResponse(accountClient.getAccountAdmins(org.getAccountIdentifier()));
    Scope scope =
        Scope.builder().accountIdentifier(org.getAccountIdentifier()).orgIdentifier(org.getIdentifier()).build();
    admins.forEach(admin -> ngUserService.addUserToScope(admin, scope, ORGANIZATION_ADMIN, SYSTEM));
  }

  public boolean deleteDefaultOrganization(String accountIdentifier) {
    return organizationService.delete(accountIdentifier, DEFAULT_ORG_IDENTIFIER, null);
  }
}
