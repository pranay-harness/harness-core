package io.harness.ng.core.service.services.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.exception.WingsException.USER_SRE;
import static io.harness.outbox.TransactionOutboxModule.OUTBOX_TRANSACTION_TEMPLATE;
import static io.harness.springdata.TransactionUtils.DEFAULT_TRANSACTION_RETRY_POLICY;

import io.harness.EntityType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.IdentifierRef;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.DuplicateFieldException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.UnexpectedException;
import io.harness.ng.DuplicateKeyExceptionParser;
import io.harness.ng.core.EntityDetail;
import io.harness.ng.core.entitysetupusage.dto.EntitySetupUsageDTO;
import io.harness.ng.core.entitysetupusage.service.EntitySetupUsageService;
import io.harness.ng.core.events.ServiceCreateEvent;
import io.harness.ng.core.events.ServiceDeleteEvent;
import io.harness.ng.core.events.ServiceUpdateEvent;
import io.harness.ng.core.events.ServiceUpsertEvent;
import io.harness.ng.core.service.entity.ServiceEntity;
import io.harness.ng.core.service.entity.ServiceEntity.ServiceEntityKeys;
import io.harness.ng.core.service.services.ServiceEntityService;
import io.harness.outbox.api.OutboxService;
import io.harness.repositories.service.spring.ServiceRepository;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.mongodb.client.result.UpdateResult;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.json.JSONObject;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.transaction.support.TransactionTemplate;

