package io.harness.ng.core.impl;

import static io.harness.ModuleType.CD;
import static io.harness.ng.core.remote.ProjectMapper.toProject;
import static io.harness.rule.OwnerRule.KARAN;
import static io.harness.utils.PageTestUtils.getPage;

import static io.github.benas.randombeans.api.EnhancedRandom.random;
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
import io.harness.eventsframework.api.AbstractProducer;
import io.harness.eventsframework.api.ProducerShutdownException;
import io.harness.eventsframework.impl.noop.NoOpProducer;
import io.harness.eventsframework.producer.Message;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.dto.ProjectDTO;
import io.harness.ng.core.dto.ProjectFilterDTO;
import io.harness.ng.core.entities.Organization;
import io.harness.ng.core.entities.Project;
import io.harness.ng.core.entities.Project.ProjectKeys;
import io.harness.ng.core.invites.entities.UserProjectMap;
import io.harness.ng.core.services.OrganizationService;
import io.harness.ng.core.user.services.api.NgUserService;
import io.harness.repositories.core.spring.ProjectRepository;
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

public class ProjectServiceImplTest extends CategoryTest {
  private ProjectRepository projectRepository;
  private OrganizationService organizationService;
  private ProjectServiceImpl projectService;
  private AbstractProducer eventProducer;
  private NgUserService ngUserService;

  @Before
  public void setup() {
    projectRepository = mock(ProjectRepository.class);
    organizationService = mock(OrganizationService.class);
    eventProducer = mock(NoOpProducer.class);
    ngUserService = mock(NgUserService.class);
    projectService = spy(new ProjectServiceImpl(projectRepository, organizationService, eventProducer, ngUserService));
  }

