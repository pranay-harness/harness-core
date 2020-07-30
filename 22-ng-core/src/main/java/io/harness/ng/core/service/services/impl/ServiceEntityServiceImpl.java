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
      throw new DuplicateFieldException(
          String.format("Service [%s] under Project[%s], Organization [%s] already exists",
              serviceEntity.getIdentifier(), serviceEntity.getProjectIdentifier(), serviceEntity.getOrgIdentifier()),
          USER_SRE, ex);
    }
  }

  @Override
  public Optional<ServiceEntity> get(
      String accountId, String orgIdentifier, String projectIdentifier, String serviceIdentifier) {
    return serviceRepository.findByAccountIdAndOrgIdentifierAndProjectIdentifierAndIdentifier(
        accountId, orgIdentifier, projectIdentifier, serviceIdentifier);
  }

  @Override
  public ServiceEntity update(@Valid ServiceEntity requestService) {
    validatePresenceOfRequiredFields(requestService.getAccountId(), requestService.getOrgIdentifier(),
        requestService.getProjectIdentifier(), requestService.getIdentifier());
    setName(requestService);
    Criteria criteria = getServiceEqualityCriteria(requestService);
    UpdateResult updateResult = serviceRepository.update(criteria, requestService);
    if (!updateResult.wasAcknowledged() || updateResult.getModifiedCount() != 1) {
      throw new InvalidRequestException(String.format(
          "Service [%s] under Project[%s], Organization [%s] couldn't be updated or doesn't exist.",
          requestService.getIdentifier(), requestService.getProjectIdentifier(), requestService.getOrgIdentifier()));
    }
    return requestService;
  }

  @Override
  public ServiceEntity upsert(@Valid ServiceEntity requestService) {
    validatePresenceOfRequiredFields(requestService.getAccountId(), requestService.getOrgIdentifier(),
        requestService.getProjectIdentifier(), requestService.getIdentifier());
    setName(requestService);
    Criteria criteria = getServiceEqualityCriteria(requestService);
    UpdateResult upsertResult = serviceRepository.upsert(criteria, requestService);
    if (!upsertResult.wasAcknowledged()) {
      throw new InvalidRequestException(String.format(
          "Service [%s] under Project[%s], Organization [%s] couldn't be upserted.", requestService.getIdentifier(),
          requestService.getProjectIdentifier(), requestService.getOrgIdentifier()));
    }
    return requestService;
  }

  @Override
  public Page<ServiceEntity> list(@NotNull Criteria criteria, @NotNull Pageable pageable) {
    return serviceRepository.findAll(criteria, pageable);
  }

  @Override
  public boolean delete(String accountId, String orgIdentifier, String projectIdentifier, String serviceIdentifier) {
    serviceRepository.deleteByAccountIdAndOrgIdentifierAndProjectIdentifierAndIdentifier(
        accountId, orgIdentifier, projectIdentifier, serviceIdentifier);
    return true;
  }

  private void setName(ServiceEntity requestService) {
    if (isEmpty(requestService.getName())) {
      requestService.setName(requestService.getIdentifier());
    }
  }

  private Criteria getServiceEqualityCriteria(@Valid ServiceEntity requestService) {
    return Criteria.where(ServiceEntityKeys.accountId)
        .is(requestService.getAccountId())
        .and(ServiceEntityKeys.orgIdentifier)
        .is(requestService.getOrgIdentifier())
        .and(ServiceEntityKeys.projectIdentifier)
        .is(requestService.getProjectIdentifier())
        .and(ServiceEntityKeys.identifier)
        .is(requestService.getIdentifier());
  }
}