@OwnedBy(HarnessTeam.PIPELINE)
@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class ServiceEntityServiceImpl implements ServiceEntityService {
  private final ServiceRepository serviceRepository;
  private final EntitySetupUsageService entitySetupUsageService;
  private static final String DUP_KEY_EXP_FORMAT_STRING =
      "Service [%s] under Project[%s], Organization [%s] already exists";
  private static final Integer QUERY_PAGE_SIZE = 10000;
  @Inject @Named(OUTBOX_TRANSACTION_TEMPLATE) private TransactionTemplate transactionTemplate;
  private final OutboxService outboxService;
  private final RetryPolicy<Object> transactionRetryPolicy = DEFAULT_TRANSACTION_RETRY_POLICY;

  void validatePresenceOfRequiredFields(Object... fields) {
    Lists.newArrayList(fields).forEach(field -> Objects.requireNonNull(field, "One of the required fields is null."));
  }

  @Override
  public ServiceEntity create(@NotNull @Valid ServiceEntity serviceEntity) {
    try {
      validatePresenceOfRequiredFields(serviceEntity.getAccountId(), serviceEntity.getOrgIdentifier(),
          serviceEntity.getProjectIdentifier(), serviceEntity.getIdentifier());
      setNameIfNotPresent(serviceEntity);
      return Failsafe.with(transactionRetryPolicy).get(() -> transactionTemplate.execute(status -> {
        ServiceEntity service = serviceRepository.save(serviceEntity);
        outboxService.save(ServiceCreateEvent.builder()
                               .accountIdentifier(serviceEntity.getAccountId())
                               .orgIdentifier(serviceEntity.getOrgIdentifier())
                               .projectIdentifier(serviceEntity.getProjectIdentifier())
                               .service(serviceEntity)
                               .build());
        return service;
      }));

    } catch (DuplicateKeyException ex) {
      throw new DuplicateFieldException(String.format(DUP_KEY_EXP_FORMAT_STRING, serviceEntity.getIdentifier(),
                                            serviceEntity.getProjectIdentifier(), serviceEntity.getOrgIdentifier()),
          USER_SRE, ex);
    }
  }

  @Override
  public Optional<ServiceEntity> get(
      String accountId, String orgIdentifier, String projectIdentifier, String serviceIdentifier, boolean deleted) {
    return serviceRepository.findByAccountIdAndOrgIdentifierAndProjectIdentifierAndIdentifierAndDeletedNot(
        accountId, orgIdentifier, projectIdentifier, serviceIdentifier, !deleted);
  }

  @Override
  public ServiceEntity update(@Valid ServiceEntity requestService) {
    validatePresenceOfRequiredFields(requestService.getAccountId(), requestService.getOrgIdentifier(),
        requestService.getProjectIdentifier(), requestService.getIdentifier());
    setNameIfNotPresent(requestService);
    Criteria criteria = getServiceEqualityCriteria(requestService, requestService.getDeleted());
    Optional<ServiceEntity> serviceEntityOptional =
        get(requestService.getAccountId(), requestService.getOrgIdentifier(), requestService.getProjectIdentifier(),
            requestService.getIdentifier(), false);
    if (serviceEntityOptional.isPresent()) {
      ServiceEntity oldService = serviceEntityOptional.get();
      return Failsafe.with(transactionRetryPolicy).get(() -> transactionTemplate.execute(status -> {
        ServiceEntity updatedResult = serviceRepository.update(criteria, requestService);
        if (updatedResult == null) {
          throw new InvalidRequestException(
              String.format("Service [%s] under Project[%s], Organization [%s] couldn't be updated or doesn't exist.",
                  requestService.getIdentifier(), requestService.getProjectIdentifier(),
                  requestService.getOrgIdentifier()));
        }
        outboxService.save(ServiceUpdateEvent.builder()
                               .accountIdentifier(requestService.getAccountId())
                               .orgIdentifier(requestService.getOrgIdentifier())
                               .projectIdentifier(requestService.getProjectIdentifier())
                               .newService(updatedResult)
                               .oldService(oldService)
                               .build());
        return updatedResult;
      }));
    } else {
      throw new InvalidRequestException(String.format(
          "Service [%s] under Project[%s], Organization [%s] doesn't exist.", requestService.getIdentifier(),
          requestService.getProjectIdentifier(), requestService.getOrgIdentifier()));
    }
  }

  @Override
  public ServiceEntity upsert(@Valid ServiceEntity requestService) {
    validatePresenceOfRequiredFields(requestService.getAccountId(), requestService.getOrgIdentifier(),
        requestService.getProjectIdentifier(), requestService.getIdentifier());
    setNameIfNotPresent(requestService);
    Criteria criteria = getServiceEqualityCriteria(requestService, requestService.getDeleted());
    return Failsafe.with(transactionRetryPolicy).get(() -> transactionTemplate.execute(status -> {
      ServiceEntity result = serviceRepository.upsert(criteria, requestService);
      if (result == null) {
        throw new InvalidRequestException(String.format(
            "Service [%s] under Project[%s], Organization [%s] couldn't be upserted.", requestService.getIdentifier(),
            requestService.getProjectIdentifier(), requestService.getOrgIdentifier()));
      }
      outboxService.save(ServiceUpsertEvent.builder()
                             .accountIdentifier(requestService.getAccountId())
                             .orgIdentifier(requestService.getOrgIdentifier())
                             .projectIdentifier(requestService.getProjectIdentifier())
                             .service(requestService)
                             .build());
      return result;
    }));
  }

  @Override
  public Page<ServiceEntity> list(@NotNull Criteria criteria, @NotNull Pageable pageable) {
    return serviceRepository.findAll(criteria, pageable);
  }

  @Override
  public List<ServiceEntity> listRunTimePermission(Criteria criteria) {
    return serviceRepository.findAllRunTimePermission(criteria);
  }

  @Override
  public boolean delete(
      String accountId, String orgIdentifier, String projectIdentifier, String serviceIdentifier, Long version) {
    ServiceEntity serviceEntity = ServiceEntity.builder()
                                      .accountId(accountId)
                                      .orgIdentifier(orgIdentifier)
                                      .projectIdentifier(projectIdentifier)
                                      .identifier(serviceIdentifier)
                                      .version(version)
                                      .build();
    checkThatServiceIsNotReferredByOthers(serviceEntity);
    Criteria criteria = getServiceEqualityCriteria(serviceEntity, false);
    Optional<ServiceEntity> serviceEntityOptional =
        get(accountId, orgIdentifier, projectIdentifier, serviceIdentifier, false);
    if (serviceEntityOptional.isPresent()) {
      return Failsafe.with(transactionRetryPolicy).get(() -> transactionTemplate.execute(status -> {
        UpdateResult updateResult = serviceRepository.delete(criteria);
        if (!updateResult.wasAcknowledged() || updateResult.getModifiedCount() != 1) {
          throw new InvalidRequestException(
              String.format("Service [%s] under Project[%s], Organization [%s] couldn't be deleted.", serviceIdentifier,
                  projectIdentifier, orgIdentifier));
        }
        outboxService.save(ServiceDeleteEvent.builder()
                               .accountIdentifier(accountId)
                               .orgIdentifier(orgIdentifier)
                               .projectIdentifier(projectIdentifier)
                               .service(serviceEntityOptional.get())
                               .build());
        return true;
      }));
    } else {
      throw new InvalidRequestException(
          String.format("Service [%s] under Project[%s], Organization [%s] doesn't exist.", serviceIdentifier,
              projectIdentifier, orgIdentifier));
    }
  }

  private void checkThatServiceIsNotReferredByOthers(ServiceEntity serviceEntity) {
    List<EntityDetail> referredByEntities;
    IdentifierRef identifierRef = IdentifierRef.builder()
                                      .accountIdentifier(serviceEntity.getAccountId())
                                      .orgIdentifier(serviceEntity.getOrgIdentifier())
                                      .projectIdentifier(serviceEntity.getProjectIdentifier())
                                      .identifier(serviceEntity.getIdentifier())
                                      .build();
    try {
      Page<EntitySetupUsageDTO> entitySetupUsageDTOS = entitySetupUsageService.listAllEntityUsage(
          0, 10, serviceEntity.getAccountId(), identifierRef.getFullyQualifiedName(), EntityType.SERVICE, "");
      referredByEntities = entitySetupUsageDTOS.stream()
                               .map(EntitySetupUsageDTO::getReferredByEntity)
                               .collect(Collectors.toCollection(LinkedList::new));
    } catch (Exception ex) {
      log.info("Encountered exception while requesting the Entity Reference records of [{}], with exception",
          serviceEntity.getIdentifier(), ex);
      throw new UnexpectedException(
          "Error while deleting the Service as was not able to check entity reference records.");
    }
    if (EmptyPredicate.isNotEmpty(referredByEntities)) {
      throw new InvalidRequestException(String.format(
          "Could not delete the Service %s as it is referenced by other entities - " + referredByEntities.toString(),
          serviceEntity.getIdentifier()));
    }
  }

  private void setNameIfNotPresent(ServiceEntity requestService) {
    if (isEmpty(requestService.getName())) {
      requestService.setName(requestService.getIdentifier());
    }
  }

  private Criteria getServiceEqualityCriteria(@Valid ServiceEntity requestService, boolean deleted) {
    Criteria criteria = Criteria.where(ServiceEntityKeys.accountId)
                            .is(requestService.getAccountId())
                            .and(ServiceEntityKeys.orgIdentifier)
                            .is(requestService.getOrgIdentifier())
                            .and(ServiceEntityKeys.projectIdentifier)
                            .is(requestService.getProjectIdentifier())
                            .and(ServiceEntityKeys.identifier)
                            .is(requestService.getIdentifier())
                            .and(ServiceEntityKeys.deleted)
                            .is(deleted);
    if (requestService.getVersion() != null) {
      criteria.and(ServiceEntityKeys.version).is(requestService.getVersion());
    }
    return criteria;
  }

  @Override
  public Page<ServiceEntity> bulkCreate(String accountId, @NotNull List<ServiceEntity> serviceEntities) {
    try {
      validateTheServicesList(serviceEntities);
      populateDefaultNameIfNotPresent(serviceEntities);
      List<ServiceEntity> outputServiceEntitiesList = (List<ServiceEntity>) serviceRepository.saveAll(serviceEntities);
      return new PageImpl<>(outputServiceEntitiesList);
    } catch (DuplicateKeyException ex) {
      throw new DuplicateFieldException(getDuplicateServiceExistsErrorMessage(ex.getMessage()), USER_SRE, ex);
    } catch (Exception ex) {
      String serviceNames = serviceEntities.stream().map(ServiceEntity::getName).collect(Collectors.joining(","));
      log.info(
          "Encountered exception while saving the service entity records of [{}], with exception", serviceNames, ex);
      throw new UnexpectedException("Encountered exception while saving the service entity records.");
    }
  }

  @Override
  public List<ServiceEntity> getAllServices(String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    return getAllServices(accountIdentifier, orgIdentifier, projectIdentifier, QUERY_PAGE_SIZE);
  }

  @Override
  public Integer findActiveServicesCountAtGivenTimestamp(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, long timestampInMs) {
    if (timestampInMs <= 0) {
      throw new InvalidRequestException("Invalid timestamp while fetching active services count : " + timestampInMs);
    }
    return serviceRepository
        .findActiveServiceCountAtGivenTimestamp(accountIdentifier, orgIdentifier, projectIdentifier, timestampInMs)
        .intValue();
  }

  @VisibleForTesting
  String getDuplicateServiceExistsErrorMessage(String exceptionString) {
    String errorMessageToBeReturned = null;
    try {
      JSONObject jsonObjectOfDuplicateKey = DuplicateKeyExceptionParser.getDuplicateKey(exceptionString);
      String orgIdentifier = jsonObjectOfDuplicateKey.getString("orgIdentifier");
      String projectIdentifier = jsonObjectOfDuplicateKey.getString("projectIdentifier");
      String identifier = jsonObjectOfDuplicateKey.getString("identifier");
      errorMessageToBeReturned = String.format(DUP_KEY_EXP_FORMAT_STRING, identifier, orgIdentifier, projectIdentifier);
    } catch (Exception ex) {
      errorMessageToBeReturned = "A Duplicate Service already exists";
    }
    return errorMessageToBeReturned;
  }

  @VisibleForTesting
  List<ServiceEntity> getAllServices(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, Integer pageSize) {
    List<ServiceEntity> serviceEntityList = new ArrayList<>();

    Criteria criteria = Criteria.where(ServiceEntityKeys.accountId)
                            .is(accountIdentifier)
                            .and(ServiceEntityKeys.orgIdentifier)
                            .is(orgIdentifier)
                            .and(ServiceEntityKeys.projectIdentifier)
                            .is(projectIdentifier);

    int pageNum = 0;
    // Query in batches of 10k
    while (true) {
      PageRequest pageRequest =
          PageRequest.of(pageNum, pageSize, Sort.by(Sort.Direction.ASC, ServiceEntityKeys.createdAt));
      Page<ServiceEntity> pageResponse = serviceRepository.findAll(criteria, pageRequest);
      if (pageResponse.isEmpty()) {
        break;
      }
      serviceEntityList.addAll(pageResponse.getContent());
      pageNum += 1;
    }

    return serviceEntityList;
  }

  private void validateTheServicesList(List<ServiceEntity> serviceEntities) {
    if (isEmpty(serviceEntities)) {
      return;
    }
    serviceEntities.forEach(serviceEntity
        -> validatePresenceOfRequiredFields(serviceEntity.getAccountId(), serviceEntity.getOrgIdentifier(),
            serviceEntity.getProjectIdentifier(), serviceEntity.getIdentifier()));
  }

  private void populateDefaultNameIfNotPresent(List<ServiceEntity> serviceEntities) {
    if (isEmpty(serviceEntities)) {
      return;
    }
    serviceEntities.forEach(this::setNameIfNotPresent);
  }
}
