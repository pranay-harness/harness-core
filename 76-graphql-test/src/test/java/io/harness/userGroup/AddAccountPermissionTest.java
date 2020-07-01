package io.harness.userGroup;

import static io.harness.rule.OwnerRule.DEEPAK;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.security.PermissionAttribute.PermissionType.APPLICATION_CREATE_DELETE;
import static software.wings.security.PermissionAttribute.PermissionType.CE_VIEWER;
import static software.wings.security.PermissionAttribute.PermissionType.TEMPLATE_MANAGEMENT;

import com.google.common.collect.ImmutableSet;
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
import software.wings.service.intfc.UserGroupService;

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
    // The case when no account permission was added to the user group
    QLTestObject qlUserGroupObject = qlExecute(query, getAccountId());
    UserGroup updatedUserGroup = userGroupService.get(getAccountId(), userGroup.getUuid());
    AccountPermissions accountPermissions = updatedUserGroup.getAccountPermissions();
    assertThat(accountPermissions).isNotNull();
    assertThat(accountPermissions.getPermissions()).isNotEmpty().hasSize(2).contains(APPLICATION_CREATE_DELETE);

    // The case when a new account permission is added to the user group
    String addQuery = String.format(
        userGroupQueryPattern, createAddAccountPermissionInput(userGroup.getUuid(), "MANAGE_TEMPLATE_LIBRARY"));
    qlExecute(addQuery, getAccountId());
    UserGroup updatedUserGroupWithTwoPerm = userGroupService.get(getAccountId(), userGroup.getUuid());
    AccountPermissions updatedAccountPermissions = updatedUserGroupWithTwoPerm.getAccountPermissions();
    assertThat(updatedAccountPermissions).isNotNull();
    assertThat(updatedAccountPermissions.getPermissions())
        .isNotEmpty()
        .hasSize(3)
        .containsExactlyInAnyOrderElementsOf(
            ImmutableSet.of(APPLICATION_CREATE_DELETE, TEMPLATE_MANAGEMENT, CE_VIEWER));
  }
}
