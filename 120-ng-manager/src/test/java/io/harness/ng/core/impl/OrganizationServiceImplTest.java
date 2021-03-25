package io.harness.ng.core.impl;

import static io.harness.ng.core.remote.OrganizationMapper.toOrganization;
import static io.harness.rule.OwnerRule.KARAN;
import static io.harness.utils.PageTestUtils.getPage;

import static java.util.Collections.emptyList;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.data.domain.Pageable.unpaged;

import io.harness.CategoryTest;
import io.harness.accesscontrol.AccessControlAdminClient;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.dto.OrganizationDTO;
import io.harness.ng.core.dto.OrganizationFilterDTO;
import io.harness.ng.core.entities.Organization;
import io.harness.ng.core.entities.Organization.OrganizationKeys;
import io.harness.ng.core.invites.entities.UserMembership;
import io.harness.ng.core.user.services.api.NgUserService;
import io.harness.outbox.OutboxEvent;
import io.harness.outbox.api.OutboxService;
import io.harness.repositories.core.spring.OrganizationRepository;
import io.harness.rule.Owner;

import io.dropwizard.jersey.validation.JerseyViolationException;
import java.util.Optional;
import org.bson.Document;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.transaction.support.TransactionTemplate;

public class OrganizationServiceImplTest extends CategoryTest {
  private OrganizationRepository organizationRepository;
  private OrganizationServiceImpl organizationService;
  private OutboxService outboxService;
  private TransactionTemplate transactionTemplate;
  private NgUserService ngUserService;
  private AccessControlAdminClient accessControlAdminClient;

  @Before
  public void setup() {
    organizationRepository = mock(OrganizationRepository.class);
    outboxService = mock(OutboxService.class);
    ngUserService = mock(NgUserService.class);
    transactionTemplate = mock(TransactionTemplate.class);
    accessControlAdminClient = mock(AccessControlAdminClient.class, RETURNS_DEEP_STUBS);
    organizationService = spy(new OrganizationServiceImpl(
        organizationRepository, outboxService, ngUserService, transactionTemplate, accessControlAdminClient));
  }

  private OrganizationDTO createOrganizationDTO(String identifier) {
    return OrganizationDTO.builder().identifier(identifier).name(randomAlphabetic(10)).build();
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testCreateOrganization() {
    String accountIdentifier = randomAlphabetic(10);
    OrganizationDTO organizationDTO = createOrganizationDTO(randomAlphabetic(10));
    Organization organization = toOrganization(organizationDTO);
    organization.setAccountIdentifier(accountIdentifier);

    when(organizationRepository.save(organization)).thenReturn(organization);
    when(ngUserService.addUserToScope((String) any(), any())).thenReturn(UserMembership.builder().build());
    when(outboxService.save(any())).thenReturn(OutboxEvent.builder().build());
    when(transactionTemplate.execute(any())).thenReturn(organization);

    Organization createdOrganization = organizationService.create(accountIdentifier, organizationDTO);

    verify(transactionTemplate, times(1)).execute(any());

    assertEquals(organization, createdOrganization);
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testUpdateExistentOrganization() {
    String accountIdentifier = randomAlphabetic(10);
    String identifier = randomAlphabetic(10);
    OrganizationDTO organizationDTO = createOrganizationDTO(identifier);
    Organization organization = toOrganization(organizationDTO);
    organization.setAccountIdentifier(accountIdentifier);
    organization.setIdentifier(identifier);

    when(organizationRepository.save(any())).thenReturn(organization);
    when(organizationService.get(accountIdentifier, identifier)).thenReturn(Optional.of(organization));

    organizationService.update(accountIdentifier, identifier, organizationDTO);
    verify(transactionTemplate, times(1)).execute(any());
  }

  @Test(expected = JerseyViolationException.class)
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testUpdateOrganization_IncorrectPayload() {
    String accountIdentifier = randomAlphabetic(10);
    String identifier = randomAlphabetic(10);
    OrganizationDTO organizationDTO = createOrganizationDTO(identifier);
    organizationDTO.setName("");
    Organization organization = toOrganization(organizationDTO);
    organization.setAccountIdentifier(accountIdentifier);
    organization.setIdentifier(identifier);
    organization.setName(randomAlphabetic(10));
    when(organizationService.get(accountIdentifier, identifier)).thenReturn(Optional.of(organization));

    organizationService.update(accountIdentifier, identifier, organizationDTO);
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testUpdateNonExistentOrganization() {
    String accountIdentifier = randomAlphabetic(10);
    String identifier = randomAlphabetic(10);
    OrganizationDTO organizationDTO = createOrganizationDTO(identifier);
    Organization organization = toOrganization(organizationDTO);
    organization.setAccountIdentifier(accountIdentifier);
    organization.setIdentifier(identifier);

    when(organizationService.get(accountIdentifier, identifier)).thenReturn(Optional.empty());

    Organization updatedOrganization = organizationService.update(accountIdentifier, identifier, organizationDTO);

    assertNull(updatedOrganization);
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testListOrganization() {
    String accountIdentifier = randomAlphabetic(10);
    String searchTerm = randomAlphabetic(5);
    ArgumentCaptor<Criteria> criteriaArgumentCaptor = ArgumentCaptor.forClass(Criteria.class);

    when(organizationRepository.findAll(any(Criteria.class), any(Pageable.class), anyBoolean()))
        .thenReturn(getPage(emptyList(), 0));

    Page<Organization> organizationPage = organizationService.list(
        accountIdentifier, unpaged(), OrganizationFilterDTO.builder().searchTerm(searchTerm).build());

    verify(organizationRepository, times(1))
        .findAll(criteriaArgumentCaptor.capture(), any(Pageable.class), anyBoolean());

    Criteria criteria = criteriaArgumentCaptor.getValue();
    Document criteriaObject = criteria.getCriteriaObject();

    assertEquals(3, criteriaObject.size());
    assertEquals(accountIdentifier, criteriaObject.get(OrganizationKeys.accountIdentifier));
    assertTrue(criteriaObject.containsKey(OrganizationKeys.deleted));

    assertEquals(0, organizationPage.getTotalElements());
  }
}