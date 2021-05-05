package io.harness.ng.core.environment.services.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.eventsframework.EventsFrameworkConstants.ENTITY_CRUD;
import static io.harness.exception.WingsException.USER_SRE;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.EntityType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.IdentifierRef;
import io.harness.data.structure.EmptyPredicate;
import io.harness.eventsframework.EventsFrameworkMetadataConstants;
import io.harness.eventsframework.api.EventsFrameworkDownException;
import io.harness.eventsframework.api.Producer;
import io.harness.eventsframework.entity_crud.EntityChangeDTO;
import io.harness.eventsframework.producer.Message;
import io.harness.exception.DuplicateFieldException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.UnexpectedException;
import io.harness.ng.core.EntityDetail;
import io.harness.ng.core.entitysetupusage.dto.EntitySetupUsageDTO;
import io.harness.ng.core.entitysetupusage.service.EntitySetupUsageService;
import io.harness.ng.core.environment.beans.Environment;
import io.harness.ng.core.environment.beans.Environment.EnvironmentKeys;
import io.harness.ng.core.environment.services.EnvironmentService;
import io.harness.repositories.environment.spring.EnvironmentRepository;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.protobuf.StringValue;
import com.mongodb.DuplicateKeyException;
import com.mongodb.client.result.UpdateResult;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(HarnessTeam.PIPELINE)
@Singleton
@Slf4j
public class EnvironmentServiceImpl implements EnvironmentService {
  private final EnvironmentRepository environmentRepository;
  private final EntitySetupUsageService entitySetupUsageService;
  private final Producer eventProducer;
  private static final String DUP_KEY_EXP_FORMAT_STRING =
      "Environment [%s] under Project[%s], Organization [%s] already exists";

  @Inject
  public EnvironmentServiceImpl(EnvironmentRepository environmentRepository,
      EntitySetupUsageService entitySetupUsageService, @Named(ENTITY_CRUD) Producer eventProducer) {
    this.environmentRepository = environmentRepository;
    this.entitySetupUsageService = entitySetupUsageService;
    this.eventProducer = eventProducer;
  }

  @Override
  public Environment create(@NotNull @Valid Environment environment) {
    try {
      validatePresenceOfRequiredFields(environment.getAccountId(), environment.getOrgIdentifier(),
          environment.getProjectIdentifier(), environment.getIdentifier());
      setName(environment);
      Environment createdEnvironment = environmentRepository.save(environment);
      publishEvent(environment.getAccountId(), environment.getOrgIdentifier(), environment.getProjectIdentifier(),
          environment.getIdentifier(), EventsFrameworkMetadataConstants.CREATE_ACTION);
      return createdEnvironment;
    } catch (DuplicateKeyException ex) {
      throw new DuplicateFieldException(String.format(DUP_KEY_EXP_FORMAT_STRING, environment.getIdentifier(),
                                            environment.getProjectIdentifier(), environment.getOrgIdentifier()),
          USER_SRE, ex);
    }
  }

  @Override
  public Optional<Environment> get(
      String accountId, String orgIdentifier, String projectIdentifier, String environmentIdentifier, boolean deleted) {
    return environmentRepository.findByAccountIdAndOrgIdentifierAndProjectIdentifierAndIdentifierAndDeletedNot(
        accountId, orgIdentifier, projectIdentifier, environmentIdentifier, !deleted);
  }

  @Override
  public Environment update(@Valid Environment requestEnvironment) {
    validatePresenceOfRequiredFields(requestEnvironment.getAccountId(), requestEnvironment.getOrgIdentifier(),
        requestEnvironment.getProjectIdentifier(), requestEnvironment.getIdentifier());
    setName(requestEnvironment);
    Criteria criteria = getEnvironmentEqualityCriteria(requestEnvironment, requestEnvironment.getDeleted());
    Environment updatedResult = environmentRepository.update(criteria, requestEnvironment);
    if (updatedResult == null) {
      throw new InvalidRequestException(
          String.format("Environment [%s] under Project[%s], Organization [%s] couldn't be updated or doesn't exist.",
              requestEnvironment.getIdentifier(), requestEnvironment.getProjectIdentifier(),
              requestEnvironment.getOrgIdentifier()));
    }
    publishEvent(requestEnvironment.getAccountId(), requestEnvironment.getOrgIdentifier(),
        requestEnvironment.getProjectIdentifier(), requestEnvironment.getIdentifier(),
        EventsFrameworkMetadataConstants.UPDATE_ACTION);
    return updatedResult;
  }

  @Override
  public Environment upsert(Environment requestEnvironment) {
    validatePresenceOfRequiredFields(requestEnvironment.getAccountId(), requestEnvironment.getOrgIdentifier(),
        requestEnvironment.getProjectIdentifier(), requestEnvironment.getIdentifier());
    setName(requestEnvironment);
    Criteria criteria = getEnvironmentEqualityCriteria(requestEnvironment, requestEnvironment.getDeleted());
    Environment updatedResult = environmentRepository.upsert(criteria, requestEnvironment);
    if (updatedResult == null) {
      throw new InvalidRequestException(
          String.format("Environment [%s] under Project[%s], Organization [%s] couldn't be upserted or doesn't exist.",
              requestEnvironment.getIdentifier(), requestEnvironment.getProjectIdentifier(),
              requestEnvironment.getOrgIdentifier()));
    }
    publishEvent(requestEnvironment.getAccountId(), requestEnvironment.getOrgIdentifier(),
        requestEnvironment.getProjectIdentifier(), requestEnvironment.getIdentifier(),
        EventsFrameworkMetadataConstants.UPSERT_ACTION);
    return updatedResult;
  }

