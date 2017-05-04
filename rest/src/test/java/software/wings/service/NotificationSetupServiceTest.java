package software.wings.service;

import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.NotificationGroup.NotificationGroupBuilder.aNotificationGroup;
import static software.wings.dl.PageRequest.Builder.aPageRequest;

import com.google.common.collect.Lists;

import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.Base;
import software.wings.beans.NotificationChannelType;
import software.wings.beans.NotificationGroup;
import software.wings.beans.SearchFilter.Operator;
import software.wings.common.UUIDGenerator;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.service.intfc.NotificationSetupService;
import software.wings.service.intfc.SettingsService;

import java.util.List;
import javax.inject.Inject;

/**
 * Created by rishi on 10/30/16.
 */
public class NotificationSetupServiceTest extends WingsBaseTest {
  @Inject @InjectMocks NotificationSetupService notificationSetupService;

  @Mock SettingsService settingsService;

  //  @Test
  //  public void shouldReturnSupportedChannelTypes() {
  //    List<SettingAttribute> settingList = Lists.newArrayList(new SettingAttribute());
  //    String appId = UUIDGenerator.getUuid();
  //    when(settingsService.getSettingAttributesByType(appId,
  //    SettingVariableTypes.SMTP.name())).thenReturn(settingList); Map<NotificationChannelType, Object> channelTypes =
  //    notificationSetupService.getSupportedChannelTypeDetails(appId);
  //    assertThat(channelTypes).isNotNull().hasSize(1).containsKey(NotificationChannelType.EMAIL);
  //  }

  @Test
  public void shouldCreateNotificationGroup() {
    String accountId = UUIDGenerator.getUuid();
    createAndAssertNotificationGroup(accountId);
  }

  @Test
  public void shouldListNotificationGroups() {
    String accountId = UUIDGenerator.getUuid();
    createAndAssertNotificationGroup(accountId);
    createAndAssertNotificationGroup(accountId);
    createAndAssertNotificationGroup(accountId);

    createAndAssertNotificationGroup(UUIDGenerator.getUuid());

    PageRequest<NotificationGroup> pageRequest = aPageRequest().addFilter("accountId", Operator.EQ, accountId).build();
    PageResponse<NotificationGroup> pageResponse = notificationSetupService.listNotificationGroups(pageRequest);
    assertThat(pageResponse)
        .isNotNull()
        .hasSize(3)
        .doesNotContainNull()
        .extracting("accountId")
        .containsExactly(accountId, accountId, accountId);
  }

  @Test
  public void shouldListNotificationGroupsByAccountId() {
    String accountId = UUIDGenerator.getUuid();
    createAndAssertNotificationGroup(accountId);
    createAndAssertNotificationGroup(accountId);
    createAndAssertNotificationGroup(accountId);

    createAndAssertNotificationGroup(UUIDGenerator.getUuid());

    List<NotificationGroup> notificationGroups = notificationSetupService.listNotificationGroups(accountId);
    assertThat(notificationGroups)
        .isNotNull()
        .hasSize(3)
        .doesNotContainNull()
        .extracting("accountId")
        .containsExactly(accountId, accountId, accountId);
  }

  @Test
  public void shouldDeleteNotificationGroup() {
    String accountId = UUIDGenerator.getUuid();
    NotificationGroup notificationGroup = createAndAssertNotificationGroup(accountId);
    boolean deleted = notificationSetupService.deleteNotificationGroups(
        notificationGroup.getAccountId(), notificationGroup.getUuid());
    assertThat(deleted).isTrue();
  }

  @Test
  public void shouldReadNotificationGroup() {
    String accountId = UUIDGenerator.getUuid();
    NotificationGroup notificationGroup = createAndAssertNotificationGroup(accountId);
    NotificationGroup notificationGroup2 =
        notificationSetupService.readNotificationGroup(notificationGroup.getAccountId(), notificationGroup.getUuid());
    assertThat(notificationGroup2).isNotNull().isEqualToIgnoringGivenFields(notificationGroup);
  }

