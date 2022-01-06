/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.dashboard.services.impl;

import static io.harness.cvng.core.services.CVNextGenConstants.CV_ANALYSIS_WINDOW_MINUTES;
import static io.harness.cvng.core.utils.DateTimeUtils.roundDownTo5MinBoundary;
import static io.harness.cvng.core.utils.DateTimeUtils.roundDownToMinBoundary;
import static io.harness.cvng.dashboard.entities.HeatMap.HeatMapResolution.FIFTEEN_MINUTES;
import static io.harness.cvng.dashboard.entities.HeatMap.HeatMapResolution.FIVE_MIN;
import static io.harness.cvng.dashboard.entities.HeatMap.HeatMapResolution.THIRTY_MINUTES;
import static io.harness.persistence.HQuery.excludeAuthority;
import static io.harness.rule.OwnerRule.KAMAL;
import static io.harness.rule.OwnerRule.KANHAIYA;
import static io.harness.rule.OwnerRule.KAPIL;
import static io.harness.rule.OwnerRule.PRAVEEN;
import static io.harness.rule.OwnerRule.RAGHU;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.data.Offset.offset;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import io.harness.CvNextGenTestBase;
import io.harness.category.element.UnitTests;
import io.harness.cvng.BuilderFactory;
import io.harness.cvng.analysis.beans.Risk;
import io.harness.cvng.analysis.services.api.AnalysisService;
import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.client.NextGenService;
import io.harness.cvng.core.beans.monitoredService.DurationDTO;
import io.harness.cvng.core.beans.monitoredService.HistoricalTrend;
import io.harness.cvng.core.beans.monitoredService.RiskData;
import io.harness.cvng.core.beans.params.ProjectParams;
import io.harness.cvng.core.entities.AppDynamicsCVConfig;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.services.api.CVConfigService;
import io.harness.cvng.core.utils.ServiceEnvKey;
import io.harness.cvng.dashboard.beans.CategoryRisksDTO;
import io.harness.cvng.dashboard.beans.EnvToServicesDTO;
import io.harness.cvng.dashboard.beans.HeatMapDTO;
import io.harness.cvng.dashboard.beans.RiskSummaryPopoverDTO;
import io.harness.cvng.dashboard.entities.HeatMap;
import io.harness.cvng.dashboard.entities.HeatMap.HeatMapKeys;
import io.harness.cvng.dashboard.entities.HeatMap.HeatMapResolution;
import io.harness.cvng.dashboard.entities.HeatMap.HeatMapRisk;
import io.harness.cvng.dashboard.services.api.HeatMapService;
import io.harness.ng.core.environment.beans.EnvironmentType;
import io.harness.ng.core.environment.dto.EnvironmentResponseDTO;
import io.harness.ng.core.service.dto.ServiceResponseDTO;
import io.harness.persistence.HPersistence;
import io.harness.reflection.ReflectionUtils;
import io.harness.rule.Owner;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class HeatMapServiceImplTest extends CvNextGenTestBase {
  @Inject private HeatMapService heatMapService;

  private String projectIdentifier;
  private String serviceIdentifier;
  private String envIdentifier;
  private String accountId;
  private String orgIdentifier;
  private CVConfig cvConfig;
  @Inject private HPersistence hPersistence;
  @Mock private CVConfigService cvConfigService;
  @Mock private NextGenService nextGenService;
  @Mock private AnalysisService analysisService;
  private Clock clock;
  private BuilderFactory builderFactory;

  @Before
  public void setUp() throws Exception {
    builderFactory = BuilderFactory.getDefault();
    accountId = builderFactory.getContext().getAccountId();
    projectIdentifier = builderFactory.getContext().getProjectIdentifier();
    orgIdentifier = builderFactory.getContext().getOrgIdentifier();
    serviceIdentifier = builderFactory.getContext().getServiceIdentifier();
    envIdentifier = builderFactory.getContext().getEnvIdentifier();
    cvConfig = new AppDynamicsCVConfig();
    clock = Clock.fixed(Instant.parse("2020-04-22T10:02:06Z"), ZoneOffset.UTC);
    MockitoAnnotations.initMocks(this);
    FieldUtils.writeField(heatMapService, "cvConfigService", cvConfigService, true);
    FieldUtils.writeField(heatMapService, "clock", clock, true);
    FieldUtils.writeField(heatMapService, "nextGenService", nextGenService, true);
    FieldUtils.writeField(heatMapService, "analysisService", analysisService, true);
    when(cvConfigService.getAvailableCategories(anyString(), anyString(), anyString(), anyString(), anyString()))
        .thenReturn(new HashSet<>(Arrays.asList(CVMonitoringCategory.PERFORMANCE)));
    when(cvConfigService.isProductionConfig(cvConfig)).thenReturn(true);
    when(cvConfigService.getConfigsOfProductionEnvironments(
             anyString(), anyString(), anyString(), anyString(), anyString(), any()))
        .thenReturn(Arrays.asList(cvConfig));
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testUpsertAddsAllFields() {
    Instant instant = Instant.now();
    heatMapService.updateRiskScore(accountId, orgIdentifier, projectIdentifier, serviceIdentifier, envIdentifier,
        cvConfig, CVMonitoringCategory.PERFORMANCE, instant, 0.6, 10, 0);
    List<HeatMap> heatMaps = hPersistence.createQuery(HeatMap.class, excludeAuthority).asList();
    Set<String> nullableFields = Sets.newHashSet(HeatMapKeys.serviceIdentifier, HeatMapKeys.envIdentifier);
    heatMaps.forEach(heatMap -> {
      List<Field> fields = ReflectionUtils.getAllDeclaredAndInheritedFields(HeatMap.class);
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
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testUpsertAndUpdate() {
    Instant instant = Instant.now();
    heatMapService.updateRiskScore(accountId, orgIdentifier, projectIdentifier, serviceIdentifier, envIdentifier,
        cvConfig, CVMonitoringCategory.PERFORMANCE, instant, 0.6, 10, 9);
    verifyUpdates(instant, 0.6, 10, 9);

    // update and test
    heatMapService.updateRiskScore(accountId, orgIdentifier, projectIdentifier, serviceIdentifier, envIdentifier,
        cvConfig, CVMonitoringCategory.PERFORMANCE, instant, 0.7, 5, 8);
    verifyUpdates(instant, 0.7, 15, 17);

    // updating with lower risk score shouldn't change anything
    heatMapService.updateRiskScore(accountId, orgIdentifier, projectIdentifier, serviceIdentifier, envIdentifier,
        cvConfig, CVMonitoringCategory.PERFORMANCE, instant, 0.5, 5, 5);
    verifyUpdates(instant, 0.7, 20, 22);
  }

  private void verifyUpdates(Instant instant, double riskScore, long anomalousMetricsCount, long anomalousLogsCount) {
    verifyHeatMaps(instant, riskScore,
        hPersistence.createQuery(HeatMap.class, excludeAuthority)
            .filter(HeatMapKeys.projectIdentifier, projectIdentifier)
            .filter(HeatMapKeys.serviceIdentifier, serviceIdentifier)
            .filter(HeatMapKeys.envIdentifier, envIdentifier)
            .asList(),
        anomalousMetricsCount, anomalousLogsCount);
    verifyHeatMaps(instant, riskScore,
        hPersistence.createQuery(HeatMap.class, excludeAuthority)
            .filter(HeatMapKeys.projectIdentifier, projectIdentifier)
            .filter(HeatMapKeys.serviceIdentifier, null)
            .filter(HeatMapKeys.envIdentifier, envIdentifier)
            .asList(),
        anomalousMetricsCount, anomalousLogsCount);
    verifyHeatMaps(instant, riskScore,
        hPersistence.createQuery(HeatMap.class, excludeAuthority)
            .filter(HeatMapKeys.projectIdentifier, projectIdentifier)
            .filter(HeatMapKeys.serviceIdentifier, null)
            .filter(HeatMapKeys.envIdentifier, null)
            .asList(),
        anomalousMetricsCount, anomalousLogsCount);
  }

  private void verifyHeatMaps(
      Instant instant, double riskScore, List<HeatMap> heatMaps, long anomalousMetricsCount, long anomalousLogsCount) {
    assertThat(heatMaps.size()).isEqualTo(HeatMapResolution.values().length);
    for (int i = 0; i < HeatMapResolution.values().length; i++) {
      HeatMapResolution heatMapResolution = HeatMapResolution.values()[i];
      HeatMap heatMap = heatMaps.get(i);
      assertThat(heatMap.getAccountId()).isEqualTo(accountId);
      assertThat(heatMap.getProjectIdentifier()).isEqualTo(projectIdentifier);
      assertThat(heatMap.getCategory()).isEqualTo(CVMonitoringCategory.PERFORMANCE);
      assertThat(heatMap.getHeatMapResolution()).isEqualTo(heatMapResolution);
      assertThat(heatMap.getHeatMapBucketStartTime())
          .isEqualTo(Instant.ofEpochMilli(instant.toEpochMilli()
              - Math.floorMod(instant.toEpochMilli(), heatMapResolution.getBucketSize().toMillis())));
      assertThat(heatMap.getHeatMapBucketEndTime())
          .isEqualTo(heatMap.getHeatMapBucketStartTime().plusMillis(heatMapResolution.getBucketSize().toMillis()));
      Set<HeatMapRisk> heatMapRisks = heatMap.getHeatMapRisks();
      assertThat(heatMapRisks.size()).isEqualTo(1);
      HeatMapRisk heatMapRisk = heatMapRisks.iterator().next();
      assertThat(heatMapRisk.getStartTime())
          .isEqualTo(Instant.ofEpochMilli(instant.toEpochMilli()
              - Math.floorMod(instant.toEpochMilli(), heatMapResolution.getResolution().toMillis())));
      assertThat(heatMapRisk.getEndTime())
          .isEqualTo(heatMapRisk.getStartTime().plusMillis(heatMapResolution.getResolution().toMillis()));
      assertThat(heatMapRisk.getRiskScore()).isEqualTo(riskScore, offset(0.001));
      assertThat(heatMapRisk.getAnomalousMetricsCount()).isEqualTo(anomalousMetricsCount);
      assertThat(heatMapRisk.getAnomalousLogsCount()).isEqualTo(anomalousLogsCount);
    }
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testUpsert_whenMultipleBoundaries() {
    double numOfUnits = 1500;
    for (int minuteBoundry = 0; minuteBoundry < numOfUnits * CV_ANALYSIS_WINDOW_MINUTES;
         minuteBoundry += CV_ANALYSIS_WINDOW_MINUTES) {
      heatMapService.updateRiskScore(accountId, orgIdentifier, projectIdentifier, serviceIdentifier, envIdentifier,
          cvConfig, CVMonitoringCategory.PERFORMANCE, Instant.ofEpochMilli(TimeUnit.MINUTES.toMillis(minuteBoundry)),
          0.6, 0, 0);
    }
    for (int i = 0; i < HeatMapResolution.values().length; i++) {
      HeatMapResolution heatMapResolution = HeatMapResolution.values()[i];
      List<HeatMap> heatMaps = hPersistence.createQuery(HeatMap.class, excludeAuthority)
                                   .filter(HeatMapKeys.heatMapResolution, heatMapResolution)
                                   .filter(HeatMapKeys.orgIdentifier, orgIdentifier)
                                   .filter(HeatMapKeys.projectIdentifier, projectIdentifier)
                                   .filter(HeatMapKeys.serviceIdentifier, serviceIdentifier)
                                   .filter(HeatMapKeys.envIdentifier, envIdentifier)
                                   .asList();
      assertThat(heatMaps.size())
          .isEqualTo((int) Math.ceil(numOfUnits * TimeUnit.MINUTES.toMillis(CV_ANALYSIS_WINDOW_MINUTES)
              / heatMapResolution.getBucketSize().toMillis()));
      for (int j = 0; j < heatMaps.size(); j++) {
        HeatMap heatMap = heatMaps.get(j);
        assertThat(heatMap.getHeatMapResolution()).isEqualTo(heatMapResolution);
        assertThat(heatMap.getHeatMapBucketStartTime())
            .isEqualTo(Instant.ofEpochMilli(j * heatMapResolution.getBucketSize().toMillis()));
        SortedSet<HeatMapRisk> heatMapRisks = new TreeSet<>(heatMap.getHeatMapRisks());
        AtomicLong timeStamp = new AtomicLong(j * heatMapResolution.getBucketSize().toMillis());
        heatMapRisks.forEach(heatMapRisk -> {
          assertThat(heatMapRisk.getStartTime()).isEqualTo(Instant.ofEpochMilli(timeStamp.get()));
          timeStamp.addAndGet(heatMapResolution.getResolution().toMillis());
          assertThat(heatMapRisk.getRiskScore()).isEqualTo(0.6, offset(0.001));
        });
      }
    }

    // update a riskscore
    Instant updateInstant = Instant.ofEpochMilli(TimeUnit.MINUTES.toMillis(680));
    heatMapService.updateRiskScore(accountId, orgIdentifier, projectIdentifier, serviceIdentifier, envIdentifier,
        cvConfig, CVMonitoringCategory.PERFORMANCE, updateInstant, 0.7, 0, 0);

    for (int i = 0; i < HeatMapResolution.values().length; i++) {
      HeatMapResolution heatMapResolution = HeatMapResolution.values()[i];
      Instant bucketBoundary = Instant.ofEpochMilli(updateInstant.toEpochMilli()
          - Math.floorMod(updateInstant.toEpochMilli(), heatMapResolution.getBucketSize().toMillis()));
      List<HeatMap> heatMaps = hPersistence.createQuery(HeatMap.class, excludeAuthority)
                                   .filter(HeatMapKeys.heatMapResolution, heatMapResolution)
                                   .filter(HeatMapKeys.heatMapBucketStartTime, bucketBoundary)
                                   .filter(HeatMapKeys.projectIdentifier, projectIdentifier)
                                   .filter(HeatMapKeys.serviceIdentifier, serviceIdentifier)
                                   .filter(HeatMapKeys.envIdentifier, envIdentifier)
                                   .asList();
      assertThat(heatMaps.size()).isEqualTo(1);
      HeatMap heatMap = heatMaps.get(0);
      Instant heatMapTimeStamp = Instant.ofEpochMilli(updateInstant.toEpochMilli()
          - Math.floorMod(updateInstant.toEpochMilli(), heatMapResolution.getResolution().toMillis()));

      AtomicBoolean verified = new AtomicBoolean(false);
      heatMap.getHeatMapRisks().forEach(heatMapRisk -> {
        if (heatMapRisk.getStartTime().equals(heatMapTimeStamp)) {
          verified.set(true);
          assertThat(heatMapRisk.getRiskScore()).isEqualTo(0.7, offset(0.0001));
        } else {
          assertThat(heatMapRisk.getRiskScore()).isEqualTo(0.6, offset(0.0001));
        }
      });
      assertThat(verified.get()).isTrue();
    }
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testGetHeatMap_whenFiveMinuteResolution() {
    // no analysis
    int startMin = 5;
    int endMin = 200;
    Map<CVMonitoringCategory, SortedSet<HeatMapDTO>> heatMap = heatMapService.getHeatMap(accountId, orgIdentifier,
        projectIdentifier, serviceIdentifier, envIdentifier, Instant.ofEpochMilli(TimeUnit.MINUTES.toMillis(startMin)),
        Instant.ofEpochMilli(TimeUnit.MINUTES.toMillis(endMin)));

    assertThat(heatMap.size()).isEqualTo(1);
    assertThat(heatMap.get(CVMonitoringCategory.PERFORMANCE).size())
        .isEqualTo((endMin - startMin) / FIVE_MIN.getResolution().toMinutes() + 1);
    Iterator<HeatMapDTO> heatMapIterator = heatMap.get(CVMonitoringCategory.PERFORMANCE).iterator();
    for (long i = startMin; i <= endMin; i += FIVE_MIN.getResolution().toMinutes()) {
      HeatMapDTO heatMapDTO = heatMapIterator.next();
      assertThat(heatMapDTO)
          .isEqualTo(HeatMapDTO.builder()
                         .startTime(TimeUnit.MINUTES.toMillis(i))
                         .endTime(TimeUnit.MINUTES.toMillis(i) + FIVE_MIN.getResolution().toMillis())
                         .build());
    }
    int numOfRiskUnits = 24;
    int riskStartBoundary = 30;
    for (int minuteBoundry = riskStartBoundary;
         minuteBoundry < riskStartBoundary + numOfRiskUnits * CV_ANALYSIS_WINDOW_MINUTES;
         minuteBoundry += CV_ANALYSIS_WINDOW_MINUTES) {
      heatMapService.updateRiskScore(accountId, orgIdentifier, projectIdentifier, serviceIdentifier, envIdentifier,
          cvConfig, CVMonitoringCategory.PERFORMANCE, Instant.ofEpochMilli(TimeUnit.MINUTES.toMillis(minuteBoundry)),
          0.01 * minuteBoundry, 0, 0);
    }

    heatMap = heatMapService.getHeatMap(accountId, orgIdentifier, projectIdentifier, null, null,
        Instant.ofEpochMilli(TimeUnit.MINUTES.toMillis(startMin)),
        Instant.ofEpochMilli(TimeUnit.MINUTES.toMillis(endMin)));

    assertThat(heatMap.get(CVMonitoringCategory.PERFORMANCE).size())
        .isEqualTo((endMin - startMin) / FIVE_MIN.getResolution().toMinutes() + 1);
    heatMapIterator = heatMap.get(CVMonitoringCategory.PERFORMANCE).iterator();
    for (long i = startMin; i <= endMin; i += FIVE_MIN.getResolution().toMinutes()) {
      HeatMapDTO heatMapDTO = heatMapIterator.next();
      if (i < riskStartBoundary || i >= riskStartBoundary + numOfRiskUnits * CV_ANALYSIS_WINDOW_MINUTES) {
        assertThat(heatMapDTO)
            .isEqualTo(HeatMapDTO.builder()
                           .startTime(TimeUnit.MINUTES.toMillis(i))
                           .endTime(TimeUnit.MINUTES.toMillis(i) + FIVE_MIN.getResolution().toMillis())
                           .build());
      } else {
        assertThat(heatMapDTO)
            .isEqualTo(HeatMapDTO.builder()
                           .startTime(TimeUnit.MINUTES.toMillis(i))
                           .endTime(TimeUnit.MINUTES.toMillis(i) + FIVE_MIN.getResolution().toMillis())
                           .riskScore(i * 0.01)
                           .build());
      }
    }
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testGetHeatMap_whenFifteenMinuteResolution() {
    // no analysis
    int startMin = 75;
    int endMin = 350;
    Map<CVMonitoringCategory, SortedSet<HeatMapDTO>> heatMap = heatMapService.getHeatMap(accountId, orgIdentifier,
        projectIdentifier, serviceIdentifier, envIdentifier, Instant.ofEpochMilli(TimeUnit.MINUTES.toMillis(startMin)),
        Instant.ofEpochMilli(TimeUnit.MINUTES.toMillis(endMin)));

    assertThat(heatMap.size()).isEqualTo(1);
    assertThat(heatMap.get(CVMonitoringCategory.PERFORMANCE).size())
        .isEqualTo((endMin - startMin) / FIFTEEN_MINUTES.getResolution().toMinutes() + 1);
    Iterator<HeatMapDTO> heatMapIterator = heatMap.get(CVMonitoringCategory.PERFORMANCE).iterator();
    for (long i = startMin; i <= endMin; i += FIFTEEN_MINUTES.getResolution().toMinutes()) {
      HeatMapDTO heatMapDTO = heatMapIterator.next();
      assertThat(heatMapDTO)
          .isEqualTo(HeatMapDTO.builder()
                         .startTime(TimeUnit.MINUTES.toMillis(i))
                         .endTime(TimeUnit.MINUTES.toMillis(i) + FIFTEEN_MINUTES.getResolution().toMillis())
                         .build());
    }
    int numOfRiskUnits = 42;
    int riskStartBoundary = 70;
    for (int minuteBoundry = riskStartBoundary;
         minuteBoundry <= riskStartBoundary + numOfRiskUnits * CV_ANALYSIS_WINDOW_MINUTES;
         minuteBoundry += CV_ANALYSIS_WINDOW_MINUTES) {
      heatMapService.updateRiskScore(accountId, orgIdentifier, projectIdentifier, serviceIdentifier, envIdentifier,
          cvConfig, CVMonitoringCategory.PERFORMANCE, Instant.ofEpochMilli(TimeUnit.MINUTES.toMillis(minuteBoundry)),
          0.01 * minuteBoundry, 0, 0);
    }

    heatMap = heatMapService.getHeatMap(accountId, orgIdentifier, projectIdentifier, null, null,
        Instant.ofEpochMilli(TimeUnit.MINUTES.toMillis(startMin)),
        Instant.ofEpochMilli(TimeUnit.MINUTES.toMillis(endMin)));

    assertThat(heatMap.get(CVMonitoringCategory.PERFORMANCE).size())
        .isEqualTo((endMin - startMin) / FIFTEEN_MINUTES.getResolution().toMinutes() + 1);
    heatMapIterator = heatMap.get(CVMonitoringCategory.PERFORMANCE).iterator();
    for (long i = startMin; i <= endMin; i += FIFTEEN_MINUTES.getResolution().toMinutes()) {
      HeatMapDTO heatMapDTO = heatMapIterator.next();
      if (i < riskStartBoundary
          || i >= riskStartBoundary + (numOfRiskUnits / 3) * FIFTEEN_MINUTES.getResolution().toMinutes()) {
        assertThat(heatMapDTO)
            .isEqualTo(HeatMapDTO.builder()
                           .startTime(TimeUnit.MINUTES.toMillis(i))
                           .endTime(TimeUnit.MINUTES.toMillis(i) + FIFTEEN_MINUTES.getResolution().toMillis())
                           .build());
      } else {
        assertThat(heatMapDTO)
            .isEqualTo(HeatMapDTO.builder()
                           .startTime(TimeUnit.MINUTES.toMillis(i))
                           .endTime(TimeUnit.MINUTES.toMillis(i) + FIFTEEN_MINUTES.getResolution().toMillis())
                           .riskScore((i + 10) * 0.01)
                           .build());
      }
    }
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testGetHeatMap_projectRollup() {
    // no analysis
    int numOfService = 3;
    int numOfEnv = 4;
    Instant instant = Instant.now();
    for (int i = 1; i <= numOfService; i++) {
      for (int j = 1; j <= numOfEnv; j++) {
        heatMapService.updateRiskScore(accountId, orgIdentifier, projectIdentifier, "service" + i, "env" + j, cvConfig,
            CVMonitoringCategory.PERFORMANCE, instant, i * j, 0, 0);
      }
    }

    List<HeatMap> heatMaps = hPersistence.createQuery(HeatMap.class, excludeAuthority)
                                 .filter(HeatMapKeys.projectIdentifier, projectIdentifier)
                                 .filter(HeatMapKeys.serviceIdentifier, null)
                                 .filter(HeatMapKeys.envIdentifier, null)
                                 .asList();
    assertThat(heatMaps.size()).isEqualTo(HeatMapResolution.values().length);
    heatMaps.forEach(heatMap -> {
      assertThat(heatMap.getHeatMapRisks().size()).isEqualTo(1);
      heatMap.getHeatMapRisks().forEach(
          heatMapRisk -> assertThat(heatMapRisk.getRiskScore()).isEqualTo(numOfEnv * numOfService, offset(0.00001)));
    });
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetHeatMapNonProdEnv_noProjectRollup() {
    when(cvConfigService.isProductionConfig(cvConfig)).thenReturn(false);
    // no analysis
    int numOfService = 3;
    int numOfEnv = 4;
    Instant instant = Instant.now();
    for (int i = 1; i <= numOfService; i++) {
      for (int j = 1; j <= numOfEnv; j++) {
        heatMapService.updateRiskScore(accountId, orgIdentifier, projectIdentifier, "service" + i, "env" + j, cvConfig,
            CVMonitoringCategory.PERFORMANCE, instant, i * j, 0, 0);
      }
    }

    List<HeatMap> heatMaps = hPersistence.createQuery(HeatMap.class, excludeAuthority)
                                 .filter(HeatMapKeys.projectIdentifier, projectIdentifier)
                                 .filter(HeatMapKeys.serviceIdentifier, null)
                                 .filter(HeatMapKeys.envIdentifier, null)
                                 .asList();
    assertThat(heatMaps.size()).isEqualTo(0);
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testGetHeatMap_ForSavedCategories() {
    Set<CVMonitoringCategory> categories = new HashSet<>();
    for (CVMonitoringCategory cvMonitoringCategory : CVMonitoringCategory.values()) {
      categories.add(cvMonitoringCategory);
      when(cvConfigService.getAvailableCategories(
               accountId, orgIdentifier, projectIdentifier, envIdentifier, serviceIdentifier))
          .thenReturn(categories);
      Map<CVMonitoringCategory, SortedSet<HeatMapDTO>> heatMap = heatMapService.getHeatMap(accountId, orgIdentifier,
          projectIdentifier, serviceIdentifier, envIdentifier, Instant.now().minus(1, ChronoUnit.HOURS), Instant.now());
      assertThat(heatMap.size()).isEqualTo(categories.size());
      categories.forEach(category -> assertThat(heatMap).containsKey(category));
    }
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetCategoryRiskScores_singleServiceEnvironment() {
    when(cvConfigService.getAvailableCategories(
             accountId, orgIdentifier, projectIdentifier, envIdentifier, serviceIdentifier))
        .thenReturn(new HashSet<>(Arrays.asList(CVMonitoringCategory.PERFORMANCE, CVMonitoringCategory.ERRORS)));
    int numOfRiskUnits = 24;
    long riskStartBoundary = TimeUnit.MILLISECONDS.toMinutes(clock.instant().toEpochMilli());
    for (long minuteBoundry = riskStartBoundary;
         minuteBoundry < riskStartBoundary + numOfRiskUnits * CV_ANALYSIS_WINDOW_MINUTES;
         minuteBoundry += CV_ANALYSIS_WINDOW_MINUTES) {
      heatMapService.updateRiskScore(accountId, orgIdentifier, projectIdentifier, serviceIdentifier, envIdentifier,
          cvConfig, CVMonitoringCategory.PERFORMANCE, Instant.ofEpochMilli(TimeUnit.MINUTES.toMillis(minuteBoundry)),
          0.01 * minuteBoundry, 0, 0);
      heatMapService.updateRiskScore(accountId, orgIdentifier, projectIdentifier, serviceIdentifier + "2",
          envIdentifier, cvConfig, CVMonitoringCategory.ERRORS,
          Instant.ofEpochMilli(TimeUnit.MINUTES.toMillis(minuteBoundry)), 0.01 * minuteBoundry, 0, 0);
    }

    CategoryRisksDTO categoryRisk = heatMapService.getCategoryRiskScores(
        accountId, orgIdentifier, projectIdentifier, serviceIdentifier, envIdentifier);

    assertThat(categoryRisk).isNotNull();
    assertThat(categoryRisk.getCategoryRisks().size()).isEqualTo(CVMonitoringCategory.values().length);
    Map<CVMonitoringCategory, Integer> categoryRiskMap = new HashMap<>();
    categoryRisk.getCategoryRisks().forEach(
        categoryRisks -> { categoryRiskMap.put(categoryRisks.getCategory(), categoryRisks.getRisk()); });
    assertThat(categoryRiskMap.containsKey(CVMonitoringCategory.PERFORMANCE)).isTrue();
    assertThat(categoryRiskMap.get(CVMonitoringCategory.PERFORMANCE)).isNotEqualTo(-1);
    assertThat(categoryRiskMap.get(CVMonitoringCategory.ERRORS)).isEqualTo(-1);
    assertThat(categoryRiskMap.get(CVMonitoringCategory.INFRASTRUCTURE)).isEqualTo(-1);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetCategoryRiskScores_singleEnvironment() {
    Map<String, Set<String>> envServiceMap = new HashMap<>();
    Set<String> services = new HashSet<>(Arrays.asList(serviceIdentifier, serviceIdentifier + "2"));
    envServiceMap.put(envIdentifier, services);

    when(cvConfigService.getEnvToServicesMap(accountId, orgIdentifier, projectIdentifier)).thenReturn(envServiceMap);
    when(cvConfigService.getAvailableCategories(anyString(), anyString(), anyString(), anyString(), anyString()))
        .thenReturn(new HashSet<>(Arrays.asList(CVMonitoringCategory.PERFORMANCE, CVMonitoringCategory.ERRORS)));
    int numOfRiskUnits = 24;
    long riskStartBoundary = TimeUnit.MILLISECONDS.toMinutes(clock.instant().toEpochMilli());
    for (long minuteBoundry = riskStartBoundary;
         minuteBoundry < riskStartBoundary + numOfRiskUnits * CV_ANALYSIS_WINDOW_MINUTES;
         minuteBoundry += CV_ANALYSIS_WINDOW_MINUTES) {
      heatMapService.updateRiskScore(accountId, orgIdentifier, projectIdentifier, serviceIdentifier, envIdentifier,
          cvConfig, CVMonitoringCategory.PERFORMANCE, Instant.ofEpochMilli(TimeUnit.MINUTES.toMillis(minuteBoundry)),
          0.01 * minuteBoundry, 0, 0);
      heatMapService.updateRiskScore(accountId, orgIdentifier, projectIdentifier, serviceIdentifier + "2",
          envIdentifier, cvConfig, CVMonitoringCategory.ERRORS,
          Instant.ofEpochMilli(TimeUnit.MINUTES.toMillis(minuteBoundry)), 0.01 * minuteBoundry, 0, 0);
    }

    CategoryRisksDTO categoryRisk =
        heatMapService.getCategoryRiskScores(accountId, orgIdentifier, projectIdentifier, null, envIdentifier);

    assertThat(categoryRisk).isNotNull();

    assertThat(categoryRisk.getCategoryRisks().size()).isEqualTo(CVMonitoringCategory.values().length);
    assertThat(categoryRisk).isNotNull();
    assertThat(categoryRisk.getCategoryRisks().size()).isEqualTo(CVMonitoringCategory.values().length);
    Map<CVMonitoringCategory, Integer> categoryRiskMap = new HashMap<>();
    categoryRisk.getCategoryRisks().forEach(
        categoryRisks -> categoryRiskMap.put(categoryRisks.getCategory(), categoryRisks.getRisk()));
    assertThat(categoryRiskMap.containsKey(CVMonitoringCategory.PERFORMANCE)).isTrue();
    assertThat(categoryRiskMap.get(CVMonitoringCategory.PERFORMANCE)).isNotEqualTo(-1);
    assertThat(categoryRiskMap.get(CVMonitoringCategory.ERRORS)).isNotEqualTo(-1);
    assertThat(categoryRiskMap.get(CVMonitoringCategory.INFRASTRUCTURE)).isEqualTo(-1);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetCategoryRiskScores_multipleServices() {
    Map<String, Set<String>> envServiceMap = new HashMap<>();
    Set<String> services = new HashSet<>(Arrays.asList(serviceIdentifier, serviceIdentifier + "2"));
    envServiceMap.put(envIdentifier, services);

    when(cvConfigService.getEnvToServicesMap(accountId, orgIdentifier, projectIdentifier)).thenReturn(envServiceMap);
    when(cvConfigService.getAvailableCategories(anyString(), anyString(), anyString(), anyString(), anyString()))
        .thenReturn(new HashSet<>(Arrays.asList(CVMonitoringCategory.PERFORMANCE, CVMonitoringCategory.ERRORS)));
    int numOfRiskUnits = 24;
    long riskStartBoundary = TimeUnit.MILLISECONDS.toMinutes(clock.instant().toEpochMilli());
    for (long minuteBoundry = riskStartBoundary;
         minuteBoundry < riskStartBoundary + numOfRiskUnits * CV_ANALYSIS_WINDOW_MINUTES;
         minuteBoundry += CV_ANALYSIS_WINDOW_MINUTES) {
      heatMapService.updateRiskScore(accountId, orgIdentifier, projectIdentifier, serviceIdentifier, envIdentifier,
          cvConfig, CVMonitoringCategory.PERFORMANCE, Instant.ofEpochMilli(TimeUnit.MINUTES.toMillis(minuteBoundry)),
          0.01 * minuteBoundry, 0, 0);
      heatMapService.updateRiskScore(accountId, orgIdentifier, projectIdentifier, serviceIdentifier + "2",
          envIdentifier, cvConfig, CVMonitoringCategory.ERRORS,
          Instant.ofEpochMilli(TimeUnit.MINUTES.toMillis(minuteBoundry)), 0.01 * minuteBoundry, 0, 0);
    }

    CategoryRisksDTO categoryRisk =
        heatMapService.getCategoryRiskScores(accountId, orgIdentifier, projectIdentifier, null, null);

    assertThat(categoryRisk).isNotNull();

    assertThat(categoryRisk.getCategoryRisks().size()).isEqualTo(CVMonitoringCategory.values().length);
    assertThat(categoryRisk).isNotNull();
    assertThat(categoryRisk.getCategoryRisks().size()).isEqualTo(CVMonitoringCategory.values().length);
    Map<CVMonitoringCategory, Integer> categoryRiskMap = new HashMap<>();
    categoryRisk.getCategoryRisks().forEach(
        categoryRisks -> { categoryRiskMap.put(categoryRisks.getCategory(), categoryRisks.getRisk()); });
    assertThat(categoryRiskMap.containsKey(CVMonitoringCategory.PERFORMANCE)).isTrue();
    assertThat(categoryRiskMap.containsKey(CVMonitoringCategory.ERRORS)).isTrue();

    assertThat(categoryRiskMap.get(CVMonitoringCategory.PERFORMANCE)).isNotEqualTo(-1);
    assertThat(categoryRiskMap.get(CVMonitoringCategory.ERRORS)).isNotEqualTo(-1);
    assertThat(categoryRiskMap.get(CVMonitoringCategory.INFRASTRUCTURE)).isEqualTo(-1);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetCategoryRiskScores_noSetup() {
    when(cvConfigService.getConfigsOfProductionEnvironments(
             anyString(), anyString(), anyString(), anyString(), anyString(), any()))
        .thenReturn(null);
    CategoryRisksDTO categoryRisk =
        heatMapService.getCategoryRiskScores(accountId, orgIdentifier, projectIdentifier, null, null);
    assertThat(categoryRisk).isNotNull();
    assertThat(categoryRisk.isHasConfigsSetup()).isFalse();
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetCategoryRiskScores_olderThan15mins() throws Exception {
    Map<String, Set<String>> envServiceMap = new HashMap<>();
    Set<String> services = new HashSet<>(Arrays.asList(serviceIdentifier, serviceIdentifier + "2"));
    envServiceMap.put(envIdentifier, services);

    when(cvConfigService.getEnvToServicesMap(accountId, orgIdentifier, projectIdentifier)).thenReturn(envServiceMap);
    when(cvConfigService.getAvailableCategories(
             accountId, orgIdentifier, projectIdentifier, envIdentifier, serviceIdentifier))
        .thenReturn(new HashSet<>(Arrays.asList(CVMonitoringCategory.PERFORMANCE, CVMonitoringCategory.ERRORS)));
    int numOfRiskUnits = 24;
    long riskStartBoundary = TimeUnit.MILLISECONDS.toMinutes(clock.instant().toEpochMilli());
    for (long minuteBoundry = riskStartBoundary;
         minuteBoundry < riskStartBoundary + numOfRiskUnits * CV_ANALYSIS_WINDOW_MINUTES;
         minuteBoundry += CV_ANALYSIS_WINDOW_MINUTES) {
      heatMapService.updateRiskScore(accountId, orgIdentifier, projectIdentifier, serviceIdentifier, envIdentifier,
          cvConfig, CVMonitoringCategory.PERFORMANCE, Instant.ofEpochMilli(TimeUnit.MINUTES.toMillis(minuteBoundry)),
          0.01 * minuteBoundry, 0, 0);
      heatMapService.updateRiskScore(accountId, orgIdentifier, projectIdentifier, serviceIdentifier + "2",
          envIdentifier, cvConfig, CVMonitoringCategory.ERRORS,
          Instant.ofEpochMilli(TimeUnit.MINUTES.toMillis(minuteBoundry)), 0.01 * minuteBoundry, 0, 0);
    }
    // make the clock reflect a much newer time
    clock = Clock.fixed(Instant.parse("2020-04-23T10:02:06Z"), ZoneOffset.UTC);
    FieldUtils.writeField(heatMapService, "clock", clock, true);

    CategoryRisksDTO categoryRisk =
        heatMapService.getCategoryRiskScores(accountId, orgIdentifier, projectIdentifier, null, null);

    assertThat(categoryRisk).isNotNull();

    assertThat(categoryRisk.getCategoryRisks().size()).isEqualTo(CVMonitoringCategory.values().length);
    assertThat(categoryRisk).isNotNull();
    assertThat(categoryRisk.getCategoryRisks().size()).isEqualTo(CVMonitoringCategory.values().length);
    Map<CVMonitoringCategory, Integer> categoryRiskMap = new HashMap<>();
    categoryRisk.getCategoryRisks().forEach(
        categoryRisks -> { categoryRiskMap.put(categoryRisks.getCategory(), categoryRisks.getRisk()); });

    assertThat(categoryRiskMap.get(CVMonitoringCategory.PERFORMANCE)).isEqualTo(-1);
    assertThat(categoryRiskMap.get(CVMonitoringCategory.ERRORS)).isEqualTo(-1);
    assertThat(categoryRiskMap.get(CVMonitoringCategory.INFRASTRUCTURE)).isEqualTo(-1);
  }
  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetRiskSummaryPopover_nullCategory() {
    EnvToServicesDTO envToServicesDTO =
        EnvToServicesDTO.builder()
            .environment(EnvironmentResponseDTO.builder()
                             .identifier(envIdentifier)
                             .type(EnvironmentType.Production)
                             .name("env")
                             .build())
            .service(ServiceResponseDTO.builder().name("Service Name").identifier(serviceIdentifier).build())
            .build();
    RiskSummaryPopoverDTO.AnalysisRisk analysisRisk =
        RiskSummaryPopoverDTO.AnalysisRisk.builder().name("exception").risk(30).build();
    when(analysisService.getTop3AnalysisRisks(any(), any(), any(), any(), any(), any()))
        .thenReturn(Collections.singletonList(analysisRisk));
    when(nextGenService.getEnvironment(anyString(), anyString(), anyString(), anyString()))
        .thenReturn(envToServicesDTO.getEnvironment());
    when(nextGenService.getService(anyString(), anyString(), anyString(), anyString()))
        .thenReturn(envToServicesDTO.getServices().iterator().next());
    when(cvConfigService.getEnvToServicesList(accountId, orgIdentifier, projectIdentifier))
        .thenReturn(Lists.newArrayList(envToServicesDTO));
    when(cvConfigService.getAvailableCategories(
             accountId, orgIdentifier, projectIdentifier, envIdentifier, serviceIdentifier))
        .thenReturn(new HashSet<>(Arrays.asList(CVMonitoringCategory.PERFORMANCE, CVMonitoringCategory.ERRORS)));
    heatMapService.updateRiskScore(accountId, orgIdentifier, projectIdentifier, serviceIdentifier, envIdentifier,
        cvConfig, CVMonitoringCategory.PERFORMANCE, clock.instant(), 0.70, 0, 0);
    heatMapService.updateRiskScore(accountId, orgIdentifier, projectIdentifier, serviceIdentifier + "2", envIdentifier,
        cvConfig, CVMonitoringCategory.ERRORS, clock.instant(), 0.60, 0, 0);

    RiskSummaryPopoverDTO riskSummaryPopoverDTO =
        heatMapService.getRiskSummaryPopover(accountId, orgIdentifier, projectIdentifier, clock.instant(), null, null);
    assertThat(riskSummaryPopoverDTO.getCategory()).isNull();
    assertThat(riskSummaryPopoverDTO.getEnvSummaries()).hasSize(1);
    RiskSummaryPopoverDTO.EnvSummary envSummary = riskSummaryPopoverDTO.getEnvSummaries().get(0);
    assertThat(envSummary.getEnvIdentifier()).isEqualTo(envIdentifier);
    assertThat(envSummary.getEnvName()).isEqualTo("env");
    assertThat(envSummary.getRiskScore()).isEqualTo(70);
    assertThat(envSummary.getServiceSummaries()).hasSize(1);
    RiskSummaryPopoverDTO.ServiceSummary serviceSummary = envSummary.getServiceSummaries().get(0);
    assertThat(serviceSummary.getServiceIdentifier()).isEqualTo(serviceIdentifier);
    assertThat(serviceSummary.getServiceName()).isEqualTo("Service Name");
    assertThat(serviceSummary.getRisk()).isEqualTo(70);
    assertThat(serviceSummary.getAnalysisRisks()).isEqualTo(Collections.singletonList(analysisRisk));
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetRiskSummaryPopover_validCategory() {
    EnvToServicesDTO envToServicesDTO =
        EnvToServicesDTO.builder()
            .environment(EnvironmentResponseDTO.builder()
                             .identifier(envIdentifier)
                             .type(EnvironmentType.Production)
                             .name("env")
                             .build())
            .service(ServiceResponseDTO.builder().name("Service Name").identifier(serviceIdentifier).build())
            .build();
    RiskSummaryPopoverDTO.AnalysisRisk analysisRisk =
        RiskSummaryPopoverDTO.AnalysisRisk.builder().name("exception").risk(30).build();
    when(analysisService.getTop3AnalysisRisks(any(), any(), any(), any(), any(), any()))
        .thenReturn(Collections.singletonList(analysisRisk));
    when(nextGenService.getEnvironment(anyString(), anyString(), anyString(), anyString()))
        .thenReturn(envToServicesDTO.getEnvironment());
    when(nextGenService.getService(anyString(), anyString(), anyString(), anyString()))
        .thenReturn(envToServicesDTO.getServices().iterator().next());
    when(cvConfigService.getEnvToServicesList(accountId, orgIdentifier, projectIdentifier))
        .thenReturn(Lists.newArrayList(envToServicesDTO));
    when(cvConfigService.getAvailableCategories(
             accountId, orgIdentifier, projectIdentifier, envIdentifier, serviceIdentifier))
        .thenReturn(new HashSet<>(Arrays.asList(CVMonitoringCategory.PERFORMANCE, CVMonitoringCategory.ERRORS)));
    heatMapService.updateRiskScore(accountId, orgIdentifier, projectIdentifier, serviceIdentifier, envIdentifier,
        cvConfig, CVMonitoringCategory.PERFORMANCE, clock.instant(), 0.70, 0, 0);
    heatMapService.updateRiskScore(accountId, orgIdentifier, projectIdentifier, serviceIdentifier, envIdentifier,
        cvConfig, CVMonitoringCategory.ERRORS, clock.instant(), 0.60, 0, 0);

    RiskSummaryPopoverDTO riskSummaryPopoverDTO = heatMapService.getRiskSummaryPopover(
        accountId, orgIdentifier, projectIdentifier, clock.instant(), null, CVMonitoringCategory.ERRORS);
    assertThat(riskSummaryPopoverDTO.getCategory()).isEqualTo(CVMonitoringCategory.ERRORS);
    assertThat(riskSummaryPopoverDTO.getEnvSummaries()).hasSize(1);
    RiskSummaryPopoverDTO.EnvSummary envSummary = riskSummaryPopoverDTO.getEnvSummaries().get(0);
    assertThat(envSummary.getEnvIdentifier()).isEqualTo(envIdentifier);
    assertThat(envSummary.getEnvName()).isEqualTo("env");
    assertThat(envSummary.getRiskScore()).isEqualTo(60);
    assertThat(envSummary.getServiceSummaries()).hasSize(1);
    RiskSummaryPopoverDTO.ServiceSummary serviceSummary = envSummary.getServiceSummaries().get(0);
    assertThat(serviceSummary.getServiceIdentifier()).isEqualTo(serviceIdentifier);
    assertThat(serviceSummary.getServiceName()).isEqualTo("Service Name");
    assertThat(serviceSummary.getRisk()).isEqualTo(60);
    assertThat(serviceSummary.getAnalysisRisks()).isEqualTo(Collections.singletonList(analysisRisk));
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testGetHistoricalData_PreConditionServiceEnvironmentListLimit() {
    List<Pair<String, String>> serviceEnvironmentIds = Collections.nCopies(11, Pair.of("serviceId", "envId"));
    assertThatThrownBy(
        () -> heatMapService.getHistoricalTrend(accountId, orgIdentifier, projectIdentifier, serviceEnvironmentIds, 24))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(
            String.format("Based on page size, the health score calculation should be done for less than 10 services"));
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testGetHistoricalData_EmptyServiceEnvironmentList() {
    List<HistoricalTrend> historicalTrendList =
        heatMapService.getHistoricalTrend(accountId, orgIdentifier, projectIdentifier, Collections.emptyList(), 24);
    assertThat(historicalTrendList.size()).isEqualTo(0);
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testGetHistoricalData_OneServiceEnvironmentOneCategoryForOneHour() {
    HeatMap heatMap = builderFactory.heatMapBuilder().build();
    heatMap.getHeatMapRisks().forEach(heatMapRisk -> heatMapRisk.setRiskScore(0.10));
    hPersistence.save(heatMap);
    HeatMap heatMapPrevious = builderFactory.heatMapBuilder().build();
    setStartTimeEndTimeAndRiskScoreWith30MinBucket(heatMapPrevious, heatMap.getHeatMapBucketStartTime(), 0.50);
    hPersistence.save(heatMapPrevious);

    List<HistoricalTrend> historicalTrendList = heatMapService.getHistoricalTrend(
        accountId, orgIdentifier, projectIdentifier, Arrays.asList(Pair.of(serviceIdentifier, envIdentifier)), 1);
    assertThat(historicalTrendList.size()).isEqualTo(1);
    assertThat(historicalTrendList.get(0).getHealthScores().size()).isEqualTo(2);
    historicalTrendList.get(0).getHealthScores().forEach(score -> {
      assertThat(score.getHealthScore()).isEqualTo(90);
      assertThat(score.getRiskStatus()).isEqualTo(Risk.HEALTHY);
    });
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testGetHistoricalData_OneServiceEnvironmentMultipleCategoryForOneHour() {
    HeatMap heatMapErrorCategory = builderFactory.heatMapBuilder().build();
    heatMapErrorCategory.getHeatMapRisks().forEach(heatMapRisk -> heatMapRisk.setRiskScore(0.10));
    hPersistence.save(heatMapErrorCategory);
    HeatMap heatMapPerformanceCategory =
        builderFactory.heatMapBuilder().category(CVMonitoringCategory.PERFORMANCE).build();
    Instant analysisTime = roundDownToMinBoundary(clock.instant(), 30);
    heatMapPerformanceCategory.getHeatMapRisks().forEach(heatMapRisk -> {
      if (ChronoUnit.MINUTES.between(heatMapRisk.getEndTime(), analysisTime) < 30) {
        heatMapRisk.setRiskScore(.50);
      }
    });
    hPersistence.save(heatMapPerformanceCategory);

    List<HistoricalTrend> historicalTrendList = heatMapService.getHistoricalTrend(
        accountId, orgIdentifier, projectIdentifier, Arrays.asList(Pair.of(serviceIdentifier, envIdentifier)), 1);

    assertThat(historicalTrendList.size()).isEqualTo(1);
    assertThat(historicalTrendList.get(0).getHealthScores().size()).isEqualTo(2);

    assertThat(historicalTrendList.get(0).getHealthScores().get(0).getHealthScore()).isEqualTo(90);
    assertThat(historicalTrendList.get(0).getHealthScores().get(0).getRiskStatus()).isEqualTo(Risk.HEALTHY);

    assertThat(historicalTrendList.get(0).getHealthScores().get(1).getHealthScore()).isEqualTo(50);
    assertThat(historicalTrendList.get(0).getHealthScores().get(1).getRiskStatus()).isEqualTo(Risk.OBSERVE);
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testGetHistoricalData_OneServiceEnvironmentOneCategoryFor24Hour() {
    Instant analysisTime = clock.instant();
    analysisTime = roundDownToMinBoundary(analysisTime, 30);
    HeatMap heatMap = builderFactory.heatMapBuilder().build();
    setStartTimeEndTimeAndRiskScoreWith30MinBucket(heatMap, analysisTime.plus(12, ChronoUnit.HOURS), 0.10);
    hPersistence.save(heatMap);

    HeatMap heatMapPrevious = builderFactory.heatMapBuilder().build();
    setStartTimeEndTimeAndRiskScoreWith30MinBucket(heatMapPrevious, heatMap.getHeatMapBucketStartTime(), 0.50);
    hPersistence.save(heatMapPrevious);

    List<HistoricalTrend> historicalTrendList = heatMapService.getHistoricalTrend(
        accountId, orgIdentifier, projectIdentifier, Arrays.asList(Pair.of(serviceIdentifier, envIdentifier)), 24);

    assertThat(historicalTrendList.size()).isEqualTo(1);
    assertThat(historicalTrendList.get(0).getHealthScores().size()).isEqualTo(48);

    for (int i = 0; i < 24; i++) {
      assertThat(historicalTrendList.get(0).getHealthScores().get(i).getHealthScore()).isEqualTo(50);
      assertThat(historicalTrendList.get(0).getHealthScores().get(i).getRiskStatus()).isEqualTo(Risk.OBSERVE);
    }
    for (int i = 24; i < 48; i++) {
      assertThat(historicalTrendList.get(0).getHealthScores().get(i).getHealthScore()).isEqualTo(90);
      assertThat(historicalTrendList.get(0).getHealthScores().get(i).getRiskStatus()).isEqualTo(Risk.HEALTHY);
    }
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testGetHistoricalData_MultipleServiceEnvironmentOneCategoryForOneHour() {
    String secondService = "secondService";
    String secondEnvironment = "secondEnvironment";
    HeatMap heatMap = builderFactory.heatMapBuilder().build();
    heatMap.getHeatMapRisks().forEach(heatMapRisk -> heatMapRisk.setRiskScore(0.10));
    hPersistence.save(heatMap);

    HeatMap heatMapPrevious = builderFactory.heatMapBuilder().build();
    setStartTimeEndTimeAndRiskScoreWith30MinBucket(heatMapPrevious, heatMap.getHeatMapBucketStartTime(), 0.50);
    hPersistence.save(heatMapPrevious);

    HeatMap anotherServiceHeatMap =
        builderFactory.heatMapBuilder().serviceIdentifier(secondService).envIdentifier(secondEnvironment).build();
    anotherServiceHeatMap.getHeatMapRisks().forEach(heatMapRisk -> heatMapRisk.setRiskScore(0.55));
    hPersistence.save(anotherServiceHeatMap);

    List<Pair<String, String>> serviceEnvIds =
        Arrays.asList(Pair.of(serviceIdentifier, envIdentifier), Pair.of(secondService, secondEnvironment));

    List<HistoricalTrend> historicalTrendList =
        heatMapService.getHistoricalTrend(accountId, orgIdentifier, projectIdentifier, serviceEnvIds, 1);
    assertThat(historicalTrendList.size()).isEqualTo(2);

    assertThat(historicalTrendList.get(0).getHealthScores().size()).isEqualTo(2);
    assertThat(historicalTrendList.get(1).getHealthScores().size()).isEqualTo(2);

    assertThat(historicalTrendList.get(0).getHealthScores().get(0).getHealthScore()).isEqualTo(90);
    assertThat(historicalTrendList.get(0).getHealthScores().get(0).getRiskStatus()).isEqualTo(Risk.HEALTHY);
    assertThat(historicalTrendList.get(0).getHealthScores().get(1).getHealthScore()).isEqualTo(90);
    assertThat(historicalTrendList.get(0).getHealthScores().get(1).getRiskStatus()).isEqualTo(Risk.HEALTHY);

    assertThat(historicalTrendList.get(1).getHealthScores().get(0).getHealthScore()).isEqualTo(45);
    assertThat(historicalTrendList.get(1).getHealthScores().get(0).getRiskStatus()).isEqualTo(Risk.NEED_ATTENTION);
    assertThat(historicalTrendList.get(1).getHealthScores().get(1).getHealthScore()).isEqualTo(45);
    assertThat(historicalTrendList.get(1).getHealthScores().get(1).getRiskStatus()).isEqualTo(Risk.NEED_ATTENTION);
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testGetHistoricalData_MultipleServiceEnvironmentOneCategoryFor48Hour() {
    Instant analysisTime = clock.instant();
    analysisTime = roundDownToMinBoundary(analysisTime, 30);
    HeatMap heatMap = builderFactory.heatMapBuilder().build();
    setStartTimeEndTimeAndRiskScoreWith30MinBucket(heatMap, analysisTime.plus(12, ChronoUnit.HOURS), 0.10);
    hPersistence.save(heatMap);

    HeatMap heatMapPrevious = builderFactory.heatMapBuilder().build();
    setStartTimeEndTimeAndRiskScoreWith30MinBucket(heatMapPrevious, heatMap.getHeatMapBucketStartTime(), 0.40);
    hPersistence.save(heatMapPrevious);

    String secondService = "secondService";
    String secondEnvironment = "secondEnvironment";
    HeatMap anotherServiceHeatMap =
        builderFactory.heatMapBuilder().serviceIdentifier(secondService).envIdentifier(secondEnvironment).build();
    anotherServiceHeatMap.getHeatMapRisks().forEach(heatMapRisk -> heatMapRisk.setRiskScore(0.55));
    hPersistence.save(anotherServiceHeatMap);

    List<Pair<String, String>> serviceEnvIds =
        Arrays.asList(Pair.of(serviceIdentifier, envIdentifier), Pair.of(secondService, secondEnvironment));
    List<HistoricalTrend> historicalTrendList =
        heatMapService.getHistoricalTrend(accountId, orgIdentifier, projectIdentifier, serviceEnvIds, 24);

    assertThat(historicalTrendList.size()).isEqualTo(2);

    assertThat(historicalTrendList.get(0).getHealthScores().size()).isEqualTo(48);
    assertThat(historicalTrendList.get(1).getHealthScores().size()).isEqualTo(48);

    for (int i = 0; i < 24; i++) {
      assertThat(historicalTrendList.get(0).getHealthScores().get(i).getHealthScore().intValue()).isEqualTo(60);
      assertThat(historicalTrendList.get(0).getHealthScores().get(i).getRiskStatus()).isEqualTo(Risk.OBSERVE);
    }
    for (int i = 24; i < 48; i++) {
      assertThat(historicalTrendList.get(0).getHealthScores().get(i).getHealthScore().intValue()).isEqualTo(90);
      assertThat(historicalTrendList.get(0).getHealthScores().get(i).getRiskStatus()).isEqualTo(Risk.HEALTHY);
    }

    for (int i = 0; i < 48; i++) {
      assertThat(historicalTrendList.get(1).getHealthScores().get(i).getHealthScore().intValue()).isEqualTo(45);
      assertThat(historicalTrendList.get(1).getHealthScores().get(i).getRiskStatus()).isEqualTo(Risk.NEED_ATTENTION);
    }
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testGetLatestRiskScoreOneServiceEnvironmentOneCategoryWithLatestBucketPresent() {
    Instant endTime = roundDownTo5MinBoundary(clock.instant());
    HeatMap heatMap = builderFactory.heatMapBuilder().heatMapResolution(FIVE_MIN).build();
    setStartTimeEndTimeAndRiskScoreWith5MinBucket(heatMap, endTime, 0.15, 0.25);
    hPersistence.save(heatMap);

    HeatMap previousHeatmap = builderFactory.heatMapBuilder().heatMapResolution(FIVE_MIN).build();
    setStartTimeEndTimeAndRiskScoreWith5MinBucket(previousHeatmap, heatMap.getHeatMapBucketStartTime(), 0.5, 0.5);
    hPersistence.save(previousHeatmap);

    List<RiskData> riskDataList = heatMapService.getLatestRiskScoreForLimitedServicesList(
        accountId, orgIdentifier, projectIdentifier, Arrays.asList(Pair.of(serviceIdentifier, envIdentifier)));

    assertThat(riskDataList.size()).isEqualTo(1);
    assertThat(riskDataList.get(0).getRiskStatus()).isEqualTo(Risk.HEALTHY);
    assertThat(riskDataList.get(0).getHealthScore()).isEqualTo(75);
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void
  testGetLatestRiskScoreOneServiceEnvironmentOneCategoryWithLatestBucketPresentAndLast10MinDataNotPresent() {
    Instant endTime = roundDownTo5MinBoundary(clock.instant());
    HeatMap heatMap = builderFactory.heatMapBuilder().heatMapResolution(FIVE_MIN).build();
    setStartTimeEndTimeAndRiskScoreWith5MinBucket(heatMap, endTime, 0.15, 0.25);
    SortedSet<HeatMapRisk> risks = new TreeSet<>(heatMap.getHeatMapRisks());
    risks.remove(risks.last());
    risks.remove(risks.last());
    heatMap.setHeatMapRisks(risks.stream().collect(Collectors.toList()));
    hPersistence.save(heatMap);

    List<RiskData> riskDataList = heatMapService.getLatestRiskScoreForLimitedServicesList(
        accountId, orgIdentifier, projectIdentifier, Arrays.asList(Pair.of(serviceIdentifier, envIdentifier)));
    assertThat(riskDataList.size()).isEqualTo(1);
    assertThat(riskDataList.get(0).getRiskStatus()).isEqualTo(Risk.HEALTHY);
    assertThat(riskDataList.get(0).getHealthScore()).isEqualTo(75);

    risks.remove(risks.last());
    heatMap.setHeatMapRisks(risks.stream().collect(Collectors.toList()));
    hPersistence.save(heatMap);

    riskDataList = heatMapService.getLatestRiskScoreForLimitedServicesList(
        accountId, orgIdentifier, projectIdentifier, Arrays.asList(Pair.of(serviceIdentifier, envIdentifier)));
    assertThat(riskDataList.size()).isEqualTo(1);
    assertThat(riskDataList.get(0).getRiskStatus()).isEqualTo(Risk.NO_DATA);
    assertThat(riskDataList.get(0).getHealthScore()).isEqualTo(null);
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testGetLatestRiskScoreOneServiceEnvironmentOneCategoryWithOnlyPreviousBucketPresent() {
    Instant endTime = roundDownTo5MinBoundary(clock.instant());

    HeatMap previousHeatmap = builderFactory.heatMapBuilder().heatMapResolution(FIVE_MIN).build();
    setStartTimeEndTimeAndRiskScoreWith5MinBucket(previousHeatmap, endTime.minus(4, ChronoUnit.HOURS), 0.50, 0.10);
    hPersistence.save(previousHeatmap);

    List<RiskData> riskDataList = heatMapService.getLatestRiskScoreForLimitedServicesList(
        accountId, orgIdentifier, projectIdentifier, Arrays.asList(Pair.of(serviceIdentifier, envIdentifier)));

    assertThat(riskDataList.size()).isEqualTo(1);
    assertThat(riskDataList.get(0).getRiskStatus()).isEqualTo(Risk.NO_DATA);
    assertThat(riskDataList.get(0).getHealthScore()).isEqualTo(null);
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testGetLatestRiskScoreOneServiceEnvironmentOneCategoryWithNoHeatMapRiskPresent() {
    Instant endTime = roundDownTo5MinBoundary(clock.instant());
    Instant startTime = endTime.minus(4, ChronoUnit.HOURS);
    HeatMap previousHeatmap =
        builderFactory.heatMapBuilder()
            .heatMapResolution(FIVE_MIN)
            .heatMapBucketStartTime(startTime)
            .heatMapBucketEndTime(endTime)
            .heatMapRisks(Arrays.asList(
                HeatMapRisk.builder().startTime(startTime).endTime(startTime.plus(5, ChronoUnit.MINUTES)).build()))
            .build();
    hPersistence.save(previousHeatmap);

    List<RiskData> riskDataList = heatMapService.getLatestRiskScoreForLimitedServicesList(
        accountId, orgIdentifier, projectIdentifier, Arrays.asList(Pair.of(serviceIdentifier, envIdentifier)));

    assertThat(riskDataList.size()).isEqualTo(1);
    assertThat(riskDataList.get(0).getRiskStatus()).isEqualTo(Risk.NO_DATA);
    assertThat(riskDataList.get(0).getHealthScore()).isEqualTo(null);
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testGetLatestRiskScoreOneServiceEnvironmentMultipleCategory() {
    Instant endTime = roundDownTo5MinBoundary(clock.instant());
    HeatMap heatMap = builderFactory.heatMapBuilder().heatMapResolution(FIVE_MIN).build();
    setStartTimeEndTimeAndRiskScoreWith5MinBucket(heatMap, endTime, 0.15, 0.25);
    hPersistence.save(heatMap);

    HeatMap heatMapPerformanceCategory =
        builderFactory.heatMapBuilder().heatMapResolution(FIVE_MIN).category(CVMonitoringCategory.PERFORMANCE).build();
    setStartTimeEndTimeAndRiskScoreWith5MinBucket(heatMapPerformanceCategory, endTime, 0.35, 0.65);
    hPersistence.save(heatMapPerformanceCategory);

    HeatMap previousHeatmap = builderFactory.heatMapBuilder().heatMapResolution(FIVE_MIN).build();
    setStartTimeEndTimeAndRiskScoreWith5MinBucket(previousHeatmap, heatMap.getHeatMapBucketStartTime(), 0.5, 0.5);
    hPersistence.save(previousHeatmap);

    List<RiskData> riskDataList = heatMapService.getLatestRiskScoreForLimitedServicesList(
        accountId, orgIdentifier, projectIdentifier, Arrays.asList(Pair.of(serviceIdentifier, envIdentifier)));

    assertThat(riskDataList.size()).isEqualTo(1);
    assertThat(riskDataList.get(0).getRiskStatus()).isEqualTo(Risk.NEED_ATTENTION);
    assertThat(riskDataList.get(0).getHealthScore().intValue()).isEqualTo(35);
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testGetLatestRiskScoreMultipleServiceEnvironmentOneCategory() {
    Instant endTime = roundDownTo5MinBoundary(clock.instant());
    HeatMap heatMap = builderFactory.heatMapBuilder().heatMapResolution(FIVE_MIN).build();
    setStartTimeEndTimeAndRiskScoreWith5MinBucket(heatMap, endTime, 0.15, 0.25);
    hPersistence.save(heatMap);

    String newService = "newService";
    String newEnv = "newEnv";
    HeatMap anotherServiceEnvHeatMap = builderFactory.heatMapBuilder()
                                           .heatMapResolution(FIVE_MIN)
                                           .serviceIdentifier(newService)
                                           .envIdentifier(newEnv)
                                           .build();
    setStartTimeEndTimeAndRiskScoreWith5MinBucket(anotherServiceEnvHeatMap, endTime, 0.11, 0.37);
    hPersistence.save(anotherServiceEnvHeatMap);

    List<RiskData> riskDataList = heatMapService.getLatestRiskScoreForLimitedServicesList(accountId, orgIdentifier,
        projectIdentifier, Arrays.asList(Pair.of(serviceIdentifier, envIdentifier), Pair.of(newService, newEnv)));

    assertThat(riskDataList.size()).isEqualTo(2);
    assertThat(riskDataList.get(0).getRiskStatus()).isEqualTo(Risk.HEALTHY);
    assertThat(riskDataList.get(0).getHealthScore()).isEqualTo(75);

    assertThat(riskDataList.get(1).getRiskStatus()).isEqualTo(Risk.OBSERVE);
    assertThat(riskDataList.get(1).getHealthScore()).isEqualTo(63);
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testGetLatestRiskScoreMultipleServiceEnvironmentMultipleCategory() {
    Instant endTime = roundDownTo5MinBoundary(clock.instant());
    HeatMap heatMap = builderFactory.heatMapBuilder().heatMapResolution(FIVE_MIN).build();
    setStartTimeEndTimeAndRiskScoreWith5MinBucket(heatMap, endTime, 0.15, 0.25);
    hPersistence.save(heatMap);

    HeatMap previousHeatmap = builderFactory.heatMapBuilder().heatMapResolution(FIVE_MIN).build();
    setStartTimeEndTimeAndRiskScoreWith5MinBucket(previousHeatmap, heatMap.getHeatMapBucketStartTime(), 0.5, 0.5);
    hPersistence.save(previousHeatmap);

    HeatMap heatMapPerformanceCategory =
        builderFactory.heatMapBuilder().heatMapResolution(FIVE_MIN).category(CVMonitoringCategory.PERFORMANCE).build();
    setStartTimeEndTimeAndRiskScoreWith5MinBucket(heatMapPerformanceCategory, endTime, 0.35, 0.65);
    hPersistence.save(heatMapPerformanceCategory);

    String newService = "newService";
    String newEnv = "newEnv";
    HeatMap anotherServiceEnvHeatMap = builderFactory.heatMapBuilder()
                                           .heatMapResolution(FIVE_MIN)
                                           .serviceIdentifier(newService)
                                           .envIdentifier(newEnv)
                                           .build();
    setStartTimeEndTimeAndRiskScoreWith5MinBucket(anotherServiceEnvHeatMap, endTime, 0.11, 0.37);
    hPersistence.save(anotherServiceEnvHeatMap);

    List<RiskData> riskDataList = heatMapService.getLatestRiskScoreForLimitedServicesList(accountId, orgIdentifier,
        projectIdentifier, Arrays.asList(Pair.of(serviceIdentifier, envIdentifier), Pair.of(newService, newEnv)));

    assertThat(riskDataList.size()).isEqualTo(2);
    assertThat(riskDataList.get(0).getRiskStatus()).isEqualTo(Risk.NEED_ATTENTION);
    assertThat(riskDataList.get(0).getHealthScore()).isEqualTo(35);

    assertThat(riskDataList.get(1).getRiskStatus()).isEqualTo(Risk.OBSERVE);
    assertThat(riskDataList.get(1).getHealthScore()).isEqualTo(63);
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testGetOverAllHealthScoreOneCategory4hrsDuration() {
    Instant endTime = clock.instant();
    createHeatMaps(endTime, FIVE_MIN, CVMonitoringCategory.ERRORS, 2);
    HistoricalTrend historicalTrend =
        heatMapService.getOverAllHealthScore(builderFactory.getContext().getProjectParams(), serviceIdentifier,
            envIdentifier, DurationDTO.FOUR_HOURS, endTime);

    assertThat(historicalTrend.getHealthScores().size()).isEqualTo(48);
    Instant time =
        getBoundaryOfResolution(endTime, FIVE_MIN.getResolution()).plusMillis(FIVE_MIN.getResolution().toMillis());
    for (int i = 47; i >= 0; i--) {
      RiskData riskData = historicalTrend.getHealthScores().get(i);
      assertThat(riskData.getEndTime()).isEqualTo(time.toEpochMilli());
      assertThat(riskData.getStartTime()).isEqualTo(time.minus(5, ChronoUnit.MINUTES).toEpochMilli());
      time = time.minus(5, ChronoUnit.MINUTES);
    }

    assertOverallHealthScoreWithInTwoHeatMap(historicalTrend);

    // Test for boundary condition
    endTime = getBoundaryOfResolution(endTime, FIVE_MIN.getBucketSize()).minus(1, ChronoUnit.MINUTES);
    historicalTrend = heatMapService.getOverAllHealthScore(builderFactory.getContext().getProjectParams(),
        serviceIdentifier, envIdentifier, DurationDTO.FOUR_HOURS, endTime);
    assertOverallHealthScoreWithInOneHeatMap(historicalTrend);
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testGetOverAllHealthScoreOneCategory4hrsDurationMultipleCategory() {
    Instant endTime = clock.instant();
    createHeatMaps(endTime, FIVE_MIN, CVMonitoringCategory.ERRORS, 2);
    List<HeatMap> heatMaps = hPersistence.createQuery(HeatMap.class).asList();
    heatMaps.forEach(heatMap -> heatMap.getHeatMapRisks().forEach(heatMapRisk -> heatMapRisk.setRiskScore(0.0)));
    hPersistence.save(heatMaps);
    Instant timeToCheckFor = getBoundaryOfResolution(endTime, FIVE_MIN.getBucketSize()).minus(1, ChronoUnit.MINUTES);
    HistoricalTrend historicalTrend =
        heatMapService.getOverAllHealthScore(builderFactory.getContext().getProjectParams(), serviceIdentifier,
            envIdentifier, DurationDTO.FOUR_HOURS, timeToCheckFor);

    assertThat(historicalTrend.getHealthScores().size()).isEqualTo(48);
    for (int i = 47; i >= 23; i--) {
      assertThat(historicalTrend.getHealthScores().get(i).getHealthScore()).isEqualTo(100);
      assertThat(historicalTrend.getHealthScores().get(i).getRiskStatus()).isEqualTo(getHealthScoreRiskStatus(100));
    }

    createHeatMaps(endTime, FIVE_MIN, CVMonitoringCategory.PERFORMANCE, 2);
    historicalTrend = heatMapService.getOverAllHealthScore(builderFactory.getContext().getProjectParams(),
        serviceIdentifier, envIdentifier, DurationDTO.FOUR_HOURS, timeToCheckFor);
    assertOverallHealthScoreWithInOneHeatMap(historicalTrend);
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testGetOverAllHealthScoreOneCategory24hrsDuration() {
    Instant endTime = clock.instant();
    createHeatMaps(endTime, THIRTY_MINUTES, CVMonitoringCategory.ERRORS, 2);
    Instant timeToCheckFor = endTime.plus(2, ChronoUnit.HOURS);
    HistoricalTrend historicalTrend =
        heatMapService.getOverAllHealthScore(builderFactory.getContext().getProjectParams(), serviceIdentifier,
            envIdentifier, DurationDTO.TWENTY_FOUR_HOURS, timeToCheckFor);
    assertThat(historicalTrend.getHealthScores().size()).isEqualTo(48);
    Instant time = getBoundaryOfResolution(timeToCheckFor, THIRTY_MINUTES.getResolution())
                       .plusMillis(THIRTY_MINUTES.getResolution().toMillis());
    for (int i = 47; i >= 0; i--) {
      RiskData riskData = historicalTrend.getHealthScores().get(i);
      assertThat(riskData.getEndTime()).isEqualTo(time.toEpochMilli());
      assertThat(riskData.getStartTime()).isEqualTo(time.minus(30, ChronoUnit.MINUTES).toEpochMilli());
      time = time.minus(30, ChronoUnit.MINUTES);
    }
    assertOverallHealthScoreWithInTwoHeatMap(historicalTrend);

    // Test for boundary condition
    timeToCheckFor = getBoundaryOfResolution(endTime, THIRTY_MINUTES.getBucketSize()).minus(1, ChronoUnit.MINUTES);
    historicalTrend = heatMapService.getOverAllHealthScore(builderFactory.getContext().getProjectParams(),
        serviceIdentifier, envIdentifier, DurationDTO.TWENTY_FOUR_HOURS, timeToCheckFor);
    assertOverallHealthScoreWithInOneHeatMap(historicalTrend);
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testGetOverAllHealthScoreOneCategory3DaysDuration() {
    Instant endTime = clock.instant();
    createHeatMaps(endTime, THIRTY_MINUTES, CVMonitoringCategory.ERRORS, 4);
    Instant timeToCheckFor = endTime;
    HistoricalTrend historicalTrend =
        heatMapService.getOverAllHealthScore(builderFactory.getContext().getProjectParams(), serviceIdentifier,
            envIdentifier, DurationDTO.THREE_DAYS, timeToCheckFor);

    assertThat(historicalTrend.getHealthScores().size()).isEqualTo(48);
    Instant time = getBoundaryOfResolution(timeToCheckFor, THIRTY_MINUTES.getResolution())
                       .plusMillis(THIRTY_MINUTES.getResolution().toMillis());
    int healthScore = 79;
    for (int i = 47; i >= 0; i--) {
      RiskData riskData = historicalTrend.getHealthScores().get(i);
      assertThat(riskData.getEndTime()).isEqualTo(time.toEpochMilli());
      assertThat(riskData.getStartTime()).isEqualTo(time.minus(90, ChronoUnit.MINUTES).toEpochMilli());
      assertHealthScore(riskData, healthScore);
      healthScore += 3;
      if (healthScore == 100) {
        healthScore = 52;
      }
      time = time.minus(90, ChronoUnit.MINUTES);
    }

    // Test for boundary condition
    timeToCheckFor = getBoundaryOfResolution(endTime, THIRTY_MINUTES.getBucketSize()).minus(1, ChronoUnit.MINUTES);
    historicalTrend = heatMapService.getOverAllHealthScore(builderFactory.getContext().getProjectParams(),
        serviceIdentifier, envIdentifier, DurationDTO.THREE_DAYS, timeToCheckFor);
    assertThat(historicalTrend.getHealthScores().size()).isEqualTo(48);

    time = getBoundaryOfResolution(timeToCheckFor, THIRTY_MINUTES.getResolution())
               .plusMillis(THIRTY_MINUTES.getResolution().toMillis());
    healthScore = 52;
    for (int i = 47; i >= 0; i--) {
      RiskData riskData = historicalTrend.getHealthScores().get(i);
      assertThat(riskData.getEndTime()).isEqualTo(time.toEpochMilli());
      assertThat(riskData.getStartTime()).isEqualTo(time.minus(90, ChronoUnit.MINUTES).toEpochMilli());
      assertHealthScore(riskData, healthScore);
      healthScore += 3;
      if (healthScore == 100) {
        healthScore = 52;
      }
      time = time.minus(90, ChronoUnit.MINUTES);
    }
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testGetOverAllHealthScoreOneCategory7DaysDuration() {
    Instant endTime = clock.instant();
    createHeatMaps(endTime, THIRTY_MINUTES, CVMonitoringCategory.ERRORS, 8);
    Instant timeToCheckFor = endTime;
    HistoricalTrend historicalTrend =
        heatMapService.getOverAllHealthScore(builderFactory.getContext().getProjectParams(), serviceIdentifier,
            envIdentifier, DurationDTO.SEVEN_DAYS, timeToCheckFor);

    assertThat(historicalTrend.getHealthScores().size()).isEqualTo(48);
    Instant time = getBoundaryOfResolution(timeToCheckFor, THIRTY_MINUTES.getResolution())
                       .plusMillis(THIRTY_MINUTES.getResolution().toMillis());

    for (int i = 47; i >= 0; i--) {
      RiskData riskData = historicalTrend.getHealthScores().get(i);
      assertThat(riskData.getEndTime()).isEqualTo(time.toEpochMilli());
      assertThat(riskData.getStartTime()).isEqualTo(time.minus(210, ChronoUnit.MINUTES).toEpochMilli());
      time = time.minus(210, ChronoUnit.MINUTES);
    }
    assertHealthScore(historicalTrend.getHealthScores().get(0), 72);
    assertHealthScore(historicalTrend.getHealthScores().get(47), 79);
    assertHealthScore(historicalTrend.getHealthScores().get(39), 87);
    assertHealthScore(historicalTrend.getHealthScores().get(38), 52);
    assertHealthScore(historicalTrend.getHealthScores().get(40), 80);

    // Test for boundary condition
    timeToCheckFor = getBoundaryOfResolution(endTime, THIRTY_MINUTES.getBucketSize()).minus(1, ChronoUnit.MINUTES);
    historicalTrend = heatMapService.getOverAllHealthScore(builderFactory.getContext().getProjectParams(),
        serviceIdentifier, envIdentifier, DurationDTO.SEVEN_DAYS, timeToCheckFor);
    assertThat(historicalTrend.getHealthScores().size()).isEqualTo(48);
    time = getBoundaryOfResolution(timeToCheckFor, THIRTY_MINUTES.getResolution())
               .plusMillis(THIRTY_MINUTES.getResolution().toMillis());
    for (int i = 47; i >= 0; i--) {
      RiskData riskData = historicalTrend.getHealthScores().get(i);
      assertThat(riskData.getEndTime()).isEqualTo(time.toEpochMilli());
      assertThat(riskData.getStartTime()).isEqualTo(time.minus(210, ChronoUnit.MINUTES).toEpochMilli());
      time = time.minus(210, ChronoUnit.MINUTES);
    }
    assertHealthScore(historicalTrend.getHealthScores().get(0), 93);
    assertHealthScore(historicalTrend.getHealthScores().get(47), 52);
    assertHealthScore(historicalTrend.getHealthScores().get(42), 87);
    assertHealthScore(historicalTrend.getHealthScores().get(41), 52);
    assertHealthScore(historicalTrend.getHealthScores().get(43), 80);
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testGetOverAllHealthScoreOneCategory30DaysDuration() {
    Instant endTime = clock.instant();
    createHeatMaps(endTime, THIRTY_MINUTES, CVMonitoringCategory.ERRORS, 31);
    Instant timeToCheckFor = endTime;
    HistoricalTrend historicalTrend =
        heatMapService.getOverAllHealthScore(builderFactory.getContext().getProjectParams(), serviceIdentifier,
            envIdentifier, DurationDTO.THIRTY_DAYS, timeToCheckFor);
    assertThat(historicalTrend.getHealthScores().size()).isEqualTo(48);
    Instant time = getBoundaryOfResolution(timeToCheckFor, THIRTY_MINUTES.getResolution())
                       .plusMillis(THIRTY_MINUTES.getResolution().toMillis());
    for (int i = 47; i >= 0; i--) {
      RiskData riskData = historicalTrend.getHealthScores().get(i);
      assertThat(riskData.getEndTime()).isEqualTo(time.toEpochMilli());
      assertThat(riskData.getStartTime()).isEqualTo(time.minus(15, ChronoUnit.HOURS).toEpochMilli());
      time = time.minus(15, ChronoUnit.HOURS);
    }
    assertHealthScore(historicalTrend.getHealthScores().get(0), 52);
    assertHealthScore(historicalTrend.getHealthScores().get(47), 52);
    assertHealthScore(historicalTrend.getHealthScores().get(35), 55);

    // Test for boundary condition
    historicalTrend = heatMapService.getOverAllHealthScore(builderFactory.getContext().getProjectParams(),
        serviceIdentifier, envIdentifier, DurationDTO.THIRTY_DAYS, timeToCheckFor);
    assertThat(historicalTrend.getHealthScores().size()).isEqualTo(48);
    time = getBoundaryOfResolution(timeToCheckFor, THIRTY_MINUTES.getResolution())
               .plusMillis(THIRTY_MINUTES.getResolution().toMillis());
    for (int i = 47; i >= 0; i--) {
      RiskData riskData = historicalTrend.getHealthScores().get(i);
      assertThat(riskData.getEndTime()).isEqualTo(time.toEpochMilli());
      assertThat(riskData.getStartTime()).isEqualTo(time.minus(15, ChronoUnit.HOURS).toEpochMilli());
      time = time.minus(15, ChronoUnit.HOURS);
    }
    assertHealthScore(historicalTrend.getHealthScores().get(0), 52);
    assertHealthScore(historicalTrend.getHealthScores().get(47), 52);
    assertHealthScore(historicalTrend.getHealthScores().get(46), 61);
  }

  @Test
  @Owner(developers = KAPIL)
  @Category(UnitTests.class)
  public void testGetLatestRiskScoreByServiceMap() {
    ProjectParams projectParams = ProjectParams.builder()
                                      .accountIdentifier(accountId)
                                      .orgIdentifier(orgIdentifier)
                                      .projectIdentifier(projectIdentifier)
                                      .build();
    Instant endTime = roundDownTo5MinBoundary(clock.instant());
    HeatMap heatMap = builderFactory.heatMapBuilder().heatMapResolution(FIVE_MIN).build();
    setStartTimeEndTimeAndRiskScoreWith5MinBucket(heatMap, endTime, 0.80, 0.75);
    hPersistence.save(heatMap);
    Map<ServiceEnvKey, RiskData> latestRiskScoreByServiceMap = heatMapService.getLatestRiskScoreByServiceMap(
        projectParams, Arrays.asList(Pair.of(serviceIdentifier, envIdentifier)));
    ServiceEnvKey serviceEnvKey =
        ServiceEnvKey.builder().serviceIdentifier(serviceIdentifier).envIdentifier(envIdentifier).build();

    assertThat(latestRiskScoreByServiceMap.keySet().size()).isEqualTo(1);
    assertThat(latestRiskScoreByServiceMap.get(serviceEnvKey).getHealthScore()).isEqualTo(25);
    assertThat(latestRiskScoreByServiceMap.get(serviceEnvKey).getRiskStatus()).isEqualTo(Risk.NEED_ATTENTION);
  }

  private void assertHealthScore(RiskData riskData, int val) {
    assertThat(riskData.getHealthScore()).isEqualTo(val);
    assertThat(getHealthScoreRiskStatus(riskData.getHealthScore())).isEqualTo(riskData.getRiskStatus());
  }

  private void assertOverallHealthScoreWithInTwoHeatMap(HistoricalTrend historicalTrend) {
    assertThat(historicalTrend.getHealthScores().size()).isEqualTo(48);
    int healthScore = 75;
    for (int i = 47; i >= 23; i--) {
      assertThat(historicalTrend.getHealthScores().get(i).getHealthScore()).isEqualTo(healthScore);
      assertThat(historicalTrend.getHealthScores().get(i).getRiskStatus())
          .isEqualTo(getHealthScoreRiskStatus(healthScore));
      healthScore++;
    }
    healthScore = 52;
    for (int i = 22; i >= 0; i--) {
      assertThat(historicalTrend.getHealthScores().get(i).getHealthScore()).isEqualTo(healthScore);
      assertThat(historicalTrend.getHealthScores().get(i).getRiskStatus())
          .isEqualTo(getHealthScoreRiskStatus(healthScore));
      healthScore++;
    }
  }

  private void assertOverallHealthScoreWithInOneHeatMap(HistoricalTrend historicalTrend) {
    assertThat(historicalTrend.getHealthScores().size()).isEqualTo(48);
    int healthScore = 52;
    for (int i = 47; i >= 0; i--) {
      assertThat(historicalTrend.getHealthScores().get(i).getHealthScore()).isEqualTo(healthScore);
      assertThat(historicalTrend.getHealthScores().get(i).getRiskStatus())
          .isEqualTo(getHealthScoreRiskStatus(healthScore));
      healthScore++;
    }
  }

  private Risk getHealthScoreRiskStatus(int healthScore) {
    if (healthScore >= 75) {
      return Risk.HEALTHY;
    } else if (healthScore >= 50) {
      return Risk.OBSERVE;
    } else if (healthScore >= 25) {
      return Risk.NEED_ATTENTION;
    } else {
      return Risk.UNHEALTHY;
    }
  }

  private void createHeatMaps(
      Instant endTime, HeatMapResolution heatMapResolution, CVMonitoringCategory category, int numberOfHeatMaps) {
    for (int i = 0; i < numberOfHeatMaps; i++) {
      endTime = getBoundaryOfResolution(endTime, heatMapResolution.getBucketSize())
                    .plusMillis(heatMapResolution.getBucketSize().toMillis());
      Instant startTime = endTime.minus(heatMapResolution.getBucketSize());
      HeatMap heatMap = builderFactory.heatMapBuilder()
                            .heatMapResolution(heatMapResolution)
                            .category(category)
                            .heatMapBucketStartTime(startTime)
                            .heatMapBucketEndTime(endTime)
                            .build();
      List<HeatMapRisk> heatMapRisks = new ArrayList<>();
      int risk = 1;
      for (Instant time = startTime; time.isBefore(endTime); time = time.plus(heatMapResolution.getResolution())) {
        heatMapRisks.add(HeatMapRisk.builder()
                             .riskScore((double) risk / 100)
                             .startTime(time)
                             .endTime(time.plus(heatMapResolution.getResolution()))
                             .build());
        risk++;
      }
      heatMap.setHeatMapRisks(heatMapRisks);
      hPersistence.save(heatMap);
      endTime = startTime.minus(1, ChronoUnit.MINUTES);
    }
  }

  private void setStartTimeEndTimeAndRiskScoreWith30MinBucket(HeatMap heatMap, Instant endTime, double riskScore) {
    Instant startTime = endTime.minus(24, ChronoUnit.HOURS);
    heatMap.setHeatMapBucketStartTime(startTime);
    heatMap.setHeatMapBucketEndTime(endTime);
    List<HeatMapRisk> heatMapRisks = new ArrayList<>();

    for (Instant time = startTime; time.isBefore(endTime); time = time.plus(30, ChronoUnit.MINUTES)) {
      heatMapRisks.add(HeatMapRisk.builder()
                           .riskScore(riskScore)
                           .startTime(time)
                           .endTime(time.plus(30, ChronoUnit.MINUTES))
                           .build());
    }
    heatMap.setHeatMapRisks(heatMapRisks);
  }

  private void setStartTimeEndTimeAndRiskScoreWith5MinBucket(
      HeatMap heatMap, Instant endTime, double firstHalfRiskScore, double secondHalfRiskScore) {
    Instant startTime = endTime.minus(4, ChronoUnit.HOURS);
    heatMap.setHeatMapBucketStartTime(startTime);
    heatMap.setHeatMapBucketEndTime(endTime);
    List<HeatMapRisk> heatMapRisks = new ArrayList<>();

    for (Instant time = startTime; time.isBefore(startTime.plus(2, ChronoUnit.HOURS));
         time = time.plus(5, ChronoUnit.MINUTES)) {
      heatMapRisks.add(HeatMapRisk.builder()
                           .riskScore(firstHalfRiskScore)
                           .startTime(time)
                           .endTime(time.plus(5, ChronoUnit.MINUTES))
                           .build());
    }
    for (Instant time = startTime.plus(2, ChronoUnit.HOURS); time.isBefore(endTime);
         time = time.plus(5, ChronoUnit.MINUTES)) {
      heatMapRisks.add(HeatMapRisk.builder()
                           .riskScore(secondHalfRiskScore)
                           .startTime(time)
                           .endTime(time.plus(5, ChronoUnit.MINUTES))
                           .build());
    }
    heatMap.setHeatMapRisks(heatMapRisks);
  }

  private Instant getBoundaryOfResolution(Instant input, Duration resolution) {
    long timeStamp = input.toEpochMilli();
    return Instant.ofEpochMilli(timeStamp - (timeStamp % resolution.toMillis()));
  }
}
