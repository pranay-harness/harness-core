package io.harness.cvng.alert;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.VUK;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.CvNextGenTest;
import io.harness.category.element.UnitTests;
import io.harness.cvng.alert.beans.AlertRuleDTO;
import io.harness.cvng.alert.beans.AlertRuleDTO.AlertCondition;
import io.harness.cvng.alert.beans.AlertRuleDTO.RiskNotify;
import io.harness.cvng.alert.beans.AlertRuleDTO.VerificationsNotify;
import io.harness.cvng.alert.entities.AlertRule;
import io.harness.cvng.alert.services.api.AlertRuleService;
import io.harness.cvng.alert.services.impl.AlertRuleServiceImpl;
import io.harness.cvng.alert.util.ActivityType;
import io.harness.cvng.alert.util.VerificationStatus;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

public class AlertRuleServiceImplTest extends CvNextGenTest {
  @Inject private HPersistence hPersistence;
  @Inject private AlertRuleService alertRuleService;

  @Spy private AlertRuleServiceImpl alertRuleServiceImpl;

  @Before
  public void setup() throws Exception {
    MockitoAnnotations.initMocks(this);
    FieldUtils.writeField(alertRuleServiceImpl, "hPersistence", hPersistence, true);
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
        .containsExactly(ActivityType.PRE_DEPLOYMENT, ActivityType.DURING_DEPLOYMENT, ActivityType.POST_DEPLOYMENT,
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
        .isEqualTo(ActivityType.DURING_DEPLOYMENT);
    assertThat(alertRule.getAlertCondition().getVerificationsNotify().getVerificationStatuses().get(0))
        .isEqualTo(VerificationStatus.VERIFICATION_FAILED);
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

    AlertRuleDTO alertRuleDTO = AlertRuleDTO.builder()
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
        .isEqualTo(ActivityType.DURING_DEPLOYMENT);
    assertThat(alertRuleFromDB.getAlertCondition().getVerificationsNotify().getVerificationStatuses().get(0))
        .isEqualTo(VerificationStatus.VERIFICATION_FAILED);
    assertThat(alertRuleFromDB.getAlertCondition().getNotify().getThreshold()).isEqualTo(50);
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void testProcessRiskScore_ChannelIsNotNotified() {
    List<String> services = Arrays.asList("ser1", "ser2", "ser3");
    List<String> environments = Arrays.asList("prod");
    List<ActivityType> activityTypes = Arrays.asList(ActivityType.POST_DEPLOYMENT);
    List<VerificationStatus> verificationStatuses = Arrays.asList(VerificationStatus.VERIFICATION_PASSED);

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

    alertRuleServiceImpl.processRiskScore(retrievedAlertRule.getAccountId(), retrievedAlertRule.getOrgIdentifier(),
        retrievedAlertRule.getProjectIdentifier(), retrievedAlertRule.getAlertCondition().getServices().get(0),
        retrievedAlertRule.getAlertCondition().getEnvironments().get(0), 0);

    verify(alertRuleServiceImpl, times(0)).notifyChannel();
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void testProcessRiskScore_ChannelIsNotified() {
    List<String> services = Arrays.asList("ser1", "ser2", "ser3");
    List<String> environments = Arrays.asList("prod");
    List<ActivityType> activityTypes = Arrays.asList(ActivityType.POST_DEPLOYMENT);
    List<VerificationStatus> verificationStatuses = Arrays.asList(VerificationStatus.VERIFICATION_PASSED);

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

    alertRuleServiceImpl.processRiskScore(retrievedAlertRule.getAccountId(), retrievedAlertRule.getOrgIdentifier(),
        retrievedAlertRule.getProjectIdentifier(), retrievedAlertRule.getAlertCondition().getServices().get(0),
        retrievedAlertRule.getAlertCondition().getEnvironments().get(0), 1);

    verify(alertRuleServiceImpl, times(1)).notifyChannel();
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void testProcessRiskScore_ServicesEnvironmentsNull_ChannelIsNotified() {
    List<ActivityType> activityTypes = Arrays.asList(ActivityType.POST_DEPLOYMENT);
    List<VerificationStatus> verificationStatuses = Arrays.asList(VerificationStatus.VERIFICATION_PASSED);

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

    alertRuleServiceImpl.processRiskScore(retrievedAlertRule.getAccountId(), retrievedAlertRule.getOrgIdentifier(),
        retrievedAlertRule.getProjectIdentifier(), null, null, 1);

    verify(alertRuleServiceImpl, times(1)).notifyChannel();
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void testProcessRiskScore_ServicesEnvironmentsEmptyList_ChannelIsNotified() {
    List<ActivityType> activityTypes = Arrays.asList(ActivityType.POST_DEPLOYMENT);
    List<VerificationStatus> verificationStatuses = Arrays.asList(VerificationStatus.VERIFICATION_PASSED);

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

    alertRuleServiceImpl.processRiskScore(retrievedAlertRule.getAccountId(), retrievedAlertRule.getOrgIdentifier(),
        retrievedAlertRule.getProjectIdentifier(), null, null, 1);

    verify(alertRuleServiceImpl, times(1)).notifyChannel();
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void testProcessRiskScore_EnabledRiskFalse_ChannelIsNotNotified() {
    List<String> services = Arrays.asList("ser1", "ser2", "ser3");
    List<String> environments = Arrays.asList("prod");
    List<ActivityType> activityTypes = Arrays.asList(ActivityType.POST_DEPLOYMENT);
    List<VerificationStatus> verificationStatuses = Arrays.asList(VerificationStatus.VERIFICATION_PASSED);

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

    alertRuleServiceImpl.processRiskScore(retrievedAlertRule.getAccountId(), retrievedAlertRule.getOrgIdentifier(),
        retrievedAlertRule.getProjectIdentifier(), null, null, 1);

    verify(alertRuleServiceImpl, times(0)).notifyChannel();
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void testProcessRiskScore_AllServicesDifferentEnvName_ChannelIsNotNotified() {
    List<String> services = Arrays.asList("ser1", "ser2", "ser3");
    List<String> environments = Arrays.asList("prod");
    List<ActivityType> activityTypes = Arrays.asList(ActivityType.POST_DEPLOYMENT);
    List<VerificationStatus> verificationStatuses = Arrays.asList(VerificationStatus.VERIFICATION_PASSED);

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

    alertRuleServiceImpl.processRiskScore(retrievedAlertRule.getAccountId(), retrievedAlertRule.getOrgIdentifier(),
        retrievedAlertRule.getProjectIdentifier(), retrievedAlertRule.getAlertCondition().getServices().get(0), "qa",
        1);

    verify(alertRuleServiceImpl, times(0)).notifyChannel();
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void testProcessRiskScore_AllEnvironmentsDifferentServiceName_ChannelIsNotNotified() {
    List<String> services = Arrays.asList("ser1", "ser2", "ser3");
    List<String> environments = Arrays.asList("prod", "qa");
    List<ActivityType> activityTypes = Arrays.asList(ActivityType.POST_DEPLOYMENT);
    List<VerificationStatus> verificationStatuses = Arrays.asList(VerificationStatus.VERIFICATION_PASSED);

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

    alertRuleServiceImpl.processRiskScore(retrievedAlertRule.getAccountId(), retrievedAlertRule.getOrgIdentifier(),
        retrievedAlertRule.getProjectIdentifier(), "test service",
        retrievedAlertRule.getAlertCondition().getEnvironments().get(0), 1);

    verify(alertRuleServiceImpl, times(0)).notifyChannel();
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void testProcessDeploymentVerification_ChannelIsNotified() {
    VerificationsNotify verificationsNotify =
        VerificationsNotify.builder()
            .activityTypes(Arrays.asList(ActivityType.POST_DEPLOYMENT))
            .verificationStatuses(Arrays.asList(VerificationStatus.VERIFICATION_PASSED))
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

    alertRuleServiceImpl.processDeploymentVerification(retrievedAlertRule.getAccountId(),
        retrievedAlertRule.getOrgIdentifier(), retrievedAlertRule.getProjectIdentifier(),
        retrievedAlertRule.getAlertCondition().getServices().get(0),
        retrievedAlertRule.getAlertCondition().getEnvironments().get(0),
        retrievedAlertRule.getAlertCondition().getVerificationsNotify().getActivityTypes().get(0),
        retrievedAlertRule.getAlertCondition().getVerificationsNotify().getVerificationStatuses().get(0));

    verify(alertRuleServiceImpl, times(1)).notifyChannel();
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void testProcessDeploymentVerification_EnabledVerificationFalse_ChannelIsNotNotified() {
    VerificationsNotify verificationsNotify =
        VerificationsNotify.builder()
            .activityTypes(Arrays.asList(ActivityType.POST_DEPLOYMENT))
            .verificationStatuses(Arrays.asList(VerificationStatus.VERIFICATION_PASSED))
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

    alertRuleServiceImpl.processDeploymentVerification(retrievedAlertRule.getAccountId(),
        retrievedAlertRule.getOrgIdentifier(), retrievedAlertRule.getProjectIdentifier(),
        retrievedAlertRule.getAlertCondition().getServices().get(0),
        retrievedAlertRule.getAlertCondition().getEnvironments().get(0),
        retrievedAlertRule.getAlertCondition().getVerificationsNotify().getActivityTypes().get(0),
        retrievedAlertRule.getAlertCondition().getVerificationsNotify().getVerificationStatuses().get(0));

    verify(alertRuleServiceImpl, times(0)).notifyChannel();
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void testProcessDeploymentVerification_ActivityTypeAndVerificationStatusNull_ChannelIsNotNotified() {
    VerificationsNotify verificationsNotify =
        VerificationsNotify.builder()
            .activityTypes(Arrays.asList(ActivityType.DURING_DEPLOYMENT, ActivityType.POST_DEPLOYMENT))
            .verificationStatuses(Arrays.asList(VerificationStatus.VERIFICATION_PASSED))
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

    alertRuleServiceImpl.processDeploymentVerification(retrievedAlertRule.getAccountId(),
        retrievedAlertRule.getOrgIdentifier(), retrievedAlertRule.getProjectIdentifier(),
        retrievedAlertRule.getAlertCondition().getServices().get(0),
        retrievedAlertRule.getAlertCondition().getEnvironments().get(0), null, null);

    verify(alertRuleServiceImpl, times(0)).notifyChannel();
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void testProcessDeploymentVerification_AllActivityTypesDifferentVerificationStatus_ChannelIsNotNotified() {
    VerificationsNotify verificationsNotify =
        VerificationsNotify.builder()
            .activityTypes(Arrays.asList(ActivityType.POST_DEPLOYMENT, ActivityType.DURING_DEPLOYMENT,
                ActivityType.INFRASTRUCTURE_CHANGE, ActivityType.CONFIG_CHANGE, ActivityType.PRE_DEPLOYMENT))
            .verificationStatuses(Arrays.asList(VerificationStatus.VERIFICATION_PASSED))
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

    alertRuleServiceImpl.processDeploymentVerification(retrievedAlertRule.getAccountId(),
        retrievedAlertRule.getOrgIdentifier(), retrievedAlertRule.getProjectIdentifier(),
        retrievedAlertRule.getAlertCondition().getServices().get(0),
        retrievedAlertRule.getAlertCondition().getEnvironments().get(0),
        retrievedAlertRule.getAlertCondition().getVerificationsNotify().getActivityTypes().get(0),
        VerificationStatus.VERIFICATION_FAILED);

    verify(alertRuleServiceImpl, times(0)).notifyChannel();
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void testProcessDeploymentVerification_AllVerificationStatusesDifferentActivityType_ChannelIsNotNotified() {
    VerificationsNotify verificationsNotify =
        VerificationsNotify.builder()
            .activityTypes(Arrays.asList(ActivityType.POST_DEPLOYMENT))
            .verificationStatuses(
                Arrays.asList(VerificationStatus.VERIFICATION_PASSED, VerificationStatus.VERIFICATION_PASSED))
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

    alertRuleServiceImpl.processDeploymentVerification(retrievedAlertRule.getAccountId(),
        retrievedAlertRule.getOrgIdentifier(), retrievedAlertRule.getProjectIdentifier(),
        retrievedAlertRule.getAlertCondition().getServices().get(0),
        retrievedAlertRule.getAlertCondition().getEnvironments().get(0), ActivityType.DURING_DEPLOYMENT,
        retrievedAlertRule.getAlertCondition().getVerificationsNotify().getVerificationStatuses().get(0));

    verify(alertRuleServiceImpl, times(0)).notifyChannel();
  }

  private AlertCondition getAlertConditionForAlertRuleDTODummyValues() {
    List<String> servicesDTO = new ArrayList<>();
    servicesDTO.add("serDTO1");
    servicesDTO.add("serDTO2");

    List<String> environmentsDTO = new ArrayList<>();
    environmentsDTO.add("qa");

    List<ActivityType> activityTypesDTO = new ArrayList();
    activityTypesDTO.add(ActivityType.DURING_DEPLOYMENT);

    List<VerificationStatus> verificationStatusesDTO = new ArrayList<>();
    verificationStatusesDTO.add(VerificationStatus.VERIFICATION_FAILED);

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
    activityTypes.add(ActivityType.POST_DEPLOYMENT);

    List<VerificationStatus> verificationStatuses = new ArrayList<>();
    verificationStatuses.add(VerificationStatus.VERIFICATION_PASSED);

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

    return AlertRuleDTO.builder()
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