  private NotificationGroup createAndAssertNotificationGroup(String accountId) {
    NotificationGroup notificationGroup =
        aNotificationGroup()
            .withName("prod_ops")
            .withEditable(true)
            .withAppId(Base.GLOBAL_APP_ID)
            .withAccountId(accountId)
            .addAddressesByChannelType(NotificationChannelType.EMAIL, Lists.newArrayList("a@b.com", "b@c.com"))
            .build();

    NotificationGroup created = notificationSetupService.createNotificationGroup(notificationGroup);
    assertThat(created).isNotNull().isEqualToComparingOnlyGivenFields(
        notificationGroup, "name", "accountId", "addressesByChannelType");
    return created;
  }

  // Move these under workflow service

  //
  //  @Test
  //  public void shouldCreateNotificationRule() {
  //    String appId = UUIDGenerator.getUuid();
  //    createAndAssertNotificationRule(appId);
  //  }

  //  @Test
  //  public void shouldListNotificationRule() {
  //    String appId = UUIDGenerator.getUuid();
  //    createAndAssertNotificationRule(appId);
  //    createAndAssertNotificationRule(appId);
  //    createAndAssertNotificationRule(appId);
  //    createAndAssertNotificationRule(UUIDGenerator.getUuid());
  //
  //    PageRequest<NotificationRule> pageRequest = aPageRequest().addFilter("appId", Operator.EQ, appId).build();
  //    PageResponse<NotificationRule> pageResponse = notificationSetupService.listNotificationRules(pageRequest);
  //    assertThat(pageResponse).isNotNull().hasSize(3).doesNotContainNull().extracting("appId").containsExactly(appId,
  //    appId, appId);
  //  }
  //
  //  @Test
  //  public void shouldListNotificationRuleByAppId() {
  //    String appId = UUIDGenerator.getUuid();
  //    createAndAssertNotificationRule(appId);
  //    createAndAssertNotificationRule(appId);
  //    createAndAssertNotificationRule(appId);
  //    createAndAssertNotificationRule(UUIDGenerator.getUuid());
  //
  //    List<NotificationRule> res = notificationSetupService.listNotificationRules(appId);
  //    assertThat(res).isNotNull().hasSize(3).doesNotContainNull().extracting("appId").containsExactly(appId, appId,
  //    appId);
  //  }
  //
  //  @Test
  //  public void shouldReadNotificationRule() {
  //    String appId = UUIDGenerator.getUuid();
  //    NotificationRule notificationRule = createAndAssertNotificationRule(appId);
  //    NotificationRule notificationRule2 = notificationSetupService.readNotificationRule(notificationRule.getAppId(),
  //    notificationRule.getUuid());
  //    assertThat(notificationRule2).isNotNull().isEqualToIgnoringGivenFields(notificationRule);
  //  }
  //
  //  @Test
  //  public void shouldDeleteNotificationRule() {
  //    String appId = UUIDGenerator.getUuid();
  //    NotificationRule notificationRule = createAndAssertNotificationRule(appId);
  //    boolean deleted = notificationSetupService.deleteNotificationRule(notificationRule.getAppId(),
  //    notificationRule.getUuid()); assertThat(deleted).isTrue();
  //  }
  //
  //  private NotificationRule createAndAssertNotificationRule(String appId) {
  //
  //    NotificationGroup notificationGroup = createAndAssertNotificationGroup(appId);
  //    NotificationRule notificationRule =
  //    aNotificationRule().withAppId(appId).addNotificationGroup(notificationGroup).build(); NotificationRule created =
  //    notificationSetupService.createNotificationRule(notificationRule);
  //    assertThat(created).isNotNull().isEqualToComparingOnlyGivenFields(notificationRule, "appId",
  //    "notificationGroups").hasFieldOrPropertyWithValue("active", true); return created;
  //  }
}
