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
  private static final String DUP_KEY_EXP_FORMAT_STRING =
      "Environment [%s] under Project[%s], Organization [%s] already exists";

  @Override
  public Environment create(@NotNull @Valid Environment environment) {
    try {
      validatePresenceOfRequiredFields(environment.getAccountId(), environment.getOrgIdentifier(),
          environment.getProjectIdentifier(), environment.getIdentifier());
      setName(environment);
      return environmentRepository.save(environment);
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
    try {
      validatePresenceOfRequiredFields(requestEnvironment.getAccountId(), requestEnvironment.getOrgIdentifier(),
          requestEnvironment.getProjectIdentifier(), requestEnvironment.getIdentifier());
      setName(requestEnvironment);
      Criteria criteria = getEnvironmentEqualityCriteria(requestEnvironment, requestEnvironment.getDeleted());
      UpdateResult updateResult = environmentRepository.update(criteria, requestEnvironment);
      if (!updateResult.wasAcknowledged() || updateResult.getModifiedCount() != 1) {
        throw new InvalidRequestException(
            String.format("Environment [%s] under Project[%s], Organization [%s] couldn't be updated or doesn't exist.",
                requestEnvironment.getIdentifier(), requestEnvironment.getProjectIdentifier(),
                requestEnvironment.getOrgIdentifier()));
      }
      return requestEnvironment;
    } catch (DuplicateKeyException ex) {
      throw new DuplicateFieldException(
          String.format(DUP_KEY_EXP_FORMAT_STRING, requestEnvironment.getIdentifier(),
              requestEnvironment.getProjectIdentifier(), requestEnvironment.getOrgIdentifier()),
          USER_SRE, ex);
    }
  }

  @Override
  public Environment upsert(Environment requestEnvironment) {
    try {
      validatePresenceOfRequiredFields(requestEnvironment.getAccountId(), requestEnvironment.getOrgIdentifier(),
          requestEnvironment.getProjectIdentifier(), requestEnvironment.getIdentifier());
      setName(requestEnvironment);
      Criteria criteria = getEnvironmentEqualityCriteria(requestEnvironment, requestEnvironment.getDeleted());
      UpdateResult upsertResult = environmentRepository.upsert(criteria, requestEnvironment);
      if (!upsertResult.wasAcknowledged()) {
        throw new InvalidRequestException(
            String.format("Environment [%s] under Project[%s], Organization [%s] couldn't be upserted.",
                requestEnvironment.getIdentifier(), requestEnvironment.getProjectIdentifier(),
                requestEnvironment.getOrgIdentifier()));
      }
      return requestEnvironment;
    } catch (DuplicateKeyException ex) {
      throw new DuplicateFieldException(
          String.format(DUP_KEY_EXP_FORMAT_STRING, requestEnvironment.getIdentifier(),
              requestEnvironment.getProjectIdentifier(), requestEnvironment.getOrgIdentifier()),
          USER_SRE, ex);
    }
  }

  @Override
  public Page<Environment> list(Criteria criteria, Pageable pageable) {
    return environmentRepository.findAll(criteria, pageable);
  }

  @Override
  public boolean delete(
      String accountId, String orgIdentifier, String projectIdentifier, String environmentIdentifier) {
    Environment environment = Environment.builder()
                                  .accountId(accountId)
                                  .orgIdentifier(orgIdentifier)
                                  .projectIdentifier(projectIdentifier)
                                  .identifier(environmentIdentifier)
                                  .build();
    Criteria criteria = getEnvironmentEqualityCriteria(environment, false);
    UpdateResult updateResult = environmentRepository.delete(criteria);
    if (!updateResult.wasAcknowledged()) {
      throw new InvalidRequestException(
          String.format("Environment [%s] under Project[%s], Organization [%s] couldn't be deleted.",
              environmentIdentifier, projectIdentifier, orgIdentifier));
    }
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

  private Criteria getEnvironmentEqualityCriteria(Environment requestEnvironment, boolean deleted) {
    return Criteria.where(EnvironmentKeys.accountId)
        .is(requestEnvironment.getAccountId())
        .and(EnvironmentKeys.orgIdentifier)
        .is(requestEnvironment.getOrgIdentifier())
        .and(EnvironmentKeys.projectIdentifier)
        .is(requestEnvironment.getProjectIdentifier())
        .and(EnvironmentKeys.identifier)
        .is(requestEnvironment.getIdentifier())
        .and(EnvironmentKeys.deleted)
        .is(deleted);
  }
}
