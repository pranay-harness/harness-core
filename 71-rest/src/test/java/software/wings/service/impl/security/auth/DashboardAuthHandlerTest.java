package software.wings.service.impl.security.auth;

import static io.harness.rule.OwnerRule.RAMA;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.ACCOUNT_NAME;
import static software.wings.utils.WingsTestConstants.DASHBOARD1_ID;
import static software.wings.utils.WingsTestConstants.USER1_ID;
import static software.wings.utils.WingsTestConstants.USER_GROUP_ID;
import static software.wings.utils.WingsTestConstants.USER_ID;

import com.google.inject.Inject;

import io.harness.beans.EmbeddedUser;
import io.harness.category.element.UnitTests;
import io.harness.dashboard.Action;
import io.harness.dashboard.DashboardAccessPermissions;
import io.harness.dashboard.DashboardSettings;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import software.wings.WingsBaseTest;
import software.wings.beans.User;
import software.wings.beans.security.UserGroup;
import software.wings.security.UserPermissionInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DashboardAuthHandlerTest extends WingsBaseTest {
  @InjectMocks @Inject private DashboardAuthHandler authHandler;

  //  private List<PermissionType> accountPermissionTypes =
  //      asList(ACCOUNT_MANAGEMENT, APPLICATION_CREATE_DELETE, USER_PERMISSION_MANAGEMENT, TEMPLATE_MANAGEMENT);
  //  private List<Action> allActions = asList(Action.CREATE, Action.UPDATE, Action.READ, Action.DELETE,
  //  Action.EXECUTE);
  private List<Action> allActions = asList(Action.MANAGE, Action.UPDATE, Action.READ, Action.DELETE);

  @Test
  @Owner(developers = RAMA)
  @Category(UnitTests.class)
  public void shouldWorkForManagePermission() {
    List<DashboardAccessPermissions> permissions = new ArrayList<>();
    List<io.harness.dashboard.Action> actions = new ArrayList<>();
    actions.add(Action.MANAGE);
    List<String> userGroups = new ArrayList<>();
    userGroups.add(USER_GROUP_ID);
    permissions.add(DashboardAccessPermissions.builder().userGroups(userGroups).allowedActions(actions).build());
    DashboardSettings dashboard1 = DashboardSettings.builder()
                                       .accountId(ACCOUNT_ID)
                                       .name(ACCOUNT_NAME)
                                       .permissions(permissions)
                                       .createdBy(EmbeddedUser.builder().uuid(USER1_ID).build())
                                       .uuid(DASHBOARD1_ID)
                                       .build();
    wingsPersistence.save(dashboard1);

    User user = User.Builder.anUser().uuid(USER_ID).build();
    UserPermissionInfo userPermissionInfo = UserPermissionInfo.builder().build();
    List<String> memberIds = new ArrayList<>();
    memberIds.add(USER_ID);
    List<UserGroup> userGroupList = new ArrayList<>();
    UserGroup userGroup = UserGroup.builder().uuid(USER_GROUP_ID).memberIds(memberIds).build();
    userGroupList.add(userGroup);
    Map<String, Set<Action>> dashboardAccessPermissions =
        authHandler.getDashboardAccessPermissions(user, ACCOUNT_ID, userPermissionInfo, userGroupList);
    assertThat(dashboardAccessPermissions.size()).isEqualTo(1);
    assertThat(dashboardAccessPermissions.get(DASHBOARD1_ID).size()).isEqualTo(1);
    assertThat(dashboardAccessPermissions.get(DASHBOARD1_ID).contains(Action.MANAGE)).isTrue();
  }

  @Test
  @Owner(developers = RAMA)
  @Category(UnitTests.class)
  public void shouldWorkForOwner() {
    List<DashboardAccessPermissions> permissions = new ArrayList<>();
    List<io.harness.dashboard.Action> actions = new ArrayList<>();
    actions.add(Action.READ);
    List<String> userGroups = new ArrayList<>();
    userGroups.add(USER_GROUP_ID);
    permissions.add(DashboardAccessPermissions.builder().userGroups(userGroups).allowedActions(actions).build());
    DashboardSettings dashboard1 = DashboardSettings.builder()
                                       .accountId(ACCOUNT_ID)
                                       .name(ACCOUNT_NAME)
                                       .permissions(permissions)
                                       .createdBy(EmbeddedUser.builder().uuid(USER_ID).build())
                                       .uuid(DASHBOARD1_ID)
                                       .build();
    wingsPersistence.save(dashboard1);

    User user = User.Builder.anUser().uuid(USER_ID).build();
    UserPermissionInfo userPermissionInfo = UserPermissionInfo.builder().build();
    List<String> memberIds = new ArrayList<>();
    memberIds.add(USER_ID);
    List<UserGroup> userGroupList = new ArrayList<>();
    UserGroup userGroup = UserGroup.builder().uuid(USER_GROUP_ID).memberIds(memberIds).build();
    userGroupList.add(userGroup);
    Map<String, Set<Action>> dashboardAccessPermissions =
        authHandler.getDashboardAccessPermissions(user, ACCOUNT_ID, userPermissionInfo, userGroupList);
    assertThat(dashboardAccessPermissions.size()).isEqualTo(1);
    assertThat(dashboardAccessPermissions.get(DASHBOARD1_ID).size()).isEqualTo(4);
    assertThat(dashboardAccessPermissions.get(DASHBOARD1_ID).containsAll(allActions)).isTrue();
  }

  @Test
  @Owner(developers = RAMA)
  @Category(UnitTests.class)
  public void shouldWorkForReadPermission() {
    List<DashboardAccessPermissions> permissions = new ArrayList<>();
    List<io.harness.dashboard.Action> actions = new ArrayList<>();
    actions.add(Action.READ);
    List<String> userGroups = new ArrayList<>();
    userGroups.add(USER_GROUP_ID);
    permissions.add(DashboardAccessPermissions.builder().userGroups(userGroups).allowedActions(actions).build());
    DashboardSettings dashboard1 = DashboardSettings.builder()
                                       .accountId(ACCOUNT_ID)
                                       .name(ACCOUNT_NAME)
                                       .permissions(permissions)
                                       .createdBy(EmbeddedUser.builder().uuid(USER1_ID).build())
                                       .uuid(DASHBOARD1_ID)
                                       .build();
    wingsPersistence.save(dashboard1);

    User user = User.Builder.anUser().uuid(USER_ID).build();
    UserPermissionInfo userPermissionInfo = UserPermissionInfo.builder().build();
    List<String> memberIds = new ArrayList<>();
    memberIds.add(USER_ID);
    List<UserGroup> userGroupList = new ArrayList<>();
    UserGroup userGroup = UserGroup.builder().uuid(USER_GROUP_ID).memberIds(memberIds).build();
    userGroupList.add(userGroup);
    Map<String, Set<Action>> dashboardAccessPermissions =
        authHandler.getDashboardAccessPermissions(user, ACCOUNT_ID, userPermissionInfo, userGroupList);
    assertThat(dashboardAccessPermissions.size()).isEqualTo(1);
    assertThat(dashboardAccessPermissions.get(DASHBOARD1_ID).size()).isEqualTo(1);
    assertThat(dashboardAccessPermissions.get(DASHBOARD1_ID).contains(Action.READ)).isTrue();
  }

  @Test
  @Owner(developers = RAMA)
  @Category(UnitTests.class)
  public void shouldNotGetAnyDashboardForNoPermission() {
    List<DashboardAccessPermissions> permissions = new ArrayList<>();
    List<io.harness.dashboard.Action> actions = new ArrayList<>();
    actions.add(Action.READ);
    List<String> userGroups = new ArrayList<>();
    userGroups.add(USER_GROUP_ID);
    permissions.add(DashboardAccessPermissions.builder().userGroups(userGroups).allowedActions(actions).build());
    DashboardSettings dashboard1 = DashboardSettings.builder()
                                       .accountId(ACCOUNT_ID)
                                       .name(ACCOUNT_NAME)
                                       .permissions(permissions)
                                       .createdBy(EmbeddedUser.builder().uuid(USER1_ID).build())
                                       .uuid(DASHBOARD1_ID)
                                       .build();
    wingsPersistence.save(dashboard1);

    User user = User.Builder.anUser().uuid(USER_ID).build();
    UserPermissionInfo userPermissionInfo = UserPermissionInfo.builder().build();
    List<UserGroup> userGroupList = new ArrayList<>();
    Map<String, Set<Action>> dashboardAccessPermissions =
        authHandler.getDashboardAccessPermissions(user, ACCOUNT_ID, userPermissionInfo, userGroupList);
    assertThat(dashboardAccessPermissions.size()).isEqualTo(0);
  }
}
