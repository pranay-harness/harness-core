package io.harness.cvng.core.services.impl;

import static io.harness.cvng.core.services.CVNextGenConstants.CV_ANALYSIS_WINDOW_MINUTES;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.persistence.HQuery.excludeAuthority;
import static io.harness.rule.OwnerRule.RAGHU;
import static io.harness.rule.TestUserProvider.testUserProvider;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import io.harness.beans.EmbeddedUser;
import io.harness.category.element.UnitTests;
import io.harness.cvng.CVNextGenBaseTest;
import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.beans.TimeSeriesCustomThresholdActions;
import io.harness.cvng.beans.TimeSeriesDataCollectionRecord;
import io.harness.cvng.beans.TimeSeriesDataCollectionRecord.TimeSeriesDataRecordGroupValue;
import io.harness.cvng.beans.TimeSeriesDataCollectionRecord.TimeSeriesDataRecordMetricValue;
import io.harness.cvng.beans.TimeSeriesMetricType;
import io.harness.cvng.beans.TimeSeriesThresholdActionType;
import io.harness.cvng.beans.TimeSeriesThresholdComparisonType;
import io.harness.cvng.beans.TimeSeriesThresholdCriteria;
import io.harness.cvng.beans.TimeSeriesThresholdType;
import io.harness.cvng.core.beans.TimeSeriesMetricDefinition;
import io.harness.cvng.core.entities.AppDynamicsCVConfig;
import io.harness.cvng.core.entities.MetricPack;
import io.harness.cvng.core.entities.TimeSeriesRecord;
import io.harness.cvng.core.entities.TimeSeriesThreshold;
import io.harness.cvng.core.services.CVNextGenConstants;
import io.harness.cvng.core.services.api.CVConfigService;
import io.harness.cvng.core.services.api.MetricPackService;
import io.harness.cvng.core.services.api.TimeSeriesService;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class TimeSeriesServiceImplTest extends CVNextGenBaseTest {
  private String cvConfigId;
  private String accountId;
  private Random random;
  private String projectIdentifier;
  @Inject private TimeSeriesService timeSeriesService;
  @Inject private HPersistence hPersistence;
  @Inject private CVConfigService cvConfigService;
  @Inject private MetricPackService metricPackService;

  @Before
  public void setUp() {
    cvConfigId = generateUuid();
    accountId = generateUuid();
    random = new Random(System.currentTimeMillis());
    testUserProvider.setActiveUser(EmbeddedUser.builder().name("user1").build());
    hPersistence.registerUserProvider(testUserProvider);
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testSave_whenAllWithinBucket() {
    int numOfMetrics = 4;
    int numOfTxnx = 5;
    long numOfMins = CV_ANALYSIS_WINDOW_MINUTES;
    List<TimeSeriesDataCollectionRecord> collectionRecords = new ArrayList<>();
    for (int i = 0; i < numOfMins; i++) {
      TimeSeriesDataCollectionRecord collectionRecord = TimeSeriesDataCollectionRecord.builder()
                                                            .accountId(accountId)
                                                            .cvConfigId(cvConfigId)
                                                            .timeStamp(TimeUnit.MINUTES.toMillis(i))
                                                            .metricValues(new HashSet<>())
                                                            .build();
      for (int j = 0; j < numOfMetrics; j++) {
        TimeSeriesDataRecordMetricValue metricValue = TimeSeriesDataRecordMetricValue.builder()
                                                          .metricName("metric-" + j)
                                                          .timeSeriesValues(new HashSet<>())
                                                          .build();
        for (int k = 0; k < numOfTxnx; k++) {
          metricValue.getTimeSeriesValues().add(
              TimeSeriesDataRecordGroupValue.builder().value(random.nextDouble()).groupName("group-" + k).build());
        }
        collectionRecord.getMetricValues().add(metricValue);
      }
      collectionRecords.add(collectionRecord);
    }
    timeSeriesService.save(collectionRecords);
    List<TimeSeriesRecord> timeSeriesRecords =
        hPersistence.createQuery(TimeSeriesRecord.class, excludeAuthority).asList();
    assertThat(timeSeriesRecords.size()).isEqualTo(numOfMetrics);
    validateSavedRecords(numOfMetrics, numOfTxnx, numOfMins, timeSeriesRecords);

    // save again the same records ans test idempotency
    timeSeriesService.save(collectionRecords);
    timeSeriesRecords = hPersistence.createQuery(TimeSeriesRecord.class, excludeAuthority).asList();
    assertThat(timeSeriesRecords.size()).isEqualTo(numOfMetrics);
    validateSavedRecords(numOfMetrics, numOfTxnx, numOfMins, timeSeriesRecords);
  }

  private void validateSavedRecords(
      int numOfMetrics, int numOfTxnx, long numOfMins, List<TimeSeriesRecord> timeSeriesRecords) {
    for (int i = 0; i < numOfMetrics; i++) {
      TimeSeriesRecord timeSeriesRecord = timeSeriesRecords.get(i);
      assertThat(timeSeriesRecord.getCvConfigId()).isEqualTo(cvConfigId);
      assertThat(timeSeriesRecord.getAccountId()).isEqualTo(accountId);
      assertThat(timeSeriesRecord.getBucketStartTime().toEpochMilli()).isEqualTo(0);
      assertThat(timeSeriesRecord.getMetricName()).isEqualTo("metric-" + i);
      assertThat(timeSeriesRecord.getTimeSeriesGroupValues().size()).isEqualTo(numOfTxnx * numOfMins);
      ArrayList<TimeSeriesRecord.TimeSeriesGroupValue> timeSeriesGroupValues =
          Lists.newArrayList(timeSeriesRecord.getTimeSeriesGroupValues());
      Collections.sort(timeSeriesGroupValues, (o1, o2) -> {
        if (StringUtils.compare(o1.getGroupName(), o2.getGroupName()) != 0) {
          return StringUtils.compare(o1.getGroupName(), o2.getGroupName());
        }
        if (o1.getTimeStamp() != o2.getTimeStamp()) {
          return Long.compare(o1.getTimeStamp().toEpochMilli(), o2.getTimeStamp().toEpochMilli());
        }
        return 0;
      });
      AtomicInteger groupNum = new AtomicInteger(0);
      AtomicInteger timeStamp = new AtomicInteger(0);
      AtomicInteger recordNum = new AtomicInteger(0);
      timeSeriesGroupValues.forEach(timeSeriesGroupValue -> {
        assertThat(timeSeriesGroupValue.getGroupName()).isEqualTo("group-" + groupNum.get());
        assertThat(timeSeriesGroupValue.getTimeStamp().toEpochMilli())
            .isEqualTo(TimeUnit.MINUTES.toMillis(timeStamp.get()));
        recordNum.incrementAndGet();
        timeStamp.incrementAndGet();

        if (recordNum.get() % numOfMins == 0) {
          timeStamp.set(0);
          groupNum.incrementAndGet();
        }
      });
    }
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testSave_whenWithinMultipleBucket() {
    int numOfMetrics = 4;
    int numOfTxnx = 5;
    int numOfMins = 17;
    int numOfTimeBuckets = 4; // 35-39, 40-44, 45-49, 50-52
    List<TimeSeriesDataCollectionRecord> collectionRecords = new ArrayList<>();
    for (int i = 35; i < 35 + numOfMins; i++) {
      TimeSeriesDataCollectionRecord collectionRecord = TimeSeriesDataCollectionRecord.builder()
                                                            .accountId(accountId)
                                                            .cvConfigId(cvConfigId)
                                                            .timeStamp(TimeUnit.MINUTES.toMillis(i))
                                                            .metricValues(new HashSet<>())
                                                            .build();
      for (int j = 0; j < numOfMetrics; j++) {
        TimeSeriesDataRecordMetricValue metricValue = TimeSeriesDataRecordMetricValue.builder()
                                                          .metricName("metric-" + j)
                                                          .timeSeriesValues(new HashSet<>())
                                                          .build();
        for (int k = 0; k < numOfTxnx; k++) {
          metricValue.getTimeSeriesValues().add(
              TimeSeriesDataRecordGroupValue.builder().value(random.nextDouble()).groupName("group-" + k).build());
        }
        collectionRecord.getMetricValues().add(metricValue);
      }
      collectionRecords.add(collectionRecord);
    }
    timeSeriesService.save(collectionRecords);
    List<TimeSeriesRecord> timeSeriesRecords =
        hPersistence.createQuery(TimeSeriesRecord.class, excludeAuthority).asList();
    assertThat(timeSeriesRecords.size()).isEqualTo(numOfTimeBuckets * numOfMetrics);
    AtomicLong timeStamp = new AtomicLong(35);
    AtomicInteger recordNum = new AtomicInteger(0);
    AtomicInteger metricNum = new AtomicInteger(0);
    timeSeriesRecords.forEach(timeSeriesRecord -> {
      assertThat(timeSeriesRecord.getMetricName()).isEqualTo("metric-" + metricNum.get());
      assertThat(timeSeriesRecord.getBucketStartTime().toEpochMilli())
          .isEqualTo(TimeUnit.MINUTES.toMillis(timeStamp.get()));
      recordNum.incrementAndGet();
      metricNum.incrementAndGet();

      if (recordNum.get() % numOfMetrics == 0) {
        metricNum.set(0);
        timeStamp.addAndGet(CV_ANALYSIS_WINDOW_MINUTES);
      }
    });
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testGetTimeSeriesMetricDefinitions() {
    metricPackService.getMetricPacks(accountId, projectIdentifier, DataSourceType.APP_DYNAMICS);
    AppDynamicsCVConfig appDynamicsCVConfig = new AppDynamicsCVConfig();
    appDynamicsCVConfig.setProjectIdentifier(projectIdentifier);
    appDynamicsCVConfig.setAccountId(accountId);
    appDynamicsCVConfig.setMetricPack(MetricPack.builder()
                                          .identifier(CVNextGenConstants.APPD_PERFORMANCE_PACK_IDENTIFIER)
                                          .metrics(Sets.newHashSet(MetricPack.MetricDefinition.builder().build()))
                                          .build());
    AppDynamicsCVConfig cvConfig = (AppDynamicsCVConfig) cvConfigService.save(appDynamicsCVConfig);

    List<TimeSeriesMetricDefinition> timeSeriesMetricDefinitions =
        timeSeriesService.getTimeSeriesMetricDefinitions(cvConfig.getUuid());
    int sizeWithoutCustom = timeSeriesMetricDefinitions.size();
    timeSeriesMetricDefinitions.forEach(timeSeriesMetricDefinition -> {
      assertThat(timeSeriesMetricDefinition.getMetricName()).isNotEmpty();
      assertThat(timeSeriesMetricDefinition.getMetricType()).isNotNull();
      assertThat(timeSeriesMetricDefinition.getActionType()).isEqualTo(TimeSeriesThresholdActionType.IGNORE);
      assertThat(timeSeriesMetricDefinition.getMetricGroupName()).isEqualTo("*");
    });

    // add fail threshold and verify
    cvConfig.getMetricPack().getMetrics().add(
        MetricPack.MetricDefinition.builder()
            .name("m1")
            .type(TimeSeriesMetricType.ERROR)
            .thresholds(
                Lists.newArrayList(TimeSeriesThreshold.builder()
                                       .metricGroupName("t1")
                                       .action(TimeSeriesThresholdActionType.FAIL)
                                       .criteria(TimeSeriesThresholdCriteria.builder()
                                                     .criteria(" > 0.6")
                                                     .action(TimeSeriesCustomThresholdActions.FAIL_AFTER_OCCURRENCES)
                                                     .occurrenceCount(5)
                                                     .thresholdType(TimeSeriesThresholdType.ACT_WHEN_HIGHER)
                                                     .type(TimeSeriesThresholdComparisonType.RATIO)
                                                     .build())
                                       .build()))
            .build());
    cvConfigService.update(cvConfig);
    timeSeriesMetricDefinitions = timeSeriesService.getTimeSeriesMetricDefinitions(cvConfig.getUuid());
    assertThat(timeSeriesMetricDefinitions.size()).isEqualTo(sizeWithoutCustom + 1);
    TimeSeriesMetricDefinition m1Definition =
        timeSeriesMetricDefinitions.stream()
            .filter(timeSeriesMetricDefinition -> timeSeriesMetricDefinition.getMetricName().equals("m1"))
            .findFirst()
            .orElse(null);
    assertThat(m1Definition).isNotNull();
    assertThat(m1Definition.getMetricName()).isEqualTo("m1");
    assertThat(m1Definition.getMetricGroupName()).isEqualTo("t1");
    assertThat(m1Definition.getActionType()).isEqualTo(TimeSeriesThresholdActionType.FAIL);
    assertThat(m1Definition.getMetricType()).isEqualTo(TimeSeriesMetricType.ERROR);
    assertThat(m1Definition.getComparisonType()).isEqualTo(TimeSeriesThresholdComparisonType.RATIO);
    assertThat(m1Definition.getAction()).isEqualTo(TimeSeriesCustomThresholdActions.FAIL_AFTER_OCCURRENCES);
    assertThat(m1Definition.getOccurrenceCount()).isEqualTo(5);
    assertThat(m1Definition.getThresholdType()).isEqualTo(TimeSeriesThresholdType.ACT_WHEN_HIGHER);
    assertThat(m1Definition.getValue()).isEqualTo(0.6, offset(0.01));
  }
}
