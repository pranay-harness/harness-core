package io.harness.cvng.alert;

import static io.harness.cvng.alert.beans.AlertRuleDTO.NotificationMethod;
import static io.harness.cvng.alert.beans.AlertRuleDTO.builder;
import static io.harness.cvng.alert.util.ActivityType.DURING_DEPLOYMENT;
import static io.harness.cvng.alert.util.ActivityType.POST_DEPLOYMENT;
import static io.harness.cvng.alert.util.VerificationStatus.VERIFICATION_FAILED;
import static io.harness.cvng.alert.util.VerificationStatus.VERIFICATION_PASSED;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.persistence.HQuery.excludeAuthority;
import static io.harness.rule.OwnerRule.VUK;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.CvNextGenTest;
import io.harness.Team;
import io.harness.category.element.UnitTests;
import io.harness.cvng.alert.beans.AlertRuleDTO;
import io.harness.cvng.alert.beans.AlertRuleDTO.AlertCondition;
import io.harness.cvng.alert.beans.AlertRuleDTO.RiskNotify;
import io.harness.cvng.alert.beans.AlertRuleDTO.VerificationsNotify;
import io.harness.cvng.alert.entities.AlertRule;
import io.harness.cvng.alert.services.api.AlertRuleService;
import io.harness.cvng.alert.util.ActivityType;
import io.harness.cvng.alert.util.VerificationStatus;
import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.ng.core.dto.NotificationSettingType;
import io.harness.notification.channeldetails.SlackChannel;
import io.harness.notification.notificationclient.NotificationClient;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class AlertRuleServiceImplTest extends CvNextGenTest {
  @Inject private HPersistence hPersistence;
  @Inject private AlertRuleService alertRuleService;
  @Mock private NotificationClient notificationClient;

  @Before
  public void setup() throws Exception {
    MockitoAnnotations.initMocks(this);
    FieldUtils.writeField(alertRuleService, "notificationClient", notificationClient, true);
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void testGetAlertRuleValidate() {
    AlertRuleDTO alertRuleDTO =
        builder()
            .alertCondition(AlertCondition.builder().verificationsNotify(VerificationsNotify.builder().build()).build())
            .build();
    alertRuleDTO.validate();

    assertThat(alertRuleDTO.getAlertCondition().isAllServices()).isTrue();
    assertThat(alertRuleDTO.getAlertCondition().isAllEnvironments()).isTrue();
    assertThat(alertRuleDTO.getAlertCondition().getServices()).isEmpty();
    assertThat(alertRuleDTO.getAlertCondition().getEnvironments()).isEmpty();

    assertThat(alertRuleDTO.getAlertCondition().getVerificationsNotify().isAllActivityTpe()).isTrue();
    assertThat(alertRuleDTO.getAlertCondition().getVerificationsNotify().isAllVerificationStatuses()).isTrue();
    assertThat(alertRuleDTO.getAlertCondition().getVerificationsNotify().getActivityTypes()).isEmpty();
    assertThat(alertRuleDTO.getAlertCondition().getVerificationsNotify().getVerificationStatuses()).isEmpty();

    alertRuleDTO.getAlertCondition().setEnvironments(Lists.newArrayList("e1", "e2"));
    alertRuleDTO.getAlertCondition().setServices(Lists.newArrayList("s1", "s2"));

    alertRuleDTO.getAlertCondition().getVerificationsNotify().setVerificationStatuses(
        Lists.newArrayList(VERIFICATION_PASSED, VERIFICATION_FAILED));
    alertRuleDTO.getAlertCondition().getVerificationsNotify().setActivityTypes(
        Lists.newArrayList(POST_DEPLOYMENT, DURING_DEPLOYMENT));

    alertRuleDTO.validate();

    assertThat(alertRuleDTO.getAlertCondition().isAllServices()).isTrue();
    assertThat(alertRuleDTO.getAlertCondition().isAllEnvironments()).isTrue();
    assertThat(alertRuleDTO.getAlertCondition().getServices()).isEmpty();
    assertThat(alertRuleDTO.getAlertCondition().getEnvironments()).isEmpty();

    assertThat(alertRuleDTO.getAlertCondition().getVerificationsNotify().isAllActivityTpe()).isTrue();
    assertThat(alertRuleDTO.getAlertCondition().getVerificationsNotify().isAllVerificationStatuses()).isTrue();
    assertThat(alertRuleDTO.getAlertCondition().getVerificationsNotify().getActivityTypes()).isEmpty();
    assertThat(alertRuleDTO.getAlertCondition().getVerificationsNotify().getVerificationStatuses()).isEmpty();

    alertRuleDTO.getAlertCondition().setAllEnvironments(false);
    alertRuleDTO.getAlertCondition().setEnvironments(Lists.newArrayList("e1", "e2"));
    alertRuleDTO.getAlertCondition().setAllServices(false);
    alertRuleDTO.getAlertCondition().setServices(Lists.newArrayList("s1", "s2"));

    alertRuleDTO.getAlertCondition().getVerificationsNotify().setAllVerificationStatuses(false);
    alertRuleDTO.getAlertCondition().getVerificationsNotify().setVerificationStatuses(
        Lists.newArrayList(VERIFICATION_PASSED, VERIFICATION_FAILED));
    alertRuleDTO.getAlertCondition().getVerificationsNotify().setAllActivityTpe(false);
    alertRuleDTO.getAlertCondition().getVerificationsNotify().setActivityTypes(
        Lists.newArrayList(POST_DEPLOYMENT, DURING_DEPLOYMENT));

    assertThat(alertRuleDTO.getAlertCondition().isAllServices()).isFalse();
    assertThat(alertRuleDTO.getAlertCondition().isAllEnvironments()).isFalse();
    assertThat(alertRuleDTO.getAlertCondition().getServices()).isEqualTo(Lists.newArrayList("s1", "s2"));
    assertThat(alertRuleDTO.getAlertCondition().getEnvironments()).isEqualTo(Lists.newArrayList("e1", "e2"));

    assertThat(alertRuleDTO.getAlertCondition().getVerificationsNotify().isAllActivityTpe()).isFalse();
    assertThat(alertRuleDTO.getAlertCondition().getVerificationsNotify().isAllVerificationStatuses()).isFalse();
    assertThat(alertRuleDTO.getAlertCondition().getVerificationsNotify().getActivityTypes())
        .isEqualTo(Lists.newArrayList(POST_DEPLOYMENT, DURING_DEPLOYMENT));
    assertThat(alertRuleDTO.getAlertCondition().getVerificationsNotify().getVerificationStatuses())
        .isEqualTo(Lists.newArrayList(VERIFICATION_PASSED, VERIFICATION_FAILED));
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void testGetAlertRuleDTO() {
    AlertRule alertRule = createAlertRule();

    hPersistence.save(alertRule);

    AlertRuleDTO alertRuleDTO = alertRuleService.getAlertRuleDTO(alertRule.getAccountId(), alertRule.getOrgIdentifier(),
        alertRule.getProjectIdentifier(), alertRule.getIdentifier());

    assertThat(alertRuleDTO).isNotNull();
    assertThat(alertRuleDTO.getIdentifier()).isEqualTo(alertRule.getIdentifier());
    assertThat(alertRuleDTO.getAccountId()).isEqualTo(alertRule.getAccountId());
    assertThat(alertRuleDTO.getOrgIdentifier()).isEqualTo(alertRule.getOrgIdentifier());
    assertThat(alertRuleDTO.getProjectIdentifier()).isEqualTo(alertRule.getProjectIdentifier());
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void testGetAlertRuleDTO_Null() {
    String accountId = generateUuid();
    String orgIdentifier = generateUuid();
    String projectIdentifier = generateUuid();
    String identifier = generateUuid();

    AlertRuleDTO alertRuleDTO =
        alertRuleService.getAlertRuleDTO(accountId, orgIdentifier, projectIdentifier, identifier);

    assertThat(alertRuleDTO).isNull();
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void testGetActivityTypes() {
    String accountId = generateUuid();
    String orgIdentifier = generateUuid();
    String projectIdentifier = generateUuid();

    List<ActivityType> activityTypes = alertRuleService.getActivityTypes(accountId, orgIdentifier, projectIdentifier);

    assertThat(activityTypes).isNotNull();
    assertThat(activityTypes)
        .containsExactly(ActivityType.PRE_DEPLOYMENT, DURING_DEPLOYMENT, POST_DEPLOYMENT,
            ActivityType.INFRASTRUCTURE_CHANGE, ActivityType.CONFIG_CHANGE);
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void testCreateAlertRule() {
    AlertRuleDTO alertRuleDTO = createAlertRuleDTO();

    AlertRuleDTO alertRule = alertRuleService.createAlertRule(alertRuleDTO);

    assertThat(alertRule).isNotNull();
    assertThat(alertRule.getIdentifier()).isEqualTo("testIdentifier");
    assertThat(alertRule.getName()).isEqualTo("testName");
    assertThat(alertRule.getAlertCondition().getVerificationsNotify().getActivityTypes().get(0))
        .isEqualTo(DURING_DEPLOYMENT);
    assertThat(alertRule.getAlertCondition().getVerificationsNotify().getVerificationStatuses().get(0))
        .isEqualTo(VERIFICATION_FAILED);
    assertThat(alertRule.getAlertCondition().getEnvironments().get(0)).isEqualTo("qa");
    assertThat(alertRule.getAlertCondition().getServices().get(0)).isEqualTo("serDTO1");
    assertThat(alertRule.getAlertCondition().getServices().get(1)).isEqualTo("serDTO2");
    assertThat(alertRule.getAlertCondition().getNotify().getThreshold()).isEqualTo(50);
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void testDeleteAlertRule() {
    AlertRule alertRule = createAlertRule();

    hPersistence.save(alertRule);

    alertRuleService.deleteAlertRule(alertRule.getAccountId(), alertRule.getOrgIdentifier(),
        alertRule.getProjectIdentifier(), alertRule.getIdentifier());

    assertThat(hPersistence.get(AlertRule.class, alertRule.getUuid())).isNull();
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void testUpdateAlertRule() {
    String uuid = generateUuid();

    AlertCondition alertCondition = getAlertConditionForAlertRuleDummyValues();

    AlertCondition alertConditionDTO = getAlertConditionForAlertRuleDTODummyValues();

    AlertRule alertRule = AlertRule.builder()
                              .uuid(uuid)
                              .name("alertRuleTestName")
                              .orgIdentifier("alertRuleOrgIdentifier")
                              .projectIdentifier("alertRuleProjectIdentifier")
                              .enabled(true)
                              .identifier("alertRuleIdentifier")
                              .alertCondition(alertCondition)
                              .build();

    AlertRuleDTO alertRuleDTO = builder()
                                    .uuid(uuid)
                                    .name("alertRuleDTOTestName")
                                    .orgIdentifier("alertRuleDTOOrgIdentifier")
                                    .projectIdentifier("alertRuleDTOProjectIdentifier")
                                    .enabled(false)
                                    .identifier("alertRuleDTOIdentifier")
                                    .alertCondition(alertConditionDTO)
                                    .build();

    hPersistence.save(alertRule);

    alertRuleService.updateAlertRule(generateUuid(), generateUuid(), generateUuid(), alertRuleDTO);

    AlertRule alertRuleFromDB = hPersistence.get(AlertRule.class, alertRule.getUuid());
    assertThat(alertRuleFromDB).isNotNull();
    assertThat(alertRuleFromDB.getName()).isEqualTo("alertRuleDTOTestName");
    assertThat(alertRuleFromDB.getOrgIdentifier()).isEqualTo("alertRuleDTOOrgIdentifier");
    assertThat(alertRuleFromDB.getProjectIdentifier()).isEqualTo("alertRuleDTOProjectIdentifier");
    assertThat(alertRuleFromDB.isEnabled()).isFalse();
    assertThat(alertRuleFromDB.getIdentifier()).isEqualTo("alertRuleDTOIdentifier");
    assertThat(alertRuleFromDB.getAlertCondition().getServices().get(0)).isEqualTo("serDTO1");
    assertThat(alertRuleFromDB.getAlertCondition().getServices().get(1)).isEqualTo("serDTO2");
    assertThat(alertRuleFromDB.getAlertCondition().getEnvironments().get(0)).isEqualTo("qa");
    assertThat(alertRuleFromDB.getAlertCondition().getVerificationsNotify().getActivityTypes().get(0))
        .isEqualTo(DURING_DEPLOYMENT);
    assertThat(alertRuleFromDB.getAlertCondition().getVerificationsNotify().getVerificationStatuses().get(0))
        .isEqualTo(VERIFICATION_FAILED);
    assertThat(alertRuleFromDB.getAlertCondition().getNotify().getThreshold()).isEqualTo(50);
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void testProcessRiskScore_ChannelIsNotNotified() {
    List<String> services = Arrays.asList("ser1", "ser2", "ser3");
    List<String> environments = Arrays.asList("prod");
    List<ActivityType> activityTypes = Arrays.asList(POST_DEPLOYMENT);
    List<VerificationStatus> verificationStatuses = Arrays.asList(VERIFICATION_PASSED);

    VerificationsNotify verificationsNotify =
        VerificationsNotify.builder().activityTypes(activityTypes).verificationStatuses(verificationStatuses).build();

    AlertCondition alertCondition = AlertCondition.builder()
                                        .enabledRisk(true)
                                        .enabledVerifications(true)
                                        .services(services)
                                        .environments(environments)
                                        .notify(RiskNotify.builder().threshold(2).build())
                                        .verificationsNotify(verificationsNotify)
                                        .build();

    AlertRule alertRule = AlertRule.builder()
                              .uuid(generateUuid())
                              .name(generateUuid())
                              .accountId(generateUuid())
                              .orgIdentifier(generateUuid())
                              .projectIdentifier(generateUuid())
                              .identifier(generateUuid())
                              .alertCondition(alertCondition)
                              .build();

    hPersistence.save(alertRule);

    AlertRule retrievedAlertRule = hPersistence.get(AlertRule.class, alertRule.getUuid());
    assertThat(retrievedAlertRule).isNotNull();

    alertRuleService.processRiskScore(retrievedAlertRule.getAccountId(), retrievedAlertRule.getOrgIdentifier(),
        retrievedAlertRule.getProjectIdentifier(), retrievedAlertRule.getAlertCondition().getServices().get(0),
        retrievedAlertRule.getAlertCondition().getEnvironments().get(0), CVMonitoringCategory.PERFORMANCE,
        Instant.now(), 0);

    ArgumentCaptor<SlackChannel> applicationArgumentCaptor = ArgumentCaptor.forClass(SlackChannel.class);
    verify(notificationClient, times(0)).sendNotificationAsync(applicationArgumentCaptor.capture());
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void testProcessRiskScore_ChannelIsNotified() {
    List<String> services = Arrays.asList("ser1", "ser2", "ser3");
    List<String> environments = Arrays.asList("prod");
    List<ActivityType> activityTypes = Arrays.asList(POST_DEPLOYMENT);
    List<VerificationStatus> verificationStatuses = Arrays.asList(VERIFICATION_PASSED);

    VerificationsNotify verificationsNotify =
        VerificationsNotify.builder().activityTypes(activityTypes).verificationStatuses(verificationStatuses).build();

    NotificationMethod notificationMethod = NotificationMethod.builder()
                                                .notificationSettingType(NotificationSettingType.Slack)
                                                .slackWebhook("testWebHook")
                                                .build();

    AlertCondition alertCondition = AlertCondition.builder()
                                        .enabledRisk(true)
                                        .enabledVerifications(true)
                                        .services(services)
                                        .environments(environments)
                                        .notify(RiskNotify.builder().threshold(40).build())
                                        .verificationsNotify(verificationsNotify)
                                        .build();

    AlertRule alertRule = AlertRule.builder()
                              .uuid(generateUuid())
                              .name(generateUuid())
                              .accountId(generateUuid())
                              .orgIdentifier(generateUuid())
                              .projectIdentifier(generateUuid())
                              .identifier(generateUuid())
                              .alertCondition(alertCondition)
                              .notificationMethod(notificationMethod)
                              .build();
    hPersistence.save(alertRule);

    AlertRule retrievedAlertRule = hPersistence.get(AlertRule.class, alertRule.getUuid());
    assertThat(retrievedAlertRule).isNotNull();

    SlackChannel slack_test = getSlackChannel(retrievedAlertRule);

    alertRuleService.processRiskScore(retrievedAlertRule.getAccountId(), retrievedAlertRule.getOrgIdentifier(),
        retrievedAlertRule.getProjectIdentifier(), retrievedAlertRule.getAlertCondition().getServices().get(0),
        retrievedAlertRule.getAlertCondition().getEnvironments().get(0), CVMonitoringCategory.PERFORMANCE,
        Instant.now(), 1);

    ArgumentCaptor<SlackChannel> applicationArgumentCaptor = ArgumentCaptor.forClass(SlackChannel.class);

    verify(notificationClient, times(1)).sendNotificationAsync(applicationArgumentCaptor.capture());
    assertThat(applicationArgumentCaptor.getValue().getAccountId()).isEqualTo(slack_test.getAccountId());
    assertThat(applicationArgumentCaptor.getValue().getSlackWebHookURLs()).isEqualTo(slack_test.getSlackWebHookURLs());
    assertThat(applicationArgumentCaptor.getValue().getTeam()).isEqualTo(slack_test.getTeam());
    assertThat(applicationArgumentCaptor.getValue().getTemplateId()).isEqualTo(slack_test.getTemplateId());
    assertThat(applicationArgumentCaptor.getValue().getTemplateData()).isNotEmpty();
    assertThat(applicationArgumentCaptor.getValue().getUserGroupIds()).isEqualTo(slack_test.getUserGroupIds());
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void testProcessRiskScore_ServicesEnvironmentsNull_ChannelIsNotified() {
    List<ActivityType> activityTypes = Arrays.asList(POST_DEPLOYMENT);
    List<VerificationStatus> verificationStatuses = Arrays.asList(VERIFICATION_PASSED);

    VerificationsNotify verificationsNotify =
        VerificationsNotify.builder().activityTypes(activityTypes).verificationStatuses(verificationStatuses).build();

    AlertCondition alertCondition = AlertCondition.builder()
                                        .enabledRisk(true)
                                        .enabledVerifications(true)
                                        .services(null)
                                        .environments(null)
                                        .notify(RiskNotify.builder().threshold(40).build())
                                        .verificationsNotify(verificationsNotify)
                                        .build();

    NotificationMethod notificationMethod = NotificationMethod.builder()
                                                .notificationSettingType(NotificationSettingType.Slack)
                                                .slackWebhook("testWebHook")
                                                .build();

    AlertRuleDTO alertRule = AlertRuleDTO.builder()
                                 .name(generateUuid())
                                 .accountId(generateUuid())
                                 .orgIdentifier(generateUuid())
                                 .projectIdentifier(generateUuid())
                                 .identifier(generateUuid())
                                 .alertCondition(alertCondition)
                                 .notificationMethod(notificationMethod)
                                 .build();

    alertRuleService.createAlertRule(alertRule);

    AlertRule retrievedAlertRule = hPersistence.createQuery(AlertRule.class, excludeAuthority).get();
    assertThat(retrievedAlertRule).isNotNull();

    SlackChannel slack_test = getSlackChannel(retrievedAlertRule);

    alertRuleService.processRiskScore(retrievedAlertRule.getAccountId(), retrievedAlertRule.getOrgIdentifier(),
        retrievedAlertRule.getProjectIdentifier(), null, null, CVMonitoringCategory.PERFORMANCE, Instant.now(), 1);

    ArgumentCaptor<SlackChannel> applicationArgumentCaptor = ArgumentCaptor.forClass(SlackChannel.class);

    verify(notificationClient, times(1)).sendNotificationAsync(applicationArgumentCaptor.capture());
    assertThat(applicationArgumentCaptor.getValue().getAccountId()).isEqualTo(slack_test.getAccountId());
    assertThat(applicationArgumentCaptor.getValue().getSlackWebHookURLs()).isEqualTo(slack_test.getSlackWebHookURLs());
    assertThat(applicationArgumentCaptor.getValue().getTeam()).isEqualTo(slack_test.getTeam());
    assertThat(applicationArgumentCaptor.getValue().getTemplateId()).isEqualTo(slack_test.getTemplateId());
    assertThat(applicationArgumentCaptor.getValue().getTemplateData()).isNotEmpty();
    assertThat(applicationArgumentCaptor.getValue().getUserGroupIds()).isEqualTo(slack_test.getUserGroupIds());
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void testProcessRiskScore_ServicesEnvironmentsEmptyList_ChannelIsNotified() {
    List<ActivityType> activityTypes = Arrays.asList(POST_DEPLOYMENT);
    List<VerificationStatus> verificationStatuses = Arrays.asList(VERIFICATION_PASSED);

    VerificationsNotify verificationsNotify =
        VerificationsNotify.builder().activityTypes(activityTypes).verificationStatuses(verificationStatuses).build();

    AlertCondition alertCondition = AlertCondition.builder()
                                        .enabledRisk(true)
                                        .enabledVerifications(true)
                                        .services(emptyList())
                                        .environments(emptyList())
                                        .notify(RiskNotify.builder().threshold(40).build())
                                        .verificationsNotify(verificationsNotify)
                                        .build();

    NotificationMethod notificationMethod = NotificationMethod.builder()
                                                .notificationSettingType(NotificationSettingType.Slack)
                                                .slackWebhook("testWebHook")
                                                .build();

    AlertRuleDTO alertRule = AlertRuleDTO.builder()
                                 .uuid(generateUuid())
                                 .name(generateUuid())
                                 .accountId(generateUuid())
                                 .orgIdentifier(generateUuid())
                                 .projectIdentifier(generateUuid())
                                 .identifier(generateUuid())
                                 .alertCondition(alertCondition)
                                 .notificationMethod(notificationMethod)
                                 .build();

    alertRuleService.createAlertRule(alertRule);

    AlertRule retrievedAlertRule = hPersistence.get(AlertRule.class, alertRule.getUuid());
    assertThat(retrievedAlertRule).isNotNull();

    SlackChannel slack_test = getSlackChannel(retrievedAlertRule);

    alertRuleService.processRiskScore(retrievedAlertRule.getAccountId(), retrievedAlertRule.getOrgIdentifier(),
        retrievedAlertRule.getProjectIdentifier(), null, null, CVMonitoringCategory.PERFORMANCE, Instant.now(), 1);

    ArgumentCaptor<SlackChannel> applicationArgumentCaptor = ArgumentCaptor.forClass(SlackChannel.class);

    verify(notificationClient, times(1)).sendNotificationAsync(applicationArgumentCaptor.capture());
    assertThat(applicationArgumentCaptor.getValue().getAccountId()).isEqualTo(slack_test.getAccountId());
    assertThat(applicationArgumentCaptor.getValue().getSlackWebHookURLs()).isEqualTo(slack_test.getSlackWebHookURLs());
    assertThat(applicationArgumentCaptor.getValue().getTeam()).isEqualTo(slack_test.getTeam());
    assertThat(applicationArgumentCaptor.getValue().getTemplateId()).isEqualTo(slack_test.getTemplateId());
    assertThat(applicationArgumentCaptor.getValue().getTemplateData()).isNotEmpty();
    assertThat(applicationArgumentCaptor.getValue().getUserGroupIds()).isEqualTo(slack_test.getUserGroupIds());
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void testProcessRiskScore_EnabledRiskFalse_ChannelIsNotNotified() {
    List<String> services = Arrays.asList("ser1", "ser2", "ser3");
    List<String> environments = Arrays.asList("prod");
    List<ActivityType> activityTypes = Arrays.asList(POST_DEPLOYMENT);
    List<VerificationStatus> verificationStatuses = Arrays.asList(VERIFICATION_PASSED);

    VerificationsNotify verificationsNotify =
        VerificationsNotify.builder().activityTypes(activityTypes).verificationStatuses(verificationStatuses).build();

    AlertCondition alertCondition = AlertCondition.builder()
                                        .enabledRisk(false)
                                        .enabledVerifications(true)
                                        .services(services)
                                        .environments(environments)
                                        .notify(RiskNotify.builder().threshold(40).build())
                                        .verificationsNotify(verificationsNotify)
                                        .build();

    AlertRule alertRule = AlertRule.builder()
                              .uuid(generateUuid())
                              .name(generateUuid())
                              .accountId(generateUuid())
                              .orgIdentifier(generateUuid())
                              .projectIdentifier(generateUuid())
                              .identifier(generateUuid())
                              .alertCondition(alertCondition)
                              .build();

    hPersistence.save(alertRule);

    AlertRule retrievedAlertRule = hPersistence.get(AlertRule.class, alertRule.getUuid());
    assertThat(retrievedAlertRule).isNotNull();

    alertRuleService.processRiskScore(retrievedAlertRule.getAccountId(), retrievedAlertRule.getOrgIdentifier(),
        retrievedAlertRule.getProjectIdentifier(), null, null, CVMonitoringCategory.PERFORMANCE, Instant.now(), 1);

    ArgumentCaptor<SlackChannel> applicationArgumentCaptor = ArgumentCaptor.forClass(SlackChannel.class);
    verify(notificationClient, times(0)).sendNotificationAsync(applicationArgumentCaptor.capture());
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void testProcessRiskScore_AllServicesDifferentEnvName_ChannelIsNotNotified() {
    List<String> services = Arrays.asList("ser1", "ser2", "ser3");
    List<String> environments = Arrays.asList("prod");
    List<ActivityType> activityTypes = Arrays.asList(POST_DEPLOYMENT);
    List<VerificationStatus> verificationStatuses = Arrays.asList(VERIFICATION_PASSED);

    VerificationsNotify verificationsNotify =
        VerificationsNotify.builder().activityTypes(activityTypes).verificationStatuses(verificationStatuses).build();

    AlertCondition alertCondition = AlertCondition.builder()
                                        .enabledRisk(true)
                                        .enabledVerifications(true)
                                        .services(services)
                                        .environments(environments)
                                        .notify(RiskNotify.builder().threshold(40).build())
                                        .verificationsNotify(verificationsNotify)
                                        .build();

    AlertRule alertRule = AlertRule.builder()
                              .uuid(generateUuid())
                              .name(generateUuid())
                              .accountId(generateUuid())
                              .orgIdentifier(generateUuid())
                              .projectIdentifier(generateUuid())
                              .identifier(generateUuid())
                              .alertCondition(alertCondition)
                              .build();

    hPersistence.save(alertRule);

    AlertRule retrievedAlertRule = hPersistence.get(AlertRule.class, alertRule.getUuid());
    assertThat(retrievedAlertRule).isNotNull();

    alertRuleService.processRiskScore(retrievedAlertRule.getAccountId(), retrievedAlertRule.getOrgIdentifier(),
        retrievedAlertRule.getProjectIdentifier(), retrievedAlertRule.getAlertCondition().getServices().get(0), "qa",
        CVMonitoringCategory.PERFORMANCE, Instant.now(), 1);

    ArgumentCaptor<SlackChannel> applicationArgumentCaptor = ArgumentCaptor.forClass(SlackChannel.class);
    verify(notificationClient, times(0)).sendNotificationAsync(applicationArgumentCaptor.capture());
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void testProcessRiskScore_AllEnvironmentsDifferentServiceName_ChannelIsNotNotified() {
    List<String> services = Arrays.asList("ser1", "ser2", "ser3");
    List<String> environments = Arrays.asList("prod", "qa");
    List<ActivityType> activityTypes = Arrays.asList(POST_DEPLOYMENT);
    List<VerificationStatus> verificationStatuses = Arrays.asList(VERIFICATION_PASSED);

    VerificationsNotify verificationsNotify =
        VerificationsNotify.builder().activityTypes(activityTypes).verificationStatuses(verificationStatuses).build();

    AlertCondition alertCondition = AlertCondition.builder()
                                        .enabledRisk(true)
                                        .enabledVerifications(true)
                                        .services(services)
                                        .environments(environments)
                                        .notify(RiskNotify.builder().threshold(40).build())
                                        .verificationsNotify(verificationsNotify)
                                        .build();

    AlertRule alertRule = AlertRule.builder()
                              .uuid(generateUuid())
                              .name(generateUuid())
                              .accountId(generateUuid())
                              .orgIdentifier(generateUuid())
                              .projectIdentifier(generateUuid())
                              .identifier(generateUuid())
                              .alertCondition(alertCondition)
                              .build();

    hPersistence.save(alertRule);

    AlertRule retrievedAlertRule = hPersistence.get(AlertRule.class, alertRule.getUuid());
    assertThat(retrievedAlertRule).isNotNull();

    alertRuleService.processRiskScore(retrievedAlertRule.getAccountId(), retrievedAlertRule.getOrgIdentifier(),
        retrievedAlertRule.getProjectIdentifier(), "test service",
        retrievedAlertRule.getAlertCondition().getEnvironments().get(0), CVMonitoringCategory.PERFORMANCE,
        Instant.now(), 1);

    ArgumentCaptor<SlackChannel> applicationArgumentCaptor = ArgumentCaptor.forClass(SlackChannel.class);
    verify(notificationClient, times(0)).sendNotificationAsync(applicationArgumentCaptor.capture());
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void testProcessDeploymentVerification_ChannelIsNotified() {
    VerificationsNotify verificationsNotify = VerificationsNotify.builder()
                                                  .activityTypes(Arrays.asList(POST_DEPLOYMENT))
                                                  .verificationStatuses(Arrays.asList(VERIFICATION_PASSED))
                                                  .build();

    AlertCondition alertCondition = AlertCondition.builder()
                                        .enabledRisk(true)
                                        .enabledVerifications(true)
                                        .services(Arrays.asList("ser1", "ser2", "ser3"))
                                        .environments(Arrays.asList("prod"))
                                        .notify(RiskNotify.builder().threshold(40).build())
                                        .verificationsNotify(verificationsNotify)
                                        .build();

    NotificationMethod notificationMethod = NotificationMethod.builder()
                                                .notificationSettingType(NotificationSettingType.Slack)
                                                .slackWebhook("testWebHook")
                                                .build();

    AlertRule alertRule = AlertRule.builder()
                              .uuid(generateUuid())
                              .name(generateUuid())
                              .accountId(generateUuid())
                              .orgIdentifier(generateUuid())
                              .projectIdentifier(generateUuid())
                              .identifier(generateUuid())
                              .alertCondition(alertCondition)
                              .notificationMethod(notificationMethod)
                              .build();

    hPersistence.save(alertRule);

    AlertRule retrievedAlertRule = hPersistence.get(AlertRule.class, alertRule.getUuid());
    assertThat(retrievedAlertRule).isNotNull();

    SlackChannel slack_test = getSlackChannel(retrievedAlertRule);

    alertRuleService.processDeploymentVerification(retrievedAlertRule.getAccountId(),
        retrievedAlertRule.getOrgIdentifier(), retrievedAlertRule.getProjectIdentifier(),
        retrievedAlertRule.getAlertCondition().getServices().get(0),
        retrievedAlertRule.getAlertCondition().getEnvironments().get(0),
        retrievedAlertRule.getAlertCondition().getVerificationsNotify().getActivityTypes().get(0),
        retrievedAlertRule.getAlertCondition().getVerificationsNotify().getVerificationStatuses().get(0));

    ArgumentCaptor<SlackChannel> applicationArgumentCaptor = ArgumentCaptor.forClass(SlackChannel.class);

    verify(notificationClient, times(1)).sendNotificationAsync(applicationArgumentCaptor.capture());
    assertThat(applicationArgumentCaptor.getValue().getAccountId()).isEqualTo(slack_test.getAccountId());
    assertThat(applicationArgumentCaptor.getValue().getSlackWebHookURLs()).isEqualTo(slack_test.getSlackWebHookURLs());
    assertThat(applicationArgumentCaptor.getValue().getTeam()).isEqualTo(slack_test.getTeam());
    assertThat(applicationArgumentCaptor.getValue().getTemplateId()).isEqualTo(slack_test.getTemplateId());
    assertThat(applicationArgumentCaptor.getValue().getTemplateData()).isNotEmpty();
    assertThat(applicationArgumentCaptor.getValue().getUserGroupIds()).isEqualTo(slack_test.getUserGroupIds());
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void testProcessDeploymentVerification_EnabledVerificationFalse_ChannelIsNotNotified() {
    VerificationsNotify verificationsNotify = VerificationsNotify.builder()
                                                  .activityTypes(Arrays.asList(POST_DEPLOYMENT))
                                                  .verificationStatuses(Arrays.asList(VERIFICATION_PASSED))
                                                  .build();

    AlertCondition alertCondition = AlertCondition.builder()
                                        .enabledRisk(true)
                                        .enabledVerifications(false)
                                        .services(Arrays.asList("ser1", "ser2", "ser3"))
                                        .environments(Arrays.asList("prod"))
                                        .notify(RiskNotify.builder().threshold(40).build())
                                        .verificationsNotify(verificationsNotify)
                                        .build();

    AlertRule alertRule = AlertRule.builder()
                              .uuid(generateUuid())
                              .name(generateUuid())
                              .accountId(generateUuid())
                              .orgIdentifier(generateUuid())
                              .projectIdentifier(generateUuid())
                              .identifier(generateUuid())
                              .alertCondition(alertCondition)
                              .build();

    hPersistence.save(alertRule);

    AlertRule retrievedAlertRule = hPersistence.get(AlertRule.class, alertRule.getUuid());
    assertThat(retrievedAlertRule).isNotNull();

    alertRuleService.processDeploymentVerification(retrievedAlertRule.getAccountId(),
        retrievedAlertRule.getOrgIdentifier(), retrievedAlertRule.getProjectIdentifier(),
        retrievedAlertRule.getAlertCondition().getServices().get(0),
        retrievedAlertRule.getAlertCondition().getEnvironments().get(0),
        retrievedAlertRule.getAlertCondition().getVerificationsNotify().getActivityTypes().get(0),
        retrievedAlertRule.getAlertCondition().getVerificationsNotify().getVerificationStatuses().get(0));

    ArgumentCaptor<SlackChannel> applicationArgumentCaptor = ArgumentCaptor.forClass(SlackChannel.class);
    verify(notificationClient, times(0)).sendNotificationAsync(applicationArgumentCaptor.capture());
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void testProcessDeploymentVerification_ActivityTypeAndVerificationStatusNull_ChannelIsNotNotified() {
    VerificationsNotify verificationsNotify = VerificationsNotify.builder()
                                                  .activityTypes(Arrays.asList(DURING_DEPLOYMENT, POST_DEPLOYMENT))
                                                  .verificationStatuses(Arrays.asList(VERIFICATION_PASSED))
                                                  .build();

    AlertCondition alertCondition = AlertCondition.builder()
                                        .enabledRisk(true)
                                        .enabledVerifications(true)
                                        .services(Arrays.asList("ser1", "ser2", "ser3"))
                                        .environments(Arrays.asList("prod"))
                                        .notify(RiskNotify.builder().threshold(40).build())
                                        .verificationsNotify(verificationsNotify)
                                        .build();

    AlertRule alertRule = AlertRule.builder()
                              .uuid(generateUuid())
                              .name(generateUuid())
                              .accountId(generateUuid())
                              .orgIdentifier(generateUuid())
                              .projectIdentifier(generateUuid())
                              .identifier(generateUuid())
                              .alertCondition(alertCondition)
                              .build();

    hPersistence.save(alertRule);

    AlertRule retrievedAlertRule = hPersistence.get(AlertRule.class, alertRule.getUuid());
    assertThat(retrievedAlertRule).isNotNull();

    alertRuleService.processDeploymentVerification(retrievedAlertRule.getAccountId(),
        retrievedAlertRule.getOrgIdentifier(), retrievedAlertRule.getProjectIdentifier(),
        retrievedAlertRule.getAlertCondition().getServices().get(0),
        retrievedAlertRule.getAlertCondition().getEnvironments().get(0), null, null);

    ArgumentCaptor<SlackChannel> applicationArgumentCaptor = ArgumentCaptor.forClass(SlackChannel.class);
    verify(notificationClient, times(0)).sendNotificationAsync(applicationArgumentCaptor.capture());
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void testProcessDeploymentVerification_AllActivityTypesDifferentVerificationStatus_ChannelIsNotNotified() {
    VerificationsNotify verificationsNotify =
        VerificationsNotify.builder()
            .activityTypes(Arrays.asList(POST_DEPLOYMENT, DURING_DEPLOYMENT, ActivityType.INFRASTRUCTURE_CHANGE,
                ActivityType.CONFIG_CHANGE, ActivityType.PRE_DEPLOYMENT))
            .verificationStatuses(Arrays.asList(VERIFICATION_PASSED))
            .build();

    AlertCondition alertCondition = AlertCondition.builder()
                                        .enabledRisk(true)
                                        .enabledVerifications(true)
                                        .services(Arrays.asList("ser1", "ser2", "ser3"))
                                        .environments(Arrays.asList("prod"))
                                        .notify(RiskNotify.builder().threshold(40).build())
                                        .verificationsNotify(verificationsNotify)
                                        .build();

    AlertRule alertRule = AlertRule.builder()
                              .uuid(generateUuid())
                              .name(generateUuid())
                              .accountId(generateUuid())
                              .orgIdentifier(generateUuid())
                              .projectIdentifier(generateUuid())
                              .identifier(generateUuid())
                              .alertCondition(alertCondition)
                              .build();

    hPersistence.save(alertRule);

    AlertRule retrievedAlertRule = hPersistence.get(AlertRule.class, alertRule.getUuid());
    assertThat(retrievedAlertRule).isNotNull();

    alertRuleService.processDeploymentVerification(retrievedAlertRule.getAccountId(),
        retrievedAlertRule.getOrgIdentifier(), retrievedAlertRule.getProjectIdentifier(),
        retrievedAlertRule.getAlertCondition().getServices().get(0),
        retrievedAlertRule.getAlertCondition().getEnvironments().get(0),
        retrievedAlertRule.getAlertCondition().getVerificationsNotify().getActivityTypes().get(0), VERIFICATION_FAILED);

    ArgumentCaptor<SlackChannel> applicationArgumentCaptor = ArgumentCaptor.forClass(SlackChannel.class);
    verify(notificationClient, times(0)).sendNotificationAsync(applicationArgumentCaptor.capture());
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void testProcessDeploymentVerification_AllVerificationStatusesDifferentActivityType_ChannelIsNotNotified() {
    VerificationsNotify verificationsNotify =
        VerificationsNotify.builder()
            .activityTypes(Arrays.asList(POST_DEPLOYMENT))
            .verificationStatuses(Arrays.asList(VERIFICATION_PASSED, VERIFICATION_PASSED))
            .build();

    AlertCondition alertCondition = AlertCondition.builder()
                                        .enabledRisk(true)
                                        .enabledVerifications(true)
                                        .services(Arrays.asList("ser1", "ser2", "ser3"))
                                        .environments(Arrays.asList("prod"))
                                        .notify(RiskNotify.builder().threshold(40).build())
                                        .verificationsNotify(verificationsNotify)
                                        .build();

    AlertRule alertRule = AlertRule.builder()
                              .uuid(generateUuid())
                              .name(generateUuid())
                              .accountId(generateUuid())
                              .orgIdentifier(generateUuid())
                              .projectIdentifier(generateUuid())
                              .identifier(generateUuid())
                              .alertCondition(alertCondition)
                              .build();

    hPersistence.save(alertRule);

    AlertRule retrievedAlertRule = hPersistence.get(AlertRule.class, alertRule.getUuid());
    assertThat(retrievedAlertRule).isNotNull();

    alertRuleService.processDeploymentVerification(retrievedAlertRule.getAccountId(),
        retrievedAlertRule.getOrgIdentifier(), retrievedAlertRule.getProjectIdentifier(),
        retrievedAlertRule.getAlertCondition().getServices().get(0),
        retrievedAlertRule.getAlertCondition().getEnvironments().get(0), DURING_DEPLOYMENT,
        retrievedAlertRule.getAlertCondition().getVerificationsNotify().getVerificationStatuses().get(0));

    ArgumentCaptor<SlackChannel> applicationArgumentCaptor = ArgumentCaptor.forClass(SlackChannel.class);
    verify(notificationClient, times(0)).sendNotificationAsync(applicationArgumentCaptor.capture());
  }

  private SlackChannel getSlackChannel(AlertRule retrievedAlertRule) {
    return SlackChannel.builder()
        .accountId(retrievedAlertRule.getAccountId())
        .slackWebHookURLs(Collections.singletonList("testWebHook"))
        .team(Team.CV)
        .templateId("slack_vanilla")
        .templateData(Collections.emptyMap())
        .userGroupIds(emptyList())
        .build();
  }

  private AlertCondition getAlertConditionForAlertRuleDTODummyValues() {
    List<String> servicesDTO = new ArrayList<>();
    servicesDTO.add("serDTO1");
    servicesDTO.add("serDTO2");

    List<String> environmentsDTO = new ArrayList<>();
    environmentsDTO.add("qa");

    List<ActivityType> activityTypesDTO = new ArrayList();
    activityTypesDTO.add(DURING_DEPLOYMENT);

    List<VerificationStatus> verificationStatusesDTO = new ArrayList<>();
    verificationStatusesDTO.add(VERIFICATION_FAILED);

    VerificationsNotify verificationsNotifyDTO = VerificationsNotify.builder()
                                                     .activityTypes(activityTypesDTO)
                                                     .verificationStatuses(verificationStatusesDTO)
                                                     .build();

    return AlertCondition.builder()
        .enabledRisk(false)
        .enabledVerifications(false)
        .services(servicesDTO)
        .environments(environmentsDTO)
        .notify(RiskNotify.builder().threshold(50).build())
        .verificationsNotify(verificationsNotifyDTO)
        .build();
  }

  private AlertCondition getAlertConditionForAlertRuleDummyValues() {
    List<String> services = new ArrayList<>();
    services.add("ser1");
    services.add("ser2");

    List<String> environments = new ArrayList<>();
    environments.add("prod");

    List<ActivityType> activityTypes = new ArrayList();
    activityTypes.add(POST_DEPLOYMENT);

    List<VerificationStatus> verificationStatuses = new ArrayList<>();
    verificationStatuses.add(VERIFICATION_PASSED);

    VerificationsNotify verificationsNotify =
        VerificationsNotify.builder().activityTypes(activityTypes).verificationStatuses(verificationStatuses).build();

    return AlertCondition.builder()
        .enabledRisk(true)
        .enabledVerifications(true)
        .services(services)
        .environments(environments)
        .notify(RiskNotify.builder().threshold(30).build())
        .verificationsNotify(verificationsNotify)
        .build();
  }

  private AlertRule createAlertRule() {
    return AlertRule.builder()
        .uuid(generateUuid())
        .accountId(generateUuid())
        .orgIdentifier(generateUuid())
        .projectIdentifier(generateUuid())
        .identifier(generateUuid())
        .build();
  }

  private AlertRuleDTO createAlertRuleDTO() {
    AlertCondition alertCondition = getAlertConditionForAlertRuleDTODummyValues();

    return builder()
        .uuid(generateUuid())
        .accountId(generateUuid())
        .name("testName")
        .orgIdentifier(generateUuid())
        .projectIdentifier(generateUuid())
        .enabled(true)
        .identifier("testIdentifier")
        .alertCondition(alertCondition)
        .build();
  }
}
