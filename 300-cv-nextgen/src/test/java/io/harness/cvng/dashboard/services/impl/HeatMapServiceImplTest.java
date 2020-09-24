package io.harness.cvng.dashboard.services.impl;

import static io.harness.cvng.core.services.CVNextGenConstants.CV_ANALYSIS_WINDOW_MINUTES;
import static io.harness.cvng.dashboard.entities.HeatMap.HeatMapResolution.FIFTEEN_MINUTES;
import static io.harness.cvng.dashboard.entities.HeatMap.HeatMapResolution.FIVE_MIN;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.persistence.HQuery.excludeAuthority;
import static io.harness.rule.OwnerRule.PRAVEEN;
import static io.harness.rule.OwnerRule.RAGHU;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;
import static org.mockito.Mockito.when;

import com.google.common.collect.Sets;
import com.google.inject.Inject;

import io.harness.CvNextGenTest;
import io.harness.category.element.UnitTests;
import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.core.beans.AppDynamicsDSConfig;
import io.harness.cvng.core.entities.MetricPack;
import io.harness.cvng.core.services.api.CVConfigService;
import io.harness.cvng.core.services.api.DSConfigService;
import io.harness.cvng.dashboard.beans.HeatMapDTO;
import io.harness.cvng.dashboard.entities.HeatMap;
import io.harness.cvng.dashboard.entities.HeatMap.HeatMapKeys;
import io.harness.cvng.dashboard.entities.HeatMap.HeatMapResolution;
import io.harness.cvng.dashboard.entities.HeatMap.HeatMapRisk;
import io.harness.cvng.dashboard.services.api.HeatMapService;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
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

public class HeatMapServiceImplTest extends CvNextGenTest {
  @Inject private HeatMapService heatMapService;

  private String projectIdentifier;
  private String serviceIdentifier;
  private String envIdentifier;
  private String accountId;
  @Inject private HPersistence hPersistence;
  @Inject private DSConfigService dsConfigService;
  @Mock private CVConfigService cvConfigService;
  private Clock clock;

