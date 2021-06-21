package io.harness.ng.serviceaccounts.service.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.events.ServiceAccountCreateEvent;
import io.harness.ng.core.events.ServiceAccountDeleteEvent;
import io.harness.ng.core.events.ServiceAccountUpdateEvent;
import io.harness.ng.serviceaccounts.dto.ServiceAccountRequestDTO;
import io.harness.ng.serviceaccounts.entities.ServiceAccount;
import io.harness.ng.serviceaccounts.service.ServiceAccountDTOMapper;
import io.harness.ng.serviceaccounts.service.api.ServiceAccountService;
import io.harness.outbox.api.OutboxService;
import io.harness.repositories.ng.serviceaccounts.ServiceAccountRepository;
import io.harness.serviceaccount.ServiceAccountDTO;

import com.google.inject.Inject;
import com.hazelcast.util.Preconditions;
import groovy.util.logging.Slf4j;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@OwnedBy(PL)
public class ServiceAccountServiceImpl implements ServiceAccountService {
  @Inject private ServiceAccountRepository serviceAccountRepository;
  @Inject private OutboxService outboxService;

  @Override
  public ServiceAccountDTO createServiceAccount(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, ServiceAccountRequestDTO requestDTO) {
    ServiceAccount existingAccount =
        serviceAccountRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndIdentifier(
            accountIdentifier, orgIdentifier, projectIdentifier, requestDTO.getIdentifier());
    Preconditions.checkState(existingAccount == null,
        "Duplicate service account with identifier " + requestDTO.getIdentifier() + " in scope");
    ServiceAccount serviceAccount = ServiceAccount.builder()
                                        .accountIdentifier(accountIdentifier)
                                        .orgIdentifier(orgIdentifier)
                                        .projectIdentifier(projectIdentifier)
                                        .name(requestDTO.getName())
                                        .identifier(requestDTO.getIdentifier())
                                        .description(requestDTO.getDescription())
                                        .build();
    serviceAccount = serviceAccountRepository.save(serviceAccount);
    ServiceAccountDTO savedDTO = ServiceAccountDTOMapper.getDTOFromServiceAccount(serviceAccount);
    outboxService.save(new ServiceAccountCreateEvent(savedDTO));
    return savedDTO;
  }

  @Override
  public ServiceAccountDTO updateServiceAccount(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String identifier, ServiceAccountRequestDTO requestDTO) {
    ServiceAccount serviceAccount =
        serviceAccountRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndIdentifier(
            accountIdentifier, orgIdentifier, projectIdentifier, identifier);
    Preconditions.checkNotNull(serviceAccount, "Service account with identifier: " + identifier + " doesn't exist");
    ServiceAccountDTO oldDTO = ServiceAccountDTOMapper.getDTOFromServiceAccount(serviceAccount);
    serviceAccount.setName(requestDTO.getName());
    serviceAccount.setDescription(requestDTO.getDescription());
    serviceAccount = serviceAccountRepository.save(serviceAccount);
    ServiceAccountDTO newDTO = ServiceAccountDTOMapper.getDTOFromServiceAccount(serviceAccount);
    outboxService.save(new ServiceAccountUpdateEvent(oldDTO, newDTO));
    return newDTO;
  }

  @Override
  public boolean deleteServiceAccount(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier) {
    ServiceAccount serviceAccount =
        serviceAccountRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndIdentifier(
            accountIdentifier, orgIdentifier, projectIdentifier, identifier);
    ServiceAccountDTO oldDTO = ServiceAccountDTOMapper.getDTOFromServiceAccount(serviceAccount);
    Preconditions.checkNotNull(serviceAccount, "Service account with identifier: " + identifier + " doesn't exist");
    long deleted = serviceAccountRepository.deleteByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndIdentifier(
        accountIdentifier, orgIdentifier, projectIdentifier, identifier);
    if (deleted > 0) {
      outboxService.save(new ServiceAccountDeleteEvent(oldDTO));
      return true;
    } else {
      return false;
    }
  }

  @Override
  public ServiceAccountDTO getServiceAccountDTO(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier) {
    ServiceAccount serviceAccount =
        serviceAccountRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndIdentifier(
            accountIdentifier, orgIdentifier, projectIdentifier, identifier);
    Preconditions.checkNotNull(serviceAccount, "Service account with identifier" + identifier + " doesn't exist");
    return ServiceAccountDTOMapper.getDTOFromServiceAccount(serviceAccount);
  }

  @Override
  public List<ServiceAccountDTO> listServiceAccounts(
      String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    List<ServiceAccount> serviceAccounts =
        serviceAccountRepository.findAllByAccountIdentifierAndOrgIdentifierAndProjectIdentifier(
            accountIdentifier, orgIdentifier, projectIdentifier);
    List<ServiceAccountDTO> serviceAccountDTOS = new ArrayList<>();
    if (isNotEmpty(serviceAccounts)) {
      serviceAccounts.forEach(
          serviceAccount -> serviceAccountDTOS.add(ServiceAccountDTOMapper.getDTOFromServiceAccount(serviceAccount)));
    }
    return serviceAccountDTOS;
  }
}
