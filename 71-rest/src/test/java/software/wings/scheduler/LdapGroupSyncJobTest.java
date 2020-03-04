package software.wings.scheduler;

import static io.harness.rule.OwnerRule.MOHIT;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import software.wings.beans.security.UserGroup;
import software.wings.beans.sso.LdapGroupResponse;
import software.wings.beans.sso.LdapSettings;
import software.wings.service.intfc.SSOSettingService;

import java.util.Collections;

@RunWith(MockitoJUnitRunner.class)
public class LdapGroupSyncJobTest {
  @InjectMocks private LdapGroupSyncJob ldapGroupSyncJob;
  @Mock private SSOSettingService ssoSettingService;

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
}
