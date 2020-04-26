package software.wings.scheduler;

import static io.harness.rule.OwnerRule.MOHIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.Account.Builder.anAccount;
import static software.wings.beans.User.Builder.anUser;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.data.structure.UUIDGenerator;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;
import software.wings.beans.Account;
import software.wings.beans.User;
import software.wings.beans.security.UserGroup;
import software.wings.beans.sso.LdapGroupResponse;
import software.wings.beans.sso.LdapSettings;
import software.wings.beans.sso.LdapUserResponse;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.SSOSettingService;
import software.wings.service.intfc.UserService;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@RunWith(MockitoJUnitRunner.class)
public class LdapGroupSyncJobTest {
  @Inject WingsPersistence wingsPersistence;
  @InjectMocks private LdapGroupSyncJob ldapGroupSyncJob;
  @Mock private SSOSettingService ssoSettingService;
  @Mock private UserService userService;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    ldapGroupSyncJob = spy(ldapGroupSyncJob);
  }

  @Test
  @Owner(developers = MOHIT)
  @Category(UnitTests.class)
  public void shouldSyncUserGroup() {
    UserGroup userGroup = mock(UserGroup.class);
    Account account = new Account();
    String ssoId = UUIDGenerator.generateUuid();
    account.setUuid(UUIDGenerator.generateUuid());
    when(userGroup.getName()).thenReturn("userGroup123");
    when(userGroup.getAccountId()).thenReturn(UUIDGenerator.generateUuid());
    userGroup.setAccountId(UUIDGenerator.generateUuid());
    doReturn(LdapGroupResponse.builder().selectable(true).build())
        .when(ldapGroupSyncJob)
        .fetchGroupDetails(any(), any(), any());
    doReturn(userGroup).when(ldapGroupSyncJob).syncUserGroupMetadata(any(), any());
    doReturn(true).when(ldapGroupSyncJob).validateUserGroupStates(any());
    ldapGroupSyncJob.syncUserGroups(
        account.getUuid(), mock(LdapSettings.class), Collections.singletonList(userGroup), ssoId);
    verify(ssoSettingService, times(1)).closeSyncFailureAlertIfOpen(account.getUuid(), ssoId);
  }

  @Test
  @Owner(developers = MOHIT)
  @Category(UnitTests.class)
  public void shouldNotThrowExceptionWhenSyncUserGroupFails() {
    UserGroup userGroup = mock(UserGroup.class);
    Account account = new Account();
    String ssoId = UUIDGenerator.generateUuid();
    account.setUuid(UUIDGenerator.generateUuid());
    when(userGroup.getName()).thenReturn("userGroup123");
    when(userGroup.getAccountId()).thenReturn(UUIDGenerator.generateUuid());
    userGroup.setAccountId(UUIDGenerator.generateUuid());
    LdapSettings ldapSettings = mock(LdapSettings.class);
    doReturn(LdapGroupResponse.builder().selectable(false).build())
        .when(ldapGroupSyncJob)
        .fetchGroupDetails(any(), any(), any());
    doReturn(userGroup).when(ldapGroupSyncJob).syncUserGroupMetadata(any(), any());
    doReturn(true).when(ldapGroupSyncJob).validateUserGroupStates(any());
    when(ldapSettings.getDisplayName()).thenReturn("display_name");

    ldapGroupSyncJob.syncUserGroups(account.getUuid(), ldapSettings, Collections.singletonList(userGroup), ssoId);
    verify(ssoSettingService, times(1))
        .raiseSyncFailureAlert(account.getUuid(), ssoId,
            "Ldap Sync failed for groups: "
                + "[" + userGroup.getName() + "]");
  }

  @Test
  @Owner(developers = MOHIT)
  @Category(UnitTests.class)
  public void testgetUserGroupsByEmailMap() {
    String accountId = UUIDGenerator.generateUuid();
    Account account = anAccount().withUuid(accountId).build();
    String email = "user@harness.io";
    User user = anUser().email(email).accounts(Arrays.asList(account)).build();
    UserGroup userGroup1 = UserGroup.builder().members(Arrays.asList(user)).accountId(accountId).build();
    UserGroup userGroup2 = UserGroup.builder().members(Arrays.asList()).accountId(accountId).build();
    LdapUserResponse ldapUser = LdapUserResponse.builder().email(email).name("user").build();
    Map<String, Set<UserGroup>> emailToUserGroups = ldapGroupSyncJob.getUserGroupsByEmailMap(accountId,
        new HashMap<LdapUserResponse, Set<UserGroup>>() {
          { put(ldapUser, new HashSet<>(Collections.singleton(userGroup2))); }
        },
        new HashMap<UserGroup, Set<User>>() {
          { put(userGroup1, new HashSet<>(Collections.singletonList(user))); }
        });
    assertThat(emailToUserGroups.get(email).contains(userGroup2)).isTrue();
    assertThat(emailToUserGroups.get(email).contains(userGroup1)).isFalse();
  }
}
