package io.harness.service;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.persistence.HQuery.excludeAuthority;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.google.common.collect.TreeBasedTable;
import com.google.inject.Inject;

import io.harness.VerificationBaseTest;
import io.harness.category.element.UnitTests;
import io.harness.service.intfc.TimeSeriesAnalysisService;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.dl.WingsPersistence;
import software.wings.metrics.TimeSeriesDataRecord;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;
import software.wings.sm.StateType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

@Slf4j
public class TimeSeriesAnalysisServiceTest extends VerificationBaseTest {
  private String cvConfigId;
  private String serviceId;
  private Random randomizer;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private TimeSeriesAnalysisService timeSeriesAnalysisService;

  @Before
  public void setup() {
    long seed = System.currentTimeMillis();
    logger.info("seed: {}", seed);
    randomizer = new Random(seed);
    cvConfigId = generateUuid();
    serviceId = generateUuid();
  }

  @Test
  @Category(UnitTests.class)
  public void testGetCVMetricRecords() {
    int numOfHosts = 5;
    int numOfTxns = 40;
    int numOfMinutes = 200;

    List<String> hosts = new ArrayList<>();
    for (int i = 0; i < numOfHosts; i++) {
      hosts.add("host-" + i);
    }

    List<String> txns = new ArrayList<>();
    for (int i = 0; i < numOfTxns; i++) {
      txns.add("txn-" + i);
    }

    hosts.forEach(host -> txns.forEach(txn -> {
      for (int k = 0; k < numOfMinutes; k++) {
        wingsPersistence.save(NewRelicMetricDataRecord.builder()
                                  .cvConfigId(cvConfigId)
                                  .serviceId(serviceId)
                                  .stateType(StateType.NEW_RELIC)
                                  .name(txn)
                                  .timeStamp(k * 1000)
                                  .dataCollectionMinute(k)
                                  .host(host)
                                  .build());
      }
    }));

    assertEquals(numOfHosts * numOfTxns * numOfMinutes,
        wingsPersistence.createQuery(NewRelicMetricDataRecord.class, excludeAuthority).asList().size());

    int analysisStartMinute = randomizer.nextInt(100);
    int analysisEndMinute = analysisStartMinute + randomizer.nextInt(102);
    logger.info("start {} end {}", analysisStartMinute, analysisEndMinute);
    final Set<NewRelicMetricDataRecord> metricRecords =
        timeSeriesAnalysisService.getMetricRecords(cvConfigId, analysisStartMinute, analysisEndMinute, null);
    int numOfMinutesAsked = analysisEndMinute - analysisStartMinute + 1;
    assertEquals("failed for start " + analysisStartMinute + " end " + analysisEndMinute,
        numOfMinutesAsked * numOfTxns * numOfHosts, metricRecords.size());

    metricRecords.forEach(metricRecord -> metricRecord.setUuid(null));
    Set<NewRelicMetricDataRecord> expectedRecords = new HashSet<>();
    hosts.forEach(host -> txns.forEach(txn -> {
      for (int k = analysisStartMinute; k <= analysisEndMinute; k++) {
        expectedRecords.add(NewRelicMetricDataRecord.builder()
                                .cvConfigId(cvConfigId)
                                .serviceId(serviceId)
                                .stateType(StateType.NEW_RELIC)
                                .name(txn)
                                .timeStamp(k * 1000)
                                .dataCollectionMinute(k)
                                .host(host)
                                .build());
      }
    }));
    assertEquals(expectedRecords, metricRecords);
  }

  @Test
  @Category(UnitTests.class)
  public void testCompressTimeSeriesMetricRecords() {
    int numOfTxns = 5;
    int numOfMetrics = 40;

    TreeBasedTable<String, String, Double> values = TreeBasedTable.create();
    TreeBasedTable<String, String, String> deeplinkMetadata = TreeBasedTable.create();

    List<String> txns = new ArrayList<>();
    for (int i = 0; i < numOfTxns; i++) {
      for (int j = 0; j < numOfMetrics; j++) {
        values.put("txn-" + i, "metric-" + j, randomizer.nextDouble());
        deeplinkMetadata.put("txn-" + i, "metric-" + j, generateUuid());
      }
    }

    final TimeSeriesDataRecord timeSeriesDataRecord = TimeSeriesDataRecord.builder()
                                                          .cvConfigId(cvConfigId)
                                                          .serviceId(serviceId)
                                                          .stateType(StateType.NEW_RELIC)
                                                          .timeStamp(1000)
                                                          .dataCollectionMinute(100)
                                                          .host(generateUuid())
                                                          .values(values)
                                                          .deeplinkMetadata(deeplinkMetadata)
                                                          .build();

    timeSeriesDataRecord.compress();

    final String recordId = wingsPersistence.save(timeSeriesDataRecord);

    TimeSeriesDataRecord savedRecord = wingsPersistence.get(TimeSeriesDataRecord.class, recordId);

    assertThat(savedRecord.getValues()).isNull();
    assertThat(savedRecord.getDeeplinkMetadata()).isNull();
    assertNotNull(savedRecord.getValuesBytes());

    savedRecord.decompress();

    assertNotNull(savedRecord.getValues());
    assertNotNull(savedRecord.getDeeplinkMetadata());
    assertThat(savedRecord.getValuesBytes()).isNull();

    assertEquals(values, savedRecord.getValues());
    assertEquals(deeplinkMetadata, savedRecord.getDeeplinkMetadata());
  }
}
