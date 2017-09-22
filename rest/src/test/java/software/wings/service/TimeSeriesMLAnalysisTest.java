package software.wings.service;

import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;
import software.wings.WingsBaseTest;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.dl.WingsPersistence;
import software.wings.resources.NewRelicResource;
import software.wings.service.impl.analysis.TSRequest;
import software.wings.service.impl.analysis.TimeSeriesMLAnalysisRecord;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;
import software.wings.utils.JsonUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.inject.Inject;

/**
 * Created by sriram_parthasarathy on 10/16/17.
 */
@RunWith(MockitoJUnitRunner.class)
public class TimeSeriesMLAnalysisTest extends WingsBaseTest {
  private String accountId;
  private String appId;
  private String stateExecutionId;
  private String workflowId;
  private String workflowExecutionId;
  private String serviceId;
  private String delegateTaskId;

  @Inject private NewRelicResource newRelicResource;
  @Mock private DelegateProxyFactory delegateProxyFactory;
  @Inject private WingsPersistence wingsPersistence;

  @Before
  public void setup() throws IOException {
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
  public void testSaveAnalysis() throws IOException {
    InputStream is = getClass().getClassLoader().getResourceAsStream("verification/TimeSeriesNRAnalysisRecords.json");
    String jsonTxt = IOUtils.toString(is);
    TimeSeriesMLAnalysisRecord record = JsonUtils.asObject(jsonTxt, TimeSeriesMLAnalysisRecord.class);
    newRelicResource.saveMLAnalysisRecords(accountId, appId, stateExecutionId, workflowExecutionId, 0, record);
  }

  @Test
  public void testSaveMetricRecords() throws IOException {
    InputStream is = getClass().getClassLoader().getResourceAsStream("./verification/TimeSeriesNRControlInput.json");
    String jsonTxt = IOUtils.toString(is);
    List<NewRelicMetricDataRecord> controlRecords =
        JsonUtils.asList(jsonTxt, new TypeReference<List<NewRelicMetricDataRecord>>() {});
    Set<String> nodes = new HashSet<>();
    for (Iterator<NewRelicMetricDataRecord> it = controlRecords.iterator(); it.hasNext();) {
      NewRelicMetricDataRecord record = it.next();
      if (record.getDataCollectionMinute() != 0) {
        it.remove();
      }
      record.setApplicationId(appId);
      record.setWorkflowId(workflowId);
      record.setWorkflowExecutionId(workflowExecutionId);
      record.setStateExecutionId(stateExecutionId);
      record.setServiceId(serviceId);
      record.setTimeStamp(record.getDataCollectionMinute());
      nodes.add(record.getHost());
    }
    newRelicResource.saveMetricData(accountId, appId, controlRecords);
    List<NewRelicMetricDataRecord> results = newRelicResource
                                                 .getMetricData(accountId, true,
                                                     TSRequest.builder()
                                                         .applicationId(appId)
                                                         .workflowId(workflowId)
                                                         .workflowExecutionId(workflowExecutionId)
                                                         .stateExecutionId(stateExecutionId)
                                                         .serviceId(serviceId)
                                                         .analysisMinute(0)
                                                         .nodes(nodes)
                                                         .build())
                                                 .getResource();
    assertEquals(results.size(), controlRecords.size());
  }
}
