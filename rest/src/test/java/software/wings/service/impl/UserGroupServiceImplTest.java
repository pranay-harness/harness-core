package software.wings.service.impl;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static software.wings.beans.Account.Builder.anAccount;
import static software.wings.dl.PageResponse.PageResponseBuilder.aPageResponse;
import static software.wings.security.EnvFilter.FilterType.PROD;
import static software.wings.security.PermissionAttribute.PermissionType.ENV;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.USER_GROUP_ID;

import com.google.common.collect.Sets;
import com.google.inject.Inject;

import io.harness.data.structure.UUIDGenerator;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.beans.User;
import software.wings.beans.security.AppPermission;
import software.wings.beans.security.UserGroup;
import software.wings.dl.PageRequest.PageRequestBuilder;
import software.wings.dl.PageResponse;
import software.wings.security.EnvFilter;
import software.wings.security.GenericEntityFilter;
import software.wings.security.GenericEntityFilter.FilterType;
import software.wings.security.PermissionAttribute.Action;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AuthService;
import software.wings.service.intfc.UserService;

import java.util.HashSet;
import java.util.List;

public class UserGroupServiceImplTest extends WingsBaseTest {
  @Mock private AccountService accountService;
  @Mock private AuthService authService;
  @Mock private UserService userService;
  @Mock private Account account;
  @Mock private User user;
  @InjectMocks @Inject private UserGroupServiceImpl userGroupService = spy(UserGroupServiceImpl.class);

  private String accountId = UUIDGenerator.generateUuid();
  private String userGroupId = UUIDGenerator.generateUuid();
  private String userGroup2Id = UUIDGenerator.generateUuid();
  private String description = "test description";
  private String name = "userGroup1";
  private String name2 = "userGroup2";
  private String user1Id = UUIDGenerator.generateUuid();
  private String user2Id = UUIDGenerator.generateUuid();
  private AppPermission envPermission = getEnvPermission();

  @Before
  public void setup() {
    doNothing().when(authService).evictAccountUserPermissionInfoCache(anyString(), anyList());
    when(accountService.get(anyString())).thenReturn(account);
    when(userService.list(any())).thenReturn(aPageResponse().withResponse(asList(user)).build());
  }

  @Test
  public void testSaveAndRead() {
    UserGroup userGroup = UserGroup.builder()
                              .accountId(accountId)
                              .uuid(userGroupId)
                              .description(description)
                              .name(name + System.currentTimeMillis())
                              .appPermissions(Sets.newHashSet(envPermission))
                              .memberIds(asList(user1Id, user2Id))
                              .build();
    UserGroup savedUserGroup = userGroupService.save(userGroup);
    compare(userGroup, savedUserGroup);

    UserGroup userGroupFromGet = userGroupService.get(accountId, userGroupId);
    compare(savedUserGroup, userGroupFromGet);

    userGroup = UserGroup.builder()
                    .accountId(accountId)
                    .uuid(userGroupId)
                    .description(description)
                    .memberIds(asList(user1Id))
                    .build();
    savedUserGroup = userGroupService.save(userGroup);
    compare(userGroup, savedUserGroup);

    userGroupFromGet = userGroupService.get(accountId, userGroupId);
    compare(savedUserGroup, userGroupFromGet);
  }

  private AppPermission getEnvPermission() {
    List<Action> allActions = asList(Action.CREATE, Action.UPDATE, Action.READ, Action.DELETE, Action.EXECUTE);
    EnvFilter envFilter = new EnvFilter();
    envFilter.setFilterTypes(Sets.newHashSet(PROD));

    return AppPermission.builder()
        .permissionType(ENV)
        .appFilter(GenericEntityFilter.builder().filterType(FilterType.ALL).build())
        .entityFilter(envFilter)
        .actions(new HashSet(allActions))
        .build();
  }

  private void compare(UserGroup lhs, UserGroup rhs) {
    assertEquals(lhs.getUuid(), rhs.getUuid());
    assertEquals(lhs.getDescription(), rhs.getDescription());
    assertEquals(lhs.getAccountId(), rhs.getAccountId());
    assertEquals(lhs.getName(), rhs.getName());
    assertEquals(lhs.getMemberIds(), rhs.getMemberIds());
    assertEquals(lhs.getAppPermissions(), rhs.getAppPermissions());
  }

  @Test
  public void testList() {
    UserGroup userGroup1 = UserGroup.builder()
                               .uuid(userGroupId)
                               .name(name)
                               .accountId(accountId)
                               .description(description)
                               .memberIds(asList(user1Id))
                               .members(asList(user))
                               .build();
    UserGroup savedUserGroup1 = userGroupService.save(userGroup1);

    UserGroup userGroup2 = UserGroup.builder()
                               .name(name2)
                               .uuid(userGroup2Id)
                               .accountId(accountId)
                               .description(description)
                               .memberIds(asList(user2Id))
                               .members(asList(user))
                               .appPermissions(Sets.newHashSet(envPermission))
                               .build();
    UserGroup savedUserGroup2 = userGroupService.save(userGroup2);

    PageResponse pageResponse = userGroupService.list(accountId, PageRequestBuilder.aPageRequest().build());
    assertNotNull(pageResponse);
    List<UserGroup> userGroupList = pageResponse.getResponse();
    assertThat(userGroupList).isNotNull();
    assertThat(userGroupList).hasSize(2);
    assertThat(userGroupList).containsExactlyInAnyOrder(savedUserGroup1, savedUserGroup2);
  }
  @Test
  public void testCloneUserGroup() {
    final UserGroup storedGroupToClone = UserGroup.builder()
                                             .uuid(USER_GROUP_ID)
                                             .appId(APP_ID)
                                             .createdBy(null)
                                             .createdAt(0)
                                             .lastUpdatedBy(null)
                                             .lastUpdatedAt(0)
                                             .keywords(null)
                                             .entityYamlPath(null)
                                             .name("Stored")
                                             .description("Desc")
                                             .accountId(ACCOUNT_ID)
                                             .memberIds(null)
                                             .members(null)
                                             .appPermissions(null)
                                             .accountPermissions(null)
                                             .build();
    userGroupService.save(storedGroupToClone);
    final Account account = anAccount().withUuid(ACCOUNT_ID).build();
    doReturn(account).when(accountService).get(ACCOUNT_ID);
    final String newName = "NewName";
    final String newDescription = "Desc";
    doNothing().when(authService).evictAccountUserPermissionInfoCache(anyString(), any());
    UserGroup cloneActual = userGroupService.cloneUserGroup(ACCOUNT_ID, USER_GROUP_ID, newName, newDescription);
    assertThat(cloneActual.getName()).isEqualTo(newName);
    assertThat(cloneActual.getDescription()).isEqualTo(newDescription);
  }
}
