package io.harness.ng.core.user.service.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.ARVIND;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
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

import com.google.inject.Inject;
import java.util.Arrays;
import java.util.List;
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

  @Before
  public void setup() {
    initMocks(this);
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
    doReturn(projects).when(userMembershipRepository).findProjectList(eq(user), any());
    doReturn(5L).when(userMembershipRepository).getProjectCount(user);

    Page<ProjectDTO> projectsResponse =
        ngUserService.listProjects("account", PageRequest.builder().pageSize(2).pageIndex(0).build());
    assertThat(projectsResponse).isNotNull();
    assertThat(projectsResponse.getTotalPages()).isEqualTo(3);
    assertThat(projectsResponse.getContent())
        .isEqualTo(projects.stream().map(ProjectMapper::writeDTO).collect(Collectors.toList()));
  }
}
