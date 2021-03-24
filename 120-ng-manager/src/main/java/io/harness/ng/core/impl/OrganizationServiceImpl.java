package io.harness.ng.core.impl;

import static io.harness.exception.WingsException.USER;
import static io.harness.exception.WingsException.USER_SRE;
import static io.harness.ng.core.remote.OrganizationMapper.toOrganization;
import static io.harness.ng.core.utils.NGUtils.validate;
import static io.harness.ng.core.utils.NGUtils.verifyValuesNotChanged;
import static io.harness.outbox.TransactionOutboxModule.OUTBOX_TRANSACTION_TEMPLATE;

import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.exception.DuplicateFieldException;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.auditevent.OrgCreateEvent;
import io.harness.ng.core.auditevent.OrgDeleteEvent;
import io.harness.ng.core.auditevent.OrgRestoreEvent;
import io.harness.ng.core.auditevent.OrgUpdateEvent;
import io.harness.ng.core.common.beans.NGTag.NGTagKeys;
import io.harness.ng.core.dto.OrganizationDTO;
import io.harness.ng.core.dto.OrganizationFilterDTO;
import io.harness.ng.core.entities.Organization;
import io.harness.ng.core.entities.Organization.OrganizationKeys;
import io.harness.ng.core.invites.entities.Role;
import io.harness.ng.core.invites.entities.UserProjectMap;
import io.harness.ng.core.remote.OrganizationMapper;
import io.harness.ng.core.services.OrganizationService;
import io.harness.ng.core.user.services.api.NgUserService;
import io.harness.outbox.api.OutboxService;
import io.harness.repositories.core.spring.OrganizationRepository;
import io.harness.security.SourcePrincipalContextBuilder;
import io.harness.security.dto.PrincipalType;
import io.harness.utils.RetryUtils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionTemplate;

@Singleton
@Slf4j
public class OrganizationServiceImpl implements OrganizationService {
  private final OrganizationRepository organizationRepository;
  private final OutboxService outboxService;
  private final NgUserService ngUserService;
  private final TransactionTemplate transactionTemplate;
  private static final String ORGANIZATION_ADMIN_ROLE_NAME = "Organization Admin";
  private final RetryPolicy<Object> transactionRetryPolicy = RetryUtils.getRetryPolicy("[Retrying] attempt: {}",
      "[Failed] attempt: {}", ImmutableList.of(TransactionException.class), Duration.ofSeconds(1), 3, log);

  @Inject
  public OrganizationServiceImpl(OrganizationRepository organizationRepository, OutboxService outboxService,
      NgUserService ngUserService, @Named(OUTBOX_TRANSACTION_TEMPLATE) TransactionTemplate transactionTemplate) {
    this.organizationRepository = organizationRepository;
    this.outboxService = outboxService;
    this.ngUserService = ngUserService;
    this.transactionTemplate = transactionTemplate;
  }

  @Override
  public Organization create(String accountIdentifier, OrganizationDTO organizationDTO) {
    Organization organization = toOrganization(organizationDTO);
    organization.setAccountIdentifier(accountIdentifier);
    try {
      validate(organization);
      Organization savedOrganization = saveOrganization(organization);
      log.info(String.format("Organization with identifier %s was successfully created", organization.getIdentifier()));
      createUserProjectMap(organization);
      return savedOrganization;
    } catch (DuplicateKeyException ex) {
      throw new DuplicateFieldException(
          String.format(
              "An organization with identifier %s is already present or was deleted", organization.getIdentifier()),
          USER_SRE, ex);
    }
  }

  private Organization saveOrganization(Organization organization) {
    return Failsafe.with(transactionRetryPolicy).get(() -> transactionTemplate.execute(status -> {
      Organization savedOrganization = organizationRepository.save(organization);
      outboxService.save(
          new OrgCreateEvent(organization.getAccountIdentifier(), OrganizationMapper.writeDto(organization)));
      return savedOrganization;
    }));
  }

  private void createUserProjectMap(Organization organization) {
    if (SourcePrincipalContextBuilder.getSourcePrincipal() != null
        && SourcePrincipalContextBuilder.getSourcePrincipal().getType() == PrincipalType.USER) {
      String userId = SourcePrincipalContextBuilder.getSourcePrincipal().getName();
      Role role = Role.builder()
                      .accountIdentifier(organization.getAccountIdentifier())
                      .orgIdentifier(organization.getIdentifier())
                      .name(ORGANIZATION_ADMIN_ROLE_NAME)
                      .build();
      UserProjectMap userProjectMap = UserProjectMap.builder()
                                          .userId(userId)
                                          .accountIdentifier(organization.getAccountIdentifier())
                                          .orgIdentifier(organization.getIdentifier())
                                          .roles(singletonList(role))
                                          .build();
      ngUserService.createUserProjectMap(userProjectMap);
    }
  }

