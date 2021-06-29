package io.harness.ng.serviceaccounts.service.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER_SRE;
import static io.harness.ng.core.utils.NGUtils.validate;
import static io.harness.ng.core.utils.NGUtils.verifyValuesNotChanged;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.DuplicateFieldException;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.AccountOrgProjectValidator;
import io.harness.ng.core.common.beans.NGTag.NGTagKeys;
import io.harness.ng.core.dto.ServiceAccountFilterDTO;
import io.harness.ng.core.entities.Organization.OrganizationKeys;
import io.harness.ng.core.events.ServiceAccountCreateEvent;
import io.harness.ng.core.events.ServiceAccountDeleteEvent;
import io.harness.ng.core.events.ServiceAccountUpdateEvent;
import io.harness.ng.serviceaccounts.dto.ServiceAccountAggregateDTO;
import io.harness.ng.serviceaccounts.entities.ServiceAccount;
import io.harness.ng.serviceaccounts.entities.ServiceAccount.ServiceAccountKeys;
import io.harness.ng.serviceaccounts.service.ServiceAccountDTOMapper;
import io.harness.ng.serviceaccounts.service.api.ServiceAccountService;
import io.harness.outbox.api.OutboxService;
import io.harness.repositories.ng.serviceaccounts.ServiceAccountRepository;
import io.harness.serviceaccount.ServiceAccountDTO;
import io.harness.utils.PageUtils;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.hazelcast.util.Preconditions;
import groovy.util.logging.Slf4j;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

@Slf4j
@OwnedBy(PL)
public class ServiceAccountServiceImpl implements ServiceAccountService {
  @Inject private ServiceAccountRepository serviceAccountRepository;
  @Inject private OutboxService outboxService;
  @Inject private AccountOrgProjectValidator accountOrgProjectValidator;

  @Override
  public ServiceAccountDTO createServiceAccount(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, ServiceAccountDTO requestDTO) {
    validateCreateServiceAccountRequest(accountIdentifier, orgIdentifier, projectIdentifier, requestDTO);
    ServiceAccount serviceAccount = ServiceAccountDTOMapper.getServiceAccountFromDTO(requestDTO);
    validate(serviceAccount);
    try {
      serviceAccount = serviceAccountRepository.save(serviceAccount);
    } catch (DuplicateKeyException ex) {
      throw new DuplicateFieldException(
          String.format("A service account with identifier %s is already present or was deleted in scope",
              requestDTO.getIdentifier()),
          USER_SRE, ex);
    }
    ServiceAccountDTO savedDTO = ServiceAccountDTOMapper.getDTOFromServiceAccount(serviceAccount);
    outboxService.save(new ServiceAccountCreateEvent(savedDTO));
    return savedDTO;
  }

  private void validateCreateServiceAccountRequest(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, ServiceAccountDTO requestDTO) {
    verifyValuesNotChanged(Lists.newArrayList(Pair.of(accountIdentifier, requestDTO.getAccountIdentifier()),
                               Pair.of(orgIdentifier, requestDTO.getOrgIdentifier()),
                               Pair.of(projectIdentifier, requestDTO.getProjectIdentifier())),
        true);
    if (!accountOrgProjectValidator.isPresent(accountIdentifier, orgIdentifier, projectIdentifier)) {
      throw new InvalidArgumentsException(String.format("Project [%s] in Org [%s] and Account [%s] does not exist",
                                              projectIdentifier, orgIdentifier, accountIdentifier),
          USER_SRE);
    }
  }

  @Override
  public ServiceAccountDTO updateServiceAccount(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String identifier, ServiceAccountDTO requestDTO) {
    ServiceAccount serviceAccount =
        serviceAccountRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndIdentifier(
            accountIdentifier, orgIdentifier, projectIdentifier, identifier);
    if (serviceAccount == null) {
      throw new InvalidRequestException(String.format("Service account with identifier: %s doesn't exist", identifier));
    }
    validateUpdateServiceAccountRequest(
        accountIdentifier, orgIdentifier, projectIdentifier, identifier, requestDTO, serviceAccount);
    ServiceAccountDTO oldDTO = ServiceAccountDTOMapper.getDTOFromServiceAccount(serviceAccount);
    serviceAccount.setName(requestDTO.getName());
    serviceAccount.setDescription(requestDTO.getDescription());
    validate(serviceAccount);
    serviceAccount = serviceAccountRepository.save(serviceAccount);
    ServiceAccountDTO newDTO = ServiceAccountDTOMapper.getDTOFromServiceAccount(serviceAccount);
    outboxService.save(new ServiceAccountUpdateEvent(oldDTO, newDTO));
    return newDTO;
  }

