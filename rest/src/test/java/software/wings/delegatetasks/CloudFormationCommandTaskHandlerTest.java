package software.wings.delegatetasks;

import static org.joor.Reflect.on;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.ACTIVITY_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;

import com.google.inject.Inject;

import com.amazonaws.services.cloudformation.model.CreateStackRequest;
import com.amazonaws.services.cloudformation.model.CreateStackResult;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.Output;
import com.amazonaws.services.cloudformation.model.Stack;
import org.junit.Before;
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
import software.wings.helpers.ext.cloudformation.response.ExistingStackInfo;
import software.wings.helpers.ext.cloudformation.response.StackSummaryInfo;
import software.wings.service.impl.AwsHelperService;
import software.wings.service.intfc.aws.delegate.AwsCFHelperServiceDelegate;
import software.wings.service.intfc.security.EncryptionService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class CloudFormationCommandTaskHandlerTest extends WingsBaseTest {
  @Mock private EncryptionService mockEncryptionService;
  @Mock private AwsHelperService mockAwsHelperService;
  @Mock private AwsCFHelperServiceDelegate mockAwsCFHelperServiceDelegate;
  @InjectMocks @Inject private CloudFormationCreateStackHandler createStackHandler;
  @InjectMocks @Inject private CloudFormationDeleteStackHandler deleteStackHandler;
  @InjectMocks @Inject private CloudFormationListStacksHandler listStacksHandler;

  @Before
  public void setUp() throws Exception {
    on(createStackHandler).set("awsCFHelperServiceDelegate", mockAwsCFHelperServiceDelegate);
    on(listStacksHandler).set("awsCFHelperServiceDelegate", mockAwsCFHelperServiceDelegate);
    on(deleteStackHandler).set("awsCFHelperServiceDelegate", mockAwsCFHelperServiceDelegate);
  }

  @Test
  public void testCreateStack() {
    String templateBody = "Template Body";
    String accessKey = "abcd";
    char[] secretKey = "pqrs".toCharArray();
    String stackNameSuffix = "Stack Name 00";
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
            .stackNameSuffix(stackNameSuffix)
            .build();
    doReturn(null).when(mockEncryptionService).decrypt(any(), any());
    CreateStackRequest createStackRequest =
        new CreateStackRequest().withStackName("HarnessStack-" + stackNameSuffix).withTemplateBody(templateBody);
    String stackId = "Stack Id 00";
    CreateStackResult createStackResult = new CreateStackResult().withStackId(stackId);
    doReturn(createStackResult).when(mockAwsHelperService).createStack(anyString(), anyString(), any(), any());
    List<Stack> createProgressList = Collections.singletonList(new Stack().withStackStatus("CREATE_IN_PROGRESS"));
    List<Stack> createCompleteList =
        Collections.singletonList(new Stack()
                                      .withStackStatus("CREATE_COMPLETE")
                                      .withOutputs(new Output().withOutputKey("vpcs").withOutputValue("vpcs"),
                                          new Output().withOutputKey("subnets").withOutputValue("subnets"),
                                          new Output().withOutputKey("securityGroups").withOutputValue("sgs")));
    doReturn(Collections.emptyList())
        .doReturn(createProgressList)
        .doReturn(createCompleteList)
        .when(mockAwsHelperService)
        .getAllStacks(anyString(), anyString(), any(), any());

    CloudFormationCommandExecutionResponse response = createStackHandler.execute(request, null);
    assertNotNull(response);
    assertEquals(CommandExecutionStatus.SUCCESS, response.getCommandExecutionStatus());
    CloudFormationCommandResponse formationCommandResponse = response.getCommandResponse();
    assertNotNull(formationCommandResponse);
    assertTrue(formationCommandResponse instanceof CloudFormationCreateStackResponse);
    CloudFormationCreateStackResponse createStackResponse =
        (CloudFormationCreateStackResponse) formationCommandResponse;
    Map<String, Object> outputMap = createStackResponse.getCloudFormationOutputMap();
    assertEquals(outputMap.size(), 3);
    validateMapContents(outputMap, "vpcs", "vpcs");
    validateMapContents(outputMap, "subnets", "subnets");
    validateMapContents(outputMap, "securityGroups", "sgs");
    ExistingStackInfo existingStackInfo = createStackResponse.getExistingStackInfo();
    assertNotNull(existingStackInfo);
    assertFalse(existingStackInfo.isStackExisted());
  }

  private void validateMapContents(Map<String, Object> map, String key, String value) {
    assertTrue(map.containsKey(key));
    assertEquals((String) map.get(key), value);
  }

  @Test
  public void testUpdateStack() {
    String templateBody = "Template Body";
    String accessKey = "abcd";
    char[] secretKey = "pqrs".toCharArray();
    String stackNameSuffix = "Stack Name 00";
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
            .stackNameSuffix(stackNameSuffix)
            .build();
    doReturn(null).when(mockEncryptionService).decrypt(any(), any());
    List<Stack> exitingList = Collections.singletonList(
        new Stack().withStackStatus("CREATE_COMPLETE").withStackName("HarnessStack-" + stackNameSuffix));
    List<Stack> updateProgressList = Collections.singletonList(new Stack().withStackStatus("UPDATE_IN_PROGRESS"));
    List<Stack> updateCompleteList = Collections.singletonList(new Stack().withStackStatus("UPDATE_COMPLETE"));
    doReturn(exitingList)
        .doReturn(updateProgressList)
        .doReturn(updateCompleteList)
        .when(mockAwsHelperService)
        .getAllStacks(anyString(), anyString(), any(), any());
    doReturn("Body").when(mockAwsCFHelperServiceDelegate).getStackBody(any(), anyString(), anyString());

    CloudFormationCommandExecutionResponse response = createStackHandler.execute(request, null);
    assertNotNull(response);
    assertEquals(CommandExecutionStatus.SUCCESS, response.getCommandExecutionStatus());
    CloudFormationCommandResponse formationCommandResponse = response.getCommandResponse();
    assertNotNull(formationCommandResponse);
    assertTrue(formationCommandResponse instanceof CloudFormationCreateStackResponse);
    CloudFormationCreateStackResponse createStackResponse =
        (CloudFormationCreateStackResponse) formationCommandResponse;
    ExistingStackInfo existingStackInfo = createStackResponse.getExistingStackInfo();
    assertNotNull(existingStackInfo);
    assertTrue(existingStackInfo.isStackExisted());
    assertEquals(existingStackInfo.getOldStackBody(), "Body");
  }

  @Test
  public void testDeleteStack() {
    String accessKey = "abcd";
    char[] secretKey = "pqrs".toCharArray();
    String stackNameSuffix = "Stack Name 01";
    CloudFormationDeleteStackRequest request =
        CloudFormationDeleteStackRequest.builder()
            .commandType(CloudFormationCommandType.DELETE_STACK)
            .accountId(ACCOUNT_ID)
            .appId(APP_ID)
            .activityId(ACTIVITY_ID)
            .commandName("Delete Stack")
            .awsConfig(AwsConfig.builder().accessKey(accessKey).accountId(ACCOUNT_ID).secretKey(secretKey).build())
            .timeoutInMs(10 * 60 * 1000)
            .stackNameSuffix(stackNameSuffix)
            .build();
    doReturn(null).when(mockEncryptionService).decrypt(any(), any());
    String stackId = "Stack Id 01";

    List<Stack> existingStackList =
        Collections.singletonList(new Stack().withStackName("HarnessStack-" + stackNameSuffix).withStackId(stackId));
    List<Stack> deleteInProgressList =
        Collections.singletonList(new Stack().withStackId(stackId).withStackStatus("DELETE_IN_PROGRESS"));
    List<Stack> deleteCompleteList =
        Collections.singletonList(new Stack().withStackId(stackId).withStackStatus("DELETE_COMPLETE"));
    doReturn(existingStackList)
        .doReturn(deleteInProgressList)
        .doReturn(deleteCompleteList)
        .when(mockAwsHelperService)
        .getAllStacks(anyString(), anyString(), any(), any());
    CloudFormationCommandExecutionResponse response = deleteStackHandler.execute(request, null);
    assertNotNull(response);
    assertEquals(CommandExecutionStatus.SUCCESS, response.getCommandExecutionStatus());
    verify(mockAwsHelperService).deleteStack(anyString(), anyString(), any(), any());
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
    doReturn(null).when(mockEncryptionService).decrypt(any(), any());
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
    doReturn(stacks)
        .when(mockAwsHelperService)
        .getAllStacks(anyString(), anyString(), any(), eq(describeStacksRequest));
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