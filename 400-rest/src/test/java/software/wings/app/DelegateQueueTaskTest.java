package software.wings.app;

import static io.harness.beans.DelegateTask.Status.PARKED;
import static io.harness.beans.FeatureName.PER_AGENT_CAPABILITIES;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.GEORGE;
import static io.harness.rule.OwnerRule.MARKO;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.beans.DelegateTask;
import io.harness.beans.DelegateTask.DelegateTaskKeys;
import io.harness.beans.DelegateTask.Status;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.TaskData;
import io.harness.ff.FeatureFlagService;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;
import io.harness.version.VersionInfoManager;

import software.wings.WingsBaseTest;
import software.wings.beans.TaskType;
import software.wings.service.impl.DelegateTaskBroadcastHelper;
import software.wings.service.intfc.DelegateTaskServiceClassic;

import com.google.inject.Inject;
import java.util.Set;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class DelegateQueueTaskTest extends WingsBaseTest {
  @Mock private DelegateTaskBroadcastHelper broadcastHelper;
  @Mock private FeatureFlagService featureFlagService;
  @Mock private DelegateTaskServiceClassic delegateService;
  @InjectMocks @Inject DelegateQueueTask delegateQueueTask;

  @Inject HPersistence persistence;
  @Inject private VersionInfoManager versionInfoManager;

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testEndTasksWithCorruptedRecord() {
    DelegateTask delegateTask = DelegateTask.builder()
                                    .accountId("FOO")
                                    .expiry(System.currentTimeMillis() - 10)
                                    .data(TaskData.builder().timeout(1).build())
                                    .build();
    persistence.save(delegateTask);
    persistence.update(delegateTask,
        persistence.createUpdateOperations(DelegateTask.class)
            .set(DelegateTaskKeys.data_parameters, "dummy".toCharArray()));

    delegateQueueTask.endTasks(asList(delegateTask.getUuid()));

    assertThat(persistence.createQuery(DelegateTask.class).count()).isEqualTo(0);
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testEndTasksWithCorruptedRecord132() {
    DelegateTask delegateTask = DelegateTask.builder()
                                    .accountId("FOO")
                                    .status(PARKED)
                                    .expiry(System.currentTimeMillis() - 10)
                                    .data(TaskData.builder().timeout(1).build())
                                    .build();
    persistence.save(delegateTask);

    delegateQueueTask.run();
    assertThat(persistence.createQuery(DelegateTask.class).count()).isEqualTo(0);
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testRebroadcastUnassignedTasksWithCapabilitiesFFDisabled() {
    String accountId = generateUuid();
    long nextBroadcastTime = System.currentTimeMillis() + 120000;

    DelegateTask delegateTask = DelegateTask.builder()
                                    .accountId(accountId)
                                    .version(versionInfoManager.getVersionInfo().getVersion())
                                    .status(Status.QUEUED)
                                    .expiry(System.currentTimeMillis() + 60000)
                                    .data(TaskData.builder().taskType(TaskType.HTTP.name()).build())
                                    .build();
    delegateTask.setBroadcastCount(1);
    delegateTask.setNextBroadcast(System.currentTimeMillis() - 10000);
    delegateTask.setPreAssignedDelegateId("delegateId");
    persistence.save(delegateTask);

    when(featureFlagService.isEnabled(PER_AGENT_CAPABILITIES, accountId)).thenReturn(false);
    when(featureFlagService.isNotEnabled(PER_AGENT_CAPABILITIES, accountId)).thenReturn(true);
    when(broadcastHelper.findNextBroadcastTimeForTask(any(DelegateTask.class))).thenReturn(nextBroadcastTime);

    delegateQueueTask.rebroadcastUnassignedTasks();

    ArgumentCaptor<DelegateTask> argumentCaptor = ArgumentCaptor.forClass(DelegateTask.class);
    verify(broadcastHelper).rebroadcastDelegateTask(argumentCaptor.capture());

    DelegateTask broadcastedDelegateTask = argumentCaptor.getValue();
    assertThat(broadcastedDelegateTask).isNotNull();
    assertThat(broadcastedDelegateTask.getBroadcastCount()).isEqualTo(2);
    assertThat(broadcastedDelegateTask.getNextBroadcast()).isGreaterThan(System.currentTimeMillis());
    assertThat(broadcastedDelegateTask.getPreAssignedDelegateId()).isNull();
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testRebroadcastUnassignedTasksWithCapabilitiesFFEnabled() {
    String accountId = generateUuid();
    String firstDelegateId = generateUuid();
    String secondDelegateId = generateUuid();

    long nextBroadcastTime = System.currentTimeMillis() + 120000;

    DelegateTask delegateTask = DelegateTask.builder()
                                    .accountId(accountId)
                                    .version(versionInfoManager.getVersionInfo().getVersion())
                                    .status(Status.QUEUED)
                                    .expiry(System.currentTimeMillis() + 60000)
                                    .data(TaskData.builder().taskType(TaskType.HTTP.name()).build())
                                    .build();
    delegateTask.setBroadcastCount(1);
    delegateTask.setNextBroadcast(System.currentTimeMillis() - 10000);
    delegateTask.setPreAssignedDelegateId(firstDelegateId);
    persistence.save(delegateTask);

    when(featureFlagService.isEnabled(PER_AGENT_CAPABILITIES, accountId)).thenReturn(true);
    when(featureFlagService.isNotEnabled(PER_AGENT_CAPABILITIES, accountId)).thenReturn(false);
    when(broadcastHelper.findNextBroadcastTimeForTask(any(DelegateTask.class))).thenReturn(nextBroadcastTime);
    when(delegateService.obtainCapableDelegateId(any(DelegateTask.class), any(Set.class))).thenReturn(secondDelegateId);

    delegateQueueTask.rebroadcastUnassignedTasks();

    ArgumentCaptor<DelegateTask> argumentCaptor = ArgumentCaptor.forClass(DelegateTask.class);
    verify(broadcastHelper).rebroadcastDelegateTask(argumentCaptor.capture());

    DelegateTask broadcastedDelegateTask = argumentCaptor.getValue();
    assertThat(broadcastedDelegateTask).isNotNull();
    assertThat(broadcastedDelegateTask.getBroadcastCount()).isEqualTo(2);
    assertThat(broadcastedDelegateTask.getNextBroadcast()).isGreaterThan(System.currentTimeMillis());
    assertThat(broadcastedDelegateTask.getPreAssignedDelegateId()).isEqualTo(secondDelegateId);
    assertThat(broadcastedDelegateTask.getAlreadyTriedDelegates()).hasSize(1);
    assertThat(broadcastedDelegateTask.getAlreadyTriedDelegates()).containsExactly(firstDelegateId);
  }
}
