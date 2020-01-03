package software.wings.delegatetasks.aws.ecs.ecstaskhandler;

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

import com.google.inject.Inject;

import com.amazonaws.services.ecs.model.DescribeServicesResult;
import com.amazonaws.services.ecs.model.Service;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.AwsConfig;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.cloudprovider.aws.EcsContainerService;
import software.wings.service.impl.AwsHelperService;

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
        AwsConfig.builder().build(), emptyList(), "us-east-1", "cluster", 1, "foo_1", mockCallback, 20);
    verify(mockEcsContainerService).updateServiceCount(any());
    verify(mockEcsContainerService).waitForTasksToBeInRunningStateButDontThrowException(any());
    verify(mockEcsContainerService).waitForServiceToReachSteadyState(eq(20), any());
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testDownsizeOlderService() {
    ExecutionLogCallback mockCallback = mock(ExecutionLogCallback.class);
    doNothing().when(mockCallback).saveExecutionLog(anyString());
    taskHelper.downsizeOlderService(
        AwsConfig.builder().build(), emptyList(), "us-east-1", "cluster", "foo_1", mockCallback);
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