  @Before
  public void setUp() throws Exception {
    projectIdentifier = generateUuid();
    serviceIdentifier = generateUuid();
    envIdentifier = generateUuid();
    accountId = generateUuid();
    AppDynamicsDSConfig dsConfig = new AppDynamicsDSConfig();
    dsConfig.setProjectIdentifier(projectIdentifier);
    dsConfig.setAccountId(accountId);
    dsConfig.setMetricPacks(Sets.newHashSet(MetricPack.builder().category(CVMonitoringCategory.PERFORMANCE).build()));
    dsConfig.setConnectorIdentifier(generateUuid());
    dsConfig.setEnvIdentifier(envIdentifier);
    dsConfig.setProductName(generateUuid());
    dsConfig.setApplicationName(generateUuid());
    dsConfig.setIdentifier(generateUuid());
    dsConfig.setServiceMappings(Sets.newHashSet(AppDynamicsDSConfig.ServiceMapping.builder()
                                                    .serviceIdentifier(serviceIdentifier)
                                                    .tierName(generateUuid())
                                                    .build()));
    dsConfigService.upsert(dsConfig);
    clock = Clock.fixed(Instant.parse("2020-04-22T10:02:06Z"), ZoneOffset.UTC);
    MockitoAnnotations.initMocks(this);
    FieldUtils.writeField(heatMapService, "cvConfigService", cvConfigService, true);
    FieldUtils.writeField(heatMapService, "clock", clock, true);
    when(cvConfigService.getAvailableCategories(accountId, projectIdentifier))
        .thenReturn(new HashSet<>(Arrays.asList(CVMonitoringCategory.PERFORMANCE)));
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testUpsertAndUpdate() {
    Instant instant = Instant.now();
    heatMapService.updateRiskScore(
        accountId, projectIdentifier, serviceIdentifier, envIdentifier, CVMonitoringCategory.PERFORMANCE, instant, 0.6);
    verifyUpdates(instant, 0.6);
    List<HeatMap> heatMaps;
    HeatMap heatMap;
    Set<HeatMapRisk> heatMapRisks;
    HeatMapRisk heatMapRisk;

    // update and test
    heatMapService.updateRiskScore(
        accountId, projectIdentifier, serviceIdentifier, envIdentifier, CVMonitoringCategory.PERFORMANCE, instant, 0.7);
    verifyUpdates(instant, 0.7);

    // updating with lower risk score shouldn't change anything
    heatMapService.updateRiskScore(
        accountId, projectIdentifier, serviceIdentifier, envIdentifier, CVMonitoringCategory.PERFORMANCE, instant, 0.5);
    verifyUpdates(instant, 0.7);
  }

  private void verifyUpdates(Instant instant, double riskScore) {
    verifyHeatMaps(instant, riskScore,
        hPersistence.createQuery(HeatMap.class, excludeAuthority)
            .filter(HeatMapKeys.projectIdentifier, projectIdentifier)
            .filter(HeatMapKeys.serviceIdentifier, serviceIdentifier)
            .filter(HeatMapKeys.envIdentifier, envIdentifier)
            .asList());
    verifyHeatMaps(instant, riskScore,
        hPersistence.createQuery(HeatMap.class, excludeAuthority)
            .filter(HeatMapKeys.projectIdentifier, projectIdentifier)
            .filter(HeatMapKeys.serviceIdentifier, null)
            .filter(HeatMapKeys.envIdentifier, envIdentifier)
            .asList());
    verifyHeatMaps(instant, riskScore,
        hPersistence.createQuery(HeatMap.class, excludeAuthority)
            .filter(HeatMapKeys.projectIdentifier, projectIdentifier)
            .filter(HeatMapKeys.serviceIdentifier, null)
            .filter(HeatMapKeys.envIdentifier, null)
            .asList());
  }

  private void verifyHeatMaps(Instant instant, double riskScore, List<HeatMap> heatMaps) {
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
          .isEqualTo(heatMap.getHeatMapBucketStartTime().plusMillis(heatMapResolution.getBucketSize().toMillis() - 1));
      Set<HeatMapRisk> heatMapRisks = heatMap.getHeatMapRisks();
      assertThat(heatMapRisks.size()).isEqualTo(1);
      HeatMapRisk heatMapRisk = heatMapRisks.iterator().next();
      assertThat(heatMapRisk.getStartTime())
          .isEqualTo(Instant.ofEpochMilli(instant.toEpochMilli()
              - Math.floorMod(instant.toEpochMilli(), heatMapResolution.getResolution().toMillis())));
      assertThat(heatMapRisk.getEndTime())
          .isEqualTo(heatMapRisk.getStartTime().plusMillis(heatMapResolution.getResolution().toMillis() - 1));
      assertThat(heatMapRisk.getRiskScore()).isEqualTo(riskScore, offset(0.001));
    }
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testUpsert_whenMultipleBoundaries() {
    double numOfUnits = 1500;
    for (int minuteBoundry = 0; minuteBoundry < numOfUnits * CV_ANALYSIS_WINDOW_MINUTES;
         minuteBoundry += CV_ANALYSIS_WINDOW_MINUTES) {
      heatMapService.updateRiskScore(accountId, projectIdentifier, serviceIdentifier, envIdentifier,
          CVMonitoringCategory.PERFORMANCE, Instant.ofEpochMilli(TimeUnit.MINUTES.toMillis(minuteBoundry)), 0.6);
    }
    for (int i = 0; i < HeatMapResolution.values().length; i++) {
      HeatMapResolution heatMapResolution = HeatMapResolution.values()[i];
      List<HeatMap> heatMaps = hPersistence.createQuery(HeatMap.class, excludeAuthority)
                                   .filter(HeatMapKeys.heatMapResolution, heatMapResolution)
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
    heatMapService.updateRiskScore(accountId, projectIdentifier, serviceIdentifier, envIdentifier,
        CVMonitoringCategory.PERFORMANCE, updateInstant, 0.7);

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
    Map<CVMonitoringCategory, SortedSet<HeatMapDTO>> heatMap = heatMapService.getHeatMap(accountId, projectIdentifier,
        serviceIdentifier, envIdentifier, Instant.ofEpochMilli(TimeUnit.MINUTES.toMillis(startMin)),
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
                         .endTime(TimeUnit.MINUTES.toMillis(i) + FIVE_MIN.getResolution().toMillis() - 1)
                         .build());
    }
    int numOfRiskUnits = 24;
    int riskStartBoundary = 30;
    for (int minuteBoundry = riskStartBoundary;
         minuteBoundry < riskStartBoundary + numOfRiskUnits * CV_ANALYSIS_WINDOW_MINUTES;
         minuteBoundry += CV_ANALYSIS_WINDOW_MINUTES) {
      heatMapService.updateRiskScore(accountId, projectIdentifier, serviceIdentifier, envIdentifier,
          CVMonitoringCategory.PERFORMANCE, Instant.ofEpochMilli(TimeUnit.MINUTES.toMillis(minuteBoundry)),
          0.01 * minuteBoundry);
    }

    heatMap = heatMapService.getHeatMap(accountId, projectIdentifier, null, null,
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
                           .endTime(TimeUnit.MINUTES.toMillis(i) + FIVE_MIN.getResolution().toMillis() - 1)
                           .build());
      } else {
        assertThat(heatMapDTO)
            .isEqualTo(HeatMapDTO.builder()
                           .startTime(TimeUnit.MINUTES.toMillis(i))
                           .endTime(TimeUnit.MINUTES.toMillis(i) + FIVE_MIN.getResolution().toMillis() - 1)
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
    Map<CVMonitoringCategory, SortedSet<HeatMapDTO>> heatMap = heatMapService.getHeatMap(accountId, projectIdentifier,
        serviceIdentifier, envIdentifier, Instant.ofEpochMilli(TimeUnit.MINUTES.toMillis(startMin)),
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
                         .endTime(TimeUnit.MINUTES.toMillis(i) + FIFTEEN_MINUTES.getResolution().toMillis() - 1)
                         .build());
    }
    int numOfRiskUnits = 42;
    int riskStartBoundary = 70;
    for (int minuteBoundry = riskStartBoundary;
         minuteBoundry <= riskStartBoundary + numOfRiskUnits * CV_ANALYSIS_WINDOW_MINUTES;
         minuteBoundry += CV_ANALYSIS_WINDOW_MINUTES) {
      heatMapService.updateRiskScore(accountId, projectIdentifier, serviceIdentifier, envIdentifier,
          CVMonitoringCategory.PERFORMANCE, Instant.ofEpochMilli(TimeUnit.MINUTES.toMillis(minuteBoundry)),
          0.01 * minuteBoundry);
    }

    heatMap = heatMapService.getHeatMap(accountId, projectIdentifier, null, null,
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
                           .endTime(TimeUnit.MINUTES.toMillis(i) + FIFTEEN_MINUTES.getResolution().toMillis() - 1)
                           .build());
      } else {
        assertThat(heatMapDTO)
            .isEqualTo(HeatMapDTO.builder()
                           .startTime(TimeUnit.MINUTES.toMillis(i))
                           .endTime(TimeUnit.MINUTES.toMillis(i) + FIFTEEN_MINUTES.getResolution().toMillis() - 1)
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
        heatMapService.updateRiskScore(
            accountId, projectIdentifier, "service" + i, "env" + j, CVMonitoringCategory.PERFORMANCE, instant, i * j);
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
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testGetHeatMap_ForSavedCategories() {
    Set<CVMonitoringCategory> categories = new HashSet<>();
    for (CVMonitoringCategory cvMonitoringCategory : CVMonitoringCategory.values()) {
      categories.add(cvMonitoringCategory);
      when(cvConfigService.getAvailableCategories(accountId, projectIdentifier)).thenReturn(categories);
      Map<CVMonitoringCategory, SortedSet<HeatMapDTO>> heatMap = heatMapService.getHeatMap(accountId, projectIdentifier,
          serviceIdentifier, envIdentifier, Instant.now().minus(1, ChronoUnit.HOURS), Instant.now());
      assertThat(heatMap.size()).isEqualTo(categories.size());
      categories.forEach(category -> assertThat(heatMap).containsKey(category));
    }
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetCategoryRiskScores_singleServiceEnvironment() {
    String orgId = generateUuid();

    when(cvConfigService.getAvailableCategories(accountId, projectIdentifier))
        .thenReturn(new HashSet<>(Arrays.asList(CVMonitoringCategory.PERFORMANCE, CVMonitoringCategory.QUALITY)));
    int numOfRiskUnits = 24;
    long riskStartBoundary = TimeUnit.MILLISECONDS.toMinutes(clock.instant().toEpochMilli());
    for (long minuteBoundry = riskStartBoundary;
         minuteBoundry < riskStartBoundary + numOfRiskUnits * CV_ANALYSIS_WINDOW_MINUTES;
         minuteBoundry += CV_ANALYSIS_WINDOW_MINUTES) {
      heatMapService.updateRiskScore(accountId, projectIdentifier, serviceIdentifier, envIdentifier,
          CVMonitoringCategory.PERFORMANCE, Instant.ofEpochMilli(TimeUnit.MINUTES.toMillis(minuteBoundry)),
          0.01 * minuteBoundry);
      heatMapService.updateRiskScore(accountId, projectIdentifier, serviceIdentifier + "2", envIdentifier,
          CVMonitoringCategory.QUALITY, Instant.ofEpochMilli(TimeUnit.MINUTES.toMillis(minuteBoundry)),
          0.01 * minuteBoundry);
    }

    Map<CVMonitoringCategory, Integer> categoryRiskMap =
        heatMapService.getCategoryRiskScores(accountId, orgId, projectIdentifier, serviceIdentifier, envIdentifier);

    assertThat(categoryRiskMap).isNotNull();
    assertThat(categoryRiskMap.size()).isEqualTo(CVMonitoringCategory.values().length);
    assertThat(categoryRiskMap.containsKey(CVMonitoringCategory.PERFORMANCE)).isTrue();
    assertThat(categoryRiskMap.get(CVMonitoringCategory.PERFORMANCE)).isNotEqualTo(-1);
    assertThat(categoryRiskMap.get(CVMonitoringCategory.QUALITY)).isEqualTo(-1);
    assertThat(categoryRiskMap.get(CVMonitoringCategory.RESOURCES)).isEqualTo(-1);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetCategoryRiskScores_singleEnvironment() {
    String orgId = generateUuid();
    Map<String, Set<String>> envServiceMap = new HashMap<>();
    Set<String> services = new HashSet<>(Arrays.asList(serviceIdentifier, serviceIdentifier + "2"));
    envServiceMap.put(envIdentifier, services);

    when(cvConfigService.getEnvToServicesMap(accountId, orgId, projectIdentifier)).thenReturn(envServiceMap);
    when(cvConfigService.getAvailableCategories(accountId, projectIdentifier))
        .thenReturn(new HashSet<>(Arrays.asList(CVMonitoringCategory.PERFORMANCE, CVMonitoringCategory.QUALITY)));
    int numOfRiskUnits = 24;
    long riskStartBoundary = TimeUnit.MILLISECONDS.toMinutes(clock.instant().toEpochMilli());
    for (long minuteBoundry = riskStartBoundary;
         minuteBoundry < riskStartBoundary + numOfRiskUnits * CV_ANALYSIS_WINDOW_MINUTES;
         minuteBoundry += CV_ANALYSIS_WINDOW_MINUTES) {
      heatMapService.updateRiskScore(accountId, projectIdentifier, serviceIdentifier, envIdentifier,
          CVMonitoringCategory.PERFORMANCE, Instant.ofEpochMilli(TimeUnit.MINUTES.toMillis(minuteBoundry)),
          0.01 * minuteBoundry);
      heatMapService.updateRiskScore(accountId, projectIdentifier, serviceIdentifier + "2", envIdentifier,
          CVMonitoringCategory.QUALITY, Instant.ofEpochMilli(TimeUnit.MINUTES.toMillis(minuteBoundry)),
          0.01 * minuteBoundry);
    }

    Map<CVMonitoringCategory, Integer> categoryRiskMap =
        heatMapService.getCategoryRiskScores(accountId, orgId, projectIdentifier, null, envIdentifier);

    assertThat(categoryRiskMap).isNotNull();

    assertThat(categoryRiskMap.size()).isEqualTo(CVMonitoringCategory.values().length);
    assertThat(categoryRiskMap.containsKey(CVMonitoringCategory.PERFORMANCE)).isTrue();
    assertThat(categoryRiskMap.get(CVMonitoringCategory.PERFORMANCE)).isNotEqualTo(-1);
    assertThat(categoryRiskMap.get(CVMonitoringCategory.QUALITY)).isNotEqualTo(-1);
    assertThat(categoryRiskMap.get(CVMonitoringCategory.RESOURCES)).isEqualTo(-1);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetCategoryRiskScores_multipleServices() {
    String orgId = generateUuid();
    Map<String, Set<String>> envServiceMap = new HashMap<>();
    Set<String> services = new HashSet<>(Arrays.asList(serviceIdentifier, serviceIdentifier + "2"));
    envServiceMap.put(envIdentifier, services);

    when(cvConfigService.getEnvToServicesMap(accountId, orgId, projectIdentifier)).thenReturn(envServiceMap);
    when(cvConfigService.getAvailableCategories(accountId, projectIdentifier))
        .thenReturn(new HashSet<>(Arrays.asList(CVMonitoringCategory.PERFORMANCE, CVMonitoringCategory.QUALITY)));
    int numOfRiskUnits = 24;
    long riskStartBoundary = TimeUnit.MILLISECONDS.toMinutes(clock.instant().toEpochMilli());
    for (long minuteBoundry = riskStartBoundary;
         minuteBoundry < riskStartBoundary + numOfRiskUnits * CV_ANALYSIS_WINDOW_MINUTES;
         minuteBoundry += CV_ANALYSIS_WINDOW_MINUTES) {
      heatMapService.updateRiskScore(accountId, projectIdentifier, serviceIdentifier, envIdentifier,
          CVMonitoringCategory.PERFORMANCE, Instant.ofEpochMilli(TimeUnit.MINUTES.toMillis(minuteBoundry)),
          0.01 * minuteBoundry);
      heatMapService.updateRiskScore(accountId, projectIdentifier, serviceIdentifier + "2", envIdentifier,
          CVMonitoringCategory.QUALITY, Instant.ofEpochMilli(TimeUnit.MINUTES.toMillis(minuteBoundry)),
          0.01 * minuteBoundry);
    }

    Map<CVMonitoringCategory, Integer> categoryRiskMap =
        heatMapService.getCategoryRiskScores(accountId, orgId, projectIdentifier, null, null);

    assertThat(categoryRiskMap).isNotNull();
    assertThat(categoryRiskMap.size()).isEqualTo(CVMonitoringCategory.values().length);
    assertThat(categoryRiskMap.containsKey(CVMonitoringCategory.PERFORMANCE)).isTrue();
    assertThat(categoryRiskMap.containsKey(CVMonitoringCategory.QUALITY)).isTrue();

    assertThat(categoryRiskMap.get(CVMonitoringCategory.PERFORMANCE)).isNotEqualTo(-1);
    assertThat(categoryRiskMap.get(CVMonitoringCategory.QUALITY)).isNotEqualTo(-1);
    assertThat(categoryRiskMap.get(CVMonitoringCategory.RESOURCES)).isEqualTo(-1);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetCategoryRiskScores_olderThan15mins() throws Exception {
    String orgId = generateUuid();
    Map<String, Set<String>> envServiceMap = new HashMap<>();
    Set<String> services = new HashSet<>(Arrays.asList(serviceIdentifier, serviceIdentifier + "2"));
    envServiceMap.put(envIdentifier, services);

    when(cvConfigService.getEnvToServicesMap(accountId, orgId, projectIdentifier)).thenReturn(envServiceMap);
    when(cvConfigService.getAvailableCategories(accountId, projectIdentifier))
        .thenReturn(new HashSet<>(Arrays.asList(CVMonitoringCategory.PERFORMANCE, CVMonitoringCategory.QUALITY)));
    int numOfRiskUnits = 24;
    long riskStartBoundary = TimeUnit.MILLISECONDS.toMinutes(clock.instant().toEpochMilli());
    for (long minuteBoundry = riskStartBoundary;
         minuteBoundry < riskStartBoundary + numOfRiskUnits * CV_ANALYSIS_WINDOW_MINUTES;
         minuteBoundry += CV_ANALYSIS_WINDOW_MINUTES) {
      heatMapService.updateRiskScore(accountId, projectIdentifier, serviceIdentifier, envIdentifier,
          CVMonitoringCategory.PERFORMANCE, Instant.ofEpochMilli(TimeUnit.MINUTES.toMillis(minuteBoundry)),
          0.01 * minuteBoundry);
      heatMapService.updateRiskScore(accountId, projectIdentifier, serviceIdentifier + "2", envIdentifier,
          CVMonitoringCategory.QUALITY, Instant.ofEpochMilli(TimeUnit.MINUTES.toMillis(minuteBoundry)),
          0.01 * minuteBoundry);
    }
    // make the clock reflect a much newer time
    clock = Clock.fixed(Instant.parse("2020-04-23T10:02:06Z"), ZoneOffset.UTC);
    FieldUtils.writeField(heatMapService, "clock", clock, true);

    Map<CVMonitoringCategory, Integer> categoryRiskMap =
        heatMapService.getCategoryRiskScores(accountId, orgId, projectIdentifier, null, null);

    assertThat(categoryRiskMap).isNotNull();
    assertThat(categoryRiskMap.size()).isEqualTo(CVMonitoringCategory.values().length);

    assertThat(categoryRiskMap.get(CVMonitoringCategory.PERFORMANCE)).isEqualTo(-1);
    assertThat(categoryRiskMap.get(CVMonitoringCategory.QUALITY)).isEqualTo(-1);
    assertThat(categoryRiskMap.get(CVMonitoringCategory.RESOURCES)).isEqualTo(-1);
  }
}
