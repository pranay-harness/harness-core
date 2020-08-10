package io.harness.ng.core.remote;

import static io.harness.rule.OwnerRule.KARAN;
import static io.harness.rule.OwnerRule.VIKAS;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.beans.NGPageResponse;
import io.harness.category.element.UnitTests;
import io.harness.ng.ModuleType;
import io.harness.ng.core.RestQueryFilterParser;
import io.harness.ng.core.dto.ProjectDTO;
import io.harness.ng.core.entities.Project;
import io.harness.ng.core.io.harness.ng.utils.PageTestUtils;
import io.harness.ng.core.services.OrganizationService;
import io.harness.ng.core.services.ProjectService;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.ArrayList;
import java.util.List;

public class AccountProjectResourceTest extends CategoryTest {
  private ProjectService projectService;
  private OrganizationService organizationService;
  private AccountProjectResource accountProjectResource;

  @Before
  public void doSetup() {
    projectService = mock(ProjectService.class);
    organizationService = mock(OrganizationService.class);
    accountProjectResource =
        new AccountProjectResource(projectService, organizationService, new RestQueryFilterParser());
  }

  private Project createProject(String orgIdentifier, String accountIdentifier, ModuleType moduleType) {
    return Project.builder()
        .id(randomAlphabetic(10))
        .accountIdentifier(accountIdentifier)
        .orgIdentifier(orgIdentifier)
        .identifier(randomAlphabetic(10))
        .module(moduleType)
        .build();
  }

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testList() {
    String orgIdentifier = randomAlphabetic(10);
    String accountIdentifier = randomAlphabetic(10);

    List<Project> projectList = new ArrayList<>();

    projectList.add(createProject(orgIdentifier, accountIdentifier, ModuleType.CD));
    projectList.add(createProject(orgIdentifier, accountIdentifier, ModuleType.CD));
    when(projectService.list(any(Criteria.class), any(Pageable.class)))
        .thenReturn(PageTestUtils.getPage(projectList, 2));

    when(organizationService.list(any(), any())).thenReturn(Page.empty());
    final NGPageResponse<ProjectDTO> allProjectDTOS =
        accountProjectResource.listProjectsBasedOnFilter(accountIdentifier, null, null, 0, 10, null).getData();
    assertNotNull("ProjectDTO should not be null", allProjectDTOS);
    assertEquals("Count of DTOs should match", projectList.size(), allProjectDTOS.getTotalElements());
    assertNotNull("Page contents should not be null", allProjectDTOS.getContent());

    List<ProjectDTO> returnedDTOs = allProjectDTOS.getContent();

    assertNotNull("Returned project DTOs page should not null ", returnedDTOs);
    assertEquals("Returned project DTOs page size should match created DTOs", projectList.size(), returnedDTOs.size());

    projectList.forEach(project -> {
      boolean isPresentInResult = allProjectDTOS.getContent().stream().anyMatch(
          dto -> dto.getOrgIdentifier().equals(orgIdentifier) && dto.getIdentifier().equals(project.getIdentifier()));
      assertTrue("Fetched DTO should be present ", isPresentInResult);
    });
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testList_For_FilterQuery_With_ModuleType() {
    ModuleType moduleType = ModuleType.CD;
    String orgIdentifier = randomAlphabetic(10);
    String accountIdentifier = randomAlphabetic(10);

    List<Project> projectList = new ArrayList<>();

    projectList.add(createProject(orgIdentifier, accountIdentifier, ModuleType.CD));
    projectList.add(createProject(orgIdentifier, accountIdentifier, ModuleType.CD));
    when(projectService.list(any(Criteria.class), any(Pageable.class)))
        .thenReturn(PageTestUtils.getPage(projectList, 2));

    when(organizationService.list(any(), any())).thenReturn(Page.empty());
    String filterQuery = "modules=in=(" + moduleType + ")";
    final NGPageResponse<ProjectDTO> projectDTOS =
        accountProjectResource.listProjectsBasedOnFilter(accountIdentifier, filterQuery, null, 0, 10, null).getData();

    assertNotNull(projectDTOS);
    assertNotNull("Page contents should not be null", projectDTOS.getContent());

    List<ProjectDTO> returnedDTOs = projectDTOS.getContent();

    assertNotNull("Returned project DTOs page should not null ", returnedDTOs);
    assertEquals(returnedDTOs.size(), projectList.size());

    projectList.forEach(createdDTO -> {
      if (createdDTO.getModules().contains(moduleType)) {
        boolean isPresentInResult = projectDTOS.getContent().stream().anyMatch(dto
            -> dto.getOrgIdentifier().equals(orgIdentifier) && dto.getIdentifier().equals(createdDTO.getIdentifier()));
        assertTrue("Fetched DTO should be present ", isPresentInResult);
      }
    });
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testList_For_Search() {
    String orgIdentifier = randomAlphabetic(10);
    String accountIdentifier = randomAlphabetic(10);

    List<Project> projectList = new ArrayList<>();

    projectList.add(createProject(orgIdentifier, accountIdentifier, ModuleType.CD));
    projectList.add(createProject(orgIdentifier, accountIdentifier, ModuleType.CD));
    when(projectService.list(any(Criteria.class), any(Pageable.class)))
        .thenReturn(PageTestUtils.getPage(projectList, 2));

    when(organizationService.list(any(), any())).thenReturn(Page.empty());
    String search = "text";
    final NGPageResponse<ProjectDTO> projectDTOS =
        accountProjectResource.listProjectsBasedOnFilter(accountIdentifier, "", search, 0, 10, null).getData();

    assertNotNull(projectDTOS);
    assertNotNull("Page contents should not be null", projectDTOS.getContent());

    List<ProjectDTO> returnedDTOs = projectDTOS.getContent();

    assertNotNull("Returned project DTOs page should not null ", returnedDTOs);
    assertEquals(returnedDTOs.size(), projectList.size());
  }
}