  @Override
  public Optional<Organization> get(String accountIdentifier, String organizationIdentifier) {
    return organizationRepository.findByAccountIdentifierAndIdentifierAndDeletedNot(
        accountIdentifier, organizationIdentifier, true);
  }

  @Override
  public Organization update(String accountIdentifier, String identifier, OrganizationDTO organizationDTO) {
    validateUpdateOrganizationRequest(identifier, organizationDTO);
    Optional<Organization> optionalOrganization = get(accountIdentifier, identifier);

    if (optionalOrganization.isPresent()) {
      Organization existingOrganization = optionalOrganization.get();
      if (Boolean.TRUE.equals(existingOrganization.getHarnessManaged())) {
        throw new InvalidRequestException(
            String.format("Update operation not supported for Default Organization (identifier: [%s])", identifier),
            USER);
      }
      Organization organization = toOrganization(organizationDTO);
      organization.setAccountIdentifier(accountIdentifier);
      organization.setId(existingOrganization.getId());
      if (organization.getVersion() == null) {
        organization.setVersion(existingOrganization.getVersion());
      }
      validate(organization);
      return Failsafe.with(transactionRetryPolicy).get(() -> transactionTemplate.execute(status -> {
        Organization updatedOrganization = organizationRepository.save(organization);
        log.info(String.format("Organization with identifier %s was successfully updated", identifier));
        outboxService.save(new OrgUpdateEvent(organization.getAccountIdentifier(),
            OrganizationMapper.writeDto(updatedOrganization), OrganizationMapper.writeDto(existingOrganization)));
        return updatedOrganization;
      }));
    }
    throw new InvalidRequestException(String.format("Organisation with identifier [%s] not found", identifier), USER);
  }

  @Override
  public Page<Organization> list(
      String accountIdentifier, Pageable pageable, OrganizationFilterDTO organizationFilterDTO) {
    Criteria criteria = createOrganizationFilterCriteria(Criteria.where(OrganizationKeys.accountIdentifier)
                                                             .is(accountIdentifier)
                                                             .and(OrganizationKeys.deleted)
                                                             .ne(Boolean.TRUE),
        organizationFilterDTO);
    return organizationRepository.findAll(
        criteria, pageable, organizationFilterDTO != null && organizationFilterDTO.isIgnoreCase());
  }

  @Override
  public Page<Organization> list(Criteria criteria, Pageable pageable) {
    return organizationRepository.findAll(criteria, pageable, false);
  }

  @Override
  public List<Organization> list(Criteria criteria) {
    return organizationRepository.findAll(criteria);
  }

  private Criteria createOrganizationFilterCriteria(Criteria criteria, OrganizationFilterDTO organizationFilterDTO) {
    if (organizationFilterDTO == null) {
      return criteria;
    }
    if (isNotBlank(organizationFilterDTO.getSearchTerm())) {
      criteria.orOperator(Criteria.where(OrganizationKeys.name).regex(organizationFilterDTO.getSearchTerm(), "i"),
          Criteria.where(OrganizationKeys.identifier).regex(organizationFilterDTO.getSearchTerm(), "i"),
          Criteria.where(OrganizationKeys.tags + "." + NGTagKeys.key).regex(organizationFilterDTO.getSearchTerm(), "i"),
          Criteria.where(OrganizationKeys.tags + "." + NGTagKeys.value)
              .regex(organizationFilterDTO.getSearchTerm(), "i"));
    }
    if (Objects.nonNull(organizationFilterDTO.getIdentifiers()) && !organizationFilterDTO.getIdentifiers().isEmpty()) {
      Criteria.where(OrganizationKeys.identifier).in(organizationFilterDTO.getIdentifiers());
    }
    return criteria;
  }

  @Override
  public boolean delete(String accountIdentifier, String organizationIdentifier, Long version) {
    return Failsafe.with(transactionRetryPolicy).get(() -> transactionTemplate.execute(status -> {
      Organization organization = organizationRepository.delete(accountIdentifier, organizationIdentifier, version);
      boolean delete = organization != null;
      if (delete) {
        log.info(String.format("Organization with identifier %s was successfully deleted", organizationIdentifier));
        outboxService.save(new OrgDeleteEvent(accountIdentifier, OrganizationMapper.writeDto(organization)));
      } else {
        log.error(String.format("Organization with identifier %s could not be deleted", organizationIdentifier));
      }
      return delete;
    }));
  }

  @Override
  public boolean restore(String accountIdentifier, String identifier) {
    return Failsafe.with(transactionRetryPolicy).get(() -> transactionTemplate.execute(status -> {
      Organization organization = organizationRepository.restore(accountIdentifier, identifier);
      boolean success = organization != null;
      if (success) {
        outboxService.save(new OrgRestoreEvent(accountIdentifier, OrganizationMapper.writeDto(organization)));
      }
      return success;
    }));
  }

  private void validateUpdateOrganizationRequest(String identifier, OrganizationDTO organization) {
    verifyValuesNotChanged(Lists.newArrayList(Pair.of(identifier, organization.getIdentifier())), false);
  }
}