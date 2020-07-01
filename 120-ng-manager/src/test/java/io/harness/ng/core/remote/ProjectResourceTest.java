package io.harness.ng.core.remote;

import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.rule.OwnerRule.ANKIT;
import static io.harness.rule.OwnerRule.VIKAS;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.ng.core.BaseTest;
import io.harness.ng.core.dto.CreateProjectDTO;
import io.harness.ng.core.dto.ProjectDTO;
import io.harness.ng.core.dto.UpdateProjectDTO;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ProjectResourceTest extends BaseTest {
  @Inject private ProjectResource projectResource;
  @Inject private AccountResource accountResource;

  @Test
  @Owner(developers = ANKIT)
  @Category(UnitTests.class)
  public void testCreateProject() {
    assertFalse(projectResource.get(randomAlphabetic(10), randomAlphabetic(10)).getData().isPresent());

    String orgIdentifier = randomAlphabetic(10);
    CreateProjectDTO createProjectDTO = random(CreateProjectDTO.class);
    ProjectDTO projectDTO = projectResource.create(orgIdentifier, createProjectDTO).getData();

    assertTrue(isNotEmpty(projectDTO.getId()));
    assertEquals(projectDTO, projectResource.get(orgIdentifier, projectDTO.getIdentifier()).getData().orElse(null));
  }

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testList_For_OrgIdFilterOnly() {
    String accountIdentifier = randomAlphabetic(10);
    String orgIdentifier = randomAlphabetic(10);
    assertTrue(projectResource.listProjectsForOrganization(orgIdentifier, 0, 10, null).getData().isEmpty());

    List<ProjectDTO> createdProjectDTOs = createProjects(orgIdentifier, accountIdentifier, 2);

    Page<ProjectDTO> projectDTOs = projectResource.listProjectsForOrganization(orgIdentifier, 0, 10, null).getData();
    assertNotNull("ProjectDTO should not be null", projectDTOs);
    assertEquals("Count of DTOs should match", createdProjectDTOs.size(), projectDTOs.getTotalElements());
    assertNotNull("Page contents should not be null", projectDTOs.getContent());

    List<ProjectDTO> returnedDTOs = projectDTOs.getContent();

    assertNotNull("Returned project DTOs page should not null ", returnedDTOs);
    assertEquals(
        "Returned project DTOs page size should match created DTOs", createdProjectDTOs.size(), returnedDTOs.size());

    createdProjectDTOs.forEach(createdDTO -> {
      boolean isPresentInResult = projectDTOs.getContent().stream().anyMatch(
          dto -> dto.getOrgIdentifier().equals(orgIdentifier) && dto.getId().equals(createdDTO.getId()));
      assertTrue("Fetched DTO should be present ", isPresentInResult);
    });
  }

  private List<ProjectDTO> createProjects(String orgIdentifier, String accountIdentifier, int count) {
    return IntStream.range(0, count)
        .mapToObj(i -> {
          CreateProjectDTO projectDTO = random(CreateProjectDTO.class);
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

    List<ProjectDTO> firstOrgProjectDTOs = createProjects(firstOrgIdentifier, accountIdentifier, 2);

    List<ProjectDTO> secondOrgProjectDTOs = createProjects(secondOrgIdentifier, accountIdentifier, 2);

    final Page<ProjectDTO> projectDTOS =
        projectResource.listProjectsForOrganization(firstOrgIdentifier, 0, 10, null).getData();
    assertNotNull("ProjectDTO should not be null", projectDTOS);
    assertEquals("Count of DTOs should match", firstOrgProjectDTOs.size(), projectDTOS.getTotalElements());
    assertNotNull("Page contents should not be null", projectDTOS.getContent());

    List<ProjectDTO> returnedDTOs = projectDTOS.getContent();

    assertNotNull("Returned project DTOs page should not null ", returnedDTOs);
    assertEquals(
        "Returned project DTOs page size should match created DTOs", firstOrgProjectDTOs.size(), returnedDTOs.size());

    firstOrgProjectDTOs.forEach(createdDTO -> {
      boolean isPresentInResult = projectDTOS.getContent().stream().anyMatch(
          dto -> dto.getOrgIdentifier().equals(firstOrgIdentifier) && dto.getId().equals(createdDTO.getId()));
      assertTrue("Fetched DTO should be present ", isPresentInResult);
    });

    final Page<ProjectDTO> allProjectDTOS =
        accountResource.listProjectsForAccount(accountIdentifier, 0, 10, null).getData();
    assertNotNull("ProjectDTO should not be null", projectDTOS);
    assertEquals("Count of DTOs should match", firstOrgProjectDTOs.size() + secondOrgProjectDTOs.size(),
        allProjectDTOS.getTotalElements());
    assertNotNull("Page contents should not be null", allProjectDTOS.getContent());

    returnedDTOs = allProjectDTOS.getContent();

    assertNotNull("Returned project DTOs page should not null ", returnedDTOs);
    assertEquals("Returned project DTOs page size should match created DTOs",
        firstOrgProjectDTOs.size() + secondOrgProjectDTOs.size(), returnedDTOs.size());

    firstOrgProjectDTOs.forEach(createdDTO -> {
      boolean isPresentInResult = allProjectDTOS.getContent().stream().anyMatch(
          dto -> dto.getOrgIdentifier().equals(firstOrgIdentifier) && dto.getId().equals(createdDTO.getId()));
      assertTrue("Fetched DTO should be present ", isPresentInResult);
    });
  }

  @Test
  @Owner(developers = ANKIT)
  @Category(UnitTests.class)
  public void testUpdateExistentProject() {
    String orgIdentifier = randomAlphabetic(10);
    CreateProjectDTO createProjectDTO = random(CreateProjectDTO.class);
    ProjectDTO createdProject = projectResource.create(orgIdentifier, createProjectDTO).getData();

    UpdateProjectDTO updateProjectDTO = random(UpdateProjectDTO.class);
    ProjectDTO updatedProject =
        projectResource.update(orgIdentifier, createdProject.getIdentifier(), updateProjectDTO).getData().orElse(null);

    assertNotNull(updatedProject);
    assertEquals(
        updatedProject, projectResource.get(orgIdentifier, createdProject.getIdentifier()).getData().orElse(null));

    assertEquals(updateProjectDTO.getName(), updatedProject.getName());
    assertEquals(updateProjectDTO.getDescription(), updatedProject.getDescription());
    assertEquals(updateProjectDTO.getOwners(), updatedProject.getOwners());
    assertEquals(updateProjectDTO.getTags(), updatedProject.getTags());

    assertEquals(createdProject.getId(), updatedProject.getId());
    assertEquals(createdProject.getAccountIdentifier(), updatedProject.getAccountIdentifier());
    assertEquals(createdProject.getOrgIdentifier(), updatedProject.getOrgIdentifier());
  }

  @Test
  @Owner(developers = ANKIT)
  @Category(UnitTests.class)
  public void testUpdateNonExistentProject() {
    String orgIdentifier = randomAlphabetic(10);
    UpdateProjectDTO updateProjectDTO = random(UpdateProjectDTO.class);
    Optional<ProjectDTO> updatedProject =
        projectResource.update(orgIdentifier, randomAlphabetic(10), updateProjectDTO).getData();

    assertFalse(updatedProject.isPresent());
  }

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testDelete() {
    String orgIdentifier = randomAlphabetic(10);
    CreateProjectDTO createOrganizationDTO = random(CreateProjectDTO.class);
    String accountIdentifier = randomAlphabetic(10);
    createOrganizationDTO.setAccountIdentifier(accountIdentifier);
    ProjectDTO firstOrganization = projectResource.create(orgIdentifier, createOrganizationDTO).getData();

    boolean isDeleted = projectResource.delete(orgIdentifier, firstOrganization.getIdentifier()).getData();
    assertThat(isDeleted).isTrue();
    assertThat(projectResource.get(orgIdentifier, firstOrganization.getIdentifier()).getData().isPresent()).isFalse();
  }
}