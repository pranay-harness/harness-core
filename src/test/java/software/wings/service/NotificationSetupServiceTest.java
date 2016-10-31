package software.wings.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static software.wings.beans.NotificationGroup.NotificationGroupBuilder.aNotificationGroup;
import static software.wings.beans.NotificationRule.NotificationRuleBuilder.aNotificationRule;
import static software.wings.dl.PageRequest.Builder.aPageRequest;

import com.google.common.collect.Lists;

import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.NotificationChannelType;
import software.wings.beans.NotificationGroup;
import software.wings.beans.NotificationRule;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingValue.SettingVariableTypes;
import software.wings.common.UUIDGenerator;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.service.intfc.NotificationSetupService;
import software.wings.service.intfc.SettingsService;

import java.util.List;
import java.util.Map;
import javax.inject.Inject;

/**
 * Created by rishi on 10/30/16.
 */
public class NotificationSetupServiceTest extends WingsBaseTest {
  @Inject @InjectMocks NotificationSetupService notificationSetupService;

  @Mock SettingsService settingsService;

  @Test
  public void shouldReturnSupportedChannelTypes() {
    List<SettingAttribute> settingList = Lists.newArrayList(new SettingAttribute());
    String appId = UUIDGenerator.getUuid();
    when(settingsService.getSettingAttributesByType(appId, SettingVariableTypes.SMTP)).thenReturn(settingList);
    Map<NotificationChannelType, Object> channelTypes = notificationSetupService.getSupportedChannelTypeDetails(appId);
    assertThat(channelTypes).isNotNull().hasSize(1).containsKey(NotificationChannelType.EMAIL);
  }

  @Test
  public void shouldCreateNotificationGroup() {
    String appId = UUIDGenerator.getUuid();
    createAndAssertNotificationGroup(appId);
  }

  @Test
  public void shouldListNotificationGroups() {
    String appId = UUIDGenerator.getUuid();
    createAndAssertNotificationGroup(appId);
    createAndAssertNotificationGroup(appId);
    createAndAssertNotificationGroup(appId);

    createAndAssertNotificationGroup(UUIDGenerator.getUuid());

    PageRequest<NotificationGroup> pageRequest = aPageRequest().addFilter("appId", Operator.EQ, appId).build();
    PageResponse<NotificationGroup> pageResponse = notificationSetupService.listNotificationGroups(pageRequest);
    assertThat(pageResponse)
        .isNotNull()
        .hasSize(3)
        .doesNotContainNull()
        .extracting("appId")
        .containsExactly(appId, appId, appId);
  }

  @Test
  public void shouldListNotificationGroupsByAppId() {
    String appId = UUIDGenerator.getUuid();
    createAndAssertNotificationGroup(appId);
    createAndAssertNotificationGroup(appId);
    createAndAssertNotificationGroup(appId);

    createAndAssertNotificationGroup(UUIDGenerator.getUuid());

    List<NotificationGroup> notificationGroups = notificationSetupService.listNotificationGroups(appId);
    assertThat(notificationGroups)
        .isNotNull()
        .hasSize(3)
        .doesNotContainNull()
        .extracting("appId")
        .containsExactly(appId, appId, appId);
  }

  @Test
  public void shouldDeleteNotificationGroup() {
    String appId = UUIDGenerator.getUuid();
    NotificationGroup notificationGroup = createAndAssertNotificationGroup(appId);
    boolean deleted =
        notificationSetupService.deleteNotificationGroups(notificationGroup.getAppId(), notificationGroup.getUuid());
    assertThat(deleted).isTrue();
  }

  private NotificationGroup createAndAssertNotificationGroup(String appId) {
    NotificationGroup notificationGroup =
        aNotificationGroup()
            .withName("prod_ops")
            .withAppId(appId)
            .addAddressesByChannelType(NotificationChannelType.EMAIL, Lists.newArrayList("a@b.com", "b@c.com"))
            .build();

    NotificationGroup created = notificationSetupService.createNotificationGroup(notificationGroup);
    assertThat(created).isNotNull().isEqualToComparingOnlyGivenFields(
        notificationGroup, "name", "appId", "addressesByChannelType");
    return created;
  }

  @Test
  public void shouldCreateNotificationRule() {
    String appId = UUIDGenerator.getUuid();
    createAndAssertNotificationRule(appId);
  }

  @Test
  public void shouldListNotificationRule() {
    String appId = UUIDGenerator.getUuid();
    createAndAssertNotificationRule(appId);
    createAndAssertNotificationRule(appId);
    createAndAssertNotificationRule(appId);
    createAndAssertNotificationRule(UUIDGenerator.getUuid());

    PageRequest<NotificationRule> pageRequest = aPageRequest().addFilter("appId", Operator.EQ, appId).build();
    PageResponse<NotificationRule> pageResponse = notificationSetupService.listNotificationRules(pageRequest);
    assertThat(pageResponse)
        .isNotNull()
        .hasSize(3)
        .doesNotContainNull()
        .extracting("appId")
        .containsExactly(appId, appId, appId);
  }

  @Test
  public void shouldListNotificationRuleByAppId() {
    String appId = UUIDGenerator.getUuid();
    createAndAssertNotificationRule(appId);
    createAndAssertNotificationRule(appId);
    createAndAssertNotificationRule(appId);
    createAndAssertNotificationRule(UUIDGenerator.getUuid());

    List<NotificationRule> res = notificationSetupService.listNotificationRules(appId);
    assertThat(res).isNotNull().hasSize(3).doesNotContainNull().extracting("appId").containsExactly(
        appId, appId, appId);
  }

  @Test
  public void shouldDeleteNotificationRule() {
    String appId = UUIDGenerator.getUuid();
    NotificationRule notificationRule = createAndAssertNotificationRule(appId);
    boolean deleted =
        notificationSetupService.deleteNotificationRule(notificationRule.getAppId(), notificationRule.getUuid());
    assertThat(deleted).isTrue();
  }

  private NotificationRule createAndAssertNotificationRule(String appId) {
    NotificationGroup notificationGroup = createAndAssertNotificationGroup(appId);
    NotificationRule notificationRule =
        aNotificationRule().withRuleName("Rule1").withAppId(appId).addNotificationGroups(notificationGroup).build();
    NotificationRule created = notificationSetupService.createNotificationRule(notificationRule);
    assertThat(created)
        .isNotNull()
        .isEqualToComparingOnlyGivenFields(notificationRule, "ruleName", "appId", "notificationGroups")
        .hasFieldOrPropertyWithValue("active", true);
    return created;
  }
}
