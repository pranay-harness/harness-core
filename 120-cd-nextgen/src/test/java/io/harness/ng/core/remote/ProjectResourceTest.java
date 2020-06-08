package io.harness.ng.core.remote;

import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.rule.OwnerRule.ANKIT;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.ng.core.BaseTest;
import io.harness.ng.core.dto.CreateProjectRequest;
import io.harness.ng.core.dto.ProjectDTO;
import io.harness.ng.core.dto.UpdateProjectRequest;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProjectResourceTest extends BaseTest {
  @Inject private ProjectResource projectResource;

  @Test
  @Owner(developers = ANKIT)
  @Category(UnitTests.class)
  public void testCreateProject() {
    assertFalse(projectResource.get(randomAlphabetic(10)).isPresent());

    CreateProjectRequest createRequest = random(CreateProjectRequest.class);
    ProjectDTO createdProject = projectResource.create(createRequest);

    assertTrue(isNotEmpty(createdProject.getId()));
    assertEquals(createdProject, projectResource.get(createdProject.getId()).orElse(null));
  }

  @Test
  @Owner(developers = ANKIT)
  @Category(UnitTests.class)
  public void testListProjects() {
    String orgId = randomAlphabetic(10);
    assertTrue(projectResource.list(orgId).isEmpty());

    List<ProjectDTO> createdProjects = new ArrayList<>();
    for (int i = 0; i < 2; i++) {
      CreateProjectRequest createRequest = random(CreateProjectRequest.class);
      createdProjects.add(projectResource.create(createRequest));
    }

    assertEquals(createdProjects, projectResource.list(orgId));
  }

  @Test
  @Owner(developers = ANKIT)
  @Category(UnitTests.class)
  public void testUpdateExistentProject() {
    CreateProjectRequest createRequest = random(CreateProjectRequest.class);
    ProjectDTO createdProject = projectResource.create(createRequest);

    UpdateProjectRequest updateRequest = random(UpdateProjectRequest.class);
    ProjectDTO updatedProject = projectResource.update(createdProject.getId(), updateRequest).orElse(null);

    assertNotNull(updatedProject);
    assertEquals(updatedProject, projectResource.get(createdProject.getId()).orElse(null));

    assertEquals(updateRequest.getName(), updatedProject.getName());
    assertEquals(updateRequest.getDescription(), updatedProject.getDescription());
    assertEquals(updateRequest.getOwners(), updatedProject.getOwners());
    assertEquals(updateRequest.getTags(), updatedProject.getTags());

    assertEquals(createdProject.getId(), updatedProject.getId());
    assertEquals(createdProject.getAccountId(), updatedProject.getAccountId());
    assertEquals(createdProject.getOrgId(), updatedProject.getOrgId());
  }

  @Test
  @Owner(developers = ANKIT)
  @Category(UnitTests.class)
  public void testUpdateNonExistentProject() {
    UpdateProjectRequest updateRequest = random(UpdateProjectRequest.class);
    Optional<ProjectDTO> updatedProject = projectResource.update(randomAlphabetic(10), updateRequest);

    assertFalse(updatedProject.isPresent());
  }
}