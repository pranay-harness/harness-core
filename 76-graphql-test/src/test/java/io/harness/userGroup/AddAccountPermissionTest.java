package io.harness.userGroup;

import static com.google.common.collect.Iterables.getFirst;
import static io.harness.rule.OwnerRule.DEEPAK;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.security.PermissionAttribute.PermissionType.APPLICATION_CREATE_DELETE;
import static software.wings.security.PermissionAttribute.PermissionType.TEMPLATE_MANAGEMENT;

import com.google.inject.Inject;

import io.harness.GraphQLTest;
import io.harness.category.element.UnitTests;
import io.harness.category.layer.GraphQLTests;
import io.harness.multiline.MultilineStringMixin;
import io.harness.rule.Owner;
import io.harness.testframework.graphql.QLTestObject;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.security.AccountPermissions;
import software.wings.beans.security.UserGroup;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.service.intfc.UserGroupService;

import java.util.Set;

@Slf4j
public class AddAccountPermissionTest extends GraphQLTest {
  @Inject UserGroupService userGroupService;
  @Inject UserGroupHelper userGroupHelper;

  private String createAddAccountPermissionInput(String userGroupId, String permission) {
    String userGroupQueryPattern = MultilineStringMixin.$.GQL(/*
    {
        userGroupId : "%s",
        accountPermission : %s
    }
   */ AddAppPermissionTest.class);
    return String.format(userGroupQueryPattern, userGroupId, permission);
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category({GraphQLTests.class, UnitTests.class})
  public void testAddingAppPermissionToUserGroup() {
    UserGroup userGroup = userGroupHelper.createUserGroup(getAccountId(), "harnessUserGroup", "sample user group");
    String userGroupQueryPattern = MultilineStringMixin.$.GQL(/*
     mutation{
        addAccountPermission(input:%s){
            clientMutationId
        }
    }
   */ AddAppPermissionTest.class);
    String query = String.format(
        userGroupQueryPattern, createAddAccountPermissionInput(userGroup.getUuid(), "CREATE_AND_DELETE_APPLICATION"));
    // The case when no account permission was added to the usergroup
    QLTestObject qlUserGroupObject = qlExecute(query, getAccountId());
    UserGroup updatedUserGroup = userGroupService.get(getAccountId(), userGroup.getUuid());
    AccountPermissions accountPermissions = updatedUserGroup.getAccountPermissions();
    assertThat(accountPermissions).isNotNull();
    Set<PermissionType> accountPermissionSet = accountPermissions.getPermissions();
    assertThat(accountPermissionSet).isNotEmpty();
    assertThat(accountPermissionSet.size()).isEqualTo(1);
    PermissionType permissionType = getFirst(accountPermissionSet, null);
    assertThat(permissionType).isNotNull();
    assertThat(permissionType).isEqualTo(APPLICATION_CREATE_DELETE);

    // The case when a new account permission is added to the usergroup
    String addQuery = String.format(
        userGroupQueryPattern, createAddAccountPermissionInput(userGroup.getUuid(), "MANAGE_TEMPLATE_LIBRARY"));
    qlExecute(addQuery, getAccountId());
    UserGroup updatedUserGroupWithTwoPerm = userGroupService.get(getAccountId(), userGroup.getUuid());
    AccountPermissions updatedAccountPermissisons = updatedUserGroupWithTwoPerm.getAccountPermissions();
    assertThat(updatedAccountPermissisons).isNotNull();
    Set<PermissionType> updatedAccountPermissionSet = updatedAccountPermissisons.getPermissions();
    assertThat(updatedAccountPermissionSet).isNotEmpty();
    assertThat(updatedAccountPermissionSet.size()).isEqualTo(2);
    assertThat(updatedAccountPermissionSet.contains(APPLICATION_CREATE_DELETE)).isTrue();
    assertThat(updatedAccountPermissionSet.contains(TEMPLATE_MANAGEMENT)).isTrue();
  }
}
