package io.harness.ng.core.api.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.rule.OwnerRule.KARAN;
import static io.harness.utils.PageTestUtils.getPage;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;
import static junit.framework.TestCase.assertEquals;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.ng.core.dto.OrganizationAggregateDTO;
import io.harness.ng.core.entities.Organization;
import io.harness.ng.core.entities.Project;
import io.harness.ng.core.services.OrganizationService;
import io.harness.ng.core.services.ProjectService;
import io.harness.ng.core.user.UserInfo;
import io.harness.ng.core.user.entities.UserMembership;
import io.harness.ng.core.user.service.NgUserService;
import io.harness.rule.Owner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@OwnedBy(PL)
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

  private List<UserInfo> getUsers(List<UserMembership> userMemberships) {
    List<UserInfo> users = new ArrayList<>();
    userMemberships.forEach(userMembership -> users.add(UserInfo.builder().uuid(userMembership.getUserId()).build()));
    return users;
  }

  private void setupNgUserService(String accountIdentifier, String orgIdentifier) {
    List<UserMembership> userMembershipList = new ArrayList<>();
    IntStream.range(0, 8).forEach(e
        -> userMembershipList.add(UserMembership.builder()
                                      .userId(randomAlphabetic(10))
                                      .scopes(Collections.singletonList(UserMembership.Scope.builder()
                                                                            .accountIdentifier(accountIdentifier)
                                                                            .orgIdentifier(orgIdentifier)
                                                                            .build()))
                                      .build()));
    when(ngUserService.listUserMemberships(any())).thenReturn(userMembershipList);
    when(ngUserService.getUsersByIds(any(), any())).thenReturn(getUsers(userMembershipList));
    List<String> adminIds =
        IntStream.range(0, 4).mapToObj(i -> userMembershipList.get(i).getUserId()).collect(toList());
    when(ngUserService.getUserIdsWithRole(any(), any())).thenReturn(adminIds);
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testGet() {
    String accountIdentifier = randomAlphabetic(10);
    String orgIdentifier = randomAlphabetic(10);

    Organization organization = getOrganization(accountIdentifier, orgIdentifier);
    when(organizationService.get(accountIdentifier, orgIdentifier)).thenReturn(Optional.of(organization));

    Map<String, Integer> projectsCount = singletonMap(organization.getIdentifier(), 3);
    when(projectService.getProjectsCountPerOrganization(eq(accountIdentifier), any())).thenReturn(projectsCount);

    setupNgUserService(accountIdentifier, orgIdentifier);

    OrganizationAggregateDTO organizationAggregateDTO =
        aggregateOrganizationService.getOrganizationAggregateDTO(accountIdentifier, orgIdentifier);

    // organization
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

    when(projectService.getProjectsCountPerOrganization(eq(accountIdentifier), any())).thenReturn(emptyMap());

    when(ngUserService.listUserMemberships(any())).thenReturn(emptyList());

    when(ngUserService.getUsersByIds(any(), any())).thenReturn(emptyList());

    OrganizationAggregateDTO organizationAggregateDTO =
        aggregateOrganizationService.getOrganizationAggregateDTO(accountIdentifier, orgIdentifier);

    // organization
    assertEquals(orgIdentifier, organizationAggregateDTO.getOrganizationResponse().getOrganization().getIdentifier());
    assertEquals(
        organization.getName(), organizationAggregateDTO.getOrganizationResponse().getOrganization().getName());

    // projects
    assertEquals(0, organizationAggregateDTO.getProjectsCount());

    // admins and collaborators
    assertEquals(0, organizationAggregateDTO.getAdmins().size());
    assertEquals(0, organizationAggregateDTO.getCollaborators().size());
  }

  private List<Organization> getOrganizations(String accountIdentifier, int count) {
    List<Organization> organizations = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      organizations.add(getOrganization(accountIdentifier, randomAlphabetic(10)));
    }
    return organizations;
  }

  private List<UserMembership> setupNgUserServiceForList(List<Organization> organizations) {
    List<UserMembership> userMembershipList = new ArrayList<>();
    List<String> userIds = IntStream.range(0, 8).mapToObj(e -> randomAlphabetic(10)).collect(toList());
    for (Organization organization : organizations) {
      IntStream.range(0, 8).forEach(e
          -> userMembershipList.add(
              UserMembership.builder()
                  .userId(userIds.get(e))
                  .scopes(Collections.singletonList(UserMembership.Scope.builder()
                                                        .accountIdentifier(organization.getAccountIdentifier())
                                                        .orgIdentifier(organization.getIdentifier())
                                                        .build()))
                  .build()));
    }
    when(ngUserService.getUserIdsWithRole(any(), any())).thenReturn(userIds.subList(0, 4));
    when(ngUserService.getUsersByIds(any(), any())).thenReturn(getUsers(userMembershipList));
    when(ngUserService.listUserMemberships(any())).thenReturn(userMembershipList);
    return userMembershipList;
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testList() {
    String accountIdentifier = randomAlphabetic(10);

    List<Organization> organizations = getOrganizations(accountIdentifier, 3);
    when(organizationService.list(accountIdentifier, Pageable.unpaged(), null)).thenReturn(getPage(organizations, 3));

    Map<String, Integer> projectsCount = new HashMap<>();
    organizations.forEach(organization -> projectsCount.put(organization.getIdentifier(), 3));
    when(projectService.getProjectsCountPerOrganization(eq(accountIdentifier), any())).thenReturn(projectsCount);

    setupNgUserServiceForList(organizations);

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

    when(projectService.getProjectsCountPerOrganization(eq(accountIdentifier), any())).thenReturn(emptyMap());

    when(ngUserService.listUserMemberships(any())).thenReturn(emptyList());

    when(ngUserService.getUsersByIds(any(), any())).thenReturn(emptyList());

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