  private ProjectDTO createProjectDTO(String accountIdentifier, String orgIdentifier, String identifier) {
    return ProjectDTO.builder()
        .accountIdentifier(accountIdentifier)
        .orgIdentifier(orgIdentifier)
        .identifier(identifier)
        .name(randomAlphabetic(10))
        .color(randomAlphabetic(10))
        .build();
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testCreateProject_CorrectPayload() {
    String accountIdentifier = randomAlphabetic(10);
    String orgIdentifier = randomAlphabetic(10);
    ProjectDTO projectDTO = createProjectDTO(accountIdentifier, orgIdentifier, randomAlphabetic(10));
    Project project = toProject(projectDTO);
    project.setAccountIdentifier(accountIdentifier);
    project.setOrgIdentifier(orgIdentifier);

    when(projectRepository.save(project)).thenReturn(project);
    when(organizationService.get(accountIdentifier, orgIdentifier)).thenReturn(Optional.of(random(Organization.class)));
    when(ngUserService.createUserProjectMap(any())).thenReturn(UserProjectMap.builder().build());

    Project createdProject = projectService.create(accountIdentifier, orgIdentifier, projectDTO);

    ArgumentCaptor<Message> producerMessage = ArgumentCaptor.forClass(Message.class);
    try {
      verify(eventProducer, times(1)).send(producerMessage.capture());
    } catch (ProducerShutdownException e) {
      e.printStackTrace();
    }

    assertEquals(project, createdProject);
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testCreateProject_IncorrectPayload() {
    String accountIdentifier = randomAlphabetic(10);
    String orgIdentifier = randomAlphabetic(10);
    ProjectDTO projectDTO =
        createProjectDTO(accountIdentifier + randomAlphabetic(1), orgIdentifier, randomAlphabetic(10));
    Project project = toProject(projectDTO);
    project.setAccountIdentifier(accountIdentifier);
    project.setOrgIdentifier(orgIdentifier);

    projectService.create(accountIdentifier, orgIdentifier, projectDTO);
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testUpdateExistentProject() {
    String accountIdentifier = randomAlphabetic(10);
    String orgIdentifier = randomAlphabetic(10);
    String identifier = randomAlphabetic(10);
    String id = randomAlphabetic(10);
    ProjectDTO projectDTO = createProjectDTO(accountIdentifier, orgIdentifier, identifier);
    Project project = toProject(projectDTO);
    project.setAccountIdentifier(accountIdentifier);
    project.setOrgIdentifier(orgIdentifier);
    project.setIdentifier(identifier);
    project.setId(id);

    when(projectRepository.save(any())).thenReturn(project);
    when(projectService.get(accountIdentifier, orgIdentifier, identifier)).thenReturn(Optional.of(project));

    Project updatedProject = projectService.update(accountIdentifier, orgIdentifier, identifier, projectDTO);

    ArgumentCaptor<Message> producerMessage = ArgumentCaptor.forClass(Message.class);
    try {
      verify(eventProducer, times(1)).send(producerMessage.capture());
    } catch (ProducerShutdownException e) {
      e.printStackTrace();
    }

    assertEquals(project, updatedProject);
  }

  @Test(expected = JerseyViolationException.class)
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testUpdateProject_IncorrectPayload() {
    String accountIdentifier = randomAlphabetic(10);
    String orgIdentifier = randomAlphabetic(10);
    String identifier = randomAlphabetic(10);
    ProjectDTO projectDTO = createProjectDTO(accountIdentifier, orgIdentifier, identifier);
    projectDTO.setName("");
    Project project = toProject(projectDTO);
    project.setAccountIdentifier(accountIdentifier);
    project.setOrgIdentifier(orgIdentifier);
    project.setName(randomAlphabetic(10));
    project.setId(randomAlphabetic(10));
    when(projectService.get(accountIdentifier, orgIdentifier, identifier)).thenReturn(Optional.of(project));
    projectService.update(accountIdentifier, orgIdentifier, identifier, projectDTO);
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testUpdateNonExistentProject() {
    String accountIdentifier = randomAlphabetic(10);
    String orgIdentifier = randomAlphabetic(10);
    String identifier = randomAlphabetic(10);
    ProjectDTO projectDTO = createProjectDTO(accountIdentifier, orgIdentifier, identifier);
    Project project = toProject(projectDTO);
    project.setAccountIdentifier(accountIdentifier);
    project.setOrgIdentifier(orgIdentifier);
    project.setIdentifier(identifier);

    when(projectService.get(accountIdentifier, orgIdentifier, identifier)).thenReturn(Optional.empty());

    Project updatedProject = projectService.update(accountIdentifier, orgIdentifier, identifier, projectDTO);

    assertNull(updatedProject);
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testListProject() {
    String accountIdentifier = randomAlphabetic(10);
    String orgIdentifier = randomAlphabetic(10);
    String searchTerm = randomAlphabetic(5);
    ArgumentCaptor<Criteria> criteriaArgumentCaptor = ArgumentCaptor.forClass(Criteria.class);

    when(projectRepository.findAll(any(Criteria.class), any(Pageable.class))).thenReturn(getPage(emptyList(), 0));

    Page<Project> projectPage = projectService.list(accountIdentifier, unpaged(),
        ProjectFilterDTO.builder().orgIdentifier(orgIdentifier).searchTerm(searchTerm).moduleType(CD).build());

    verify(projectRepository, times(1)).findAll(criteriaArgumentCaptor.capture(), any(Pageable.class));

    Criteria criteria = criteriaArgumentCaptor.getValue();
    Document criteriaObject = criteria.getCriteriaObject();

    assertEquals(5, criteriaObject.size());
    assertEquals(accountIdentifier, criteriaObject.get(ProjectKeys.accountIdentifier));
    assertEquals(orgIdentifier, criteriaObject.get(ProjectKeys.orgIdentifier));
    assertTrue(criteriaObject.containsKey(ProjectKeys.deleted));
    assertTrue(criteriaObject.containsKey(ProjectKeys.modules));

    assertEquals(0, projectPage.getTotalElements());
  }
}