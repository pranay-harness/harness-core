package software.wings.service;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;
import static software.wings.beans.NotificationGroup.NotificationGroupBuilder.aNotificationGroup;
import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import org.apache.commons.collections.CollectionUtils;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.Base;
import software.wings.beans.NotificationChannelType;
import software.wings.beans.NotificationGroup;
import software.wings.beans.SearchFilter.Operator;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.service.intfc.NotificationSetupService;
import software.wings.service.intfc.SettingsService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by rishi on 10/30/16.
 */
public class NotificationSetupServiceTest extends WingsBaseTest {
  @Inject @InjectMocks NotificationSetupService notificationSetupService;

  @Mock SettingsService settingsService;

  //  @Test
  //  public void shouldReturnSupportedChannelTypes() {
  //    List<SettingAttribute> settingList = Lists.newArrayList(new SettingAttribute());
  //    String appId = UUIDGenerator.generateUuid();
  //    when(settingsService.getSettingAttributesByType(appId,
  //    SettingVariableTypes.SMTP.name())).thenReturn(settingList); Map<NotificationChannelType, Object> channelTypes =
  //    notificationSetupService.getSupportedChannelTypeDetails(appId);
  //    assertThat(channelTypes).isNotNull().hasSize(1).containsKey(NotificationChannelType.EMAIL);
  //  }

  @Test
  public void shouldCreateNotificationGroup() {
    String accountId = generateUuid();
    createAndAssertNotificationGroup(accountId);
    String name1 = "name1" + System.currentTimeMillis();
    NotificationGroup notificationGroup1 = createAndAssertNotificationGroup(accountId, name1, true);
    String name2 = "name2" + System.currentTimeMillis();
    NotificationGroup notificationGroup2 = createAndAssertNotificationGroup(accountId, name2, false);
    notificationSetupService.deleteNotificationGroups(accountId, notificationGroup1.getUuid());
    notificationSetupService.deleteNotificationGroups(accountId, notificationGroup2.getUuid());
  }

  @Test
  public void listDefaultNotificationGroup() {
    String accountId = generateUuid();

    String name1 = "name1" + System.currentTimeMillis();
    NotificationGroup notificationGroup1 = createAndAssertNotificationGroup(accountId, name1, false);
    List<NotificationGroup> notificationGroups = notificationSetupService.listDefaultNotificationGroup(accountId);
    assertTrue(CollectionUtils.isEmpty(notificationGroups));

    String name2 = "name2" + System.currentTimeMillis();
    NotificationGroup notificationGroup2 = createAndAssertNotificationGroup(accountId, name2, true);
    notificationGroups = notificationSetupService.listDefaultNotificationGroup(accountId);
    assertThat(notificationGroups)
        .isNotNull()
        .hasSize(1)
        .doesNotContainNull()
        .extracting("name")
        .containsExactly(name2);

    notificationSetupService.deleteNotificationGroups(accountId, notificationGroup1.getUuid());
    notificationSetupService.deleteNotificationGroups(accountId, notificationGroup2.getUuid());
  }

  @Test
  public void updateNotificationGroupDefaultValue() {
    String accountId = generateUuid();

    String name1 = "name1" + System.currentTimeMillis();
    NotificationGroup notificationGroup1 = createAndAssertNotificationGroup(accountId, name1, true);
    List<NotificationGroup> notificationGroups = notificationSetupService.listDefaultNotificationGroup(accountId);
    assertThat(notificationGroups)
        .isNotNull()
        .hasSize(1)
        .doesNotContainNull()
        .extracting("name")
        .containsExactly(name1);

    String name2 = "name2" + System.currentTimeMillis();
    NotificationGroup notificationGroup2 = createAndAssertNotificationGroup(accountId, name2, true);
    notificationGroups = notificationSetupService.listDefaultNotificationGroup(accountId);
    assertThat(notificationGroups)
        .isNotNull()
        .hasSize(1)
        .doesNotContainNull()
        .extracting("name")
        .containsExactly(name2);

    notificationSetupService.deleteNotificationGroups(accountId, notificationGroup1.getUuid());
    notificationSetupService.deleteNotificationGroups(accountId, notificationGroup2.getUuid());
  }