  private void validateUpdateServiceAccountRequest(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String identifier, ServiceAccountDTO requestDTO, ServiceAccount serviceAccount) {
    verifyValuesNotChanged(Lists.newArrayList(Pair.of(accountIdentifier, requestDTO.getAccountIdentifier()),
                               Pair.of(orgIdentifier, requestDTO.getOrgIdentifier()),
                               Pair.of(projectIdentifier, requestDTO.getProjectIdentifier()),
                               Pair.of(identifier, requestDTO.getIdentifier())),
        true);
    verifyValuesNotChanged(
        Lists.newArrayList(Pair.of(serviceAccount.getAccountIdentifier(), requestDTO.getAccountIdentifier()),
            Pair.of(serviceAccount.getAccountIdentifier(), requestDTO.getOrgIdentifier()),
            Pair.of(serviceAccount.getProjectIdentifier(), requestDTO.getProjectIdentifier())),
        true);
  }

  @Override
  public boolean deleteServiceAccount(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier) {
    ServiceAccount serviceAccount =
        serviceAccountRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndIdentifier(
            accountIdentifier, orgIdentifier, projectIdentifier, identifier);
    if (serviceAccount == null) {
      throw new InvalidRequestException(String.format("Service account with identifier: %s doesn't exist", identifier));
    }
    ServiceAccountDTO oldDTO = ServiceAccountDTOMapper.getDTOFromServiceAccount(serviceAccount);
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
  public PageResponse<ServiceAccountAggregateDTO> listAggregateServiceAccounts(String accountIdentifier,
      String orgIdentifier, String projectIdentifier, List<String> identifiers, Pageable pageable,
      ServiceAccountFilterDTO filterDTO) {
    Criteria criteria = createServiceAccountFilterCriteria(
        Criteria.where(ServiceAccountKeys.accountIdentifier).is(accountIdentifier), filterDTO);
    Page<ServiceAccount> serviceAccounts = serviceAccountRepository.findAll(criteria, pageable);
    // TODO: Add tokens count and role details in aggregate dto
    return PageUtils.getNGPageResponse(serviceAccounts.map(serviceAccount -> {
      ServiceAccountDTO serviceAccountDTO = ServiceAccountDTOMapper.getDTOFromServiceAccount(serviceAccount);
      return ServiceAccountAggregateDTO.builder()
          .serviceAccount(serviceAccountDTO)
          .createdAt(serviceAccount.getCreatedAt())
          .lastModifiedAt(serviceAccount.getLastModifiedAt())
          .tokensCount(0)
          .roleAssignmentsMetadataDTO(new ArrayList<>())
          .build();
    }));
  }

  @Override
  public List<ServiceAccountDTO> listServiceAccounts(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, List<String> identifiers) {
    List<ServiceAccount> serviceAccounts;
    if (identifiers.isEmpty()) {
      serviceAccounts = serviceAccountRepository.findAllByAccountIdentifierAndOrgIdentifierAndProjectIdentifier(
          accountIdentifier, orgIdentifier, projectIdentifier);
    } else {
      serviceAccounts =
          serviceAccountRepository.findAllByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndIdentifierIsIn(
              accountIdentifier, orgIdentifier, projectIdentifier, identifiers);
    }

    List<ServiceAccountDTO> serviceAccountDTOS = new ArrayList<>();
    if (isNotEmpty(serviceAccounts)) {
      serviceAccounts.forEach(
          serviceAccount -> serviceAccountDTOS.add(ServiceAccountDTOMapper.getDTOFromServiceAccount(serviceAccount)));
    }
    return serviceAccountDTOS;
  }

  private Criteria createServiceAccountFilterCriteria(
      Criteria criteria, ServiceAccountFilterDTO serviceAccountFilterDTO) {
    if (serviceAccountFilterDTO == null) {
      return criteria;
    }
    if (isNotBlank(serviceAccountFilterDTO.getSearchTerm())) {
      criteria.orOperator(Criteria.where(OrganizationKeys.name).regex(serviceAccountFilterDTO.getSearchTerm(), "i"),
          Criteria.where(OrganizationKeys.identifier).regex(serviceAccountFilterDTO.getSearchTerm(), "i"),
          Criteria.where(OrganizationKeys.tags + "." + NGTagKeys.key)
              .regex(serviceAccountFilterDTO.getSearchTerm(), "i"),
          Criteria.where(OrganizationKeys.tags + "." + NGTagKeys.value)
              .regex(serviceAccountFilterDTO.getSearchTerm(), "i"));
    }
    if (Objects.nonNull(serviceAccountFilterDTO.getIdentifiers())
        && !serviceAccountFilterDTO.getIdentifiers().isEmpty()) {
      criteria.and(OrganizationKeys.identifier).in(serviceAccountFilterDTO.getIdentifiers());
    }
    return criteria;
  }
}
