/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.delegatetasks.aws.ecs.ecstaskhandler;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.rule.OwnerRule.SATYAM;

import static java.util.Collections.emptyList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.AwsConfig;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.cloudprovider.aws.EcsContainerService;
import software.wings.service.impl.AwsHelperService;

import com.amazonaws.services.ecs.model.DescribeServicesResult;
import com.amazonaws.services.ecs.model.Service;
import com.google.inject.Inject;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@TargetModule(HarnessModule._930_DELEGATE_TASKS)
@OwnedBy(CDP)
public class EcsSwapRoutesCommandTaskHelperTest extends WingsBaseTest {
  @Mock private AwsHelperService mockAwsHelperService;
  @Mock private EcsContainerService mockEcsContainerService;

  @InjectMocks @Inject private EcsSwapRoutesCommandTaskHelper taskHelper;

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testUpsizeOlderService() {
    ExecutionLogCallback mockCallback = mock(ExecutionLogCallback.class);
    doNothing().when(mockCallback).saveExecutionLog(anyString());
    doReturn(new DescribeServicesResult().withServices(new Service().withDesiredCount(0)))
        .when(mockAwsHelperService)
        .describeServices(anyString(), any(), anyList(), any());
    taskHelper.upsizeOlderService(
        AwsConfig.builder().build(), emptyList(), "us-east-1", "cluster", 1, "foo_1", mockCallback, 20, false);
    verify(mockEcsContainerService).updateServiceCount(any());
    verify(mockEcsContainerService).waitForTasksToBeInRunningStateWithHandledExceptions(any());
    verify(mockEcsContainerService).waitForServiceToReachSteadyState(eq(20), any());
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testDownsizeOlderService() {
    ExecutionLogCallback mockCallback = mock(ExecutionLogCallback.class);
    doNothing().when(mockCallback).saveExecutionLog(anyString());
    taskHelper.downsizeOlderService(
        AwsConfig.builder().build(), emptyList(), "us-east-1", "cluster", "foo_1", mockCallback, 1);
    verify(mockEcsContainerService).updateServiceCount(any());
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testUpdateServiceTags() {
    ExecutionLogCallback mockCallback = mock(ExecutionLogCallback.class);
    doNothing().when(mockCallback).saveExecutionLog(anyString());
    doReturn(new DescribeServicesResult().withServices(new Service().withServiceArn("arn_2")))
        .doReturn(new DescribeServicesResult().withServices(new Service().withServiceArn("arn_1")))
        .when(mockAwsHelperService)
        .describeServices(anyString(), any(), anyList(), any());
    taskHelper.updateServiceTags(
        AwsConfig.builder().build(), emptyList(), "us-east-1", "cluster", "foo_2", "foo_1", false, mockCallback);
    verify(mockAwsHelperService, times(2)).untagService(anyString(), anyList(), any(), any());
    verify(mockAwsHelperService, times(2)).tagService(anyString(), anyList(), any(), any());
  }
}
