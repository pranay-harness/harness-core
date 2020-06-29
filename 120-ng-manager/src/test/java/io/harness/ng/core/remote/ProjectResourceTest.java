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
  private static final String ACCOUNT_ID = "kmpySmUISimoRrJL6NL73w";
  @Inject private ProjectResource projectResource;

  @Test
  @Owner(developers = ANKIT)
  @Category(UnitTests.class)
  public void testCreateProject() {
    assertFalse(projectResource.get(randomAlphabetic(10)).getData().isPresent());

    CreateProjectDTO createProjectDTO = random(CreateProjectDTO.class);
    ProjectDTO projectDTO = projectResource.create(createProjectDTO).getData();

    assertTrue(isNotEmpty(projectDTO.getId()));
    assertEquals(projectDTO, projectResource.get(projectDTO.getId()).getData().orElse(null));
  }

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testList_For_OrgIdFilterOnly() {
    String orgId = "ABC";
    assertTrue(projectResource.list(orgId, "", 0, 10, null).getData().isEmpty());

    List<ProjectDTO> createdProjectDTOs = createProjects(orgId, ACCOUNT_ID, 2);

    Page<ProjectDTO> projectDTOs = projectResource.list(orgId, "", 0, 10, null).getData();
    assertNotNull("ProjectDTO should not be null", projectDTOs);
    assertEquals("Count of DTOs should match", createdProjectDTOs.size(), projectDTOs.getTotalElements());
    assertNotNull("Page contents should not be null", projectDTOs.getContent());

    List<ProjectDTO> returnedDTOs = projectDTOs.getContent();

    assertNotNull("Returned project DTOs page should not null ", returnedDTOs);
    assertEquals(
        "Returned project DTOs page size should match created DTOs", createdProjectDTOs.size(), returnedDTOs.size());

    createdProjectDTOs.forEach(createdDTO -> {
      boolean isPresentInResult = projectDTOs.getContent().stream().anyMatch(
          dto -> dto.getOrgId().equals(orgId) && dto.getId().equals(createdDTO.getId()));
      assertTrue("Fetched DTO should be present ", isPresentInResult);
    });
  }

  private List<ProjectDTO> createProjects(String orgId, String accountId, int count) {
    return IntStream.range(0, count)
        .mapToObj(i -> {
          CreateProjectDTO projectDTO = random(CreateProjectDTO.class);
          projectDTO.setOrgId(orgId);
          projectDTO.setAccountId(accountId);
          return projectResource.create(projectDTO).getData();
        })
        .collect(Collectors.toList());
  }

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testList_For_FilterQuery_With_OrgId_Or_AccountId() {
    String firstOrgId = "ABC";

    List<ProjectDTO> firstOrgProjectDTOs = createProjects(firstOrgId, ACCOUNT_ID, 2);

    String secondOrgId = "XYZ";

    List<ProjectDTO> secondOrgProjectDTOs = createProjects(secondOrgId, ACCOUNT_ID, 2);

    final Page<ProjectDTO> projectDTOS = projectResource.list(null, "orgId==" + firstOrgId, 0, 10, null).getData();
    assertNotNull("ProjectDTO should not be null", projectDTOS);
    assertEquals("Count of DTOs should match", firstOrgProjectDTOs.size(), projectDTOS.getTotalElements());
    assertNotNull("Page contents should not be null", projectDTOS.getContent());

    List<ProjectDTO> returnedDTOs = projectDTOS.getContent();

    assertNotNull("Returned project DTOs page should not null ", returnedDTOs);
    assertEquals(
        "Returned project DTOs page size should match created DTOs", firstOrgProjectDTOs.size(), returnedDTOs.size());

    firstOrgProjectDTOs.forEach(createdDTO -> {
      boolean isPresentInResult = projectDTOS.getContent().stream().anyMatch(
          dto -> dto.getOrgId().equals(firstOrgId) && dto.getId().equals(createdDTO.getId()));
      assertTrue("Fetched DTO should be present ", isPresentInResult);
    });

    final Page<ProjectDTO> allProjectDTOS =
        projectResource.list(null, "accountId==" + ACCOUNT_ID, 0, 10, null).getData();
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
          dto -> dto.getOrgId().equals(firstOrgId) && dto.getId().equals(createdDTO.getId()));
      assertTrue("Fetched DTO should be present ", isPresentInResult);
    });
  }

  @Test
  @Owner(developers = ANKIT)
  @Category(UnitTests.class)
  public void testUpdateExistentProject() {
    CreateProjectDTO createProjectDTO = random(CreateProjectDTO.class);
    ProjectDTO createdProject = projectResource.create(createProjectDTO).getData();

    UpdateProjectDTO updateProjectDTO = random(UpdateProjectDTO.class);
    ProjectDTO updatedProject = projectResource.update(createdProject.getId(), updateProjectDTO).getData().orElse(null);

    assertNotNull(updatedProject);
    assertEquals(updatedProject, projectResource.get(createdProject.getId()).getData().orElse(null));

    assertEquals(updateProjectDTO.getName(), updatedProject.getName());
    assertEquals(updateProjectDTO.getDescription(), updatedProject.getDescription());
    assertEquals(updateProjectDTO.getOwners(), updatedProject.getOwners());
    assertEquals(updateProjectDTO.getTags(), updatedProject.getTags());

    assertEquals(createdProject.getId(), updatedProject.getId());
    assertEquals(createdProject.getAccountId(), updatedProject.getAccountId());
    assertEquals(createdProject.getOrgId(), updatedProject.getOrgId());
  }

  @Test
  @Owner(developers = ANKIT)
  @Category(UnitTests.class)
  public void testUpdateNonExistentProject() {
    UpdateProjectDTO updateProjectDTO = random(UpdateProjectDTO.class);
    Optional<ProjectDTO> updatedProject = projectResource.update(randomAlphabetic(10), updateProjectDTO).getData();

    assertFalse(updatedProject.isPresent());
  }

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testDelete() {
    CreateProjectDTO createOrganizationDTO = random(CreateProjectDTO.class);
    String accountId = randomAlphabetic(10);
    createOrganizationDTO.setAccountId(accountId);
    ProjectDTO firstOrganization = projectResource.create(createOrganizationDTO).getData();

    boolean isDeleted = projectResource.delete(firstOrganization.getId()).getData();
    assertThat(isDeleted).isTrue();
    assertThat(projectResource.get(firstOrganization.getId()).getData().isPresent()).isFalse();
  }
}