package io.harness.ng.core.services.api.impl;

import static io.harness.exception.WingsException.USER_SRE;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.DuplicateFieldException;
import io.harness.ng.core.dao.api.repositories.spring.OrganizationRepository;
import io.harness.ng.core.entities.Organization;
import io.harness.ng.core.entities.Organization.OrganizationKeys;
import io.harness.ng.core.services.api.OrganizationService;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.Objects;
import java.util.Optional;
import javax.validation.constraints.NotNull;

@Singleton
@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
@Slf4j
public class OrganizationServiceImpl implements OrganizationService {
  private final OrganizationRepository organizationRepository;

  void validatePresenceOfRequiredFields(Object... fields) {
    Lists.newArrayList(fields).forEach(field -> Objects.requireNonNull(field, "One of the required fields is null."));
  }

  @Override
  public Organization create(Organization organization) {
    try {
      return organizationRepository.save(organization);
    } catch (DuplicateKeyException ex) {
      throw new DuplicateFieldException(String.format("Organization [%s] under account [%s] already exists",
                                            organization.getIdentifier(), organization.getAccountId()),
          USER_SRE, ex);
    }
  }

  @Override
  public Optional<Organization> get(String organizationId) {
    return organizationRepository.findByIdAndDeletedNot(organizationId, Boolean.TRUE);
  }

  @Override
  public Organization update(Organization organization) {
    validatePresenceOfRequiredFields(organization.getAccountId(), organization.getIdentifier(), organization.getId());
    return organizationRepository.save(organization);
  }

  @Override
  public Page<Organization> list(@NotNull String accountId, @NotNull Criteria criteria, Pageable pageable) {
    criteria = criteria.and(OrganizationKeys.accountId).is(accountId).and(OrganizationKeys.deleted).ne(Boolean.TRUE);
    return organizationRepository.findAll(criteria, pageable);
  }

  @Override
  public boolean delete(String organizationId) {
    Optional<Organization> organizationOptional = get(organizationId);
    if (organizationOptional.isPresent()) {
      Organization organization = organizationOptional.get();
      organization.setDeleted(true);
      organizationRepository.save(organization);
      return true;
    }
    return false;
  }
}
