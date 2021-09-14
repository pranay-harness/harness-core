/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl;

import static io.harness.beans.FeatureName.PER_AGENT_CAPABILITIES;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.MARKO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.beans.DelegateTask;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.TaskData;
import io.harness.ff.FeatureFlagService;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.DelegateTaskBroadcast;

import com.google.inject.Inject;
import java.util.Collections;
import org.atmosphere.cpr.Broadcaster;
import org.atmosphere.cpr.BroadcasterFactory;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class DelegateTaskBroadcastHelperTest extends WingsBaseTest {
  @Mock private BroadcasterFactory broadcasterFactory;
  @Mock private FeatureFlagService featureFlagService;
  @InjectMocks @Inject private DelegateTaskBroadcastHelper broadcastHelper;

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testRebroadcastDelegateTaskWithNullTask() {
    broadcastHelper.rebroadcastDelegateTask(null);
    verify(featureFlagService, never()).isEnabled(eq(PER_AGENT_CAPABILITIES), any());
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testRebroadcastDelegateTask() {
    DelegateTask delegateTask = DelegateTask.builder()
                                    .version(generateUuid())
                                    .accountId(generateUuid())
                                    .uuid(generateUuid())
                                    .data(TaskData.builder().async(true).build())
                                    .alreadyTriedDelegates(Collections.singleton(generateUuid()))
                                    .build();

    // Test with FF enabled and no pre assigned delegate
    when(featureFlagService.isEnabled(PER_AGENT_CAPABILITIES, delegateTask.getAccountId())).thenReturn(true);

    broadcastHelper.rebroadcastDelegateTask(delegateTask);

    verify(featureFlagService).isEnabled(PER_AGENT_CAPABILITIES, delegateTask.getAccountId());
    verify(broadcasterFactory, never()).lookup(anyString(), eq(true));

    // Test with FF enabled and pre assigned delegate
    delegateTask.setPreAssignedDelegateId(generateUuid());
    Broadcaster broadcaster = mock(Broadcaster.class);
    when(broadcasterFactory.lookup(anyString(), eq(true))).thenReturn(broadcaster);

    broadcastHelper.rebroadcastDelegateTask(delegateTask);

    verify(featureFlagService, times(2)).isEnabled(PER_AGENT_CAPABILITIES, delegateTask.getAccountId());
    verify(broadcasterFactory).lookup(anyString(), eq(true));

    ArgumentCaptor<DelegateTaskBroadcast> argumentCaptor = ArgumentCaptor.forClass(DelegateTaskBroadcast.class);
    verify(broadcaster).broadcast(argumentCaptor.capture());

    DelegateTaskBroadcast delegateTaskBroadcast = argumentCaptor.getValue();
    assertThat(delegateTaskBroadcast).isNotNull();
    assertThat(delegateTaskBroadcast.getVersion()).isEqualTo(delegateTask.getVersion());
    assertThat(delegateTaskBroadcast.getAccountId()).isEqualTo(delegateTask.getAccountId());
    assertThat(delegateTaskBroadcast.getTaskId()).isEqualTo(delegateTask.getUuid());
    assertThat(delegateTaskBroadcast.isAsync()).isEqualTo(delegateTask.getData().isAsync());
    assertThat(delegateTaskBroadcast.getPreAssignedDelegateId()).isEqualTo(delegateTask.getPreAssignedDelegateId());
    assertThat(delegateTaskBroadcast.getAlreadyTriedDelegates()).isEqualTo(delegateTask.getAlreadyTriedDelegates());

    // Test with FF disabled
    when(featureFlagService.isEnabled(PER_AGENT_CAPABILITIES, delegateTask.getAccountId())).thenReturn(false);
    delegateTask.setPreAssignedDelegateId(null);
    delegateTask.setAlreadyTriedDelegates(null);

    broadcastHelper.rebroadcastDelegateTask(delegateTask);

    verify(featureFlagService, times(3)).isEnabled(PER_AGENT_CAPABILITIES, delegateTask.getAccountId());
    verify(broadcasterFactory, times(2)).lookup(anyString(), eq(true));

    argumentCaptor = ArgumentCaptor.forClass(DelegateTaskBroadcast.class);
    verify(broadcaster, times(2)).broadcast(argumentCaptor.capture());

    delegateTaskBroadcast = argumentCaptor.getValue();
    assertThat(delegateTaskBroadcast).isNotNull();
    assertThat(delegateTaskBroadcast.getVersion()).isEqualTo(delegateTask.getVersion());
    assertThat(delegateTaskBroadcast.getAccountId()).isEqualTo(delegateTask.getAccountId());
    assertThat(delegateTaskBroadcast.getTaskId()).isEqualTo(delegateTask.getUuid());
    assertThat(delegateTaskBroadcast.isAsync()).isEqualTo(delegateTask.getData().isAsync());
    assertThat(delegateTaskBroadcast.getPreAssignedDelegateId()).isEqualTo(delegateTask.getPreAssignedDelegateId());
    assertThat(delegateTaskBroadcast.getAlreadyTriedDelegates()).isEqualTo(delegateTask.getAlreadyTriedDelegates());
  }
}
