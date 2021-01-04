package io.harness.ng.core.event;

import static io.harness.NGConstants.DEFAULT_ORG_IDENTIFIER;

import static java.util.Collections.emptyMap;

import io.harness.ng.core.dto.OrganizationDTO;
import io.harness.ng.core.services.OrganizationService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class DefaultOrganizationManager {
  private final OrganizationService organizationService;

  @Inject
  public DefaultOrganizationManager(OrganizationService organizationService) {
    this.organizationService = organizationService;
  }

  public void createDefaultOrganization(String accountIdentifier) {
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