  @Override
  public Page<Environment> list(Criteria criteria, Pageable pageable) {
    return environmentRepository.findAll(criteria, pageable);
  }

  @Override
  public boolean delete(
      String accountId, String orgIdentifier, String projectIdentifier, String environmentIdentifier, Long version) {
    Environment environment = Environment.builder()
                                  .accountId(accountId)
                                  .orgIdentifier(orgIdentifier)
                                  .projectIdentifier(projectIdentifier)
                                  .identifier(environmentIdentifier)
                                  .version(version)
                                  .build();
    checkThatEnvironmentIsNotReferredByOthers(environment);
    Criteria criteria = getEnvironmentEqualityCriteria(environment, false);
    UpdateResult updateResult = environmentRepository.delete(criteria);
    if (!updateResult.wasAcknowledged() || updateResult.getModifiedCount() != 1) {
      throw new InvalidRequestException(
          String.format("Environment [%s] under Project[%s], Organization [%s] couldn't be deleted.",
              environmentIdentifier, projectIdentifier, orgIdentifier));
    }
    publishEvent(accountId, orgIdentifier, projectIdentifier, environmentIdentifier,
        EventsFrameworkMetadataConstants.DELETE_ACTION);
    return true;
  }

  private void checkThatEnvironmentIsNotReferredByOthers(Environment environment) {
    List<EntityDetail> referredByEntities;
    IdentifierRef identifierRef = IdentifierRef.builder()
                                      .accountIdentifier(environment.getAccountId())
                                      .orgIdentifier(environment.getOrgIdentifier())
                                      .projectIdentifier(environment.getProjectIdentifier())
                                      .identifier(environment.getIdentifier())
                                      .build();
    try {
      Page<EntitySetupUsageDTO> entitySetupUsageDTOS = entitySetupUsageService.listAllEntityUsage(
          0, 10, environment.getAccountId(), identifierRef.getFullyQualifiedName(), EntityType.ENVIRONMENT, "");
      referredByEntities = entitySetupUsageDTOS.stream()
                               .map(EntitySetupUsageDTO::getReferredByEntity)
                               .collect(Collectors.toCollection(LinkedList::new));
    } catch (Exception ex) {
      log.info("Encountered exception while requesting the Entity Reference records of [{}], with exception",
          environment.getIdentifier(), ex);
      throw new UnexpectedException(
          "Error while deleting the Environment as was not able to check entity reference records.");
    }
    if (EmptyPredicate.isNotEmpty(referredByEntities)) {
      throw new InvalidRequestException(
          String.format("Could not delete the Environment %s as it is referenced by other entities - "
                  + referredByEntities.toString(),
              environment.getIdentifier()));
    }
  }

  void validatePresenceOfRequiredFields(Object... fields) {
    Lists.newArrayList(fields).forEach(field -> Objects.requireNonNull(field, "One of the required fields is null."));
  }

  private void setName(Environment requestEnvironment) {
    if (isEmpty(requestEnvironment.getName())) {
      requestEnvironment.setName(requestEnvironment.getIdentifier());
    }
  }

  private Criteria getEnvironmentEqualityCriteria(Environment requestEnvironment, boolean deleted) {
    Criteria criteria = Criteria.where(EnvironmentKeys.accountId)
                            .is(requestEnvironment.getAccountId())
                            .and(EnvironmentKeys.orgIdentifier)
                            .is(requestEnvironment.getOrgIdentifier())
                            .and(EnvironmentKeys.projectIdentifier)
                            .is(requestEnvironment.getProjectIdentifier())
                            .and(EnvironmentKeys.identifier)
                            .is(requestEnvironment.getIdentifier())
                            .and(EnvironmentKeys.deleted)
                            .is(deleted);

    if (requestEnvironment.getVersion() != null) {
      criteria.and(EnvironmentKeys.version).is(requestEnvironment.getVersion());
    }

    return criteria;
  }

  private void publishEvent(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier, String action) {
    try {
      EntityChangeDTO.Builder environmentChangeEvent = EntityChangeDTO.newBuilder()
                                                           .setAccountIdentifier(StringValue.of(accountIdentifier))
                                                           .setIdentifier(StringValue.of(identifier));
      if (isNotBlank(orgIdentifier)) {
        environmentChangeEvent.setOrgIdentifier(StringValue.of(orgIdentifier));
      }
      if (isNotBlank(projectIdentifier)) {
        environmentChangeEvent.setProjectIdentifier(StringValue.of(projectIdentifier));
      }
      eventProducer.send(
          Message.newBuilder()
              .putAllMetadata(ImmutableMap.of("accountId", accountIdentifier,
                  EventsFrameworkMetadataConstants.ENTITY_TYPE, EventsFrameworkMetadataConstants.ENVIRONMENT_ENTITY,
                  EventsFrameworkMetadataConstants.ACTION, action))
              .setData(environmentChangeEvent.build().toByteString())
              .build());
    } catch (EventsFrameworkDownException e) {
      log.error("Failed to send event to events framework environment Identifier: {}", identifier, e);
    }
  }
}
