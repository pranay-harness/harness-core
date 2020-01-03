package software.wings.delegatetasks;

import static io.harness.rule.OwnerRule.PRAVEEN;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.service.impl.newrelic.NewRelicMetricDataRecord.DEFAULT_GROUP_NAME;

import com.google.common.collect.ImmutableMap;

import io.harness.CategoryTest;
import io.harness.beans.DelegateTask;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.task.TaskParameters;
import io.harness.rule.Owner;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;
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

@Slf4j
public class APMDataCollectionTaskTest extends CategoryTest {
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

    DelegateTask task = DelegateTask.builder()
                            .async(true)
                            .accountId(accountId)
                            .appId(appId)
                            .waitId(waitId)
                            .data(TaskData.builder()
                                      .taskType(TaskType.APM_METRIC_DATA_COLLECTION_TASK.name())
                                      .parameters(new Object[] {dataCollectionInfo})
                                      .timeout(TimeUnit.MINUTES.toMillis(Integer.parseInt(timeDuration) + 120))
                                      .build())
                            .envId(envId)
                            .infrastructureMappingId(infrastructureMappingId)
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
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testBatchingHosts() throws Exception {
    setup();
    DataCollectionTaskResult tr =
        dataCollectionTask.initDataCollection((TaskParameters) dataCollectionTask.getParameters()[0]);
    String batchUrl = "urlData{$harness_batch{pod_name:${host},'|'}}";
    List<String> batchedHosts =
        (List<String>) useReflectionToMakeInnerClassVisible().invoke(dataCollectionTask.getDataCollector(tr), batchUrl);
    assertThat(batchedHosts).hasSize(1);
    assertThat(batchedHosts.get(0)).isEqualTo("urlData{pod_name:test.host.node1|pod_name:test.host.node2}");
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testMoreThanFiftyHostsInBatch() throws Exception {
    setup();
    Map<String, String> hostList = new HashMap<>();
    for (int i = 0; i < 52; i++) {
      hostList.put("test.host.node" + i, DEFAULT_GROUP_NAME);
    }

    dataCollectionInfo.setHosts(hostList);
    DataCollectionTaskResult tr =
        dataCollectionTask.initDataCollection((TaskParameters) dataCollectionTask.getParameters()[0]);
    String batchUrl = "urlData{$harness_batch{pod_name:${host},'|'}}";
    List<String> batchedHosts =
        (List<String>) useReflectionToMakeInnerClassVisible().invoke(dataCollectionTask.getDataCollector(tr), batchUrl);
    assertThat(batchedHosts).hasSize(4);
    // Since hostList in the CollectionTask class is a set, the order isn't maintained. So wecant compare directly.
    int occurrenceCount1 = StringUtils.countMatches(batchedHosts.get(0), "test.host.node");
    int occurrenceCount2 = StringUtils.countMatches(batchedHosts.get(1), "test.host.node");
    int occurrenceCount3 = StringUtils.countMatches(batchedHosts.get(2), "test.host.node");
    int occurrenceCount4 = StringUtils.countMatches(batchedHosts.get(3), "test.host.node");
    assertThat(occurrenceCount1 == 15).isTrue();
    assertThat(occurrenceCount2 == 15).isTrue();
    assertThat(occurrenceCount3 == 15).isTrue();
    assertThat(occurrenceCount4 == 7).isTrue();
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testEmptyEncryptedCredentialsInitDataCollection() {
    setup();
    APMDataCollectionInfo info = (APMDataCollectionInfo) dataCollectionTask.getParameters()[0];
    info.setEncryptedDataDetails(null);
    DataCollectionTaskResult tr =
        dataCollectionTask.initDataCollection((TaskParameters) dataCollectionTask.getParameters()[0]);
  }
}
