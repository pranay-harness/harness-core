package io.harness.ng.core.environment.services.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.exception.WingsException.USER_SRE;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.DuplicateFieldException;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.environment.beans.Environment;
import io.harness.ng.core.environment.respositories.spring.EnvironmentRepository;
import io.harness.ng.core.environment.services.EnvironmentService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
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
    Optional<Environment> existingEnvironment =
        get(requestEnvironment.getAccountId(), requestEnvironment.getOrgIdentifier(),
            requestEnvironment.getProjectIdentifier(), requestEnvironment.getIdentifier());
    if (!existingEnvironment.isPresent()) {
      throw new InvalidRequestException(
          String.format("No environment exists with the Identifier %s", requestEnvironment.getIdentifier()));
    }
    setName(requestEnvironment);
    return update(existingEnvironment.get(), requestEnvironment);
  }

  @Override
  public Environment upsert(Environment requestEnvironment) {
    Optional<Environment> existingEnvironment =
        get(requestEnvironment.getAccountId(), requestEnvironment.getOrgIdentifier(),
            requestEnvironment.getProjectIdentifier(), requestEnvironment.getIdentifier());
    setName(requestEnvironment);
    return existingEnvironment.map(env -> update(env, requestEnvironment)).orElseGet(() -> create(requestEnvironment));
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

  private Environment update(Environment existingEnvironment, Environment requestEnvironment) {
    Environment environment = applyUpdateToEnvironmentEntity(existingEnvironment, requestEnvironment);
    return environmentRepository.save(environment);
  }

  private Environment applyUpdateToEnvironmentEntity(Environment existingEnvironment, Environment requestEnvironment) {
    requestEnvironment.setId(existingEnvironment.getId());
    requestEnvironment.setVersion(existingEnvironment.getVersion());
    requestEnvironment.setCreatedAt(existingEnvironment.getCreatedAt());
    BeanUtils.copyProperties(requestEnvironment, existingEnvironment);
    return existingEnvironment;
  }

  private void setName(Environment requestEnvironment) {
    if (isEmpty(requestEnvironment.getName())) {
      requestEnvironment.setName(requestEnvironment.getIdentifier());
    }
  }
}
