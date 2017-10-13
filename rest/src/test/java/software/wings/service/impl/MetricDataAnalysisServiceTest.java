package software.wings.service.impl;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.wings.WingsBaseTest;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.dl.WingsPersistence;
import software.wings.rules.RealMongo;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;
import software.wings.service.intfc.MetricDataAnalysisService;
import software.wings.service.intfc.analysis.ClusterLevel;
import software.wings.service.intfc.elk.ElkAnalysisService;
import software.wings.sm.StateType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import javax.inject.Inject;

/**
 * Created by rsingh on 10/13/17.
 */
public class MetricDataAnalysisServiceTest extends WingsBaseTest {
  private String accountId;
  private String appId;
  private String stateExecutionId;
  private String workflowId;
  private String workflowExecutionId;
  private String serviceId;
  private String delegateTaskId;
  private Random r;

  @Mock private DelegateProxyFactory delegateProxyFactory;
  @Inject private MetricDataAnalysisService analysisService;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private ElkAnalysisService elkAnalysisService;

  @Before
  public void setup() {
    long seed = System.currentTimeMillis();
    System.out.println("random seed: " + seed);
    r = new Random(seed);
    accountId = UUID.randomUUID().toString();
    appId = UUID.randomUUID().toString();
    stateExecutionId = UUID.randomUUID().toString();
    workflowId = UUID.randomUUID().toString();
    workflowExecutionId = UUID.randomUUID().toString();
    serviceId = UUID.randomUUID().toString();
    delegateTaskId = UUID.randomUUID().toString();
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @RealMongo
  public void saveMetricDataDuplicate() throws IOException {
    List<NewRelicMetricDataRecord> records = new ArrayList<>();
    String metricName = UUID.randomUUID().toString();
    String host = UUID.randomUUID().toString();
    long timeStamp = System.currentTimeMillis();
    int dataCollectionMinute = r.nextInt(100);
    records.add(NewRelicMetricDataRecord.builder()
                    .stateType(StateType.NEW_RELIC)
                    .name(metricName)
                    .host(host)
                    .timeStamp(timeStamp)
                    .workflowExecutionId(workflowExecutionId)
                    .stateExecutionId(stateExecutionId)
                    .serviceId(serviceId)
                    .workflowId(workflowId)
                    .level(ClusterLevel.H0)
                    .dataCollectionMinute(dataCollectionMinute)
                    .error(r.nextDouble())
                    .response95th(r.nextDouble())
                    .build());
    records.add(NewRelicMetricDataRecord.builder()
                    .stateType(StateType.NEW_RELIC)
                    .name(metricName)
                    .host(host)
                    .timeStamp(timeStamp)
                    .workflowExecutionId(workflowExecutionId)
                    .stateExecutionId(stateExecutionId)
                    .serviceId(serviceId)
                    .workflowId(workflowId)
                    .level(ClusterLevel.H0)
                    .dataCollectionMinute(dataCollectionMinute)
                    .apdexScore(r.nextDouble())
                    .stalls(r.nextDouble())
                    .build());

    assertEquals(records.get(0).getName(), records.get(1).getName());
    assertEquals(records.get(0).getHost(), records.get(1).getHost());
    assertEquals(records.get(0).getTimeStamp(), records.get(1).getTimeStamp());
    assertEquals(records.get(0).getWorkflowExecutionId(), records.get(1).getWorkflowExecutionId());
    assertEquals(records.get(0).getStateExecutionId(), records.get(1).getStateExecutionId());
    assertEquals(records.get(0).getServiceId(), records.get(1).getServiceId());
    assertEquals(records.get(0).getLevel(), records.get(1).getLevel());
    assertEquals(records.get(0).getStateType(), records.get(1).getStateType());
    analysisService.saveMetricData(accountId, appId, records);
    List<NewRelicMetricDataRecord> savedRecords = analysisService.getRecords(StateType.NEW_RELIC, workflowExecutionId,
        stateExecutionId, workflowId, serviceId, Collections.singleton(host), dataCollectionMinute);
    assertEquals(1, savedRecords.size());
    assertEquals(records.get(0), savedRecords.get(0));
  }
}
