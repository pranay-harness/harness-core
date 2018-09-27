package software.wings.service.impl;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.Account.Builder.anAccount;
import static software.wings.beans.User.Builder.anUser;
import static software.wings.security.EnvFilter.FilterType.PROD;
import static software.wings.security.PermissionAttribute.PermissionType.ENV;
import static software.wings.service.impl.UserServiceImpl.ADD_ROLE_EMAIL_TEMPLATE_NAME;
import static software.wings.service.impl.UserServiceImpl.INVITE_EMAIL_TEMPLATE_NAME;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.ACCOUNT_NAME;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.COMPANY_NAME;
import static software.wings.utils.WingsTestConstants.PASSWORD;
import static software.wings.utils.WingsTestConstants.USER_EMAIL;
import static software.wings.utils.WingsTestConstants.USER_GROUP_ID;
import static software.wings.utils.WingsTestConstants.USER_NAME;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import io.harness.beans.PageRequest.PageRequestBuilder;
import io.harness.beans.PageResponse;
import io.harness.data.structure.UUIDGenerator;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.beans.Role;
import software.wings.beans.User;
import software.wings.beans.security.AppPermission;
import software.wings.beans.security.UserGroup;
import software.wings.dl.WingsPersistence;
import software.wings.helpers.ext.mail.EmailData;
import software.wings.security.EnvFilter;
import software.wings.security.GenericEntityFilter;
import software.wings.security.GenericEntityFilter.FilterType;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.UserThreadLocal;
import software.wings.security.authentication.AuthenticationMechanism;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AuthService;
import software.wings.service.intfc.EmailNotificationService;
import software.wings.service.intfc.RoleService;
import software.wings.service.intfc.UserService;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

public class UserGroupServiceImplTest extends WingsBaseTest {
  @Mock private AuthService authService;
  @Mock private RoleService roleService;
  @Mock private AccountService accountService;
  @Mock private EmailNotificationService emailNotificationService;
  private Account account = Account.Builder.anAccount()
                                .withAccountName(ACCOUNT_NAME)
                                .withCompanyName(COMPANY_NAME)
                                .withAuthenticationMechanism(AuthenticationMechanism.USER_PASSWORD)
                                .withUuid(ACCOUNT_ID)
                                .build();
  private User user = anUser()
                          .withAppId(APP_ID)
                          .withEmail(USER_EMAIL)
                          .withName(USER_NAME)
                          .withPassword(PASSWORD)
                          .withAccountName(ACCOUNT_NAME)
                          .withCompanyName(COMPANY_NAME)
                          .build();
  private User userWithAccount = anUser()
                                     .withAppId(APP_ID)
                                     .withEmail(USER_EMAIL)
                                     .withName(USER_NAME)
                                     .withPassword(PASSWORD)
                                     .withAccountName(ACCOUNT_NAME)
                                     .withCompanyName(COMPANY_NAME)
                                     .withAccounts(Lists.newArrayList(account))
                                     .build();

  @Inject private WingsPersistence wingsPersistence;
  //  @InjectMocks @Inject private AccountService accountService = spy(AccountServiceImpl.class);
  @InjectMocks @Inject private UserService userService = spy(UserServiceImpl.class);
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
    when(accountService.save(any())).thenReturn(account);
    when(roleService.getAccountAdminRole(any()))
        .thenReturn(Role.Builder.aRole().withAccountId(ACCOUNT_ID).withUuid(UUIDGenerator.generateUuid()).build());
    try {
      userService.register(user);
    } catch (Exception ex) {
      // This was needed since some subsequent operation fails after user registration.
      // This happens only in test scenario. Catching the exception for now.
    }
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

    PageResponse pageResponse = userGroupService.list(accountId, PageRequestBuilder.aPageRequest().build(), true);
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

