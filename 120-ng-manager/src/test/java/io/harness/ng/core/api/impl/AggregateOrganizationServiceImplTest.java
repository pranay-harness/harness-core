package io.harness.ng.core.api.impl;

import static io.harness.ng.core.api.impl.AggregateOrganizationServiceImpl.ORGANIZATION_ADMIN_ROLE_NAME;
import static io.harness.rule.OwnerRule.KARAN;
import static io.harness.utils.PageTestUtils.getPage;

import static java.util.Collections.emptyList;
import static junit.framework.TestCase.assertEquals;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.ng.core.dto.OrganizationAggregateDTO;
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
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public class AggregateOrganizationServiceImplTest extends CategoryTest {
  private ProjectService projectService;
  private OrganizationService organizationService;
  private NgUserService ngUserService;
  private AggregateOrganizationServiceImpl aggregateOrganizationService;

  @Before
  public void setup() {
    projectService = mock(ProjectService.class);
    organizationService = mock(OrganizationService.class);
    ngUserService = mock(NgUserService.class);
    aggregateOrganizationService =
        spy(new AggregateOrganizationServiceImpl(organizationService, projectService, ngUserService));
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
    roleNames.add(ORGANIZATION_ADMIN_ROLE_NAME);
    roleNames.add("Organization Viewer");
    roleNames.add("Organization Editor");

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
    userProjectMaps.forEach(userProjectMap -> { users.add(User.builder().uuid(userProjectMap.getUserId()).build()); });
    return users;
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testGet() {
    String accountIdentifier = randomAlphabetic(10);
    String orgIdentifier = randomAlphabetic(10);

    Organization organization = getOrganization(accountIdentifier, orgIdentifier);
    when(organizationService.get(accountIdentifier, orgIdentifier)).thenReturn(Optional.of(organization));

    List<Project> projects = getProjects(accountIdentifier, orgIdentifier, 3);
    when(projectService.list(any())).thenReturn(projects);

    List<UserProjectMap> userProjectMaps = getUserProjectMapList(accountIdentifier, orgIdentifier, null);
    when(ngUserService.listUserProjectMap(any())).thenReturn(userProjectMaps);

    List<User> users = getUsers(userProjectMaps);
    when(ngUserService.getUsersByIds(any())).thenReturn(users);

    OrganizationAggregateDTO organizationAggregateDTO =
        aggregateOrganizationService.getOrganizationAggregateDTO(accountIdentifier, orgIdentifier);

    // organization
    assertEquals(
        accountIdentifier, organizationAggregateDTO.getOrganizationResponse().getOrganization().getAccountIdentifier());
    assertEquals(orgIdentifier, organizationAggregateDTO.getOrganizationResponse().getOrganization().getIdentifier());
    assertEquals(
        organization.getName(), organizationAggregateDTO.getOrganizationResponse().getOrganization().getName());

    // projects
    assertEquals(3, organizationAggregateDTO.getProjectsCount());

    // admins and collaborators
    assertEquals(4, organizationAggregateDTO.getAdmins().size());
    assertEquals(4, organizationAggregateDTO.getCollaborators().size());
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testGet_OtherFieldsMissing() {
    String accountIdentifier = randomAlphabetic(10);
    String orgIdentifier = randomAlphabetic(10);

    Organization organization = getOrganization(accountIdentifier, orgIdentifier);
    when(organizationService.get(accountIdentifier, orgIdentifier)).thenReturn(Optional.of(organization));

    when(projectService.list(any())).thenReturn(emptyList());

    when(ngUserService.listUserProjectMap(any())).thenReturn(emptyList());

    when(ngUserService.getUsersByIds(any())).thenReturn(emptyList());

    OrganizationAggregateDTO organizationAggregateDTO =
        aggregateOrganizationService.getOrganizationAggregateDTO(accountIdentifier, orgIdentifier);

    // organization
    assertEquals(
        accountIdentifier, organizationAggregateDTO.getOrganizationResponse().getOrganization().getAccountIdentifier());
    assertEquals(orgIdentifier, organizationAggregateDTO.getOrganizationResponse().getOrganization().getIdentifier());
    assertEquals(
        organization.getName(), organizationAggregateDTO.getOrganizationResponse().getOrganization().getName());

    // projects
    assertEquals(0, organizationAggregateDTO.getProjectsCount());

    // admins and collaborators
    assertEquals(0, organizationAggregateDTO.getAdmins().size());
    assertEquals(0, organizationAggregateDTO.getCollaborators().size());
  }

  private List<Project> getProjects(String accountIdentifier, String orgIdentifier, int count) {
    List<Project> projects = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      projects.add(getProject(accountIdentifier, orgIdentifier, randomAlphabetic(10)));
    }
    return projects;
  }

  private List<Project> getProjects(String accountIdentifier, List<String> orgIdentifiers) {
    List<Project> projects = new ArrayList<>();
    orgIdentifiers.forEach(orgIdentifier -> projects.addAll(getProjects(accountIdentifier, orgIdentifier, 3)));
    return projects;
  }

  private List<Organization> getOrganizations(String accountIdentifier, int count) {
    List<Organization> organizations = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      organizations.add(getOrganization(accountIdentifier, randomAlphabetic(10)));
    }
    return organizations;
  }

  private List<UserProjectMap> getUserProjectMapList(List<Organization> organizations) {
    List<UserProjectMap> userProjectMapList = new ArrayList<>();
    organizations.forEach(project
        -> userProjectMapList.addAll(
            getUserProjectMapList(project.getAccountIdentifier(), project.getIdentifier(), null)));
    return userProjectMapList;
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testList() {
    String accountIdentifier = randomAlphabetic(10);

    List<Organization> organizations = getOrganizations(accountIdentifier, 3);
    when(organizationService.list(accountIdentifier, Pageable.unpaged(), null)).thenReturn(getPage(organizations, 3));

    List<Project> projects = getProjects(
        accountIdentifier, organizations.stream().map(Organization::getIdentifier).collect(Collectors.toList()));
    when(projectService.list(any())).thenReturn(projects);

    List<UserProjectMap> userProjectMaps = getUserProjectMapList(organizations);
    when(ngUserService.listUserProjectMap(any())).thenReturn(userProjectMaps);

    List<User> users = getUsers(userProjectMaps);
    when(ngUserService.getUsersByIds(any())).thenReturn(users);

    Page<OrganizationAggregateDTO> organizationAggregateDTOs =
        aggregateOrganizationService.listOrganizationAggregateDTO(accountIdentifier, Pageable.unpaged(), null);

    // organizations
    assertEquals(3, organizationAggregateDTOs.getContent().size());

    // projects
    organizationAggregateDTOs.getContent().forEach(
        organizationAggregateDTO -> assertEquals(3, organizationAggregateDTO.getProjectsCount()));

    // admins and collaborators
    organizationAggregateDTOs.getContent().forEach(
        organizationAggregateDTO -> assertEquals(4, organizationAggregateDTO.getAdmins().size()));
    organizationAggregateDTOs.getContent().forEach(
        organizationAggregateDTO -> assertEquals(4, organizationAggregateDTO.getCollaborators().size()));
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testList_OtherFieldsMissing() {
    String accountIdentifier = randomAlphabetic(10);

    List<Organization> organizations = getOrganizations(accountIdentifier, 3);
    when(organizationService.list(accountIdentifier, Pageable.unpaged(), null)).thenReturn(getPage(organizations, 3));

    when(projectService.list(any())).thenReturn(emptyList());

    when(ngUserService.listUserProjectMap(any())).thenReturn(emptyList());

    when(ngUserService.getUsersByIds(any())).thenReturn(emptyList());

    Page<OrganizationAggregateDTO> organizationAggregateDTOs =
        aggregateOrganizationService.listOrganizationAggregateDTO(accountIdentifier, Pageable.unpaged(), null);

    // organizations
    assertEquals(3, organizationAggregateDTOs.getContent().size());

    // projects
    organizationAggregateDTOs.getContent().forEach(
        organizationAggregateDTO -> assertEquals(0, organizationAggregateDTO.getProjectsCount()));

    // admins and collaborators
    organizationAggregateDTOs.getContent().forEach(
        organizationAggregateDTO -> assertEquals(0, organizationAggregateDTO.getAdmins().size()));
    organizationAggregateDTOs.getContent().forEach(
        organizationAggregateDTO -> assertEquals(0, organizationAggregateDTO.getCollaborators().size()));
  }
}
