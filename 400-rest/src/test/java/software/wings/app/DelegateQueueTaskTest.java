/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.app;

import static io.harness.beans.DelegateTask.Status.PARKED;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.GEORGE;
import static io.harness.rule.OwnerRule.HARSH;
import static io.harness.rule.OwnerRule.JENNY;

import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.DELEGATE_ID;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
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
import software.wings.beans.DelegateTaskBroadcast;
import software.wings.beans.TaskType;
import software.wings.service.impl.DelegateTaskBroadcastHelper;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.DelegateTaskServiceClassic;

import com.google.inject.Inject;
import java.util.Arrays;
import java.util.LinkedList;
import org.atmosphere.cpr.Broadcaster;
import org.atmosphere.cpr.BroadcasterFactory;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class DelegateQueueTaskTest extends WingsBaseTest {
  @InjectMocks @Inject private DelegateTaskBroadcastHelper broadcastHelper;
  @Mock private FeatureFlagService featureFlagService;
  @Mock private DelegateTaskServiceClassic delegateTaskServiceClassic;
  @Mock private DelegateService delegateService;
  @InjectMocks @Inject DelegateQueueTask delegateQueueTask;
  @Inject private BroadcasterFactory broadcasterFactory;

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
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void testEndTasksWithAnortedRecord() {
    DelegateTask delegateTask = DelegateTask.builder()
                                    .accountId("FOO")
                                    .status(Status.ABORTED)
                                    .expiry(System.currentTimeMillis() - 10)
                                    .data(TaskData.builder().timeout(1).build())
                                    .build();
    persistence.save(delegateTask);

    delegateQueueTask.run();
    assertThat(persistence.createQuery(DelegateTask.class).count()).isEqualTo(0);
  }

  @Test
  @Owner(developers = JENNY)
  @Category(UnitTests.class)
  public void testRebroadcastUnassignedTasks_sync() {
    DelegateTask delegateTask = DelegateTask.builder()
                                    .accountId(ACCOUNT_ID)
                                    .version(versionInfoManager.getVersionInfo().getVersion())
                                    .status(Status.QUEUED)
                                    .expiry(System.currentTimeMillis() + 60000)
                                    .data(TaskData.builder().taskType(TaskType.HTTP.name()).async(false).build())
                                    .build();
    Broadcaster broadcaster = mock(Broadcaster.class);
    when(broadcasterFactory.lookup(anyString(), eq(true))).thenReturn(broadcaster);
    delegateTask.setBroadcastCount(0);
    delegateTask.setNextBroadcast(System.currentTimeMillis());
    delegateTask.setEligibleToExecuteDelegateIds(new LinkedList<>(Arrays.asList(DELEGATE_ID)));
    persistence.save(delegateTask);
    when(delegateService.checkDelegateConnected(anyString(), anyString())).thenReturn(true);
    delegateQueueTask.rebroadcastUnassignedTasks();

    ArgumentCaptor<DelegateTaskBroadcast> argumentCaptor = ArgumentCaptor.forClass(DelegateTaskBroadcast.class);
    verify(broadcaster, times(1)).broadcast(argumentCaptor.capture());

    DelegateTaskBroadcast delegateTaskBroadcast = argumentCaptor.getValue();
    assertThat(delegateTaskBroadcast).isNotNull();
    assertThat(delegateTaskBroadcast.getVersion()).isEqualTo(delegateTask.getVersion());
    assertThat(delegateTaskBroadcast.getAccountId()).isEqualTo(delegateTask.getAccountId());
    assertThat(delegateTaskBroadcast.getTaskId()).isEqualTo(delegateTask.getUuid());
    assertThat(delegateTaskBroadcast.getBroadcastToDelegatesIds()).isNotEmpty();
  }

  @Test
  @Owner(developers = JENNY)
  @Category(UnitTests.class)
  public void testRebroadcastUnassignedTasks_async() {
    DelegateTask delegateTask = DelegateTask.builder()
                                    .accountId(ACCOUNT_ID)
                                    .version(versionInfoManager.getVersionInfo().getVersion())
                                    .status(Status.QUEUED)
                                    .expiry(System.currentTimeMillis() + 60000)
                                    .data(TaskData.builder().taskType(TaskType.HTTP.name()).async(true).build())
                                    .build();
    Broadcaster broadcaster = mock(Broadcaster.class);
    when(broadcasterFactory.lookup(anyString(), eq(true))).thenReturn(broadcaster);
    delegateTask.setBroadcastCount(0);
    delegateTask.setNextBroadcast(System.currentTimeMillis());
    delegateTask.setEligibleToExecuteDelegateIds(new LinkedList<>(Arrays.asList(DELEGATE_ID)));
    persistence.save(delegateTask);
    when(delegateService.checkDelegateConnected(anyString(), anyString())).thenReturn(true);
    delegateQueueTask.rebroadcastUnassignedTasks();

    ArgumentCaptor<DelegateTaskBroadcast> argumentCaptor = ArgumentCaptor.forClass(DelegateTaskBroadcast.class);
    verify(broadcaster, times(1)).broadcast(argumentCaptor.capture());

    DelegateTaskBroadcast delegateTaskBroadcast = argumentCaptor.getValue();
    assertThat(delegateTaskBroadcast).isNotNull();
    assertThat(delegateTaskBroadcast.getVersion()).isEqualTo(delegateTask.getVersion());
    assertThat(delegateTaskBroadcast.getAccountId()).isEqualTo(delegateTask.getAccountId());
    assertThat(delegateTaskBroadcast.getTaskId()).isEqualTo(delegateTask.getUuid());
    assertThat(delegateTaskBroadcast.getBroadcastToDelegatesIds()).isNotEmpty();
  }

  @Test
  @Owner(developers = JENNY)
  @Category(UnitTests.class)
  public void testRebroadcastUnassignedTaskWhenNoEligibleDelegates() {
    String accountId = generateUuid();
    long nextBroadcastTime = System.currentTimeMillis() + 120000;

    DelegateTask delegateTask = DelegateTask.builder()
                                    .accountId(accountId)
                                    .version(versionInfoManager.getVersionInfo().getVersion())
                                    .status(Status.QUEUED)
                                    .expiry(System.currentTimeMillis() + 60000)
                                    .data(TaskData.builder().async(false).build())
                                    .build();
    Broadcaster broadcaster = mock(Broadcaster.class);
    delegateTask.setBroadcastCount(0);
    delegateTask.setNextBroadcast(System.currentTimeMillis());
    persistence.save(delegateTask);

    delegateQueueTask.rebroadcastUnassignedTasks();
    ArgumentCaptor<DelegateTaskBroadcast> argumentCaptor = ArgumentCaptor.forClass(DelegateTaskBroadcast.class);
    verify(broadcaster, times(0)).broadcast(argumentCaptor.capture());
  }

  @Test
  @Owner(developers = JENNY)
  @Category(UnitTests.class)
  public void testRebroadcastUnassignedTasksToNonActiveDelegates() {
    String accountId = generateUuid();
    DelegateTask delegateTask = DelegateTask.builder()
                                    .accountId(accountId)
                                    .version(versionInfoManager.getVersionInfo().getVersion())
                                    .status(Status.QUEUED)
                                    .expiry(System.currentTimeMillis() + 60000)
                                    .data(TaskData.builder().async(false).build())
                                    .build();
    Broadcaster broadcaster = mock(Broadcaster.class);
    delegateTask.setBroadcastCount(0);
    delegateTask.setNextBroadcast(System.currentTimeMillis());
    persistence.save(delegateTask);

    delegateQueueTask.rebroadcastUnassignedTasks();
    ArgumentCaptor<DelegateTaskBroadcast> argumentCaptor = ArgumentCaptor.forClass(DelegateTaskBroadcast.class);
    verify(broadcaster, times(0)).broadcast(argumentCaptor.capture());
  }
}