  @Test
  public void shouldListNotificationGroups() {
    String accountId = generateUuid();
    createAndAssertNotificationGroup(accountId);
    createAndAssertNotificationGroup(accountId);
    createAndAssertNotificationGroup(accountId);

    createAndAssertNotificationGroup(generateUuid());

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
    String accountId = generateUuid();
    createAndAssertNotificationGroup(accountId);
    createAndAssertNotificationGroup(accountId);
    createAndAssertNotificationGroup(accountId);

    createAndAssertNotificationGroup(generateUuid());

    List<NotificationGroup> notificationGroups = notificationSetupService.listNotificationGroups(accountId);
    assertThat(notificationGroups)
        .isNotNull()
        .hasSize(3)
        .doesNotContainNull()
        .extracting("accountId")
        .containsExactly(accountId, accountId, accountId);
  }

  @Test
  public void shouldListNotificationGroupsByAccountIdName() {
    String accountId = generateUuid();
    createAndAssertNotificationGroup(accountId);
    createAndAssertNotificationGroup(accountId);
    createAndAssertNotificationGroup(accountId);

    createAndAssertNotificationGroup(generateUuid());

    List<NotificationGroup> notificationGroups = notificationSetupService.listNotificationGroups(accountId, "prod_ops");
    assertThat(notificationGroups)
        .isNotNull()
        .hasSize(3)
        .doesNotContainNull()
        .extracting("accountId")
        .containsExactly(accountId, accountId, accountId);
  }

  @Test
  public void shouldDeleteNotificationGroup() {
    String accountId = generateUuid();
    NotificationGroup notificationGroup = createAndAssertNotificationGroup(accountId);
    boolean deleted = notificationSetupService.deleteNotificationGroups(
        notificationGroup.getAccountId(), notificationGroup.getUuid());
    assertThat(deleted).isTrue();
  }

  @Test
  public void shouldReadNotificationGroup() {
    String accountId = generateUuid();
    NotificationGroup notificationGroup = createAndAssertNotificationGroup(accountId);
    NotificationGroup notificationGroup2 =
        notificationSetupService.readNotificationGroup(notificationGroup.getAccountId(), notificationGroup.getUuid());
    assertThat(notificationGroup2).isNotNull().isEqualToIgnoringGivenFields(notificationGroup);
  }

  private NotificationGroup createAndAssertNotificationGroup(String accountId) {
    return createAndAssertNotificationGroup(accountId, "prod_ops", true);
  }

  private NotificationGroup createAndAssertNotificationGroup(
      String accountId, String name, boolean defaultNotificationGroupForAccount) {
    NotificationGroup notificationGroup =
        aNotificationGroup()
            .withName(name)
            .withEditable(true)
            .withAppId(Base.GLOBAL_APP_ID)
            .withAccountId(accountId)
            .withDefaultNotificationGroupForAccount(defaultNotificationGroupForAccount)
            .addAddressesByChannelType(NotificationChannelType.EMAIL, Lists.newArrayList("a@b.com", "b@c.com"))
            .build();

    NotificationGroup created = notificationSetupService.createNotificationGroup(notificationGroup);
    assertThat(created).isNotNull().isEqualToComparingOnlyGivenFields(
        notificationGroup, "name", "accountId", "addressesByChannelType", "defaultNotificationGroupForAccount");
    return created;
  }

