package io.harness.ng.core.user.service.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.ARVIND;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.core.dto.ProjectDTO;
import io.harness.ng.core.entities.Project;
import io.harness.ng.core.remote.ProjectMapper;
import io.harness.repositories.user.spring.UserMembershipRepository;
import io.harness.rule.Owner;
import io.harness.security.SourcePrincipalContextBuilder;
import io.harness.security.dto.Principal;
import io.harness.security.dto.PrincipalType;
import io.harness.user.remote.UserClient;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Page;

@OwnedBy(PL)
public class NgUserServiceImplTest extends CategoryTest {
  @Mock private UserClient userClient;
  @Mock private UserMembershipRepository userMembershipRepository;
  @Inject @InjectMocks NgUserServiceImpl ngUserService;

  private static String ACCOUNT_IDENTIFIER = "A";
  private static String ORG_IDENTIFIER = "O";
  private static String PROJECT_IDENTIFIER = "P";

  @Before
  public void setup() {
    initMocks(this);
  }

  @Test
  @Owner(developers = ARVIND)
  @Category(UnitTests.class)
  public void testFilterUsersWithScopeMembership() {
    List<String> users = Lists.newArrayList("u1", "u2", "u3");
    Set<String> filteredUsers = Sets.newHashSet("u1", "u2");

    doReturn(filteredUsers)
        .when(userMembershipRepository)
        .filterUsersWithMembership(users, ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER);

    Set<String> result =
        ngUserService.filterUsersWithScopeMembership(users, ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER);
    assertThat(result).isEqualTo(filteredUsers);
  }

  @Test
  @Owner(developers = ARVIND)
  @Category(UnitTests.class)
  public void testListProjects() {
    assertThatThrownBy(() -> ngUserService.listProjects("account", PageRequest.builder().build()))
        .isInstanceOf(IllegalStateException.class);

    String user = generateUuid();
    Principal principal = mock(Principal.class);
    when(principal.getType()).thenReturn(PrincipalType.USER);
    when(principal.getName()).thenReturn(user);
    SourcePrincipalContextBuilder.setSourcePrincipal(principal);
    Project proj1 = Project.builder().name("P1").build();
    Project proj2 = Project.builder().name("P2").build();
    List<Project> projects = Arrays.asList(proj1, proj2);
    doReturn(projects).when(userMembershipRepository).findProjectList(eq(user), any(), eq(false), any());
    doReturn(5L).when(userMembershipRepository).getProjectCount(any(), anyString());

    Page<ProjectDTO> projectsResponse =
        ngUserService.listProjects("account", PageRequest.builder().pageSize(2).pageIndex(0).build());
    assertThat(projectsResponse).isNotNull();
    assertThat(projectsResponse.getTotalPages()).isEqualTo(3);
    assertThat(projectsResponse.getContent())
        .isEqualTo(projects.stream().map(ProjectMapper::writeDTO).collect(Collectors.toList()));
  }
}
