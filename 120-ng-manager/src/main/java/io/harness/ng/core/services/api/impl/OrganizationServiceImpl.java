package io.harness.ng.core.services.api.impl;

import static io.harness.exception.WingsException.USER_SRE;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.DuplicateFieldException;
import io.harness.ng.core.dao.api.OrganizationRepository;
import io.harness.ng.core.entities.Organization;
import io.harness.ng.core.services.api.OrganizationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Singleton
@Slf4j
public class OrganizationServiceImpl implements OrganizationService {
  private final OrganizationRepository organizationRepository;

  // TODO{phoenikx} Move this to a separate util class if needed at other places
  void validatePresenceOfRequiredFields(Object... fields) {
    Lists.newArrayList(fields).forEach(field -> Objects.requireNonNull(field, "One of the required fields is null."));
  }

  @Inject
  OrganizationServiceImpl(OrganizationRepository organizationRepository) {
    this.organizationRepository = organizationRepository;
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
    return organizationRepository.findById(organizationId);
  }

  @Override
  public Organization update(Organization organization) {
    validatePresenceOfRequiredFields(organization.getAccountId(), organization.getIdentifier(), organization.getId());
    return organizationRepository.save(organization);
  }

  @Override
  public List<Organization> getAll(String accountId) {
    return organizationRepository.findByAccountId(accountId);
  }
}