  @Test
  public void TestGetUserEmailAddressFromNotificationGroups() {
    String accountId = generateUuid();

    NotificationGroup notificationGroup = createAndAssertNotificationGroup(accountId);
    List userEmailAddresses =
        notificationSetupService.getUserEmailAddressFromNotificationGroups(accountId, asList(notificationGroup));
    assertThat(userEmailAddresses.size()).isEqualTo(2);
    assertThat(userEmailAddresses).contains("a@b.com");
    assertThat(userEmailAddresses).contains("b@c.com");
    assertThat(userEmailAddresses).doesNotContain("xyz@abc.com");

    Map<NotificationChannelType, List<String>> notificationChannelTypeListMap = new HashMap<>();
    notificationChannelTypeListMap.put(NotificationChannelType.EMAIL, null);
    notificationGroup.setAddressesByChannelType(notificationChannelTypeListMap);
    notificationSetupService.updateNotificationGroup(notificationGroup);
    userEmailAddresses =
        notificationSetupService.getUserEmailAddressFromNotificationGroups(accountId, asList(notificationGroup));
    assertThat(userEmailAddresses.size()).isEqualTo(0);

    notificationChannelTypeListMap = new HashMap<>();
    notificationChannelTypeListMap.put(NotificationChannelType.EMAIL, asList(""));
    notificationGroup.setAddressesByChannelType(notificationChannelTypeListMap);
    notificationSetupService.updateNotificationGroup(notificationGroup);
    userEmailAddresses =
        notificationSetupService.getUserEmailAddressFromNotificationGroups(accountId, asList(notificationGroup));
    assertThat(userEmailAddresses.size()).isEqualTo(0);
  }

  // Move these under workflow service

  //
  //  @Test
  //  public void shouldCreateNotificationRule() {
  //    String appId = UUIDGenerator.generateUuid();
  //    createAndAssertNotificationRule(appId);
  //  }

  //  @Test
  //  public void shouldListNotificationRule() {
  //    String appId = UUIDGenerator.generateUuid();
  //    createAndAssertNotificationRule(appId);
  //    createAndAssertNotificationRule(appId);
  //    createAndAssertNotificationRule(appId);
  //    createAndAssertNotificationRule(UUIDGenerator.generateUuid());
  //
  //    PageRequest<NotificationRule> pageRequest = aPageRequest().addFilter("appId", Operator.EQ, appId).build();
  //    PageResponse<NotificationRule> pageResponse = notificationSetupService.listNotificationRules(pageRequest);
  //    assertThat(pageResponse).isNotNull().hasSize(3).doesNotContainNull().extracting("appId").containsExactly(appId,
  //    appId, appId);
  //  }
  //
  //  @Test
  //  public void shouldListNotificationRuleByAppId() {
  //    String appId = UUIDGenerator.generateUuid();
  //    createAndAssertNotificationRule(appId);
  //    createAndAssertNotificationRule(appId);
  //    createAndAssertNotificationRule(appId);
  //    createAndAssertNotificationRule(UUIDGenerator.generateUuid());
  //
  //    List<NotificationRule> res = notificationSetupService.listNotificationRules(appId);
  //    assertThat(res).isNotNull().hasSize(3).doesNotContainNull().extracting("appId").containsExactly(appId, appId,
  //    appId);
  //  }
  //
  //  @Test
  //  public void shouldReadNotificationRule() {
  //    String appId = UUIDGenerator.generateUuid();
  //    NotificationRule notificationRule = createAndAssertNotificationRule(appId);
  //    NotificationRule notificationRule2 = notificationSetupService.readNotificationRule(notificationRule.getAppId(),
  //    notificationRule.generateUuid());
  //    assertThat(notificationRule2).isNotNull().isEqualToIgnoringGivenFields(notificationRule);
  //  }
  //
  //  @Test
  //  public void shouldDeleteNotificationRule() {
  //    String appId = UUIDGenerator.generateUuid();
  //    NotificationRule notificationRule = createAndAssertNotificationRule(appId);
  //    boolean deleted = notificationSetupService.deleteNotificationRule(notificationRule.getAppId(),
  //    notificationRule.generateUuid()); assertThat(deleted).isTrue();
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
