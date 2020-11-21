package io.harness.ng.core.impl;

import static io.harness.ModuleType.CD;
import static io.harness.ng.core.remote.ProjectMapper.toProject;
import static io.harness.rule.OwnerRule.KARAN;
import static io.harness.security.encryption.EncryptionType.GCP_KMS;
import static io.harness.utils.PageTestUtils.getPage;

import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.data.domain.Pageable.unpaged;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.connector.apis.dto.ConnectorResponseDTO;
import io.harness.connector.services.ConnectorService;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.api.NGSecretManagerService;
import io.harness.ng.core.api.repositories.spring.ProjectRepository;
import io.harness.ng.core.dto.ProjectDTO;
import io.harness.ng.core.dto.ProjectFilterDTO;
import io.harness.ng.core.entities.Organization;
import io.harness.ng.core.entities.Project;
import io.harness.ng.core.entities.Project.ProjectKeys;
import io.harness.ng.core.services.OrganizationService;
import io.harness.rule.Owner;
import io.harness.secretmanagerclient.dto.GcpKmsConfigDTO;

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
  private NGSecretManagerService ngSecretManagerService;
  private ConnectorService secretManagerConnectorService;
  private ProjectServiceImpl projectService;

  @Before
  public void setup() {
    projectRepository = mock(ProjectRepository.class);
    organizationService = mock(OrganizationService.class);
    ngSecretManagerService = mock(NGSecretManagerService.class);
    secretManagerConnectorService = mock(ConnectorService.class);
    projectService = spy(new ProjectServiceImpl(
        projectRepository, organizationService, ngSecretManagerService, secretManagerConnectorService));
  }

  private ProjectDTO createProjectDTO(String accountIdentifier, String orgIdentifier, String identifier) {
    return ProjectDTO.builder()
        .accountIdentifier(accountIdentifier)
        .orgIdentifier(orgIdentifier)
        .identifier(identifier)
        .name(randomAlphabetic(10))
        .color(randomAlphabetic(10))
        .owners(singletonList(randomAlphabetic(10)))
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
    when(ngSecretManagerService.getGlobalSecretManager(accountIdentifier))
        .thenReturn(GcpKmsConfigDTO.builder().encryptionType(GCP_KMS).build());
    when(secretManagerConnectorService.create(any(), eq(accountIdentifier))).thenReturn(new ConnectorResponseDTO());
    when(organizationService.get(accountIdentifier, orgIdentifier)).thenReturn(Optional.of(random(Organization.class)));

    Project createdProject = projectService.create(accountIdentifier, orgIdentifier, projectDTO);

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
    ProjectDTO projectDTO = createProjectDTO(accountIdentifier, orgIdentifier, identifier);
    Project project = toProject(projectDTO);
    project.setAccountIdentifier(accountIdentifier);
    project.setOrgIdentifier(orgIdentifier);
    project.setIdentifier(identifier);

    when(projectRepository.update(any(), any())).thenReturn(project);

    Project updatedProject = projectService.update(accountIdentifier, orgIdentifier, identifier, projectDTO);

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

    when(projectRepository.update(any(), any())).thenReturn(null);

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
