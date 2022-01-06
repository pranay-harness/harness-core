/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.userGroup;

import static io.harness.rule.OwnerRule.DEEPAK;

import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_APPLICATIONS;
import static software.wings.security.PermissionAttribute.PermissionType.TEMPLATE_MANAGEMENT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.GraphQLTest;
import io.harness.category.element.UnitTests;
import io.harness.category.layer.GraphQLTests;
import io.harness.multiline.MultilineStringMixin;
import io.harness.rule.Owner;
import io.harness.testframework.graphql.QLTestObject;

import software.wings.beans.security.AccountPermissions;
import software.wings.beans.security.UserGroup;
import software.wings.service.intfc.UserGroupService;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;

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
    assertThat(accountPermissions.getPermissions()).containsExactly(MANAGE_APPLICATIONS);

    // The case when a new account permission is added to the user group
    String addQuery = String.format(
        userGroupQueryPattern, createAddAccountPermissionInput(userGroup.getUuid(), "MANAGE_TEMPLATE_LIBRARY"));
    qlExecute(addQuery, getAccountId());
    UserGroup updatedUserGroupWithTwoPerm = userGroupService.get(getAccountId(), userGroup.getUuid());
    AccountPermissions updatedAccountPermissions = updatedUserGroupWithTwoPerm.getAccountPermissions();
    assertThat(updatedAccountPermissions).isNotNull();
    assertThat(updatedAccountPermissions.getPermissions())
        .containsExactlyInAnyOrderElementsOf(ImmutableSet.of(MANAGE_APPLICATIONS, TEMPLATE_MANAGEMENT));
  }
}
