package io.harness.userGroup;

import static com.google.common.collect.Iterables.getFirst;
import static com.google.common.collect.Iterables.getLast;
import static io.harness.rule.OwnerRule.DEEPAK;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.security.GenericEntityFilter.FilterType.ALL;
import static software.wings.security.PermissionAttribute.Action.CREATE;
import static software.wings.security.PermissionAttribute.Action.DELETE;
import static software.wings.security.PermissionAttribute.Action.READ;
import static software.wings.security.PermissionAttribute.Action.UPDATE;
import static software.wings.security.PermissionAttribute.PermissionType.ALL_APP_ENTITIES;
import static software.wings.security.PermissionAttribute.PermissionType.SERVICE;

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
import software.wings.beans.security.AppPermission;
import software.wings.beans.security.UserGroup;
import software.wings.security.GenericEntityFilter;
import software.wings.security.PermissionAttribute;
import software.wings.service.intfc.UserGroupService;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
public class AddAppPermissionTest extends GraphQLTest {
  @Inject UserGroupHelper userGroupHelper;
  @Inject UserGroupService userGroupService;

  private String createAllEntityAllAppsPermissionString(String userGroupId) {
    String appPermissionString = $GQL(
        /*
{
       userGroupId: "%s",
       clientMutationId: "abc",
       appPermission: {
            permissionType: ALL,
            applications  : {
               filterType :  ALL
            },
            actions: [CREATE, UPDATE ,DELETE ,READ]
       }
    }*/ userGroupId);

    return appPermissionString;
  }

  private String createAllEntityAllServicePermission(String userGroupId) {
    String appPermissionString = $GQL(
        /*
     {
       userGroupId: "%s",
       clientMutationId: "abc",
       appPermission: {
            permissionType: SERVICE,
            applications  : {
               filterType :  ALL
            },
            services : {
                filterType :  ALL
            },
            actions: [CREATE, UPDATE ,DELETE ,READ]
       }
    }*/ userGroupId);

    return appPermissionString;
  }

  private Set<PermissionAttribute.Action> createAllActionSet() {
    List<PermissionAttribute.Action> actionsList = Arrays.asList(READ, CREATE, UPDATE, DELETE);
    return new HashSet<>(actionsList);
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category({GraphQLTests.class, UnitTests.class})
  public void testAddingAppPermissionToUserGroup() {
    UserGroup userGroup = userGroupHelper.createUserGroup(getAccountId(), "harnessUserGroup", "sample user group");
    GenericEntityFilter entityFilter = GenericEntityFilter.builder().filterType(ALL).build();
    AppPermission appPermissionOriginal1 = AppPermission.builder()
                                               .appFilter(entityFilter)
                                               .permissionType(ALL_APP_ENTITIES)
                                               .actions(createAllActionSet())
                                               .build();
    String userGroupQueryPattern = MultilineStringMixin.$.GQL(/*
     mutation{
        addAppPermission(input:%s){
            clientMutationId
        }
    }
    */ AddAppPermissionTest.class);
    String query = String.format(userGroupQueryPattern, createAllEntityAllAppsPermissionString(userGroup.getUuid()));
    // The case when no application permission was added to the usergroup
    final QLTestObject qlUserGroupObject = qlExecute(query, getAccountId());
    UserGroup updatedUserGroup = userGroupService.get(getAccountId(), userGroup.getUuid());
    Set<AppPermission> appPermissions = updatedUserGroup.getAppPermissions();
    assertThat(appPermissions).isNotEmpty();
    assertThat(appPermissions.size()).isEqualTo(1);
    AppPermission appPermissionSaved = getFirst(appPermissions, null);
    assertThat(appPermissionSaved.equals(appPermissionOriginal1)).isTrue();

    // The case when we are adding one more application permission to the usergroup
    AppPermission appPermissionOriginal2 = AppPermission.builder()
                                               .appFilter(entityFilter)
                                               .permissionType(SERVICE)
                                               .entityFilter(entityFilter)
                                               .actions(createAllActionSet())
                                               .build();
    String addQuery = String.format(userGroupQueryPattern, createAllEntityAllServicePermission(userGroup.getUuid()));
    final QLTestObject updatedUserGroupObject = qlExecute(addQuery, getAccountId());
    updatedUserGroup = userGroupService.get(getAccountId(), userGroup.getUuid());
    appPermissions = updatedUserGroup.getAppPermissions();
    assertThat(appPermissions).isNotEmpty();
    assertThat(appPermissions.size()).isEqualTo(2);
    AppPermission appPermissionSaved1 = getFirst(appPermissions, null);
    AppPermission appPermissionSaved2 = getLast(appPermissions, null);
    boolean savedFirstEqualsOriginalFirst =
        appPermissionSaved1.equals(appPermissionOriginal1) && appPermissionSaved2.equals(appPermissionOriginal2);
    boolean savedFirstEqualsOriginalSecond =
        appPermissionSaved1.equals(appPermissionOriginal2) && appPermissionSaved2.equals(appPermissionOriginal1);
    assertThat(savedFirstEqualsOriginalFirst || savedFirstEqualsOriginalSecond).isTrue();
  }
}
