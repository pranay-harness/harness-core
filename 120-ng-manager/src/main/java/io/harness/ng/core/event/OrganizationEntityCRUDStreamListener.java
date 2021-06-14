package io.harness.ng.core.event;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.ACCOUNT_ENTITY;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.ACTION;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.CREATE_ACTION;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.DELETE_ACTION;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.ENTITY_TYPE;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.RESTORE_ACTION;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.consumer.Message;
import io.harness.eventsframework.entity_crud.account.AccountEntityChangeDTO;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.entities.Organization;
import io.harness.ng.core.entities.Organization.OrganizationKeys;
import io.harness.ng.core.services.OrganizationService;
import io.harness.ng.resourcegroup.migration.DefaultResourceGroupCreationService;
import io.harness.resourcegroupclient.remote.ResourceGroupClient;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(PL)
@Slf4j
@Singleton
public class OrganizationEntityCRUDStreamListener implements MessageListener {
  private final OrganizationService organizationService;
  private final DefaultOrganizationManager defaultOrganizationManager;
  private final DefaultResourceGroupCreationService defaultResourceGroupCreationService;

  @Inject
  public OrganizationEntityCRUDStreamListener(OrganizationService organizationService,
      DefaultOrganizationManager defaultOrganizationManager, ResourceGroupClient resourceGroupClient,
      DefaultResourceGroupCreationService defaultResourceGroupCreationService) {
    this.organizationService = organizationService;
    this.defaultOrganizationManager = defaultOrganizationManager;
    this.defaultResourceGroupCreationService = defaultResourceGroupCreationService;
  }

  @Override
  public boolean handleMessage(Message message) {
    if (message != null && message.hasMessage()) {
      Map<String, String> metadataMap = message.getMessage().getMetadataMap();
      if (metadataMap != null && metadataMap.get(ENTITY_TYPE) != null) {
        String entityType = metadataMap.get(ENTITY_TYPE);
        if (ACCOUNT_ENTITY.equals(entityType)) {
          AccountEntityChangeDTO accountEntityChangeDTO;
          try {
            accountEntityChangeDTO = AccountEntityChangeDTO.parseFrom(message.getMessage().getData());
          } catch (InvalidProtocolBufferException e) {
            throw new InvalidRequestException(
                String.format("Exception in unpacking EntityChangeDTO for key %s", message.getId()), e);
          }
          String action = metadataMap.get(ACTION);
          if (action != null) {
            return processAccountEntityChangeEvent(accountEntityChangeDTO, action);
          }
        }
      }
    }
    return true;
  }

  private boolean processAccountEntityChangeEvent(AccountEntityChangeDTO accountEntityChangeDTO, String action) {
    switch (action) {
      case CREATE_ACTION:
        return processAccountCreateEvent(accountEntityChangeDTO);
      case DELETE_ACTION:
        return processAccountDeleteEvent(accountEntityChangeDTO);
      case RESTORE_ACTION:
        return processAccountRestoreEvent(accountEntityChangeDTO);
      default:
    }
    return true;
  }

  private boolean processAccountCreateEvent(AccountEntityChangeDTO accountEntityChangeDTO) {
    defaultOrganizationManager.createDefaultOrganization(accountEntityChangeDTO.getAccountId());
    defaultResourceGroupCreationService.createDefaultResourceGroup(accountEntityChangeDTO.getAccountId(), null, null);
    return true;
  }

  private boolean processAccountDeleteEvent(AccountEntityChangeDTO accountEntityChangeDTO) {
    String accountIdentifier = accountEntityChangeDTO.getAccountId();
    Criteria criteria = Criteria.where(OrganizationKeys.accountIdentifier)
                            .is(accountIdentifier)
                            .and(OrganizationKeys.deleted)
                            .ne(Boolean.TRUE);
    List<Organization> organizations = organizationService.list(criteria);
    AtomicBoolean success = new AtomicBoolean(true);
    organizations.forEach(organization -> {
      if (!organizationService.delete(organization.getAccountIdentifier(), organization.getIdentifier(), null)) {
        log.error(String.format("Delete operation failed for organization with accountIdentifier %s and identifier %s",
            organization.getAccountIdentifier(), organization.getIdentifier()));
        success.set(false);
      }
    });
    if (success.get()) {
      log.info(String.format(
          "Successfully completed deletion for organizations in account with identifier %s", accountIdentifier));
    }
    return success.get();
  }

  private boolean processAccountRestoreEvent(AccountEntityChangeDTO accountEntityChangeDTO) {
    String accountIdentifier = accountEntityChangeDTO.getAccountId();
    Criteria criteria = Criteria.where(OrganizationKeys.accountIdentifier)
                            .is(accountIdentifier)
                            .and(OrganizationKeys.deleted)
                            .is(Boolean.TRUE);
    List<Organization> organizations = organizationService.list(criteria);
    AtomicBoolean success = new AtomicBoolean(true);
    organizations.forEach(organization -> {
      if (!organizationService.restore(organization.getAccountIdentifier(), organization.getIdentifier())) {
        log.error(String.format("Restore operation failed for organization with accountIdentifier %s and identifier %s",
            organization.getAccountIdentifier(), organization.getIdentifier()));
        success.set(false);
      }
    });
    if (success.get()) {
      log.info(String.format(
          "Successfully completed restoration for organizations in account with identifier %s", accountIdentifier));
    }
    return success.get();
  }
}
