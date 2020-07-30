package io.harness.ng.core.environment.services.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.exception.WingsException.USER_SRE;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.mongodb.client.result.UpdateResult;
import io.harness.exception.DuplicateFieldException;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.environment.beans.Environment;
import io.harness.ng.core.environment.beans.Environment.EnvironmentKeys;
import io.harness.ng.core.environment.respositories.spring.EnvironmentRepository;
import io.harness.ng.core.environment.services.EnvironmentService;
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
public class EnvironmentServiceImpl implements EnvironmentService {
  private final EnvironmentRepository environmentRepository;

  @Override
  public Environment create(@NotNull @Valid Environment environment) {
    try {
      validatePresenceOfRequiredFields(environment.getAccountId(), environment.getOrgIdentifier(),
          environment.getProjectIdentifier(), environment.getIdentifier());
      setName(environment);
      return environmentRepository.save(environment);
    } catch (DuplicateKeyException ex) {
      throw new DuplicateFieldException(
          String.format("Environment [%s] under Project[%s], Organization [%s] already exists",
              environment.getIdentifier(), environment.getProjectIdentifier(), environment.getOrgIdentifier()),
          USER_SRE, ex);
    }
  }

  @Override
  public Optional<Environment> get(
      String accountId, String orgIdentifier, String projectIdentifier, String environmentIdentifier) {
    return environmentRepository.findByAccountIdAndOrgIdentifierAndProjectIdentifierAndIdentifier(
        accountId, orgIdentifier, projectIdentifier, environmentIdentifier);
  }

  @Override
  public Environment update(@Valid Environment requestEnvironment) {
    validatePresenceOfRequiredFields(requestEnvironment.getAccountId(), requestEnvironment.getOrgIdentifier(),
        requestEnvironment.getProjectIdentifier(), requestEnvironment.getIdentifier());
    setName(requestEnvironment);
    Criteria criteria = getEnvironmentEqualityCriteria(requestEnvironment);
    UpdateResult updateResult = environmentRepository.update(criteria, requestEnvironment);
    if (!updateResult.wasAcknowledged() || updateResult.getModifiedCount() != 1) {
      throw new InvalidRequestException(
          String.format("Environment [%s] under Project[%s], Organization [%s] couldn't be updated or doesn't exist.",
              requestEnvironment.getIdentifier(), requestEnvironment.getProjectIdentifier(),
              requestEnvironment.getOrgIdentifier()));
    }
    return requestEnvironment;
  }

  @Override
  public Environment upsert(Environment requestEnvironment) {
    validatePresenceOfRequiredFields(requestEnvironment.getAccountId(), requestEnvironment.getOrgIdentifier(),
        requestEnvironment.getProjectIdentifier(), requestEnvironment.getIdentifier());
    setName(requestEnvironment);
    Criteria criteria = getEnvironmentEqualityCriteria(requestEnvironment);
    UpdateResult updateResult = environmentRepository.upsert(criteria, requestEnvironment);
    if (!updateResult.wasAcknowledged()) {
      throw new InvalidRequestException(
          String.format("Environment [%s] under Project[%s], Organization [%s] couldn't be upserted.",
              requestEnvironment.getIdentifier(), requestEnvironment.getProjectIdentifier(),
              requestEnvironment.getOrgIdentifier()));
    }
    return requestEnvironment;
  }

  @Override
  public Page<Environment> list(Criteria criteria, Pageable pageable) {
    return environmentRepository.findAll(criteria, pageable);
  }

  @Override
  public boolean delete(
      String accountId, String orgIdentifier, String projectIdentifier, String environmentIdentifier) {
    environmentRepository.deleteByAccountIdAndOrgIdentifierAndProjectIdentifierAndIdentifier(
        accountId, orgIdentifier, projectIdentifier, environmentIdentifier);
    return true;
  }

  void validatePresenceOfRequiredFields(Object... fields) {
    Lists.newArrayList(fields).forEach(field -> Objects.requireNonNull(field, "One of the required fields is null."));
  }

  private void setName(Environment requestEnvironment) {
    if (isEmpty(requestEnvironment.getName())) {
      requestEnvironment.setName(requestEnvironment.getIdentifier());
    }
  }

  private Criteria getEnvironmentEqualityCriteria(Environment requestEnvironment) {
    return Criteria.where(EnvironmentKeys.accountId)
        .is(requestEnvironment.getAccountId())
        .and(EnvironmentKeys.orgIdentifier)
        .is(requestEnvironment.getOrgIdentifier())
        .and(EnvironmentKeys.projectIdentifier)
        .is(requestEnvironment.getProjectIdentifier())
        .and(EnvironmentKeys.identifier)
        .is(requestEnvironment.getIdentifier());
  }
}
