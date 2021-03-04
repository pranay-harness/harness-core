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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.data.domain.Pageable.unpaged;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.eventsframework.api.Producer;
import io.harness.eventsframework.api.ProducerShutdownException;
import io.harness.eventsframework.impl.noop.NoOpProducer;
import io.harness.eventsframework.producer.Message;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.dto.OrganizationDTO;
import io.harness.ng.core.dto.OrganizationFilterDTO;
import io.harness.ng.core.entities.Organization;
import io.harness.ng.core.entities.Organization.OrganizationKeys;
import io.harness.ng.core.invites.entities.UserProjectMap;
import io.harness.ng.core.user.services.api.NgUserService;
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

public class OrganizationServiceImplTest extends CategoryTest {
  private OrganizationRepository organizationRepository;
  private OrganizationServiceImpl organizationService;
  private Producer eventProducer;
  private NgUserService ngUserService;

  @Before
  public void setup() {
    organizationRepository = mock(OrganizationRepository.class);
    eventProducer = mock(NoOpProducer.class);
    ngUserService = mock(NgUserService.class);
    organizationService = spy(new OrganizationServiceImpl(organizationRepository, eventProducer, ngUserService));
  }

  private OrganizationDTO createOrganizationDTO(String accountIdentifier, String identifier) {
    return OrganizationDTO.builder()
        .accountIdentifier(accountIdentifier)
        .identifier(identifier)
        .name(randomAlphabetic(10))
        .build();
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testCreateOrganization() {
    String accountIdentifier = randomAlphabetic(10);
    OrganizationDTO organizationDTO = createOrganizationDTO(accountIdentifier, randomAlphabetic(10));
    Organization organization = toOrganization(organizationDTO);
    organization.setAccountIdentifier(accountIdentifier);

    when(organizationRepository.save(organization)).thenReturn(organization);
    when(ngUserService.createUserProjectMap(any())).thenReturn(UserProjectMap.builder().build());

    Organization createdOrganization = organizationService.create(accountIdentifier, organizationDTO);

    ArgumentCaptor<Message> producerMessage = ArgumentCaptor.forClass(Message.class);
    try {
      verify(eventProducer, times(1)).send(producerMessage.capture());
    } catch (ProducerShutdownException e) {
      e.printStackTrace();
    }

    assertEquals(organization, createdOrganization);
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testCreateOrganization_IncorrectPayload() {
    String accountIdentifier = randomAlphabetic(10);
    OrganizationDTO organizationDTO =
        createOrganizationDTO(accountIdentifier + randomAlphabetic(1), randomAlphabetic(10));
    Organization organization = toOrganization(organizationDTO);
    organization.setAccountIdentifier(accountIdentifier);

    organizationService.create(accountIdentifier, organizationDTO);
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testUpdateExistentOrganization() {
    String accountIdentifier = randomAlphabetic(10);
    String identifier = randomAlphabetic(10);
    OrganizationDTO organizationDTO = createOrganizationDTO(accountIdentifier, identifier);
    Organization organization = toOrganization(organizationDTO);
    organization.setAccountIdentifier(accountIdentifier);
    organization.setIdentifier(identifier);

    when(organizationRepository.save(any())).thenReturn(organization);
    when(organizationService.get(accountIdentifier, identifier)).thenReturn(Optional.of(organization));

    Organization updatedOrganization = organizationService.update(accountIdentifier, identifier, organizationDTO);

    ArgumentCaptor<Message> producerMessage = ArgumentCaptor.forClass(Message.class);
    try {
      verify(eventProducer, times(1)).send(producerMessage.capture());
    } catch (ProducerShutdownException e) {
      e.printStackTrace();
    }

    assertEquals(organization, updatedOrganization);
  }

  @Test(expected = JerseyViolationException.class)
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testUpdateOrganization_IncorrectPayload() {
    String accountIdentifier = randomAlphabetic(10);
    String identifier = randomAlphabetic(10);
    OrganizationDTO organizationDTO = createOrganizationDTO(accountIdentifier, identifier);
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
    OrganizationDTO organizationDTO = createOrganizationDTO(accountIdentifier, identifier);
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

    when(organizationRepository.findAll(any(Criteria.class), any(Pageable.class))).thenReturn(getPage(emptyList(), 0));

    Page<Organization> organizationPage = organizationService.list(
        accountIdentifier, unpaged(), OrganizationFilterDTO.builder().searchTerm(searchTerm).build());

    verify(organizationRepository, times(1)).findAll(criteriaArgumentCaptor.capture(), any(Pageable.class));

    Criteria criteria = criteriaArgumentCaptor.getValue();
    Document criteriaObject = criteria.getCriteriaObject();

    assertEquals(3, criteriaObject.size());
    assertEquals(accountIdentifier, criteriaObject.get(OrganizationKeys.accountIdentifier));
    assertTrue(criteriaObject.containsKey(OrganizationKeys.deleted));

    assertEquals(0, organizationPage.getTotalElements());
  }
}