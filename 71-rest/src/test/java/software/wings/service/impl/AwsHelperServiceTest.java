package software.wings.service.impl;

import static io.harness.rule.OwnerRule.BRETT;
import static io.harness.rule.OwnerRule.RUSHABH;
import static io.harness.rule.OwnerRule.SATYAM;
import static org.assertj.core.api.Assertions.assertThat;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.model.Activity;
import com.amazonaws.services.autoscaling.model.DescribeScalingActivitiesRequest;
import com.amazonaws.services.autoscaling.model.DescribeScalingActivitiesResult;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.CreateStackRequest;
import com.amazonaws.services.cloudformation.model.DeleteStackRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackEventsRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackEventsResult;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksResult;
import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.cloudformation.model.StackEvent;
import com.amazonaws.services.cloudformation.model.UpdateStackRequest;
import io.harness.aws.AwsCallTracker;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import software.wings.WingsBaseTest;
import software.wings.beans.AwsConfig;
import software.wings.beans.command.LogCallback;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AwsHelperServiceTest extends WingsBaseTest {
  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldGetInstanceId() {
    AwsHelperService awsHelperService = new AwsHelperService();
    assertThat(awsHelperService.getHostnameFromPrivateDnsName("ip-172-31-18-241.ec2.internal"))
        .isEqualTo("ip-172-31-18-241");
    assertThat(awsHelperService.getHostnameFromPrivateDnsName("ip-172-31-18-241.us-west-2.compute.internal"))
        .isEqualTo("ip-172-31-18-241");
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void shouldUpdateStack() {
    String accessKey = "abcd";
    char[] secretKey = "pqrs".toCharArray();
    String stackName = "Stack Name";
    String region = "us-west-1";
    UpdateStackRequest request = new UpdateStackRequest().withStackName(stackName);
    AmazonCloudFormationClient mockClient = mock(AmazonCloudFormationClient.class);
    AwsHelperService service = spy(new AwsHelperService());
    doReturn(mockClient).when(service).getAmazonCloudFormationClient(any(), any());
    AwsCallTracker mockTracker = mock(AwsCallTracker.class);
    doNothing().when(mockTracker).trackCFCall(anyString());
    on(service).set("tracker", mockTracker);
    service.updateStack(region, request, AwsConfig.builder().accessKey(accessKey).secretKey(secretKey).build());
    verify(mockClient).updateStack(request);
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void shouldDeleteStack() {
    String accessKey = "abcd";
    char[] secretKey = "pqrs".toCharArray();
    String stackName = "Stack Name";
    String region = "us-west-1";
    DeleteStackRequest request = new DeleteStackRequest().withStackName(stackName);
    AmazonCloudFormationClient mockClient = mock(AmazonCloudFormationClient.class);
    AwsHelperService service = spy(new AwsHelperService());
    doReturn(mockClient).when(service).getAmazonCloudFormationClient(any(), any());
    AwsCallTracker mockTracker = mock(AwsCallTracker.class);
    doNothing().when(mockTracker).trackCFCall(anyString());
    on(service).set("tracker", mockTracker);
    service.deleteStack(region, request, AwsConfig.builder().accessKey(accessKey).secretKey(secretKey).build());
    verify(mockClient).deleteStack(request);
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void shouldDescribeStack() {
    String accessKey = "qwer";
    char[] secretKey = "pqrs".toCharArray();
    String stackName = "Stack Name";
    String region = "us-west-1";
    DescribeStacksRequest request = new DescribeStacksRequest().withStackName(stackName);
    AmazonCloudFormationClient mockClient = mock(AmazonCloudFormationClient.class);
    AwsHelperService service = spy(new AwsHelperService());
    doReturn(mockClient).when(service).getAmazonCloudFormationClient(any(), any());
    DescribeStacksResult result = new DescribeStacksResult().withStacks(new Stack().withStackName(stackName));
    doReturn(result).when(mockClient).describeStacks(request);
    AwsCallTracker mockTracker = mock(AwsCallTracker.class);
    doNothing().when(mockTracker).trackCFCall(anyString());
    on(service).set("tracker", mockTracker);
    DescribeStacksResult actual =
        service.describeStacks(region, request, AwsConfig.builder().accessKey(accessKey).secretKey(secretKey).build());
    assertThat(actual).isNotNull();
    assertThat(actual.getStacks().size()).isEqualTo(1);
    assertThat(actual.getStacks().get(0).getStackName()).isEqualTo(stackName);
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void shouldGetAllEvents() {
    String accessKey = "qwer";
    char[] secretKey = "pqrs".toCharArray();
    String stackName = "Stack Name";
    String region = "us-west-1";
    DescribeStackEventsRequest request = new DescribeStackEventsRequest().withStackName(stackName);
    AmazonCloudFormationClient mockClient = mock(AmazonCloudFormationClient.class);
    AwsHelperService service = spy(new AwsHelperService());
    doReturn(mockClient).when(service).getAmazonCloudFormationClient(any(), any());
    DescribeStackEventsResult result =
        new DescribeStackEventsResult().withStackEvents(new StackEvent().withStackName(stackName).withEventId("id"));
    doReturn(result).when(mockClient).describeStackEvents(request);
    AwsCallTracker mockTracker = mock(AwsCallTracker.class);
    doNothing().when(mockTracker).trackCFCall(anyString());
    on(service).set("tracker", mockTracker);
    List<StackEvent> events = service.getAllStackEvents(
        region, request, AwsConfig.builder().accessKey(accessKey).secretKey(secretKey).build());
    assertThat(events).isNotNull();
    assertThat(events.size()).isEqualTo(1);
    assertThat(events.get(0).getStackName()).isEqualTo(stackName);
    assertThat(events.get(0).getEventId()).isEqualTo("id");
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void shouldCreateStack() {
    String accessKey = "abcd";
    char[] secretKey = "pqrs".toCharArray();
    String stackName = "Stack Name";
    String region = "us-west-1";
    CreateStackRequest request = new CreateStackRequest().withStackName(stackName);
    AmazonCloudFormationClient mockClient = mock(AmazonCloudFormationClient.class);
    AwsHelperService service = spy(new AwsHelperService());
    doReturn(mockClient).when(service).getAmazonCloudFormationClient(any(), any());
    AwsCallTracker mockTracker = mock(AwsCallTracker.class);
    doNothing().when(mockTracker).trackCFCall(anyString());
    on(service).set("tracker", mockTracker);
    service.createStack(region, request, AwsConfig.builder().accessKey(accessKey).secretKey(secretKey).build());
    verify(mockClient).createStack(request);
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void shouldListStacks() {
    String accessKey = "qwer";
    char[] secretKey = "pqrs".toCharArray();
    String stackName = "Stack Name";
    String region = "us-west-1";
    DescribeStacksRequest request = new DescribeStacksRequest().withStackName(stackName);
    AmazonCloudFormationClient mockClient = mock(AmazonCloudFormationClient.class);
    AwsHelperService service = spy(new AwsHelperService());
    doReturn(mockClient).when(service).getAmazonCloudFormationClient(any(), any());
    DescribeStacksResult result = new DescribeStacksResult().withStacks(new Stack().withStackName(stackName));
    doReturn(result).when(mockClient).describeStacks(request);
    AwsCallTracker mockTracker = mock(AwsCallTracker.class);
    doNothing().when(mockTracker).trackCFCall(anyString());
    on(service).set("tracker", mockTracker);
    List<Stack> stacks =
        service.getAllStacks(region, request, AwsConfig.builder().accessKey(accessKey).secretKey(secretKey).build());
    assertThat(stacks).isNotNull();
    assertThat(stacks.size()).isEqualTo(1);
    assertThat(stacks.get(0).getStackName()).isEqualTo(stackName);
  }

  @Test
  @Owner(developers = RUSHABH)
  @Category(UnitTests.class)
  public void testDescribeAutoScalingGroupActivities() {
    Activity incompleteActivity = new Activity()
                                      .withActivityId("TestID1")
                                      .withAutoScalingGroupName("TestAutoScalingGroup")
                                      .withCause("TestCause1")
                                      .withDescription("TestDescription1")
                                      .withDetails("TestDetails1")
                                      .withProgress(50)
                                      .withStatusCode("TestStatusCode1")
                                      .withStatusMessage("TestStatusMessage1");

    Activity completeActivity = new Activity()
                                    .withActivityId("TestID2")
                                    .withAutoScalingGroupName("TestAutoScalingGroup")
                                    .withCause("TestCause2")
                                    .withDescription("TestDescription2")
                                    .withDetails("TestDetails2")
                                    .withProgress(100)
                                    .withStatusCode("TestStatusCode2")
                                    .withStatusMessage("TestStatusMessage2");

    DescribeScalingActivitiesResult result =
        new DescribeScalingActivitiesResult().withActivities(incompleteActivity, completeActivity);

    AmazonAutoScalingClient client = mock(AmazonAutoScalingClient.class);
    when(client.describeScalingActivities(any(DescribeScalingActivitiesRequest.class))).thenReturn(result);

    LogCallback logCallback = mock(LogCallback.class);

    List<String> logResult = new ArrayList<>();

    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        logResult.add((String) invocation.getArguments()[0]);
        return null;
      }
    })
        .when(logCallback)
        .saveExecutionLog(Mockito.anyString());

    Set<String> completedActivities = new HashSet<>();

    AwsHelperService awsHelperService = new AwsHelperService();
    AwsCallTracker mockTracker = mock(AwsCallTracker.class);
    doNothing().when(mockTracker).trackASGCall(anyString());
    on(awsHelperService).set("tracker", mockTracker);
    awsHelperService.describeAutoScalingGroupActivities(
        client, "TestAutoScalingGroup", completedActivities, logCallback, false);

    assertThat(logResult.size()).isEqualTo(2);

    assertThat(logResult.get(0))
        .isEqualTo(
            "AutoScalingGroup [TestAutoScalingGroup] activity [TestDescription1] progress [50 percent] , statuscode [TestStatusCode1]  details [TestDetails1]");
    assertThat(logResult.get(1))
        .isEqualTo(
            "AutoScalingGroup [TestAutoScalingGroup] activity [TestDescription2] progress [100 percent] , statuscode [TestStatusCode2]  details [TestDetails2]");

    assertThat(completedActivities.size()).isEqualTo(1);
    assertThat(completedActivities).contains("TestID2");

    logResult.clear();
    completedActivities.clear();

    awsHelperService.describeAutoScalingGroupActivities(
        client, "TestAutoScalingGroup", completedActivities, logCallback, true);

    // logResult.stream().forEach(s -> logger.info(s));

    assertThat(logResult.size()).isEqualTo(2);

    assertThat(logResult.get(0))
        .isEqualTo(
            "AutoScalingGroup [TestAutoScalingGroup] activity [TestDescription1] progress [50 percent] , statuscode [TestStatusCode1]  details [TestDetails1] cause [TestCause1]");
    assertThat(logResult.get(1))
        .isEqualTo(
            "AutoScalingGroup [TestAutoScalingGroup] activity [TestDescription2] progress [100 percent] , statuscode [TestStatusCode2]  details [TestDetails2] cause [TestCause2]");

    assertThat(completedActivities.size()).isEqualTo(1);
    assertThat(completedActivities).contains("TestID2");
  }
}
