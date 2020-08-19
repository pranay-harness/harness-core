package io.harness.ng.core.remote;

import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static io.harness.rule.OwnerRule.ANKUSH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.ng.core.dto.CreateInviteListDTO;
import io.harness.ng.core.dto.RoleDTO;
import io.harness.ng.core.models.Role;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class RolesResourceTest extends CategoryTest {
  private RolesResource rolesResource;

  @Before
  public void doSetup() {
    rolesResource = spy(new RolesResource());
  }

  @Test
  @Owner(developers = ANKUSH)
  @Category(UnitTests.class)
  public void testCreateOrganization() {
    CreateInviteListDTO createInviteListDTO = random(CreateInviteListDTO.class);
    String accountIdentifier = random(String.class);
    String orgIdentifier = random(String.class);
    String projectIdentifier = random(String.class);

    List<Role> rolesList = getRolesList(accountIdentifier, orgIdentifier, projectIdentifier);
    when(rolesResource.getRolesList(accountIdentifier, orgIdentifier, projectIdentifier)).thenReturn(rolesList);

    Optional<List<RoleDTO>> optionalRoles =
        rolesResource.get(accountIdentifier, orgIdentifier, projectIdentifier).getData();

    //    Here I have coupled the dummy data produced by getRolesList to the assertions. Is this right way to go?
    assertThat(optionalRoles.isPresent()).isTrue();
    List<RoleDTO> roles = optionalRoles.get();
    assertThat(roles.size()).isEqualTo(3);
    verify(rolesResource).getRolesList(any(), any(), any());
  }

  List<Role> getRolesList(String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    List<Role> rolesList = new ArrayList<>();
    List<String> roleNames = Arrays.asList("Project Admin", "Project Member", "Project Viewer");
    for (String roleName : roleNames) {
      Role role = Role.builder()
                      .accountIdentifier(accountIdentifier)
                      .orgIdentifier(orgIdentifier)
                      .projectIdentifier(projectIdentifier)
                      .name(roleName)
                      .build();
      rolesList.add(role);
    }
    return rolesList;
  }
}