  @Test
  public void shouldUpdateUserGroup() {
    ArgumentCaptor<EmailData> emailDataArgumentCaptor = ArgumentCaptor.forClass(EmailData.class);
    when(accountService.get(ACCOUNT_ID)).thenReturn(account);

    GenericEntityFilter appFilter = GenericEntityFilter.builder().filterType(FilterType.ALL).build();
    AppPermission appPermission =
        AppPermission.builder().permissionType(PermissionType.ALL_APP_ENTITIES).appFilter(appFilter).build();
    UserGroup userGroup1 = UserGroup.builder()
                               .accountId(ACCOUNT_ID)
                               .name("USER_GROUP1")
                               .appPermissions(Sets.newHashSet(appPermission))
                               .build();
    UserGroup userGroup2 = UserGroup.builder()
                               .accountId(ACCOUNT_ID)
                               .name("USER_GROUP2")
                               .appPermissions(Sets.newHashSet(appPermission))
                               .build();
    UserGroup userGroup3 = UserGroup.builder()
                               .accountId(ACCOUNT_ID)
                               .name("USER_GROUP3")
                               .appPermissions(Sets.newHashSet(appPermission))
                               .build();

    userGroup1 = userGroupService.save(userGroup1);
    userGroup2 = userGroupService.save(userGroup2);
    userGroup3 = userGroupService.save(userGroup3);

    // Update operation 1
    User userAfterUpdate =
        userService.updateUserGroupsOfUser(user.getUuid(), Arrays.asList(userGroup1, userGroup2), ACCOUNT_ID, true);
    userGroup1 = userGroupService.get(ACCOUNT_ID, userGroup1.getUuid());
    userGroup2 = userGroupService.get(ACCOUNT_ID, userGroup2.getUuid());
    assertThat(userAfterUpdate.getUserGroups()).size().isEqualTo(2);
    assertThat(userAfterUpdate.getUserGroups().stream().map(UserGroup::getUuid).collect(Collectors.toSet()))
        .containsExactlyInAnyOrder(userGroup1.getUuid(), userGroup2.getUuid());

    verify(emailNotificationService, atLeastOnce()).send(emailDataArgumentCaptor.capture());
    List<EmailData> emailsData = emailDataArgumentCaptor.getAllValues();
    assertFalse(emailsData.stream()
                    .filter(emailData -> emailData.getTemplateName().equals(INVITE_EMAIL_TEMPLATE_NAME))
                    .collect(Collectors.toList())
                    .size()
        == 1);

    userGroup1 = userGroupService.get(ACCOUNT_ID, userGroup1.getUuid());
    userGroup2 = userGroupService.get(ACCOUNT_ID, userGroup2.getUuid());
    userGroup3 = userGroupService.get(ACCOUNT_ID, userGroup3.getUuid());
    assertThat(userGroup1.getMemberIds()).containsExactly(user.getUuid());
    assertThat(userGroup2.getMemberIds()).containsExactly(user.getUuid());
    assertThat(userGroup3.getMemberIds()).isNullOrEmpty();

    user.setName("John Doe");
    userAfterUpdate = userService.updateUserGroupsAndNameOfUser(
        user.getUuid(), Arrays.asList(userGroup1, userGroup3), user.getName(), ACCOUNT_ID, true);
    assertThat(userAfterUpdate.getUserGroups().size()).isEqualTo(2);
    assertThat(userAfterUpdate.getUserGroups().stream().map(UserGroup::getUuid).collect(Collectors.toSet()))
        .containsExactlyInAnyOrder(userGroup1.getUuid(), userGroup3.getUuid());
    assertThat(userAfterUpdate.getName()).isEqualTo("John Doe");

    userGroup1 = userGroupService.get(ACCOUNT_ID, userGroup1.getUuid());
    userGroup2 = userGroupService.get(ACCOUNT_ID, userGroup2.getUuid());
    userGroup3 = userGroupService.get(ACCOUNT_ID, userGroup3.getUuid());
    assertThat(userGroup1.getMemberIds()).containsExactly(user.getUuid());
    assertThat(userGroup2.getMemberIds()).isNullOrEmpty();
    assertThat(userGroup3.getMemberIds()).containsExactly(user.getUuid());
    verify(emailNotificationService, atLeastOnce()).send(emailDataArgumentCaptor.capture());
    emailsData = emailDataArgumentCaptor.getAllValues();
    assertFalse(emailsData.stream()
                    .filter(emailData -> emailData.getTemplateName().equals(ADD_ROLE_EMAIL_TEMPLATE_NAME))
                    .collect(Collectors.toList())
                    .size()
        == 1);
  }

