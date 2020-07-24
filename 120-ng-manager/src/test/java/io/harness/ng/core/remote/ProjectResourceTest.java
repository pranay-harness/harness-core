package io.harness.ng.core.remote;

import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.rule.OwnerRule.VIKAS;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.beans.NGPageResponse;
import io.harness.category.element.UnitTests;
import io.harness.ng.ModuleType;
import io.harness.ng.core.dto.CreateProjectDTO;
import io.harness.ng.core.dto.ProjectDTO;
import io.harness.ng.core.dto.UpdateProjectDTO;
import io.harness.ng.core.entities.Project;
import io.harness.ng.core.io.harness.ng.utils.PageTestUtils;
import io.harness.ng.core.services.api.ProjectService;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ProjectResourceTest extends CategoryTest {
  private ProjectService projectService;
  private ProjectResource projectResource;

  @Before
  public void doSetup() {
    projectService = mock(ProjectService.class);
    projectResource = new ProjectResource(projectService);
  }

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testCreateProject() {
    String orgIdentifier = randomAlphabetic(10);
    String projectIdentifier = randomAlphabetic(10);
    when(projectService.get(orgIdentifier, projectIdentifier)).thenReturn(Optional.empty());
    assertFalse(projectResource.get(orgIdentifier, projectIdentifier).getData().isPresent());

    CreateProjectDTO createProjectDTO = random(CreateProjectDTO.class);
    Project createdProject = createProject(orgIdentifier, createProjectDTO.getIdentifier());
    when(projectService.create(any(Project.class))).thenReturn(createdProject);
    when(projectService.get(orgIdentifier, projectIdentifier)).thenReturn(Optional.of(createdProject));

    ProjectDTO projectDTO = projectResource.create(orgIdentifier, createProjectDTO).getData();

    assertTrue(isNotEmpty(projectDTO.getIdentifier()));

    when(projectService.get(orgIdentifier, createProjectDTO.getIdentifier())).thenReturn(Optional.of(createdProject));
    assertEquals(projectDTO, projectResource.get(orgIdentifier, projectIdentifier).getData().orElse(null));
  }

  private Project createProject(String orgIdentifier, String projectIdentifier) {
    return Project.builder()
        .id(randomAlphabetic(10))
        .identifier(projectIdentifier)
        .orgIdentifier(orgIdentifier)
        .identifier(randomAlphabetic(10))
        .build();
  }

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testList_For_OrgIdFilterOnly() {
    String accountIdentifier = randomAlphabetic(10);
    String orgIdentifier = randomAlphabetic(10);

    List<Project> projectList = new ArrayList<>();
    when(projectService.list(any(Criteria.class), any(Pageable.class)))
        .thenReturn(PageTestUtils.getPage(projectList, 0));
    assertTrue(projectResource.listProjectsForOrganization(orgIdentifier, 0, 10, null).getData().isEmpty());

    projectList.add(createProject(orgIdentifier, accountIdentifier));
    projectList.add(createProject(orgIdentifier, accountIdentifier));
    when(projectService.list(any(Criteria.class), any(Pageable.class)))
        .thenReturn(PageTestUtils.getPage(projectList, 2));

    NGPageResponse<ProjectDTO> projectDTOs =
        projectResource.listProjectsForOrganization(orgIdentifier, 0, 10, null).getData();
    assertNotNull("ProjectDTO should not be null", projectDTOs);
    assertEquals("Count of DTOs should match", 2, projectDTOs.getTotalElements());
    assertNotNull("Page contents should not be null", projectDTOs.getContent());

    List<ProjectDTO> returnedDTOs = projectDTOs.getContent();

    assertNotNull("Returned project DTOs page should not null ", returnedDTOs);
    assertEquals("Returned project DTOs page size should match created DTOs", 2, returnedDTOs.size());

    projectList.forEach(project -> {
      boolean isPresentInResult = projectDTOs.getContent().stream().anyMatch(
          dto -> dto.getOrgIdentifier().equals(orgIdentifier) && dto.getIdentifier().equals(project.getIdentifier()));
      assertTrue("Fetched DTO should be present ", isPresentInResult);
    });
  }

  private List<ProjectDTO> createProjects(String orgIdentifier, String accountIdentifier, int count) {
    return IntStream.range(0, count)
        .mapToObj(i -> {
          CreateProjectDTO projectDTO = random(CreateProjectDTO.class);
          projectDTO.getModules().clear();
          if (i == 0) {
            projectDTO.getModules().add(ModuleType.CD);
          }
          projectDTO.setAccountIdentifier(accountIdentifier);
          return projectResource.create(orgIdentifier, projectDTO).getData();
        })
        .collect(Collectors.toList());
  }

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testList_For_FilterQuery_With_OrgId_Or_AccountId() {
    String firstOrgIdentifier = randomAlphabetic(10);
    String secondOrgIdentifier = randomAlphabetic(10);
    String accountIdentifier = randomAlphabetic(10);

    List<Project> firstProjectList = new ArrayList<>();

    firstProjectList.add(createProject(firstOrgIdentifier, accountIdentifier));
    firstProjectList.add(createProject(firstOrgIdentifier, accountIdentifier));
    when(projectService.list(any(Criteria.class), any(Pageable.class)))
        .thenReturn(PageTestUtils.getPage(firstProjectList, 2));

    final NGPageResponse<ProjectDTO> projectDTOS =
        projectResource.listProjectsForOrganization(firstOrgIdentifier, 0, 10, null).getData();
    assertNotNull("ProjectDTO should not be null", projectDTOS);
    assertEquals("Count of DTOs should match", firstProjectList.size(), projectDTOS.getTotalElements());
    assertNotNull("Page contents should not be null", projectDTOS.getContent());

    List<ProjectDTO> returnedDTOs = projectDTOS.getContent();

    assertNotNull("Returned project DTOs page should not null ", returnedDTOs);
    assertEquals(
        "Returned project DTOs page size should match created DTOs", firstProjectList.size(), returnedDTOs.size());

    firstProjectList.forEach(project -> {
      boolean isPresentInResult = projectDTOS.getContent().stream().anyMatch(dto
          -> dto.getOrgIdentifier().equals(firstOrgIdentifier) && dto.getIdentifier().equals(project.getIdentifier()));
      assertTrue("Fetched DTO should be present ", isPresentInResult);
    });

    List<Project> secondProjectList = new ArrayList<>();

    secondProjectList.add(createProject(secondOrgIdentifier, accountIdentifier));
    secondProjectList.add(createProject(secondOrgIdentifier, accountIdentifier));
    when(projectService.list(any(Criteria.class), any(Pageable.class)))
        .thenReturn(PageTestUtils.getPage(secondProjectList, 2));
  }

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testUpdateExistentProject() {
    String orgIdentifier = randomAlphabetic(10);
    String projectIdentifier = randomAlphabetic(10);

    Project createdProject = createProject(orgIdentifier, projectIdentifier);
    when(projectService.get(orgIdentifier, projectIdentifier)).thenReturn(Optional.of(createdProject));

    UpdateProjectDTO updateProjectDTO = random(UpdateProjectDTO.class);
    Project updateProject = ProjectMapper.applyUpdateToProject(createdProject, updateProjectDTO);

    when(projectService.update(any(Project.class))).thenReturn(updateProject);

    when(projectService.get(orgIdentifier, projectIdentifier)).thenReturn(Optional.of(updateProject));

    ProjectDTO returnedUpdatedProject =
        projectResource.update(orgIdentifier, projectIdentifier, updateProjectDTO).getData().orElse(null);

    assertNotNull(returnedUpdatedProject);
    assertEquals(returnedUpdatedProject, projectResource.get(orgIdentifier, projectIdentifier).getData().orElse(null));

    assertEquals(updateProjectDTO.getName(), returnedUpdatedProject.getName());
    assertEquals(updateProjectDTO.getDescription(), returnedUpdatedProject.getDescription());
    assertEquals(updateProjectDTO.getOwners(), returnedUpdatedProject.getOwners());
    assertEquals(updateProjectDTO.getTags(), returnedUpdatedProject.getTags());

    assertEquals(createdProject.getIdentifier(), returnedUpdatedProject.getIdentifier());
    assertEquals(createdProject.getAccountIdentifier(), returnedUpdatedProject.getAccountIdentifier());
    assertEquals(createdProject.getOrgIdentifier(), returnedUpdatedProject.getOrgIdentifier());
  }

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testUpdateNonExistentProject() {
    String orgIdentifier = randomAlphabetic(10);
    String projectIdentifier = randomAlphabetic(10);
    when(projectService.get(orgIdentifier, projectIdentifier)).thenReturn(Optional.empty());

    UpdateProjectDTO updateProjectDTO = random(UpdateProjectDTO.class);

    Optional<ProjectDTO> updatedProject =
        projectResource.update(orgIdentifier, projectIdentifier, updateProjectDTO).getData();

    assertFalse(updatedProject.isPresent());
  }

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testDelete() {
    String orgIdentifier = randomAlphabetic(10);
    String projectIdentifier = randomAlphabetic(10);
    when(projectService.get(orgIdentifier, projectIdentifier)).thenReturn(Optional.empty());
    when(projectService.delete(orgIdentifier, projectIdentifier)).thenReturn(Boolean.TRUE);
    boolean isDeleted = projectResource.delete(orgIdentifier, projectIdentifier).getData();
    assertThat(isDeleted).isTrue();
    assertThat(projectResource.get(orgIdentifier, projectIdentifier).getData().isPresent()).isFalse();
  }
}