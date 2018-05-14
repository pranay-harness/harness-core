package software.wings.delegatetasks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.ACTIVITY_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;

import com.google.inject.Inject;

import com.amazonaws.services.cloudformation.model.CreateStackRequest;
import com.amazonaws.services.cloudformation.model.CreateStackResult;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.Output;
import com.amazonaws.services.cloudformation.model.Stack;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.AwsConfig;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.delegatetasks.cloudformation.cloudformationtaskhandler.CloudFormationCreateStackHandler;
import software.wings.delegatetasks.cloudformation.cloudformationtaskhandler.CloudFormationDeleteStackHandler;
import software.wings.delegatetasks.cloudformation.cloudformationtaskhandler.CloudFormationListStacksHandler;
import software.wings.helpers.ext.cloudformation.request.CloudFormationCommandRequest.CloudFormationCommandType;
import software.wings.helpers.ext.cloudformation.request.CloudFormationCreateStackRequest;
import software.wings.helpers.ext.cloudformation.request.CloudFormationDeleteStackRequest;
import software.wings.helpers.ext.cloudformation.request.CloudFormationListStacksRequest;
import software.wings.helpers.ext.cloudformation.response.CloudFormationCommandExecutionResponse;
import software.wings.helpers.ext.cloudformation.response.CloudFormationCommandResponse;
import software.wings.helpers.ext.cloudformation.response.CloudFormationCreateStackResponse;
import software.wings.helpers.ext.cloudformation.response.CloudFormationListStacksResponse;
import software.wings.helpers.ext.cloudformation.response.StackSummaryInfo;
import software.wings.service.impl.AwsHelperService;
import software.wings.service.intfc.security.EncryptionService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class CloudFormationCommandTaskHandlerTest extends WingsBaseTest {
  @Mock private EncryptionService mockEncryptionService;
  @Mock private AwsHelperService mockAwsHelperService;
  @InjectMocks @Inject private CloudFormationCreateStackHandler createStackHandler;
  @InjectMocks @Inject private CloudFormationDeleteStackHandler deleteStackHandler;
  @InjectMocks @Inject private CloudFormationListStacksHandler listStacksHandler;

  @Test
  public void testCreateStack() {
    String templateBody = "Template Body";
    String accessKey = "abcd";
    char[] secretKey = "pqrs".toCharArray();
    String stackName = "Stack Name 00";
    CloudFormationCreateStackRequest request =
        CloudFormationCreateStackRequest.builder()
            .commandType(CloudFormationCommandType.CREATE_STACK)
            .accountId(ACCOUNT_ID)
            .appId(APP_ID)
            .activityId(ACTIVITY_ID)
            .commandName("Create Stack")
            .awsConfig(AwsConfig.builder().accessKey(accessKey).accountId(ACCOUNT_ID).secretKey(secretKey).build())
            .timeoutInMs(10 * 60 * 1000)
            .createType(CloudFormationCreateStackRequest.CLOUD_FORMATION_STACK_CREATE_BODY)
            .data(templateBody)
            .stackName(stackName)
            .build();
    doNothing().when(mockEncryptionService).decrypt(any(), any());
    CreateStackRequest createStackRequest =
        new CreateStackRequest().withStackName(stackName).withTemplateBody(templateBody);
    String stackId = "Stack Id 00";
    CreateStackResult createStackResult = new CreateStackResult().withStackId(stackId);
    doReturn(createStackResult).when(mockAwsHelperService).createStack(anyString(), any(), eq(createStackRequest));
    DescribeStacksRequest describeStacksRequest = new DescribeStacksRequest().withStackName(stackId);
    List<Stack> createProgressList = Collections.singletonList(new Stack().withStackStatus("CREATE_IN_PROGRESS"));
    List<Stack> createCompleteList =
        Collections.singletonList(new Stack()
                                      .withStackStatus("CREATE_COMPLETE")
                                      .withOutputs(new Output().withOutputKey("region").withOutputValue("us-east-1"),
                                          new Output().withOutputKey("vpcs").withOutputValue("vpcs"),
                                          new Output().withOutputKey("subnets").withOutputValue("subnets"),
                                          new Output().withOutputKey("securityGroups").withOutputValue("sgs"),
                                          new Output().withOutputKey("tags").withOutputValue("k1:v1")));
    when(mockAwsHelperService.getAllStacks(anyString(), any(), eq(describeStacksRequest)))
        .thenReturn(createProgressList)
        .thenReturn(createCompleteList);
    CloudFormationCommandExecutionResponse response = createStackHandler.execute(request, null);
    assertNotNull(response);
    assertEquals(CommandExecutionStatus.SUCCESS, response.getCommandExecutionStatus());
    CloudFormationCommandResponse formationCommandResponse = response.getCommandResponse();
    assertNotNull(formationCommandResponse);
    assertTrue(formationCommandResponse instanceof CloudFormationCreateStackResponse);
    CloudFormationCreateStackResponse createStackResponse =
        (CloudFormationCreateStackResponse) formationCommandResponse;
    assertEquals("us-east-1", createStackResponse.getRegion());
    assertStringListContents(createStackResponse.getVpcs(), "vpcs");
    assertStringListContents(createStackResponse.getSubnets(), "subnets");
    assertStringListContents(createStackResponse.getSecurityGroups(), "sgs");
    Map<String, Object> tagMap = createStackResponse.getTagMap();
    assertNotNull(tagMap);
    assertEquals(tagMap.size(), 1);
    assertTrue(tagMap.containsKey("k1"));
    assertEquals(tagMap.get("k1"), "v1");
  }

  private void assertStringListContents(List<String> strings, String... values) {
    assertNotNull(strings);
    assertEquals(strings.size(), values.length);
    for (int i = 0; i < strings.size(); i++) {
      assertEquals(values[i], strings.get(i));
    }
  }

  @Test
  public void testDeleteStack() {
    String accessKey = "abcd";
    char[] secretKey = "pqrs".toCharArray();
    String stackId = "Stack Id 01";
    CloudFormationDeleteStackRequest request =
        CloudFormationDeleteStackRequest.builder()
            .commandType(CloudFormationCommandType.DELETE_STACK)
            .accountId(ACCOUNT_ID)
            .appId(APP_ID)
            .activityId(ACTIVITY_ID)
            .commandName("Delete Stack")
            .awsConfig(AwsConfig.builder().accessKey(accessKey).accountId(ACCOUNT_ID).secretKey(secretKey).build())
            .timeoutInMs(10 * 60 * 1000)
            .stackId(stackId)
            .build();
    doNothing().when(mockEncryptionService).decrypt(any(), any());
    DescribeStacksRequest describeStacksRequest = new DescribeStacksRequest().withStackName(stackId);
    List<Stack> deleteInProgressList =
        Collections.singletonList(new Stack().withStackId(stackId).withStackStatus("DELETE_IN_PROGRESS"));
    List<Stack> deleteCompleteList =
        Collections.singletonList(new Stack().withStackId(stackId).withStackStatus("DELETE_COMPLETE"));
    when(mockAwsHelperService.getAllStacks(anyString(), any(), eq(describeStacksRequest)))
        .thenReturn(deleteInProgressList)
        .thenReturn(deleteCompleteList);
    CloudFormationCommandExecutionResponse response = deleteStackHandler.execute(request, null);
    assertNotNull(response);
    assertEquals(CommandExecutionStatus.SUCCESS, response.getCommandExecutionStatus());
    verify(mockAwsHelperService).deleteStack(anyString(), any(), any());
  }

  @Test
  public void testListStacks() {
    String accessKey = "abcd";
    char[] secretKey = "pqrs".toCharArray();
    CloudFormationListStacksRequest request =
        CloudFormationListStacksRequest.builder()
            .commandType(CloudFormationCommandType.GET_STACKS)
            .accountId(ACCOUNT_ID)
            .appId(APP_ID)
            .activityId(ACTIVITY_ID)
            .commandName("List Stack")
            .awsConfig(AwsConfig.builder().accessKey(accessKey).accountId(ACCOUNT_ID).secretKey(secretKey).build())
            .timeoutInMs(10 * 60 * 1000)
            .build();
    doNothing().when(mockEncryptionService).decrypt(any(), any());
    DescribeStacksRequest describeStacksRequest = new DescribeStacksRequest();
    List<Stack> stacks = Arrays.asList(new Stack()
                                           .withStackId("sId1")
                                           .withStackName("sName1")
                                           .withStackStatus("sStatus1")
                                           .withStackStatusReason("sReason1"),
        new Stack()
            .withStackId("sId2")
            .withStackName("sName2")
            .withStackStatus("sStatus2")
            .withStackStatusReason("sReason2"));
    doReturn(stacks).when(mockAwsHelperService).getAllStacks(anyString(), any(), eq(describeStacksRequest));
    CloudFormationCommandExecutionResponse response = listStacksHandler.execute(request, null);
    assertNotNull(response);
    assertEquals(CommandExecutionStatus.SUCCESS, response.getCommandExecutionStatus());
    CloudFormationCommandResponse formationCommandResponse = response.getCommandResponse();
    assertNotNull(formationCommandResponse);
    assertTrue(formationCommandResponse instanceof CloudFormationListStacksResponse);
    CloudFormationListStacksResponse listStacksResponse = (CloudFormationListStacksResponse) formationCommandResponse;
    List<StackSummaryInfo> summaryInfos = listStacksResponse.getStackSummaryInfos();
    assertNotNull(summaryInfos);
    assertEquals(2, summaryInfos.size());
    validateStackSummaryInfo(summaryInfos.get(0), "sId1", "sName1", "sStatus1", "sReason1");
    validateStackSummaryInfo(summaryInfos.get(1), "sId2", "sName2", "sStatus2", "sReason2");
  }

  private void validateStackSummaryInfo(
      StackSummaryInfo info, String stackId, String stackName, String stackStatus, String stackReason) {
    assertNotNull(info);
    assertEquals(info.getStackId(), stackId);
    assertEquals(info.getStackName(), stackName);
    assertEquals(info.getStackStatus(), stackStatus);
    assertEquals(info.getStackStatusReason(), stackReason);
  }
}