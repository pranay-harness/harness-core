package io.harness.ng.core.api.impl;

import static io.harness.ng.core.api.impl.AggregateProjectServiceImpl.PROJECT_ADMIN_ROLE_NAME;
import static io.harness.rule.OwnerRule.KARAN;
import static io.harness.utils.PageTestUtils.getPage;

import static java.util.Collections.emptyList;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNull;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.ng.core.dto.ProjectAggregateDTO;
import io.harness.ng.core.entities.Organization;
import io.harness.ng.core.entities.Project;
import io.harness.ng.core.invites.entities.Role;
import io.harness.ng.core.invites.entities.UserProjectMap;
import io.harness.ng.core.services.OrganizationService;
import io.harness.ng.core.services.ProjectService;
import io.harness.ng.core.user.User;
import io.harness.ng.core.user.services.api.NgUserService;
import io.harness.rule.Owner;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public class AggregateProjectServiceImplTest extends CategoryTest {
  private ProjectService projectService;
  private OrganizationService organizationService;
  private NgUserService ngUserService;
  private AggregateProjectServiceImpl aggregateProjectService;

  @Before
  public void setup() {
    projectService = mock(ProjectService.class);
    organizationService = mock(OrganizationService.class);
    ngUserService = mock(NgUserService.class);
    aggregateProjectService = spy(new AggregateProjectServiceImpl(projectService, organizationService, ngUserService));
  }

  private Project getProject(String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    return Project.builder()
        .accountIdentifier(accountIdentifier)
        .orgIdentifier(orgIdentifier)
        .identifier(projectIdentifier)
        .name(randomAlphabetic(10))
        .build();
  }

  private Organization getOrganization(String accountIdentifier, String orgIdentifier) {
    return Organization.builder()
        .accountIdentifier(accountIdentifier)
        .identifier(orgIdentifier)
        .name(randomAlphabetic(10))
        .build();
  }

  private List<UserProjectMap> getUserProjectMapList(
      String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    List<UserProjectMap> userProjectMapList = new ArrayList<>();
    List<String> roleNames = new ArrayList<>();
    roleNames.add(PROJECT_ADMIN_ROLE_NAME);
    roleNames.add("Project Viewer");
    roleNames.add("Project Editor");

    for (int i = 0; i < (1 << roleNames.size()); i++) {
      List<Role> roles = new ArrayList<>();
      for (int j = 0; j < roleNames.size(); j++) {
        if (((i >> j) & 1) == 1) {
          Role role = Role.builder()
                          .accountIdentifier(accountIdentifier)
                          .orgIdentifier(orgIdentifier)
                          .projectIdentifier(projectIdentifier)
                          .name(roleNames.get(j))
                          .build();
          roles.add(role);
        }
      }
      UserProjectMap userProjectMap = UserProjectMap.builder()
                                          .accountIdentifier(accountIdentifier)
                                          .orgIdentifier(orgIdentifier)
                                          .projectIdentifier(projectIdentifier)
                                          .userId(randomAlphabetic(10))
                                          .roles(roles)
                                          .build();
      userProjectMapList.add(userProjectMap);
    }
    return userProjectMapList;
  }

  private List<User> getUsers(List<UserProjectMap> userProjectMaps) {
    List<User> users = new ArrayList<>();
    userProjectMaps.forEach(userProjectMap -> users.add(User.builder().uuid(userProjectMap.getUserId()).build()));
    return users;
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testGet() {
    String accountIdentifier = randomAlphabetic(10);
    String orgIdentifier = randomAlphabetic(10);
    String projectIdentifier = randomAlphabetic(10);

    Project project = getProject(accountIdentifier, orgIdentifier, projectIdentifier);
    when(projectService.get(accountIdentifier, orgIdentifier, projectIdentifier)).thenReturn(Optional.of(project));

    Organization organization = getOrganization(accountIdentifier, orgIdentifier);
    when(organizationService.get(accountIdentifier, orgIdentifier)).thenReturn(Optional.of(organization));

    List<UserProjectMap> userProjectMaps = getUserProjectMapList(accountIdentifier, orgIdentifier, projectIdentifier);
    when(ngUserService.listUserProjectMap(any())).thenReturn(userProjectMaps);

    List<User> users = getUsers(userProjectMaps);
    when(ngUserService.getUsersByIds(any())).thenReturn(users);

    ProjectAggregateDTO projectAggregateDTO =
        aggregateProjectService.getProjectAggregateDTO(accountIdentifier, orgIdentifier, projectIdentifier);

    // project
    assertEquals(accountIdentifier, projectAggregateDTO.getProjectResponse().getProject().getAccountIdentifier());
    assertEquals(orgIdentifier, projectAggregateDTO.getProjectResponse().getProject().getOrgIdentifier());
    assertEquals(projectIdentifier, projectAggregateDTO.getProjectResponse().getProject().getIdentifier());
    assertEquals(project.getName(), projectAggregateDTO.getProjectResponse().getProject().getName());

    // organization
    assertEquals(accountIdentifier, projectAggregateDTO.getOrganization().getAccountIdentifier());
    assertEquals(orgIdentifier, projectAggregateDTO.getOrganization().getIdentifier());
    assertEquals(organization.getName(), projectAggregateDTO.getOrganization().getName());

    // admins and collaborators
    assertEquals(4, projectAggregateDTO.getAdmins().size());
    assertEquals(4, projectAggregateDTO.getCollaborators().size());
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testGet_OtherFieldsMissing() {
    String accountIdentifier = randomAlphabetic(10);
    String orgIdentifier = randomAlphabetic(10);
    String projectIdentifier = randomAlphabetic(10);

    Project project = getProject(accountIdentifier, orgIdentifier, projectIdentifier);
    when(projectService.get(accountIdentifier, orgIdentifier, projectIdentifier)).thenReturn(Optional.of(project));

    when(organizationService.get(accountIdentifier, orgIdentifier)).thenReturn(Optional.empty());

    when(ngUserService.listUserProjectMap(any())).thenReturn(emptyList());

    when(ngUserService.getUsersByIds(any())).thenReturn(emptyList());

    ProjectAggregateDTO projectAggregateDTO =
        aggregateProjectService.getProjectAggregateDTO(accountIdentifier, orgIdentifier, projectIdentifier);

    // project
    assertEquals(accountIdentifier, projectAggregateDTO.getProjectResponse().getProject().getAccountIdentifier());
    assertEquals(orgIdentifier, projectAggregateDTO.getProjectResponse().getProject().getOrgIdentifier());
    assertEquals(projectIdentifier, projectAggregateDTO.getProjectResponse().getProject().getIdentifier());
    assertEquals(project.getName(), projectAggregateDTO.getProjectResponse().getProject().getName());

    // organization
    assertNull(projectAggregateDTO.getOrganization());

    // admins and collaborators
    assertEquals(0, projectAggregateDTO.getAdmins().size());
    assertEquals(0, projectAggregateDTO.getCollaborators().size());
  }

  private List<Project> getProjects(String accountIdentifier, String orgIdentifier, int count) {
    List<Project> projects = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      projects.add(getProject(accountIdentifier, orgIdentifier, randomAlphabetic(10)));
    }
    return projects;
  }

  private List<Organization> getOrganizations(String accountIdentifier, String... orgIdentifiers) {
    List<Organization> organizations = new ArrayList<>();
    for (String orgIdentifier : orgIdentifiers) {
      organizations.add(getOrganization(accountIdentifier, orgIdentifier));
    }
    return organizations;
  }

  private List<UserProjectMap> getUserProjectMapList(List<Project> projects) {
    List<UserProjectMap> userProjectMapList = new ArrayList<>();
    projects.forEach(project -> {
      userProjectMapList.addAll(
          getUserProjectMapList(project.getAccountIdentifier(), project.getOrgIdentifier(), project.getIdentifier()));
    });
    return userProjectMapList;
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testList() {
    String accountIdentifier = randomAlphabetic(10);
    String orgIdentifier1 = randomAlphabetic(10);
    String orgIdentifier2 = randomAlphabetic(10);

    List<Project> projects = getProjects(accountIdentifier, orgIdentifier1, 2);
    projects.addAll(getProjects(accountIdentifier, orgIdentifier2, 3));
    when(projectService.list(accountIdentifier, Pageable.unpaged(), null)).thenReturn(getPage(projects, 5));

    List<Organization> organizations = getOrganizations(accountIdentifier, orgIdentifier1, orgIdentifier2);
    when(organizationService.list(any())).thenReturn(organizations);

    List<UserProjectMap> userProjectMaps = getUserProjectMapList(projects);
    when(ngUserService.listUserProjectMap(any())).thenReturn(userProjectMaps);

    List<User> users = getUsers(userProjectMaps);
    when(ngUserService.getUsersByIds(any())).thenReturn(users);

    Page<ProjectAggregateDTO> projectAggregateDTOs =
        aggregateProjectService.listProjectAggregateDTO(accountIdentifier, Pageable.unpaged(), null);

    // projects
    assertEquals(5, projectAggregateDTOs.getContent().size());

    // organizations
    projectAggregateDTOs.getContent().forEach(projectAggregateDTO
        -> assertEquals(projectAggregateDTO.getProjectResponse().getProject().getOrgIdentifier(),
            projectAggregateDTO.getOrganization().getIdentifier()));

    // admins and collaborators
    projectAggregateDTOs.getContent().forEach(
        projectAggregateDTO -> assertEquals(4, projectAggregateDTO.getAdmins().size()));
    projectAggregateDTOs.getContent().forEach(
        projectAggregateDTO -> assertEquals(4, projectAggregateDTO.getCollaborators().size()));
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testList_OtherFieldsMissing() {
    String accountIdentifier = randomAlphabetic(10);
    String orgIdentifier1 = randomAlphabetic(10);
    String orgIdentifier2 = randomAlphabetic(10);

    List<Project> projects = getProjects(accountIdentifier, orgIdentifier1, 2);
    projects.addAll(getProjects(accountIdentifier, orgIdentifier2, 3));
    when(projectService.list(accountIdentifier, Pageable.unpaged(), null)).thenReturn(getPage(projects, 5));

    when(organizationService.list(any())).thenReturn(emptyList());

    when(ngUserService.listUserProjectMap(any())).thenReturn(emptyList());

    when(ngUserService.getUsersByIds(any())).thenReturn(emptyList());

    Page<ProjectAggregateDTO> projectAggregateDTOs =
        aggregateProjectService.listProjectAggregateDTO(accountIdentifier, Pageable.unpaged(), null);

    // projects
    assertEquals(5, projectAggregateDTOs.getContent().size());

    // organizations
    projectAggregateDTOs.getContent().forEach(projectAggregateDTO -> assertNull(projectAggregateDTO.getOrganization()));

    // admins and collaborators
    projectAggregateDTOs.getContent().forEach(
        projectAggregateDTO -> assertEquals(0, projectAggregateDTO.getAdmins().size()));
    projectAggregateDTOs.getContent().forEach(
        projectAggregateDTO -> assertEquals(0, projectAggregateDTO.getCollaborators().size()));
  }
}
