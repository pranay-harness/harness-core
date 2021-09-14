/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.notification.orchestration.handlers;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.rule.OwnerRule.NAMAN;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.engine.observers.beans.OrchestrationStartInfo;
import io.harness.notification.PipelineEventType;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.notification.NotificationHelper;
import io.harness.rule.Owner;

import java.util.concurrent.ExecutorService;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(PIPELINE)
public class PipelineStartNotificationHandlerTest extends CategoryTest {
  @Mock ExecutorService executorService;
  @Mock NotificationHelper notificationHelper;
  @InjectMocks PipelineStartNotificationHandler pipelineStartNotificationHandler;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testOnStart() {
    Ambiance ambiance = Ambiance.getDefaultInstance();
    OrchestrationStartInfo orchestrationStartInfo = OrchestrationStartInfo.builder().ambiance(ambiance).build();
    pipelineStartNotificationHandler.onStart(orchestrationStartInfo);
    verify(notificationHelper, times(1)).sendNotification(ambiance, PipelineEventType.PIPELINE_START, null, null);
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testGetInformExecutorService() {
    assertThat(pipelineStartNotificationHandler.getInformExecutorService()).isEqualTo(executorService);
  }
}
