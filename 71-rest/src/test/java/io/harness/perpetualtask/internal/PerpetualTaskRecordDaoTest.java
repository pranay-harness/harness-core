package io.harness.perpetualtask.internal;

import static io.harness.rule.OwnerRule.HITESH;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.perpetualtask.PerpetualTaskClientContext;
import io.harness.perpetualtask.PerpetualTaskType;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import software.wings.WingsBaseTest;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class PerpetualTaskRecordDaoTest extends WingsBaseTest {
  @InjectMocks @Inject private PerpetualTaskRecordDao perpetualTaskRecordDao;

  private final String CLUSTER_ID = "clusterId";
  private final String CLUSTER_NAME = "clusterName";
  private final String ACCOUNT_ID = "test-account-id";
  private final String DELEGATE_ID = "test-delegate-id1";
  private final String CLOUD_PROVIDER_ID = "cloudProviderId";
  private final long HEARTBEAT_MILLIS = Instant.now().toEpochMilli();

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testSetDelegateId() {
    String taskId = perpetualTaskRecordDao.save(PerpetualTaskRecord.builder().accountId(ACCOUNT_ID).build());
    perpetualTaskRecordDao.setDelegateId(taskId, DELEGATE_ID);
    PerpetualTaskRecord task = perpetualTaskRecordDao.getTask(taskId);
    assertThat(task).isNotNull();
    assertThat(task.getDelegateId()).isEqualTo(DELEGATE_ID);
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testResetDelegateIdForTask() {
    String taskId = perpetualTaskRecordDao.save(getPerpetualTaskRecord());
    perpetualTaskRecordDao.resetDelegateIdForTask(ACCOUNT_ID, taskId);
    PerpetualTaskRecord task = perpetualTaskRecordDao.getTask(taskId);
    assertThat(task).isNotNull();
    assertThat(task.getDelegateId()).isEqualTo("");
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testRemovePerpetualTask() {
    String taskId = perpetualTaskRecordDao.save(getPerpetualTaskRecord());
    perpetualTaskRecordDao.remove(ACCOUNT_ID, taskId);
    PerpetualTaskRecord task = perpetualTaskRecordDao.getTask(taskId);
    assertThat(task).isNull();
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testSaveHeartbeat() {
    String taskId = perpetualTaskRecordDao.save(getPerpetualTaskRecord());
    PerpetualTaskRecord task = perpetualTaskRecordDao.getTask(taskId);
    boolean saveHeartbeat = perpetualTaskRecordDao.saveHeartbeat(task, HEARTBEAT_MILLIS);
    assertThat(saveHeartbeat).isTrue();
    PerpetualTaskRecord perpetualTaskRecord = perpetualTaskRecordDao.getTask(taskId);
    assertThat(perpetualTaskRecord).isNotNull();
    assertThat(perpetualTaskRecord.getLastHeartbeat()).isEqualTo(HEARTBEAT_MILLIS);
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testListAssignedTaskIds() {
    String taskIdOne = perpetualTaskRecordDao.save(getPerpetualTaskRecord());
    String taskIdTwo = perpetualTaskRecordDao.save(getPerpetualTaskRecord());
    List<String> taskIds = perpetualTaskRecordDao.listAssignedTaskIds(DELEGATE_ID, ACCOUNT_ID);
    assertThat(taskIds).hasSize(2);
    assertThat(taskIds).containsExactlyInAnyOrder(taskIdOne, taskIdTwo);
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testGetExistingPerpetualTask() {
    PerpetualTaskClientContext clientContext = getClientContext();
    PerpetualTaskRecord perpetualTaskRecord = getPerpetualTaskRecord();
    String taskId = perpetualTaskRecordDao.save(perpetualTaskRecord);
    Optional<PerpetualTaskRecord> existingPerpetualTask =
        perpetualTaskRecordDao.getExistingPerpetualTask(ACCOUNT_ID, PerpetualTaskType.K8S_WATCH, clientContext);
    PerpetualTaskRecord savedPerpetualTaskRecord = existingPerpetualTask.get();
    assertThat(savedPerpetualTaskRecord).isNotNull();
    assertThat(savedPerpetualTaskRecord.getClientContext()).isEqualTo(getClientContext());
  }

  public PerpetualTaskClientContext getClientContext() {
    Map<String, String> clientParamMap = new HashMap<>();
    clientParamMap.put(CLOUD_PROVIDER_ID, CLOUD_PROVIDER_ID);
    clientParamMap.put(CLUSTER_ID, CLUSTER_ID);
    clientParamMap.put(CLUSTER_NAME, CLUSTER_NAME);
    return new PerpetualTaskClientContext(clientParamMap);
  }

  public PerpetualTaskRecord getPerpetualTaskRecord() {
    return PerpetualTaskRecord.builder()
        .accountId(ACCOUNT_ID)
        .perpetualTaskType(PerpetualTaskType.K8S_WATCH)
        .clientContext(getClientContext())
        .delegateId(DELEGATE_ID)
        .build();
  }
}
