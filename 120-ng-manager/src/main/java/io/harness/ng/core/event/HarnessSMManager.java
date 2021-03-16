package io.harness.ng.core.event;

import static io.harness.NGConstants.HARNESS_SECRET_MANAGER_IDENTIFIER;
import static io.harness.ng.NextGenModule.CONNECTOR_DECORATOR_SERVICE;
import static io.harness.ng.core.utils.NGUtils.getConnectorRequestDTO;
import static io.harness.ng.core.utils.NGUtils.getDefaultHarnessSecretManagerName;

import io.harness.connector.ConnectorDTO;
import io.harness.connector.services.ConnectorService;
import io.harness.ng.core.AccountOrgProjectValidator;
import io.harness.ng.core.DefaultOrganization;
import io.harness.ng.core.OrgIdentifier;
import io.harness.ng.core.ProjectIdentifier;
import io.harness.ng.core.api.NGSecretManagerService;
import io.harness.secretmanagerclient.dto.SecretManagerConfigDTO;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class HarnessSMManager {
  private final NGSecretManagerService ngSecretManagerService;
  private final ConnectorService secretManagerConnectorService;
  private final AccountOrgProjectValidator accountOrgProjectValidator;

  @Inject
  public HarnessSMManager(NGSecretManagerService ngSecretManagerService,
      @Named(CONNECTOR_DECORATOR_SERVICE) ConnectorService secretManagerConnectorService,
      AccountOrgProjectValidator accountOrgProjectValidator) {
    this.ngSecretManagerService = ngSecretManagerService;
    this.secretManagerConnectorService = secretManagerConnectorService;
    this.accountOrgProjectValidator = accountOrgProjectValidator;
  }

  @DefaultOrganization
  public void createHarnessSecretManager(
      String accountIdentifier, @OrgIdentifier String orgIdentifier, @ProjectIdentifier String projectIdentifier) {
    if (isHarnessSecretManagerPresent(accountIdentifier, orgIdentifier, projectIdentifier)) {
      log.info(String.format(
          "Harness Secret Manager for accountIdentifier %s, orgIdentifier %s and projectIdentifier %s already present",
          accountIdentifier, orgIdentifier, projectIdentifier));
      return;
    }
    if (!accountOrgProjectValidator.isPresent(accountIdentifier, orgIdentifier, projectIdentifier)) {
      log.info(String.format(
          "Parent entity with accountIdentifier %s, orgIdentifier %s and projectIdentifier %s does not exist, skipping creation of Harness Secret Manager",
          accountIdentifier, orgIdentifier, projectIdentifier));
      return;
    }
    SecretManagerConfigDTO globalSecretManager = ngSecretManagerService.getGlobalSecretManager(accountIdentifier);
    globalSecretManager.setIdentifier(HARNESS_SECRET_MANAGER_IDENTIFIER);
    globalSecretManager.setName(getDefaultHarnessSecretManagerName(globalSecretManager.getEncryptionType()));
    globalSecretManager.setProjectIdentifier(projectIdentifier);
    globalSecretManager.setOrgIdentifier(orgIdentifier);
    globalSecretManager.setDefault(true);
    ConnectorDTO connectorDTO = getConnectorRequestDTO(globalSecretManager, true);
    secretManagerConnectorService.create(connectorDTO, accountIdentifier);
  }

  @DefaultOrganization
  private boolean isHarnessSecretManagerPresent(
      String accountIdentifier, @OrgIdentifier String orgIdentifier, @ProjectIdentifier String projectIdentifier) {
    return secretManagerConnectorService
        .get(accountIdentifier, orgIdentifier, projectIdentifier, HARNESS_SECRET_MANAGER_IDENTIFIER)
        .isPresent();
  }

  @DefaultOrganization
  public boolean deleteHarnessSecretManager(
      String accountIdentifier, @OrgIdentifier String orgIdentifier, @ProjectIdentifier String projectIdentifier) {
    return secretManagerConnectorService.delete(
        accountIdentifier, orgIdentifier, projectIdentifier, HARNESS_SECRET_MANAGER_IDENTIFIER);
  }
}
