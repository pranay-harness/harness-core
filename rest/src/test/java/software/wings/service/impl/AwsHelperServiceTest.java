package software.wings.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
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
import com.amazonaws.services.cloudformation.model.DeleteStackRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksResult;
import com.amazonaws.services.cloudformation.model.Stack;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import software.wings.WingsBaseTest;
import software.wings.beans.command.LogCallback;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by anubhaw on 3/3/17.
 */
public class AwsHelperServiceTest extends WingsBaseTest {
  @Test
  public void shouldGetInstanceId() {
    AwsHelperService awsHelperService = new AwsHelperService();
    assertThat(awsHelperService.getHostnameFromPrivateDnsName("ip-172-31-18-241.ec2.internal"))
        .isEqualTo("ip-172-31-18-241");
    assertThat(awsHelperService.getHostnameFromPrivateDnsName("ip-172-31-18-241.us-west-2.compute.internal"))
        .isEqualTo("ip-172-31-18-241");
  }

  @Test
  public void shouldDeleteStack() {
    String accessKey = "abcd";
    char[] secretKey = "pqrs".toCharArray();
    String stackName = "Stack Name";
    DeleteStackRequest request = new DeleteStackRequest().withStackName(stackName);
    AmazonCloudFormationClient mockClient = mock(AmazonCloudFormationClient.class);
    AwsHelperService service = spy(new AwsHelperService());
    doReturn(mockClient).when(service).getAmazonCloudFormationClient(accessKey, secretKey);
    service.deleteStack(accessKey, secretKey, request);
    verify(mockClient).deleteStack(request);
  }

  @Test
  public void shouldListStacks() {
    String accessKey = "qwer";
    char[] secretKey = "tyui".toCharArray();
    String stackName = "Stack Name";
    DescribeStacksRequest request = new DescribeStacksRequest().withStackName(stackName);
    AmazonCloudFormationClient mockClient = mock(AmazonCloudFormationClient.class);
    AwsHelperService service = spy(new AwsHelperService());
    doReturn(mockClient).when(service).getAmazonCloudFormationClient(accessKey, secretKey);
    DescribeStacksResult result = new DescribeStacksResult().withStacks(new Stack().withStackName(stackName));
    doReturn(result).when(mockClient).describeStacks(request);
    List<Stack> stacks = service.getAllStacks(accessKey, secretKey, request);
    assertThat(stacks).isNotNull();
    assertThat(stacks.size()).isEqualTo(1);
    assertThat(stacks.get(0).getStackName()).isEqualTo(stackName);
  }

  @Test
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
