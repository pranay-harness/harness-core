package io.harness.ng.core.service.services.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.exception.WingsException.USER_SRE;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.mongodb.client.result.UpdateResult;
import io.harness.exception.DuplicateFieldException;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.service.entity.ServiceEntity;
import io.harness.ng.core.service.entity.ServiceEntity.ServiceEntityKeys;
import io.harness.ng.core.service.respositories.spring.ServiceRepository;
import io.harness.ng.core.service.services.ServiceEntityService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.Objects;
import java.util.Optional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class ServiceEntityServiceImpl implements ServiceEntityService {
  private final ServiceRepository serviceRepository;
  private static final String DUP_KEY_EXP_FORMAT_STRING =
      "Service [%s] under Project[%s], Organization [%s] already exists";

  void validatePresenceOfRequiredFields(Object... fields) {
    Lists.newArrayList(fields).forEach(field -> Objects.requireNonNull(field, "One of the required fields is null."));
  }

  @Override
  public ServiceEntity create(@NotNull @Valid ServiceEntity serviceEntity) {
    try {
      validatePresenceOfRequiredFields(serviceEntity.getAccountId(), serviceEntity.getOrgIdentifier(),
          serviceEntity.getProjectIdentifier(), serviceEntity.getIdentifier());
      setName(serviceEntity);
      return serviceRepository.save(serviceEntity);
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
    try {
      validatePresenceOfRequiredFields(requestService.getAccountId(), requestService.getOrgIdentifier(),
          requestService.getProjectIdentifier(), requestService.getIdentifier());
      setName(requestService);
      Criteria criteria = getServiceEqualityCriteria(requestService, requestService.getDeleted());
      UpdateResult updateResult = serviceRepository.update(criteria, requestService);
      if (!updateResult.wasAcknowledged() || updateResult.getModifiedCount() != 1) {
        throw new InvalidRequestException(String.format(
            "Service [%s] under Project[%s], Organization [%s] couldn't be updated or doesn't exist.",
            requestService.getIdentifier(), requestService.getProjectIdentifier(), requestService.getOrgIdentifier()));
      }
      return requestService;
    } catch (DuplicateKeyException ex) {
      throw new DuplicateFieldException(String.format(DUP_KEY_EXP_FORMAT_STRING, requestService.getIdentifier(),
                                            requestService.getProjectIdentifier(), requestService.getOrgIdentifier()),
          USER_SRE, ex);
    }
  }

  @Override
  public ServiceEntity upsert(@Valid ServiceEntity requestService) {
    try {
      validatePresenceOfRequiredFields(requestService.getAccountId(), requestService.getOrgIdentifier(),
          requestService.getProjectIdentifier(), requestService.getIdentifier());
      setName(requestService);
      Criteria criteria = getServiceEqualityCriteria(requestService, requestService.getDeleted());
      UpdateResult upsertResult = serviceRepository.upsert(criteria, requestService);
      if (!upsertResult.wasAcknowledged()) {
        throw new InvalidRequestException(String.format(
            "Service [%s] under Project[%s], Organization [%s] couldn't be upserted or doesn't exist.",
            requestService.getIdentifier(), requestService.getProjectIdentifier(), requestService.getOrgIdentifier()));
      }
      return requestService;
    } catch (DuplicateKeyException ex) {
      throw new DuplicateFieldException(String.format(DUP_KEY_EXP_FORMAT_STRING, requestService.getIdentifier(),
                                            requestService.getProjectIdentifier(), requestService.getOrgIdentifier()),
          USER_SRE, ex);
    }
  }

  @Override
  public Page<ServiceEntity> list(@NotNull Criteria criteria, @NotNull Pageable pageable) {
    return serviceRepository.findAll(criteria, pageable);
  }

  @Override
  public boolean delete(String accountId, String orgIdentifier, String projectIdentifier, String serviceIdentifier) {
    ServiceEntity serviceEntity = ServiceEntity.builder()
                                      .accountId(accountId)
                                      .orgIdentifier(orgIdentifier)
                                      .projectIdentifier(projectIdentifier)
                                      .identifier(serviceIdentifier)
                                      .build();
    Criteria criteria = getServiceEqualityCriteria(serviceEntity, false);
    UpdateResult updateResult = serviceRepository.delete(criteria);
    if (!updateResult.wasAcknowledged()) {
      throw new InvalidRequestException(
          String.format("Service [%s] under Project[%s], Organization [%s] couldn't be deleted.", serviceIdentifier,
              projectIdentifier, orgIdentifier));
    }
    return true;
  }

  private void setName(ServiceEntity requestService) {
    if (isEmpty(requestService.getName())) {
      requestService.setName(requestService.getIdentifier());
    }
  }

  private Criteria getServiceEqualityCriteria(@Valid ServiceEntity requestService, boolean deleted) {
    return Criteria.where(ServiceEntityKeys.accountId)
        .is(requestService.getAccountId())
        .and(ServiceEntityKeys.orgIdentifier)
        .is(requestService.getOrgIdentifier())
        .and(ServiceEntityKeys.projectIdentifier)
        .is(requestService.getProjectIdentifier())
        .and(ServiceEntityKeys.identifier)
        .is(requestService.getIdentifier())
        .and(ServiceEntityKeys.deleted)
        .is(deleted);
  }
}