  @Test
  public void testUpdateMembers() {
    ArgumentCaptor<EmailData> emailDataArgumentCaptor = ArgumentCaptor.forClass(EmailData.class);
    when(accountService.get(ACCOUNT_ID)).thenReturn(account);

    GenericEntityFilter appFilter = GenericEntityFilter.builder().filterType(FilterType.ALL).build();
    AppPermission appPermission =
        AppPermission.builder().permissionType(PermissionType.ALL_APP_ENTITIES).appFilter(appFilter).build();
    UserGroup userGroup1 = UserGroup.builder()
                               .accountId(ACCOUNT_ID)
                               .name("USER_GROUP1")
                               .appPermissions(Sets.newHashSet(appPermission))
                               .build();
    User user1 = anUser()
                     .withAppId(APP_ID)
                     .withEmail("user1@wings.software")
                     .withName(USER_NAME)
                     .withPassword(PASSWORD)
                     .withAccountName(ACCOUNT_NAME)
                     .withCompanyName(COMPANY_NAME)
                     .withAccounts(Lists.newArrayList(account))
                     .build();

    User user2 = anUser()
                     .withAppId(APP_ID)
                     .withEmail("user2@wings.software")
                     .withName(USER_NAME)
                     .withPassword(PASSWORD)
                     .withAccountName(ACCOUNT_NAME)
                     .withCompanyName(COMPANY_NAME)
                     .withAccounts(Lists.newArrayList(account))
                     .build();

    userGroup1 = userGroupService.save(userGroup1);
    try {
      userService.register(user1);
      userService.register(user2);
    } catch (IndexOutOfBoundsException e) {
      // Ignoring the primary account fetch failure
    }
    userGroup1.setMembers(Arrays.asList(user1, user2));
    userGroupService.updateMembers(userGroup1, true);
    verify(emailNotificationService, atLeastOnce()).send(emailDataArgumentCaptor.capture());
    List<EmailData> emailsData = emailDataArgumentCaptor.getAllValues();
    assertFalse(emailsData.stream()
                    .filter(emailData -> emailData.getTemplateName().equals(ADD_ROLE_EMAIL_TEMPLATE_NAME))
                    .collect(Collectors.toList())
                    .size()
        == 2);
  }

  @Test
  public void testIsUserAuthorizedToAcceptOrRejectApproval() {
    User user = createUser("User");
    wingsPersistence.save(user);
    UserThreadLocal.set(user);

    assertThat(userGroupService.verifyUserAuthorizedToAcceptOrRejectApproval(ACCOUNT_ID, null)).isFalse();
    assertThat(userGroupService.verifyUserAuthorizedToAcceptOrRejectApproval(null, null)).isFalse();

    UserGroup userGroup = createUserGroup(null);
    wingsPersistence.save(userGroup);
    assertThat(userGroupService.verifyUserAuthorizedToAcceptOrRejectApproval(ACCOUNT_ID, asList(userGroup.getUuid())))
        .isFalse();
    assertThat(userGroupService.verifyUserAuthorizedToAcceptOrRejectApproval(null, asList(userGroup.getUuid())))
        .isFalse();

    userGroup = createUserGroup(asList(user.getUuid()));
    wingsPersistence.save(userGroup);
    assertThat(userGroupService.verifyUserAuthorizedToAcceptOrRejectApproval(ACCOUNT_ID, asList(userGroup.getUuid())))
        .isTrue();
    assertThat(userGroupService.verifyUserAuthorizedToAcceptOrRejectApproval(null, asList(userGroup.getUuid())))
        .isTrue();

    User user1 = createUser("User1");
    wingsPersistence.save(user1);
    UserThreadLocal.set(user1);
    assertThat(userGroupService.verifyUserAuthorizedToAcceptOrRejectApproval(ACCOUNT_ID, asList(userGroup.getUuid())))
        .isFalse();
    assertThat(userGroupService.verifyUserAuthorizedToAcceptOrRejectApproval(null, asList(userGroup.getUuid())))
        .isFalse();

    user.setEmailVerified(false);
    UserThreadLocal.set(user);
    assertThat(userGroupService.verifyUserAuthorizedToAcceptOrRejectApproval(ACCOUNT_ID, asList(userGroup.getUuid())))
        .isFalse();
  }

  private User createUser(String userId) {
    return anUser().withUuid(userId).withAppId(APP_ID).withEmailVerified(true).withEmail(USER_EMAIL).build();
  }

  private UserGroup createUserGroup(List<String> memberIds) {
    return UserGroup.builder().accountId(ACCOUNT_ID).uuid(USER_GROUP_ID).memberIds(memberIds).build();
  }
}
