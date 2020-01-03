package software.wings.security.saml;

import static io.harness.rule.OwnerRule.GEORGE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.User;
import software.wings.beans.UserInvite;
import software.wings.beans.security.UserGroup;
import software.wings.security.authentication.SamlUserAuthorization;
import software.wings.service.intfc.UserGroupService;
import software.wings.service.intfc.UserService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SamlUserGroupSyncTest extends WingsBaseTest {
  @Mock UserGroupService userGroupService;
  @Mock UserService userService;
  @Inject @InjectMocks private SamlUserGroupSync samlUserGroupSync;

  User testUser = new User();
  @Captor ArgumentCaptor<UserGroup> userGroupArgumentCaptor;
  @Captor ArgumentCaptor<UserInvite> userInviteArgumentCaptor;

  @Before
  public void initMocks() throws IOException {
    testUser.setEmail("test@harness.io");
    when(userService.getUserByEmail(anyString())).thenReturn(testUser);
    when(userService.inviteUser(userInviteArgumentCaptor.capture())).thenReturn(new UserInvite());
    when(userGroupService.removeMembers(userGroupArgumentCaptor.capture(), anyList(), anyBoolean()))
        .thenReturn(new UserGroup());
    when(userGroupService.getUserGroupsBySsoId("accountId", "oktaSsoId")).thenReturn(createTestUserGroups());
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testSamlGroupSyncShouldPass() {
    SamlUserAuthorization samlUserAuthorization = SamlUserAuthorization.builder()
                                                      .email("test@harenss.io")
                                                      .userGroups(Arrays.asList("engineering", "admin"))
                                                      .build();
    samlUserGroupSync.syncUserGroup(samlUserAuthorization, "accountId", "oktaSsoId");

    // verify that the user has been removed from qaUserGroup since `qa` group was not in samlUserAuthorization and
    // user was in this group previously.
    UserGroup value = userGroupArgumentCaptor.getValue();
    assertThat("qa").isEqualTo(userGroupArgumentCaptor.getValue().getSsoGroupId());
    verify(userGroupService, times(1)).removeMembers(any(UserGroup.class), anyList(), anyBoolean());

    // verify that the user has been added in the adminUserGroup since `admin` group was in samlUserAuthorization and
    // user was not this group previously.
    assertThat(1).isEqualTo(userInviteArgumentCaptor.getValue().getUserGroups().size());
    assertThat("admin").isEqualTo(userInviteArgumentCaptor.getValue().getUserGroups().get(0).getSsoGroupId());
  }

  private List<UserGroup> createTestUserGroups() {
    ArrayList<UserGroup> userGroups = new ArrayList<>();
    UserGroup engineeringUserGroup = UserGroup.builder()
                                         .name("engineeringUserGroup")
                                         .ssoGroupId("engineering")
                                         .members(Arrays.asList(testUser))
                                         .build();
    UserGroup qaUserGroup =
        UserGroup.builder().name("qaUserGroup").ssoGroupId("qa").members(Arrays.asList(testUser)).build();
    UserGroup adminUserGroup =
        UserGroup.builder().name("adminUserGroup").ssoGroupId("admin").members(Collections.emptyList()).build();
    UserGroup testUserGroup =
        UserGroup.builder().name("testUserGroup").ssoGroupId("test").members(Collections.emptyList()).build();
    return Arrays.asList(engineeringUserGroup, qaUserGroup, adminUserGroup, testUserGroup);
  }
}
