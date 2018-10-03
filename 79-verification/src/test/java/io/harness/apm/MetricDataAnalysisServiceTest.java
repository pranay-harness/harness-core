package io.harness.apm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.google.inject.Inject;

import io.harness.VerificationBaseTest;
import io.harness.service.intfc.TimeSeriesAnalysisService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.analysis.TimeSeriesMLAnalysisRecord;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord;
import software.wings.sm.StateType;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Created by rsingh on 9/7/18.
 */
public class MetricDataAnalysisServiceTest extends VerificationBaseTest {
  private String accountId;
  private String appId;
  private String stateExecutionId;
  private String workflowId;
  private String workflowExecutionId;
  private String serviceId;
  private String groupName;
  private String delegateTaskId;
  private Integer analysisMinute;

  @Inject private WingsPersistence wingsPersistence;
  @Inject private TimeSeriesAnalysisService metricDataAnalysisService;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    accountId = UUID.randomUUID().toString();
    appId = UUID.randomUUID().toString();
    stateExecutionId = UUID.randomUUID().toString();
    workflowId = UUID.randomUUID().toString();
    workflowExecutionId = UUID.randomUUID().toString();
    serviceId = UUID.randomUUID().toString();
    groupName = "groupName-";
    delegateTaskId = UUID.randomUUID().toString();
    analysisMinute = 10;
  }

  @Test
  public void testSaveAnalysisRecordsML() {
    int numOfGroups = 5;
    int numOfMinutes = 10;
    for (int i = 0; i < numOfGroups; i++) {
      for (int j = 1; j <= numOfMinutes; j++) {
        TimeSeriesMLAnalysisRecord mlAnalysisResponse = TimeSeriesMLAnalysisRecord.builder().build();
        mlAnalysisResponse.setCreatedAt(j);
        mlAnalysisResponse.setTransactions(Collections.EMPTY_MAP);
        metricDataAnalysisService.saveAnalysisRecordsML(StateType.DYNA_TRACE, accountId, appId, stateExecutionId,
            workflowExecutionId, workflowId, serviceId, groupName + i, j, delegateTaskId, "-1", mlAnalysisResponse);
      }
    }

    assertEquals(numOfGroups * numOfMinutes,
        wingsPersistence.createQuery(TimeSeriesMLAnalysisRecord.class)
            .filter("stateExecutionId", stateExecutionId)
            .filter("appId", appId)
            .asList()
            .size());
    List<NewRelicMetricAnalysisRecord> resultList =
        metricDataAnalysisService.getMetricsAnalysis(appId, stateExecutionId, workflowExecutionId);

    assertNotNull(resultList);
    assertEquals(numOfGroups, resultList.size());
    resultList.forEach(record -> assertEquals(numOfMinutes, record.getAnalysisMinute()));
  }
}
