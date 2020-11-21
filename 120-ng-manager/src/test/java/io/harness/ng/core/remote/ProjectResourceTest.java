package io.harness.ng.core.remote;

import static io.harness.ng.core.remote.ProjectMapper.toProject;
import static io.harness.rule.OwnerRule.KARAN;
import static io.harness.utils.PageTestUtils.getPage;

import static java.lang.Long.parseLong;
import static java.util.Collections.singletonList;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.ModuleType;
import io.harness.category.element.UnitTests;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.dto.ProjectDTO;
import io.harness.ng.core.dto.ProjectFilterDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.core.entities.Project;
import io.harness.ng.core.services.ProjectService;
import io.harness.rule.Owner;

import java.util.Optional;
import javax.ws.rs.NotFoundException;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;

public class ProjectResourceTest extends CategoryTest {
  private ProjectService projectService;
  private ProjectResource projectResource;

  String accountIdentifier = randomAlphabetic(10);
  String orgIdentifier = randomAlphabetic(10);
  String identifier = randomAlphabetic(10);
  String name = randomAlphabetic(10);

  @Before
  public void setup() {
    projectService = mock(ProjectService.class);
    projectResource = new ProjectResource(projectService);
  }

  private ProjectDTO getProjectDTO(String accountIdentifier, String orgIdentifier, String identifier, String name) {
    return ProjectDTO.builder()
        .accountIdentifier(accountIdentifier)
        .orgIdentifier(orgIdentifier)
        .identifier(identifier)
        .name(name)
        .build();
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testCreate() {
    ProjectDTO projectDTO = getProjectDTO(accountIdentifier, orgIdentifier, identifier, name);
    Project project = toProject(projectDTO);
    project.setVersion((long) 0);

    when(projectService.create(accountIdentifier, orgIdentifier, projectDTO)).thenReturn(project);

    ResponseDTO<ProjectDTO> responseDTO = projectResource.create(accountIdentifier, orgIdentifier, projectDTO);

    assertEquals(project.getVersion().toString(), responseDTO.getEntityTag());
    assertEquals(accountIdentifier, responseDTO.getData().getAccountIdentifier());
    assertEquals(orgIdentifier, responseDTO.getData().getOrgIdentifier());
    assertEquals(identifier, responseDTO.getData().getIdentifier());
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testGet() {
    ProjectDTO projectDTO = getProjectDTO(accountIdentifier, orgIdentifier, identifier, name);
    Project project = toProject(projectDTO);
    project.setVersion((long) 0);

    when(projectService.get(accountIdentifier, orgIdentifier, identifier)).thenReturn(Optional.of(project));

    ResponseDTO<ProjectDTO> responseDTO = projectResource.get(identifier, accountIdentifier, orgIdentifier);

    assertEquals(project.getVersion().toString(), responseDTO.getEntityTag());
    assertEquals(accountIdentifier, responseDTO.getData().getAccountIdentifier());
    assertEquals(orgIdentifier, responseDTO.getData().getOrgIdentifier());
    assertEquals(identifier, responseDTO.getData().getIdentifier());

    when(projectService.get(accountIdentifier, orgIdentifier, identifier)).thenReturn(Optional.empty());

    boolean exceptionThrown = false;
    try {
      projectResource.get(identifier, accountIdentifier, orgIdentifier);
    } catch (NotFoundException exception) {
      exceptionThrown = true;
    }

    assertTrue(exceptionThrown);
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testList() {
    String searchTerm = randomAlphabetic(10);
    PageRequest pageRequest = PageRequest.builder().pageIndex(0).pageSize(10).build();
    ProjectDTO projectDTO = getProjectDTO(accountIdentifier, orgIdentifier, identifier, name);
    projectDTO.setModules(singletonList(ModuleType.CD));
    Project project = toProject(projectDTO);
    project.setVersion((long) 0);
    ArgumentCaptor<ProjectFilterDTO> argumentCaptor = ArgumentCaptor.forClass(ProjectFilterDTO.class);

    when(projectService.list(eq(accountIdentifier), any(), any())).thenReturn(getPage(singletonList(project), 1));

    ResponseDTO<PageResponse<ProjectDTO>> response =
        projectResource.list(accountIdentifier, orgIdentifier, true, ModuleType.CD, searchTerm, pageRequest);

    verify(projectService, times(1)).list(eq(accountIdentifier), any(), argumentCaptor.capture());
    ProjectFilterDTO projectFilterDTO = argumentCaptor.getValue();

    assertEquals(searchTerm, projectFilterDTO.getSearchTerm());
    assertEquals(ModuleType.CD, projectFilterDTO.getModuleType());
    assertEquals(1, response.getData().getPageItemCount());
    assertEquals(accountIdentifier, response.getData().getContent().get(0).getAccountIdentifier());
    assertEquals(orgIdentifier, response.getData().getContent().get(0).getOrgIdentifier());
    assertEquals(identifier, response.getData().getContent().get(0).getIdentifier());
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testUpdate() {
    String ifMatch = "0";
    ProjectDTO projectDTO = getProjectDTO(accountIdentifier, orgIdentifier, identifier, name);
    Project project = toProject(projectDTO);
    project.setVersion(parseLong(ifMatch) + 1);

    when(projectService.update(accountIdentifier, orgIdentifier, identifier, projectDTO)).thenReturn(project);

    ResponseDTO<ProjectDTO> response =
        projectResource.update(ifMatch, identifier, accountIdentifier, orgIdentifier, projectDTO);

    assertEquals("1", response.getEntityTag());
    assertEquals(accountIdentifier, response.getData().getAccountIdentifier());
    assertEquals(orgIdentifier, response.getData().getOrgIdentifier());
    assertEquals(identifier, response.getData().getIdentifier());
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testDelete() {
    String ifMatch = "0";

    when(projectService.delete(accountIdentifier, orgIdentifier, identifier, Long.valueOf(ifMatch))).thenReturn(true);

    ResponseDTO<Boolean> response = projectResource.delete(ifMatch, identifier, accountIdentifier, orgIdentifier);

    assertNull(response.getEntityTag());
    assertTrue(response.getData());
  }
}
