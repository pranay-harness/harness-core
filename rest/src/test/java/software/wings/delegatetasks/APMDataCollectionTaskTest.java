package software.wings.delegatetasks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static software.wings.beans.DelegateTask.Builder.aDelegateTask;
import static software.wings.service.impl.newrelic.NewRelicMetricDataRecord.DEFAULT_GROUP_NAME;

import com.google.common.collect.ImmutableMap;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTask;
import software.wings.beans.TaskType;
import software.wings.service.impl.analysis.DataCollectionTaskResult;
import software.wings.service.impl.apm.APMDataCollectionInfo;
import software.wings.sm.StateType;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class APMDataCollectionTaskTest {
  private static final Logger logger = LoggerFactory.getLogger(APMDataCollectionTaskTest.class);

  APMDataCollectionInfo dataCollectionInfo;
  private APMDataCollectionTask dataCollectionTask;

  private void setup() {
    String delegateId = UUID.randomUUID().toString();
    String appId = UUID.randomUUID().toString();
    String envId = UUID.randomUUID().toString();
    String waitId = UUID.randomUUID().toString();
    String accountId = UUID.randomUUID().toString();
    String infrastructureMappingId = UUID.randomUUID().toString();
    String timeDuration = "10";
    dataCollectionInfo = APMDataCollectionInfo.builder()
                             .startTime(12312321123L)
                             .stateType(StateType.APM_VERIFICATION)
                             .dataCollectionFrequency(2)
                             .hosts(ImmutableMap.<String, String>builder()
                                        .put("test.host.node1", DEFAULT_GROUP_NAME)
                                        .put("test.host.node2", DEFAULT_GROUP_NAME)
                                        .build())
                             .encryptedDataDetails(new ArrayList<>())
                             .dataCollectionMinute(0)
                             .build();

    DelegateTask task = aDelegateTask()
                            .withTaskType(TaskType.APM_METRIC_DATA_COLLECTION_TASK)
                            .withAccountId(accountId)
                            .withAppId(appId)
                            .withWaitId(waitId)
                            .withParameters(new Object[] {dataCollectionInfo})
                            .withEnvId(envId)
                            .withInfrastructureMappingId(infrastructureMappingId)
                            .withTimeout(TimeUnit.MINUTES.toMillis(Integer.parseInt(timeDuration) + 120))
                            .build();
    dataCollectionTask = new APMDataCollectionTask(delegateId, task, null, null);
  }
  private Method useReflectionToMakeInnerClassVisible() throws Exception {
    Class[] innerClasses = dataCollectionTask.getClass().getDeclaredClasses();
    logger.info("" + innerClasses);
    Class[] parameterTypes = new Class[1];
    parameterTypes[0] = java.lang.String.class;
    Method m = innerClasses[0].getDeclaredMethod("resolveBatchHosts", parameterTypes);
    m.setAccessible(true);
    return m;
  }
  @Test
  public void testBatchingHosts() throws Exception {
    setup();
    DataCollectionTaskResult tr = dataCollectionTask.initDataCollection(dataCollectionTask.getParameters());
    String batchUrl = "urlData{$harness_batch{pod_name:${host},'|'}}";
    List<String> batchedHosts =
        (List<String>) useReflectionToMakeInnerClassVisible().invoke(dataCollectionTask.getDataCollector(tr), batchUrl);
    assertEquals("batched hosts should have 1 item", 1, batchedHosts.size());
    assertEquals(
        "Batched string should be", "urlData{pod_name:test.host.node1|pod_name:test.host.node2}", batchedHosts.get(0));
  }

  @Test
  public void testMoreThanFiftyHostsInBatch() throws Exception {
    setup();
    Map<String, String> hostList = new HashMap<>();
    for (int i = 0; i < 52; i++) {
      hostList.put("test.host.node" + i, DEFAULT_GROUP_NAME);
    }

    dataCollectionInfo.setHosts(hostList);
    DataCollectionTaskResult tr = dataCollectionTask.initDataCollection(dataCollectionTask.getParameters());
    String batchUrl = "urlData{$harness_batch{pod_name:${host},'|'}}";
    List<String> batchedHosts =
        (List<String>) useReflectionToMakeInnerClassVisible().invoke(dataCollectionTask.getDataCollector(tr), batchUrl);
    assertEquals("batched hosts should have 2 items", 4, batchedHosts.size());
    // Since hostList in the CollectionTask class is a set, the order isn't maintained. So wecant compare directly.
    int occuranceCount1 = StringUtils.countMatches(batchedHosts.get(0), "test.host.node");
    int occuranceCount2 = StringUtils.countMatches(batchedHosts.get(1), "test.host.node");
    int occuranceCount3 = StringUtils.countMatches(batchedHosts.get(2), "test.host.node");
    int occuranceCount4 = StringUtils.countMatches(batchedHosts.get(3), "test.host.node");
    assertTrue("Firstbatch has 15 hosts", occuranceCount1 == 15);
    assertTrue("Second batch has 15 hosts", occuranceCount2 == 15);
    assertTrue("Third batch has 15 hosts", occuranceCount3 == 15);
    assertTrue("Fourth batch has 15 hosts", occuranceCount4 == 7);
  }

  @Test
  public void testEmptyEncryptedCredentialsInitDataCollection() {
    setup();
    APMDataCollectionInfo info = (APMDataCollectionInfo) dataCollectionTask.getParameters()[0];
    info.setEncryptedDataDetails(null);
    DataCollectionTaskResult tr = dataCollectionTask.initDataCollection(dataCollectionTask.getParameters());
  }
}
