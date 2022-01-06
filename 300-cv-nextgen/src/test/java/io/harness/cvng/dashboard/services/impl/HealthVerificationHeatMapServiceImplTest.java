/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.dashboard.services.impl;

import static io.harness.cvng.beans.DataSourceType.APP_DYNAMICS;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.persistence.HQuery.excludeAuthority;
import static io.harness.rule.OwnerRule.KAMAL;
import static io.harness.rule.OwnerRule.PRAVEEN;
import static io.harness.rule.OwnerRule.RAGHU;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import io.harness.CvNextGenTestBase;
import io.harness.category.element.UnitTests;
import io.harness.cvng.activity.beans.ActivityVerificationResultDTO.CategoryRisk;
import io.harness.cvng.activity.entities.Activity;
import io.harness.cvng.activity.entities.KubernetesActivity;
import io.harness.cvng.activity.services.api.ActivityService;
import io.harness.cvng.analysis.beans.Risk;
import io.harness.cvng.analysis.entities.HealthVerificationPeriod;
import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.core.entities.AppDynamicsCVConfig;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.entities.SplunkCVConfig;
import io.harness.cvng.core.entities.VerificationTask;
import io.harness.cvng.core.entities.VerificationTask.DeploymentInfo;
import io.harness.cvng.core.services.api.VerificationTaskService;
import io.harness.cvng.dashboard.entities.HealthVerificationHeatMap;
import io.harness.cvng.dashboard.entities.HealthVerificationHeatMap.AggregationLevel;
import io.harness.cvng.dashboard.services.api.HealthVerificationHeatMapService;
import io.harness.cvng.verificationjob.services.api.VerificationJobInstanceService;
import io.harness.persistence.HPersistence;
import io.harness.reflection.ReflectionUtils;
import io.harness.rule.Owner;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import java.lang.reflect.Field;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class HealthVerificationHeatMapServiceImplTest extends CvNextGenTestBase {
  @Mock private ActivityService activityService;
  @Mock private VerificationJobInstanceService verificationJobInstanceService;
  @Mock private VerificationTaskService verificationTaskService;
  @Inject private VerificationTaskService realVerificationTaskService;

  @Inject private HealthVerificationHeatMapService heatMapService;

  private String projectIdentifier;
  private String serviceIdentifier;
  private String envIdentifier;
  private String accountId;
  private String verificationTaskId;
  private String verificationJobInstanceId;
  private String activityId;
  private String cvConfigId;
  private long startTime = 1603780200000l;

  @Inject private HPersistence hPersistence;

  @Before
  public void setup() throws Exception {
    verificationTaskId = generateUuid();
    envIdentifier = generateUuid();
    serviceIdentifier = generateUuid();
    accountId = generateUuid();
    projectIdentifier = generateUuid();
    verificationJobInstanceId = generateUuid();
    activityId = generateUuid();
    cvConfigId = generateUuid();

    MockitoAnnotations.initMocks(this);

    FieldUtils.writeField(heatMapService, "verificationTaskService", verificationTaskService, true);
    FieldUtils.writeField(heatMapService, "verificationJobInstanceService", verificationJobInstanceService, true);
    FieldUtils.writeField(heatMapService, "activityService", activityService, true);

    when(verificationTaskService.getCVConfigId(verificationTaskId)).thenReturn(cvConfigId);
    when(verificationJobInstanceService.getEmbeddedCVConfig(any(), any())).thenReturn(getAppDCVConfig());
    when(activityService.get(activityId)).thenReturn(getActivity());
    when(verificationTaskService.get(verificationTaskId))
        .thenReturn(VerificationTask.builder()
                        .taskInfo(DeploymentInfo.builder()
                                      .cvConfigId(cvConfigId)
                                      .verificationJobInstanceId(verificationJobInstanceId)
                                      .build())
                        .build());
    when(activityService.getByVerificationJobInstanceId(verificationJobInstanceId)).thenReturn(getActivity());
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testUpdateRiskScore_create() {
    Instant endTime = Instant.now();
    heatMapService.updateRisk(verificationTaskId, 1.0, endTime, HealthVerificationPeriod.PRE_ACTIVITY);

    List<HealthVerificationHeatMap> heatMaps = hPersistence.createQuery(HealthVerificationHeatMap.class).asList();
    assertThat(heatMaps.size()).isEqualTo(2);

    HealthVerificationHeatMap heatMap = heatMaps.get(0);
    validateHeatMaps(heatMap, 1.0, endTime, HealthVerificationPeriod.PRE_ACTIVITY,
        HealthVerificationHeatMap.AggregationLevel.VERIFICATION_TASK, CVMonitoringCategory.PERFORMANCE);

    HealthVerificationHeatMap activityLevelMap = heatMaps.get(1);
    validateHeatMaps(activityLevelMap, 1.0, endTime, HealthVerificationPeriod.PRE_ACTIVITY,
        HealthVerificationHeatMap.AggregationLevel.ACTIVITY, CVMonitoringCategory.PERFORMANCE);
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testUpsertAddsAllFields() {
    Instant endTime = Instant.now();
    heatMapService.updateRisk(verificationTaskId, 1.0, endTime, HealthVerificationPeriod.PRE_ACTIVITY);

    List<HealthVerificationHeatMap> heatMaps =
        hPersistence.createQuery(HealthVerificationHeatMap.class, excludeAuthority).asList();
    Set<String> nullableFields = Sets.newHashSet();
    heatMaps.forEach(heatMap -> {
      List<Field> fields = ReflectionUtils.getAllDeclaredAndInheritedFields(HealthVerificationHeatMap.class);
      fields.stream().filter(field -> !nullableFields.contains(field.getName())).forEach(field -> {
        try {
          field.setAccessible(true);
          assertThat(field.get(heatMap)).withFailMessage("field %s is null", field.getName()).isNotNull();
        } catch (IllegalAccessException e) {
          throw new RuntimeException(e);
        }
      });
    });
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testUpdateRiskScore_createAndUpdate() {
    Instant endTime = Instant.now();
    heatMapService.updateRisk(
        verificationTaskId, 1.0, endTime.minus(Duration.ofMinutes(5)), HealthVerificationPeriod.PRE_ACTIVITY);
    heatMapService.updateRisk(verificationTaskId, 0.0, endTime, HealthVerificationPeriod.PRE_ACTIVITY);

    List<HealthVerificationHeatMap> heatMaps = hPersistence.createQuery(HealthVerificationHeatMap.class).asList();
    assertThat(heatMaps.size()).isEqualTo(2);

    HealthVerificationHeatMap heatMap = heatMaps.get(0);
    validateHeatMaps(heatMap, 0.0, endTime, HealthVerificationPeriod.PRE_ACTIVITY,
        HealthVerificationHeatMap.AggregationLevel.VERIFICATION_TASK, CVMonitoringCategory.PERFORMANCE);

    HealthVerificationHeatMap activityLevelMap = heatMaps.get(1);
    validateHeatMaps(activityLevelMap, 0.0, endTime, HealthVerificationPeriod.PRE_ACTIVITY,
        HealthVerificationHeatMap.AggregationLevel.ACTIVITY, CVMonitoringCategory.PERFORMANCE);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testUpdateRiskScore_createPreAndPostActivity() {
    Instant endTime = Instant.now();
    heatMapService.updateRisk(verificationTaskId, 1.0, endTime, HealthVerificationPeriod.PRE_ACTIVITY);
    heatMapService.updateRisk(
        verificationTaskId, 0.0, endTime.plus(Duration.ofMinutes(15)), HealthVerificationPeriod.POST_ACTIVITY);

    List<HealthVerificationHeatMap> heatMaps = hPersistence.createQuery(HealthVerificationHeatMap.class).asList();
    assertThat(heatMaps.size()).isEqualTo(4);

    HealthVerificationHeatMap heatMap = heatMaps.get(0);
    validateHeatMaps(heatMap, 1.0, endTime, HealthVerificationPeriod.PRE_ACTIVITY,
        HealthVerificationHeatMap.AggregationLevel.VERIFICATION_TASK, CVMonitoringCategory.PERFORMANCE);

    HealthVerificationHeatMap activityLevelMap = heatMaps.get(1);
    validateHeatMaps(activityLevelMap, 1.0, endTime, HealthVerificationPeriod.PRE_ACTIVITY,
        HealthVerificationHeatMap.AggregationLevel.ACTIVITY, CVMonitoringCategory.PERFORMANCE);

    HealthVerificationHeatMap postHeatMap = heatMaps.get(2);
    validateHeatMaps(postHeatMap, 0.0, endTime.plus(Duration.ofMinutes(15)), HealthVerificationPeriod.POST_ACTIVITY,
        HealthVerificationHeatMap.AggregationLevel.VERIFICATION_TASK, CVMonitoringCategory.PERFORMANCE);

    HealthVerificationHeatMap postActivityLevelMap = heatMaps.get(3);
    validateHeatMaps(postActivityLevelMap, 0.0, endTime.plus(Duration.ofMinutes(15)),
        HealthVerificationPeriod.POST_ACTIVITY, HealthVerificationHeatMap.AggregationLevel.ACTIVITY,
        CVMonitoringCategory.PERFORMANCE);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testUpdateRiskScore_createMultipleCategory() {
    Instant endTime = Instant.now();
    heatMapService.updateRisk(verificationTaskId, 1.0, endTime, HealthVerificationPeriod.PRE_ACTIVITY);

    CVConfig cvConfig = getAppDCVConfig();
    cvConfig.setCategory(CVMonitoringCategory.ERRORS);
    when(verificationTaskService.getCVConfigId(verificationTaskId + "-2")).thenReturn(cvConfigId);
    when(verificationJobInstanceService.getEmbeddedCVConfig(eq(cvConfigId + "-2"), any())).thenReturn(cvConfig);
    when(activityService.get(activityId)).thenReturn(getActivity());
    when(activityService.getByVerificationJobInstanceId(verificationJobInstanceId + "-2")).thenReturn(getActivity());
    when(verificationTaskService.get(verificationTaskId + "-2"))
        .thenReturn(VerificationTask.builder()
                        .taskInfo(DeploymentInfo.builder()
                                      .verificationJobInstanceId(verificationJobInstanceId + "-2")
                                      .cvConfigId(cvConfigId + "-2")
                                      .build())
                        .build());
    heatMapService.updateRisk(verificationTaskId + "-2", 1.0, endTime, HealthVerificationPeriod.PRE_ACTIVITY);

    List<HealthVerificationHeatMap> heatMaps = hPersistence.createQuery(HealthVerificationHeatMap.class).asList();
    assertThat(heatMaps.size()).isEqualTo(4);

    HealthVerificationHeatMap heatMap = heatMaps.get(0);
    validateHeatMaps(heatMap, 1.0, endTime, HealthVerificationPeriod.PRE_ACTIVITY,
        HealthVerificationHeatMap.AggregationLevel.VERIFICATION_TASK, CVMonitoringCategory.PERFORMANCE);

    HealthVerificationHeatMap activityLevelMap = heatMaps.get(1);
    validateHeatMaps(activityLevelMap, 1.0, endTime, HealthVerificationPeriod.PRE_ACTIVITY,
        HealthVerificationHeatMap.AggregationLevel.ACTIVITY, CVMonitoringCategory.PERFORMANCE);

    HealthVerificationHeatMap errorsHeatmap = heatMaps.get(2);
    validateHeatMaps(errorsHeatmap, 1.0, endTime, HealthVerificationPeriod.PRE_ACTIVITY,
        HealthVerificationHeatMap.AggregationLevel.VERIFICATION_TASK, CVMonitoringCategory.ERRORS);

    HealthVerificationHeatMap errorsActivityLevel = heatMaps.get(3);
    validateHeatMaps(errorsActivityLevel, 1.0, endTime, HealthVerificationPeriod.PRE_ACTIVITY,
        HealthVerificationHeatMap.AggregationLevel.ACTIVITY, CVMonitoringCategory.ERRORS);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testUpdateRiskScore_createMultipleConfigsSameCategory() {
    Instant endTime = Instant.now();
    heatMapService.updateRisk(verificationTaskId, 0.0, endTime, HealthVerificationPeriod.PRE_ACTIVITY);

    CVConfig cvConfig = getSplunkConfig();
    when(verificationTaskService.getCVConfigId(verificationTaskId + "-2")).thenReturn(cvConfigId + "-2");
    when(verificationJobInstanceService.getEmbeddedCVConfig(eq(cvConfigId + "-2"), any())).thenReturn(cvConfig);
    when(activityService.get(activityId)).thenReturn(getActivity());
    when(activityService.getByVerificationJobInstanceId(verificationJobInstanceId + "-2")).thenReturn(getActivity());
    when(verificationTaskService.get(verificationTaskId + "-2"))
        .thenReturn(VerificationTask.builder()
                        .taskInfo(DeploymentInfo.builder()
                                      .verificationJobInstanceId(verificationJobInstanceId + "-2")
                                      .cvConfigId(cvConfigId + "-2")
                                      .build())
                        .build());
    heatMapService.updateRisk(verificationTaskId + "-2", 1.0, endTime, HealthVerificationPeriod.PRE_ACTIVITY);

    List<HealthVerificationHeatMap> heatMaps = hPersistence.createQuery(HealthVerificationHeatMap.class).asList();
    assertThat(heatMaps.size()).isEqualTo(3);

    HealthVerificationHeatMap heatMap = heatMaps.get(0);
    validateHeatMaps(heatMap, 0.0, endTime, HealthVerificationPeriod.PRE_ACTIVITY,
        HealthVerificationHeatMap.AggregationLevel.VERIFICATION_TASK, CVMonitoringCategory.PERFORMANCE);

    HealthVerificationHeatMap activityLevelMap = heatMaps.get(1);
    validateHeatMaps(activityLevelMap, 1.0, endTime, HealthVerificationPeriod.PRE_ACTIVITY,
        HealthVerificationHeatMap.AggregationLevel.ACTIVITY, CVMonitoringCategory.PERFORMANCE);

    HealthVerificationHeatMap errorsHeatmap = heatMaps.get(2);
    validateHeatMaps(errorsHeatmap, 1.0, endTime, HealthVerificationPeriod.PRE_ACTIVITY,
        HealthVerificationHeatMap.AggregationLevel.VERIFICATION_TASK, CVMonitoringCategory.PERFORMANCE);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetAggregatedRisk() {
    Instant endTime = Instant.now();
    heatMapService.updateRisk(verificationTaskId, 1.0, endTime, HealthVerificationPeriod.PRE_ACTIVITY);
    heatMapService.updateRisk(
        verificationTaskId, 0.7, endTime.plus(Duration.ofMinutes(15)), HealthVerificationPeriod.POST_ACTIVITY);

    Set<CategoryRisk> preActivityRisks =
        heatMapService.getAggregatedRisk(activityId, HealthVerificationPeriod.PRE_ACTIVITY);
    Set<CategoryRisk> postActivityRisks =
        heatMapService.getAggregatedRisk(activityId, HealthVerificationPeriod.POST_ACTIVITY);

    assertThat(preActivityRisks).isNotNull();
    assertThat(postActivityRisks).isNotNull();

    assertThat(preActivityRisks.size()).isEqualTo(CVMonitoringCategory.values().length);
    assertThat(postActivityRisks.size()).isEqualTo(CVMonitoringCategory.values().length);

    for (CategoryRisk categoryRisk : preActivityRisks) {
      if (categoryRisk.getCategory().equals(CVMonitoringCategory.PERFORMANCE)) {
        assertThat(categoryRisk.getRisk()).isEqualTo(100.0);
      } else {
        assertThat(categoryRisk.getRisk()).isEqualTo(-1.0);
      }
    }

    for (CategoryRisk categoryRisk : postActivityRisks) {
      if (categoryRisk.getCategory().equals(CVMonitoringCategory.PERFORMANCE)) {
        assertThat(categoryRisk.getRisk()).isEqualTo(70.0);
      } else {
        assertThat(categoryRisk.getRisk()).isEqualTo(-1.0);
      }
    }
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetVerificationJobInstanceAggregatedRisk() {
    Instant endTime = Instant.now();
    heatMapService.updateRisk(verificationTaskId, 1.0, endTime, HealthVerificationPeriod.PRE_ACTIVITY);
    heatMapService.updateRisk(
        verificationTaskId, 0.7, endTime.plus(Duration.ofMinutes(15)), HealthVerificationPeriod.POST_ACTIVITY);
    when(verificationTaskService.maybeGetVerificationTaskIds(anyString(), anyString()))
        .thenReturn(Collections.singleton(verificationTaskId));
    Set<CategoryRisk> preActivityRisks = heatMapService.getVerificationJobInstanceAggregatedRisk(
        accountId, verificationJobInstanceId, HealthVerificationPeriod.PRE_ACTIVITY);
    Set<CategoryRisk> postActivityRisks = heatMapService.getVerificationJobInstanceAggregatedRisk(
        accountId, verificationJobInstanceId, HealthVerificationPeriod.POST_ACTIVITY);

    assertThat(preActivityRisks).isNotNull();
    assertThat(postActivityRisks).isNotNull();

    assertThat(preActivityRisks.size()).isEqualTo(CVMonitoringCategory.values().length);
    assertThat(postActivityRisks.size()).isEqualTo(CVMonitoringCategory.values().length);

    for (CategoryRisk categoryRisk : preActivityRisks) {
      if (categoryRisk.getCategory().equals(CVMonitoringCategory.PERFORMANCE)) {
        assertThat(categoryRisk.getRisk()).isEqualTo(100.0);
      } else {
        assertThat(categoryRisk.getRisk()).isEqualTo(-1.0);
      }
    }

    for (CategoryRisk categoryRisk : postActivityRisks) {
      if (categoryRisk.getCategory().equals(CVMonitoringCategory.PERFORMANCE)) {
        assertThat(categoryRisk.getRisk()).isEqualTo(70.0);
      } else {
        assertThat(categoryRisk.getRisk()).isEqualTo(-1.0);
      }
    }
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetVerificationJobInstanceAggregatedRisk_maxOfVerificationTasks() throws IllegalAccessException {
    Instant endTime = Instant.now();
    FieldUtils.writeField(heatMapService, "verificationTaskService", realVerificationTaskService, true);
    String verificationTaskId1 = realVerificationTaskService.createDeploymentVerificationTask(
        accountId, generateUuid(), verificationJobInstanceId, APP_DYNAMICS);
    heatMapService.updateRisk(verificationTaskId1, .5, endTime, HealthVerificationPeriod.PRE_ACTIVITY);
    heatMapService.updateRisk(
        verificationTaskId1, 0.7, endTime.plus(Duration.ofMinutes(15)), HealthVerificationPeriod.POST_ACTIVITY);
    String verificationTaskId2 = realVerificationTaskService.createDeploymentVerificationTask(
        accountId, generateUuid(), verificationJobInstanceId, APP_DYNAMICS);
    heatMapService.updateRisk(verificationTaskId2, 0.9, endTime, HealthVerificationPeriod.PRE_ACTIVITY);
    heatMapService.updateRisk(
        verificationTaskId2, 0.8, endTime.plus(Duration.ofMinutes(15)), HealthVerificationPeriod.POST_ACTIVITY);
    Set<CategoryRisk> preActivityRisks = heatMapService.getVerificationJobInstanceAggregatedRisk(
        accountId, verificationJobInstanceId, HealthVerificationPeriod.PRE_ACTIVITY);
    Set<CategoryRisk> postActivityRisks = heatMapService.getVerificationJobInstanceAggregatedRisk(
        accountId, verificationJobInstanceId, HealthVerificationPeriod.POST_ACTIVITY);

    assertThat(preActivityRisks).isNotNull();
    assertThat(postActivityRisks).isNotNull();

    assertThat(preActivityRisks.size()).isEqualTo(CVMonitoringCategory.values().length);
    assertThat(postActivityRisks.size()).isEqualTo(CVMonitoringCategory.values().length);

    for (CategoryRisk categoryRisk : preActivityRisks) {
      if (categoryRisk.getCategory().equals(CVMonitoringCategory.PERFORMANCE)) {
        assertThat(categoryRisk.getRisk()).isEqualTo(90.0);
      } else {
        assertThat(categoryRisk.getRisk()).isEqualTo(-1.0);
      }
    }

    for (CategoryRisk categoryRisk : postActivityRisks) {
      if (categoryRisk.getCategory().equals(CVMonitoringCategory.PERFORMANCE)) {
        assertThat(categoryRisk.getRisk()).isEqualTo(80.0);
      } else {
        assertThat(categoryRisk.getRisk()).isEqualTo(-1.0);
      }
    }
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetVerificationJobInstanceAggregatedRisk_zeroVerificationTaskIds() throws IllegalAccessException {
    FieldUtils.writeField(heatMapService, "verificationTaskService", realVerificationTaskService, true);
    Set<CategoryRisk> preActivityRisks = heatMapService.getVerificationJobInstanceAggregatedRisk(
        accountId, verificationJobInstanceId, HealthVerificationPeriod.PRE_ACTIVITY);
    Set<CategoryRisk> postActivityRisks = heatMapService.getVerificationJobInstanceAggregatedRisk(
        accountId, verificationJobInstanceId, HealthVerificationPeriod.POST_ACTIVITY);

    assertThat(preActivityRisks).isNotNull();
    assertThat(postActivityRisks).isNotNull();

    assertThat(preActivityRisks.size()).isEqualTo(CVMonitoringCategory.values().length);
    assertThat(postActivityRisks.size()).isEqualTo(CVMonitoringCategory.values().length);

    for (CategoryRisk categoryRisk : preActivityRisks) {
      assertThat(categoryRisk.getRisk()).isEqualTo(-1.0);
    }

    for (CategoryRisk categoryRisk : postActivityRisks) {
      assertThat(categoryRisk.getRisk()).isEqualTo(-1.0);
    }
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetVerificationRisk() {
    Instant endTime = Instant.now();
    heatMapService.updateRisk(verificationTaskId, 1.0, endTime, HealthVerificationPeriod.PRE_ACTIVITY);
    heatMapService.updateRisk(
        verificationTaskId, 0.0, endTime.plus(Duration.ofMinutes(15)), HealthVerificationPeriod.POST_ACTIVITY);

    Set<String> taskIds = new HashSet<>();
    taskIds.add(verificationTaskId);
    when(verificationTaskService.getVerificationTaskIds(eq(accountId), anyString())).thenReturn(taskIds);
    Optional<Risk> risk = heatMapService.getVerificationRisk(accountId, verificationJobInstanceId);

    assertThat(risk).isPresent();
    assertThat(risk.get()).isEqualTo(Risk.HEALTHY);
  }

  private void validateHeatMaps(HealthVerificationHeatMap heatMap, Double riskScore, Instant endTime,
      HealthVerificationPeriod healthVerificationPeriod, AggregationLevel aggregationLevel,
      CVMonitoringCategory category) {
    assertThat(heatMap.getRiskScore()).isEqualTo(riskScore);
    assertThat(heatMap.getCategory()).isEqualTo(category);
    assertThat(heatMap.getAggregationLevel()).isEqualTo(aggregationLevel);
    assertThat(heatMap.getActivityId()).isEqualTo(activityId);
    assertThat(heatMap.getServiceIdentifier()).isEqualTo(serviceIdentifier);
    assertThat(heatMap.getEnvIdentifier()).isEqualTo(envIdentifier);
    assertThat(heatMap.getHealthVerificationPeriod()).isEqualTo(healthVerificationPeriod);
    assertThat(heatMap.getEndTime()).isEqualTo(endTime);
  }

  private CVConfig getAppDCVConfig() {
    AppDynamicsCVConfig cvConfig = new AppDynamicsCVConfig();
    cvConfig.setProjectIdentifier(projectIdentifier);
    cvConfig.setEnvIdentifier(envIdentifier);
    cvConfig.setServiceIdentifier(serviceIdentifier);
    cvConfig.setAccountId(accountId);
    cvConfig.setUuid(cvConfigId);
    cvConfig.setCategory(CVMonitoringCategory.PERFORMANCE);
    return cvConfig;
  }

  private CVConfig getSplunkConfig() {
    SplunkCVConfig cvConfig = new SplunkCVConfig();
    cvConfig.setProjectIdentifier(projectIdentifier);
    cvConfig.setEnvIdentifier(envIdentifier);
    cvConfig.setServiceIdentifier(serviceIdentifier);
    cvConfig.setAccountId(accountId);
    cvConfig.setUuid(cvConfigId + "-2");
    cvConfig.setCategory(CVMonitoringCategory.PERFORMANCE);
    return cvConfig;
  }

  private Activity getActivity() {
    KubernetesActivity activity = KubernetesActivity.builder().build();
    activity.setVerificationJobInstanceIds(Arrays.asList(verificationJobInstanceId));
    activity.setUuid(activityId);
    activity.setActivityStartTime(Instant.ofEpochMilli(startTime));
    return activity;
  }
}
