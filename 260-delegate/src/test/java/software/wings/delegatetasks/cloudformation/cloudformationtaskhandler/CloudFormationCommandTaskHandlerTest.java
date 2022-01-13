/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.delegatetasks.cloudformation.cloudformationtaskhandler;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.rule.OwnerRule.RAGHVENDRA;
import static io.harness.rule.OwnerRule.SATYAM;

import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.ACTIVITY_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;

import static com.amazonaws.services.cloudformation.model.StackStatus.UPDATE_ROLLBACK_COMPLETE;
import static com.amazonaws.services.cloudformation.model.StackStatus.UPDATE_ROLLBACK_FAILED;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.category.element.UnitTests;
import io.harness.exception.ExceptionUtils;
import io.harness.logging.CommandExecutionStatus;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.WingsBaseTest;
import software.wings.beans.AwsConfig;
import software.wings.beans.GitConfig;
import software.wings.beans.GitFileConfig;
import software.wings.beans.GitOperationContext;
import software.wings.beans.command.ExecutionLogCallback;
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
import software.wings.utils.GitUtilsDelegate;

import com.amazonaws.services.cloudformation.model.CreateStackRequest;
import com.amazonaws.services.cloudformation.model.CreateStackResult;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.Output;
import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.cloudformation.model.StackEvent;
import com.amazonaws.services.cloudformation.model.UpdateStackResult;
import com.google.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@TargetModule(HarnessModule._930_DELEGATE_TASKS)
@OwnedBy(CDP)
public class CloudFormationCommandTaskHandlerTest extends WingsBaseTest {
  @Mock private EncryptionService mockEncryptionService;
  @Mock private AwsHelperService mockAwsHelperService;
  @Mock private AwsCFHelperServiceDelegate mockAwsCFHelperServiceDelegate;
  @Mock private GitUtilsDelegate gitUtilsDelegate;
  @Mock private GitOperationContext gitOperationContext;
  @InjectMocks @Inject private CloudFormationCreateStackHandler createStackHandler;
  @InjectMocks @Inject private CloudFormationDeleteStackHandler deleteStackHandler;
  @InjectMocks @Inject private CloudFormationListStacksHandler listStacksHandler;

  @Before
  public void setUp() throws Exception {
    initMocks(this);
    on(createStackHandler).set("awsCFHelperServiceDelegate", mockAwsCFHelperServiceDelegate);
    on(listStacksHandler).set("awsCFHelperServiceDelegate", mockAwsCFHelperServiceDelegate);
    on(deleteStackHandler).set("awsCFHelperServiceDelegate", mockAwsCFHelperServiceDelegate);
    when(mockEncryptionService.decrypt(any(), any(), eq(false))).thenReturn(null);
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testGetIfStackExists() {
    String customStackName = "CUSTOM_STACK_NAME";
    String stackId = "STACK_ID";
    doReturn(singletonList(new Stack().withStackId(stackId).withStackName(customStackName)))
        .when(mockAwsHelperService)
        .getAllStacks(anyString(), any(), any());
    Optional<Stack> stack =
        createStackHandler.getIfStackExists(customStackName, "foo", AwsConfig.builder().build(), "us-east-1");
    assertThat(stack.isPresent()).isTrue();
    assertThat(stackId).isEqualTo(stack.get().getStackId());
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testCreateStack() {
    String templateBody = "Template Body";
    char[] accessKey = "abcd".toCharArray();
    char[] secretKey = "pqrs".toCharArray();
    String stackNameSuffix = "Stack Name 00";
    String roleArn = "roleArn";
    CloudFormationCreateStackRequest request =
        CloudFormationCreateStackRequest.builder()
            .commandType(CloudFormationCommandType.CREATE_STACK)
            .accountId(ACCOUNT_ID)
            .appId(APP_ID)
            .activityId(ACTIVITY_ID)
            .cloudFormationRoleArn(roleArn)
            .commandName("Create Stack")
            .awsConfig(AwsConfig.builder().accessKey(accessKey).accountId(ACCOUNT_ID).secretKey(secretKey).build())
            .timeoutInMs(10 * 60 * 1000)
            .createType(CloudFormationCreateStackRequest.CLOUD_FORMATION_STACK_CREATE_BODY)
            .data(templateBody)
            .stackNameSuffix(stackNameSuffix)
            .build();
    CreateStackRequest createStackRequest = new CreateStackRequest()
                                                .withStackName("HarnessStack-" + stackNameSuffix)
                                                .withTemplateBody(templateBody)
                                                .withRoleARN(roleArn);
    String stackId = "Stack Id 00";
    CreateStackResult createStackResult = new CreateStackResult().withStackId(stackId);
    doReturn(createStackResult).when(mockAwsHelperService).createStack(anyString(), any(), any());
    List<Stack> createProgressList = singletonList(new Stack().withStackStatus("CREATE_IN_PROGRESS"));
    List<Stack> createCompleteList =
        singletonList(new Stack()
                          .withStackStatus("CREATE_COMPLETE")
                          .withOutputs(new Output().withOutputKey("vpcs").withOutputValue("vpcs"),
                              new Output().withOutputKey("subnets").withOutputValue("subnets"),
                              new Output().withOutputKey("securityGroups").withOutputValue("sgs")));
    doReturn(Collections.emptyList())
        .doReturn(createProgressList)
        .doReturn(createCompleteList)
        .when(mockAwsHelperService)
        .getAllStacks(anyString(), any(), any());

    CloudFormationCommandExecutionResponse response = createStackHandler.execute(request, null);
    assertThat(response).isNotNull();
    assertThat(response.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
    CloudFormationCommandResponse formationCommandResponse = response.getCommandResponse();
    assertThat(formationCommandResponse).isNotNull();
    assertThat(formationCommandResponse instanceof CloudFormationCreateStackResponse).isTrue();
    CloudFormationCreateStackResponse createStackResponse =
        (CloudFormationCreateStackResponse) formationCommandResponse;
    Map<String, Object> outputMap = createStackResponse.getCloudFormationOutputMap();
    assertThat(3).isEqualTo(outputMap.size());
    validateMapContents(outputMap, "vpcs", "vpcs");
    validateMapContents(outputMap, "subnets", "subnets");
    validateMapContents(outputMap, "securityGroups", "sgs");
    ExistingStackInfo existingStackInfo = createStackResponse.getExistingStackInfo();
    assertThat(existingStackInfo).isNotNull();
    assertThat(existingStackInfo.isStackExisted()).isFalse();
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testCreateStackWithNoStacksFound() {
    String templateBody = "Template Body";
    char[] accessKey = "abcd".toCharArray();
    char[] secretKey = "pqrs".toCharArray();
    String stackNameSuffix = "Stack Name 00";
    String roleArn = "roleArn";
    CloudFormationCreateStackRequest request =
        CloudFormationCreateStackRequest.builder()
            .commandType(CloudFormationCommandType.CREATE_STACK)
            .accountId(ACCOUNT_ID)
            .appId(APP_ID)
            .activityId(ACTIVITY_ID)
            .cloudFormationRoleArn(roleArn)
            .commandName("Create Stack")
            .awsConfig(AwsConfig.builder().accessKey(accessKey).accountId(ACCOUNT_ID).secretKey(secretKey).build())
            .timeoutInMs(10 * 60 * 1000)
            .createType(CloudFormationCreateStackRequest.CLOUD_FORMATION_STACK_CREATE_BODY)
            .data(templateBody)
            .stackNameSuffix(stackNameSuffix)
            .build();
    String stackId = "Stack Id 00";
    CreateStackResult createStackResult = new CreateStackResult().withStackId(stackId);
    doReturn(createStackResult).when(mockAwsHelperService).createStack(anyString(), any(), any());
    List<Stack> createProgressList = emptyList();
    doReturn(Collections.emptyList())
        .doReturn(createProgressList)
        .when(mockAwsHelperService)
        .getAllStacks(anyString(), any(), any());

    CloudFormationCommandExecutionResponse response = createStackHandler.execute(request, null);
    assertThat(response).isNotNull();
    assertThat(response.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.FAILURE);
    CloudFormationCommandResponse formationCommandResponse = response.getCommandResponse();
    assertThat(formationCommandResponse).isNull();
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testCreateStackGit() {
    char[] accessKey = "abcd".toCharArray();
    char[] secretKey = "pqrs".toCharArray();
    String data = "data";
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
            .gitConfig(GitConfig.builder().repoUrl("").branch("").build())
            .gitFileConfig(GitFileConfig.builder().filePath("").commitId("").build())
            .createType(CloudFormationCreateStackRequest.CLOUD_FORMATION_STACK_CREATE_GIT)
            .stackNameSuffix(stackNameSuffix)
            .build();
    doReturn(gitOperationContext)
        .when(gitUtilsDelegate)
        .cloneRepo(any(GitConfig.class), any(GitFileConfig.class), anyListOf(EncryptedDataDetail.class));
    doReturn(data).when(gitUtilsDelegate).resolveAbsoluteFilePath(any(GitOperationContext.class), anyString());
    doReturn(data).when(gitUtilsDelegate).getRequestDataFromFile(anyString());
    String stackId = "Stack Id 00";
    CreateStackResult createStackResult = new CreateStackResult().withStackId(stackId);
    doReturn(createStackResult).when(mockAwsHelperService).createStack(anyString(), any(), any());
    List<Stack> createProgressList = singletonList(new Stack().withStackStatus("CREATE_IN_PROGRESS"));
    List<Stack> createCompleteList =
        singletonList(new Stack()
                          .withStackStatus("CREATE_COMPLETE")
                          .withOutputs(new Output().withOutputKey("vpcs").withOutputValue("vpcs"),
                              new Output().withOutputKey("subnets").withOutputValue("subnets"),
                              new Output().withOutputKey("securityGroups").withOutputValue("sgs")));
    doReturn(Collections.emptyList())
        .doReturn(createProgressList)
        .doReturn(createCompleteList)
        .when(mockAwsHelperService)
        .getAllStacks(anyString(), any(), any());

    CloudFormationCommandExecutionResponse response = createStackHandler.execute(request, null);

    assertThat(response).isNotNull();
    assertThat(data).isEqualTo(request.getData());
    assertThat(response.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
    CloudFormationCommandResponse formationCommandResponse = response.getCommandResponse();
    assertThat(formationCommandResponse).isNotNull();
    assertThat(formationCommandResponse instanceof CloudFormationCreateStackResponse).isTrue();
    CloudFormationCreateStackResponse createStackResponse =
        (CloudFormationCreateStackResponse) formationCommandResponse;
    Map<String, Object> outputMap = createStackResponse.getCloudFormationOutputMap();
    assertThat(3).isEqualTo(outputMap.size());
    validateMapContents(outputMap, "vpcs", "vpcs");
    validateMapContents(outputMap, "subnets", "subnets");
    validateMapContents(outputMap, "securityGroups", "sgs");
    ExistingStackInfo existingStackInfo = createStackResponse.getExistingStackInfo();
    assertThat(existingStackInfo).isNotNull();
    assertThat(existingStackInfo.isStackExisted()).isFalse();
  }

  private void validateMapContents(Map<String, Object> map, String key, String value) {
    assertThat(map.containsKey(key)).isTrue();
    assertThat(value).isEqualTo((String) map.get(key));
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testUpdateStack() {
    String templateBody = "Template Body";
    char[] accessKey = "abcd".toCharArray();
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
    List<Stack> exitingList =
        singletonList(new Stack().withStackStatus("CREATE_COMPLETE").withStackName("HarnessStack-" + stackNameSuffix));
    List<Stack> updateProgressList = singletonList(new Stack().withStackStatus("UPDATE_IN_PROGRESS"));
    List<Stack> updateCompleteList = singletonList(new Stack().withStackStatus("UPDATE_COMPLETE"));
    doReturn(exitingList)
        .doReturn(updateProgressList)
        .doReturn(updateCompleteList)
        .when(mockAwsHelperService)
        .getAllStacks(anyString(), any(), any());
    UpdateStackResult updateStackResult = new UpdateStackResult();
    updateStackResult.setStackId("StackId1");
    doReturn(updateStackResult).when(mockAwsHelperService).updateStack(anyString(), any(), any());
    doReturn("Body").when(mockAwsCFHelperServiceDelegate).getStackBody(any(), anyString(), anyString());

    CloudFormationCommandExecutionResponse response = createStackHandler.execute(request, null);
    assertThat(response).isNotNull();
    assertThat(response.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
    CloudFormationCommandResponse formationCommandResponse = response.getCommandResponse();
    assertThat(formationCommandResponse).isNotNull();
    assertThat(formationCommandResponse instanceof CloudFormationCreateStackResponse).isTrue();
    CloudFormationCreateStackResponse createStackResponse =
        (CloudFormationCreateStackResponse) formationCommandResponse;
    ExistingStackInfo existingStackInfo = createStackResponse.getExistingStackInfo();
    assertThat(existingStackInfo).isNotNull();
    assertThat(existingStackInfo.isStackExisted()).isTrue();
    assertThat("Body").isEqualTo(existingStackInfo.getOldStackBody());
  }

  @Test
  @Owner(developers = RAGHVENDRA)
  @Category(UnitTests.class)
  public void testUpdateStackWithExistingStackInUpdateRollbackCompleteState() {
    String templateBody = "Template Body";
    char[] accessKey = "abcd".toCharArray();
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
    List<Stack> exitingList =
        singletonList(new Stack().withStackStatus("CREATE_COMPLETE").withStackName("HarnessStack-" + stackNameSuffix));
    List<Stack> updateCompleteList =
        singletonList(new Stack().withStackStatus("UPDATE_ROLLBACK_COMPLETE").withStackId("stackId1"));
    doReturn(exitingList)
        .doReturn(updateCompleteList)
        .when(mockAwsHelperService)
        .getAllStacks(anyString(), any(), any());
    UpdateStackResult updateStackResult = new UpdateStackResult();
    doReturn(updateStackResult).when(mockAwsHelperService).updateStack(anyString(), any(), any());
    doReturn("Body").when(mockAwsCFHelperServiceDelegate).getStackBody(any(), anyString(), anyString());

    CloudFormationCommandExecutionResponse response = createStackHandler.execute(request, null);
    assertThat(response).isNotNull();
    assertThat(response.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
    CloudFormationCommandResponse formationCommandResponse = response.getCommandResponse();
    assertThat(formationCommandResponse).isNotNull();
    assertThat(formationCommandResponse instanceof CloudFormationCreateStackResponse).isTrue();
    CloudFormationCreateStackResponse createStackResponse =
        (CloudFormationCreateStackResponse) formationCommandResponse;
    assertThat(createStackResponse.getStackId()).isEqualTo("stackId1");
    ExistingStackInfo existingStackInfo = createStackResponse.getExistingStackInfo();
    assertThat(existingStackInfo).isNotNull();
    assertThat(existingStackInfo.isStackExisted()).isTrue();
    assertThat("Body").isEqualTo(existingStackInfo.getOldStackBody());
  }

  @Test
  @Owner(developers = RAGHVENDRA)
  @Category(UnitTests.class)
  public void testUpdateStackWithExistingStackInUpdateRollbackFailedState() {
    String templateBody = "Template Body";
    char[] accessKey = "abcd".toCharArray();
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
    List<Stack> exitingList =
        singletonList(new Stack().withStackStatus("CREATE_COMPLETE").withStackName("HarnessStack-" + stackNameSuffix));
    List<Stack> updateCompleteList =
        singletonList(new Stack().withStackStatus("UPDATE_ROLLBACK_FAILED").withStackId("stackId1"));
    doReturn(exitingList)
        .doReturn(updateCompleteList)
        .when(mockAwsHelperService)
        .getAllStacks(anyString(), any(), any());
    UpdateStackResult updateStackResult = new UpdateStackResult();
    doReturn(updateStackResult).when(mockAwsHelperService).updateStack(anyString(), any(), any());
    doReturn("Body").when(mockAwsCFHelperServiceDelegate).getStackBody(any(), anyString(), anyString());

    CloudFormationCommandExecutionResponse response = createStackHandler.execute(request, null);
    assertThat(response).isNotNull();
    assertThat(response.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.FAILURE);
    assertThat(response.getErrorMessage())
        .isEqualTo(
            "# Existing stack with name null is already in status: UPDATE_ROLLBACK_FAILED, therefore exiting with failure");
    CloudFormationCommandResponse formationCommandResponse = response.getCommandResponse();
    assertThat(formationCommandResponse).isNotNull();
    assertThat(formationCommandResponse instanceof CloudFormationCreateStackResponse).isTrue();
    CloudFormationCreateStackResponse createStackResponse =
        (CloudFormationCreateStackResponse) formationCommandResponse;
    assertThat(createStackResponse.getStackId()).isEqualTo("stackId1");
    assertThat(createStackResponse.getStackStatus()).isEqualTo(UPDATE_ROLLBACK_FAILED.name());
    ExistingStackInfo existingStackInfo = createStackResponse.getExistingStackInfo();
    assertThat(existingStackInfo).isNotNull();
    assertThat(existingStackInfo.isStackExisted()).isTrue();
    assertThat("Body").isEqualTo(existingStackInfo.getOldStackBody());
  }

  @Test
  @Owner(developers = RAGHVENDRA)
  @Category(UnitTests.class)
  public void testUpdateStackWithStackStatusToMarkAsSuccess() {
    String templateBody = "Template Body";
    char[] accessKey = "abcd".toCharArray();
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
            .stackStatusesToMarkAsSuccess(singletonList(UPDATE_ROLLBACK_COMPLETE))
            .createType(CloudFormationCreateStackRequest.CLOUD_FORMATION_STACK_CREATE_BODY)
            .data(templateBody)
            .stackNameSuffix(stackNameSuffix)
            .build();
    List<Stack> exitingList =
        singletonList(new Stack().withStackStatus("CREATE_COMPLETE").withStackName("HarnessStack-" + stackNameSuffix));
    List<Stack> updateProgressList = singletonList(new Stack().withStackStatus("UPDATE_IN_PROGRESS"));
    List<Stack> updateCompleteList = singletonList(new Stack().withStackStatus("UPDATE_ROLLBACK_COMPLETE"));
    doReturn(exitingList)
        .doReturn(updateProgressList)
        .doReturn(updateCompleteList)
        .when(mockAwsHelperService)
        .getAllStacks(anyString(), any(), any());
    UpdateStackResult updateStackResult = new UpdateStackResult();
    updateStackResult.setStackId("StackId1");
    doReturn(updateStackResult).when(mockAwsHelperService).updateStack(anyString(), any(), any());
    doReturn("Body").when(mockAwsCFHelperServiceDelegate).getStackBody(any(), anyString(), anyString());

    CloudFormationCommandExecutionResponse response = createStackHandler.execute(request, null);
    assertThat(response).isNotNull();
    assertThat(response.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
    CloudFormationCommandResponse formationCommandResponse = response.getCommandResponse();
    assertThat(formationCommandResponse).isNotNull();
    assertThat(formationCommandResponse instanceof CloudFormationCreateStackResponse).isTrue();
    CloudFormationCreateStackResponse createStackResponse =
        (CloudFormationCreateStackResponse) formationCommandResponse;
    ExistingStackInfo existingStackInfo = createStackResponse.getExistingStackInfo();
    assertThat(existingStackInfo).isNotNull();
    assertThat(existingStackInfo.isStackExisted()).isTrue();
    assertThat("Body").isEqualTo(existingStackInfo.getOldStackBody());
  }

  @Test
  @Owner(developers = RAGHVENDRA)
  @Category(UnitTests.class)
  public void testUpdateStackGit() {
    String templateBody = "Template Body";
    char[] accessKey = "abcd".toCharArray();
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
            .gitConfig(GitConfig.builder().repoUrl("").branch("").build())
            .gitFileConfig(GitFileConfig.builder().filePath("").commitId("").build())
            .createType(CloudFormationCreateStackRequest.CLOUD_FORMATION_STACK_CREATE_GIT)
            .data(templateBody)
            .stackNameSuffix(stackNameSuffix)
            .build();
    List<Stack> exitingList =
        singletonList(new Stack().withStackStatus("CREATE_COMPLETE").withStackName("HarnessStack-" + stackNameSuffix));
    List<Stack> updateProgressList = singletonList(new Stack().withStackStatus("UPDATE_IN_PROGRESS"));
    List<Stack> updateCompleteList = singletonList(new Stack().withStackStatus("UPDATE_COMPLETE"));
    doReturn(exitingList)
        .doReturn(updateProgressList)
        .doReturn(updateCompleteList)
        .when(mockAwsHelperService)
        .getAllStacks(anyString(), any(), any());
    UpdateStackResult updateStackResult = new UpdateStackResult();
    updateStackResult.setStackId("StackId1");
    doReturn(updateStackResult).when(mockAwsHelperService).updateStack(anyString(), any(), any());
    doReturn("Body").when(mockAwsCFHelperServiceDelegate).getStackBody(any(), anyString(), anyString());

    CloudFormationCommandExecutionResponse response = createStackHandler.execute(request, null);
    assertThat(response).isNotNull();
    assertThat(response.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
    CloudFormationCommandResponse formationCommandResponse = response.getCommandResponse();
    assertThat(formationCommandResponse).isNotNull();
    assertThat(formationCommandResponse instanceof CloudFormationCreateStackResponse).isTrue();
    CloudFormationCreateStackResponse createStackResponse =
        (CloudFormationCreateStackResponse) formationCommandResponse;
    ExistingStackInfo existingStackInfo = createStackResponse.getExistingStackInfo();
    assertThat(existingStackInfo).isNotNull();
    assertThat(existingStackInfo.isStackExisted()).isTrue();
    assertThat("Body").isEqualTo(existingStackInfo.getOldStackBody());
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testDeleteStack() {
    char[] accessKey = "abcd".toCharArray();
    char[] secretKey = "pqrs".toCharArray();
    String stackNameSuffix = "Stack Name 01";
    String roleArn = "RoleArn";
    CloudFormationDeleteStackRequest request =
        CloudFormationDeleteStackRequest.builder()
            .commandType(CloudFormationCommandType.DELETE_STACK)
            .accountId(ACCOUNT_ID)
            .appId(APP_ID)
            .activityId(ACTIVITY_ID)
            .commandName("Delete Stack")
            .cloudFormationRoleArn(roleArn)
            .awsConfig(AwsConfig.builder().accessKey(accessKey).accountId(ACCOUNT_ID).secretKey(secretKey).build())
            .timeoutInMs(10 * 60 * 1000)
            .stackNameSuffix(stackNameSuffix)
            .build();
    String stackId = "Stack Id 01";

    List<Stack> existingStackList =
        singletonList(new Stack().withStackName("HarnessStack-" + stackNameSuffix).withStackId(stackId));
    List<Stack> deleteInProgressList =
        singletonList(new Stack().withStackId(stackId).withStackStatus("DELETE_IN_PROGRESS"));
    List<Stack> deleteCompleteList = singletonList(new Stack().withStackId(stackId).withStackStatus("DELETE_COMPLETE"));
    doReturn(existingStackList)
        .doReturn(deleteInProgressList)
        .doReturn(deleteCompleteList)
        .when(mockAwsHelperService)
        .getAllStacks(anyString(), any(), any());
    CloudFormationCommandExecutionResponse response = deleteStackHandler.execute(request, null);
    assertThat(response).isNotNull();
    assertThat(response.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
    verify(mockAwsHelperService).deleteStack(anyString(), any(), any());
  }

  @Test
  @Owner(developers = RAGHVENDRA)
  @Category(UnitTests.class)
  public void testDeleteStackNoExistingStack() {
    char[] accessKey = "abcd".toCharArray();
    char[] secretKey = "pqrs".toCharArray();
    String stackNameSuffix = "Stack Name 01";
    String roleArn = "RoleArn";
    CloudFormationDeleteStackRequest request =
        CloudFormationDeleteStackRequest.builder()
            .commandType(CloudFormationCommandType.DELETE_STACK)
            .accountId(ACCOUNT_ID)
            .appId(APP_ID)
            .activityId(ACTIVITY_ID)
            .commandName("Delete Stack")
            .cloudFormationRoleArn(roleArn)
            .awsConfig(AwsConfig.builder().accessKey(accessKey).accountId(ACCOUNT_ID).secretKey(secretKey).build())
            .timeoutInMs(10 * 60 * 1000)
            .stackNameSuffix(stackNameSuffix + "nomatch")
            .build();
    List<Stack> existingStackList = emptyList();
    doReturn(existingStackList).when(mockAwsHelperService).getAllStacks(anyString(), any(), any());
    CloudFormationCommandExecutionResponse response = deleteStackHandler.execute(request, null);
    assertThat(response).isNotNull();
    assertThat(response.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
  }

  @Test
  @Owner(developers = RAGHVENDRA)
  @Category(UnitTests.class)
  public void testDeleteStackWithException() {
    char[] accessKey = "abcd".toCharArray();
    char[] secretKey = "pqrs".toCharArray();
    String stackNameSuffix = "Stack Name 01";
    String roleArn = "RoleArn";
    CloudFormationDeleteStackRequest request =
        CloudFormationDeleteStackRequest.builder()
            .commandType(CloudFormationCommandType.DELETE_STACK)
            .accountId(ACCOUNT_ID)
            .appId(APP_ID)
            .activityId(ACTIVITY_ID)
            .commandName("Delete Stack")
            .cloudFormationRoleArn(roleArn)
            .awsConfig(AwsConfig.builder().accessKey(accessKey).accountId(ACCOUNT_ID).secretKey(secretKey).build())
            .timeoutInMs(10 * 60 * 1000)
            .stackNameSuffix(stackNameSuffix)
            .build();
    Exception ex = new RuntimeException("This is an exception");
    String stackId = "Stack Id 01";
    List<Stack> existingStackList =
        singletonList(new Stack().withStackName("HarnessStack-" + stackNameSuffix).withStackId(stackId));
    doReturn(existingStackList).when(mockAwsHelperService).getAllStacks(anyString(), any(), any());
    doThrow(ex).when(mockAwsHelperService).deleteStack(anyString(), any(), any());
    CloudFormationCommandExecutionResponse response = deleteStackHandler.execute(request, null);
    assertThat(response).isNotNull();
    assertThat(response.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.FAILURE);
  }

  @Test
  @Owner(developers = RAGHVENDRA)
  @Category(UnitTests.class)
  public void testDeleteStackWithTimeout() {
    char[] accessKey = "abcd".toCharArray();
    char[] secretKey = "pqrs".toCharArray();
    String stackNameSuffix = "Stack Name 01";
    String roleArn = "RoleArn";
    CloudFormationDeleteStackRequest request =
        CloudFormationDeleteStackRequest.builder()
            .commandType(CloudFormationCommandType.DELETE_STACK)
            .accountId(ACCOUNT_ID)
            .appId(APP_ID)
            .activityId(ACTIVITY_ID)
            .commandName("Delete Stack")
            .cloudFormationRoleArn(roleArn)
            .awsConfig(AwsConfig.builder().accessKey(accessKey).accountId(ACCOUNT_ID).secretKey(secretKey).build())
            .timeoutInMs(1)
            .stackNameSuffix(stackNameSuffix)
            .build();
    String stackId = "Stack Id 01";

    List<Stack> existingStackList =
        singletonList(new Stack().withStackName("HarnessStack-" + stackNameSuffix).withStackId(stackId));
    List<Stack> stackList = singletonList(new Stack().withStackId(stackId).withStackStatus("DELETE_IN_PROGRESS"));
    doReturn(existingStackList).doReturn(stackList).when(mockAwsHelperService).getAllStacks(anyString(), any(), any());
    CloudFormationCommandExecutionResponse response = deleteStackHandler.execute(request, null);
    assertThat(response).isNotNull();
    assertThat(response.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.FAILURE);
    verify(mockAwsHelperService).deleteStack(anyString(), any(), any());
  }

  @Test
  @Owner(developers = RAGHVENDRA)
  @Category(UnitTests.class)
  public void testDeleteStackStatusDeleteFailed() {
    testFailureForDeleteStackStatus("DELETE_FAILED");
  }

  @Test
  @Owner(developers = RAGHVENDRA)
  @Category(UnitTests.class)
  public void testDeleteStackStatusUnknown() {
    testFailureForDeleteStackStatus("Unknown");
  }

  @Test
  @Owner(developers = RAGHVENDRA)
  @Category(UnitTests.class)
  public void testPrintStackEvents() {
    String stackName = "HarnessStack-test";
    long stackEventTs = 1000;
    String roleArn = "roleArn";
    String templateBody = "Template Body";
    char[] accessKey = "abcd".toCharArray();
    char[] secretKey = "pqrs".toCharArray();
    String stackNameSuffix = "Stack Name 00";
    Date timeStamp = new Date();
    CloudFormationCreateStackRequest request =
        CloudFormationCreateStackRequest.builder()
            .commandType(CloudFormationCommandType.CREATE_STACK)
            .accountId(ACCOUNT_ID)
            .appId(APP_ID)
            .activityId(ACTIVITY_ID)
            .cloudFormationRoleArn(roleArn)
            .commandName("Create Stack")
            .awsConfig(AwsConfig.builder().accessKey(accessKey).accountId(ACCOUNT_ID).secretKey(secretKey).build())
            .timeoutInMs(10 * 60 * 1000)
            .createType(CloudFormationCreateStackRequest.CLOUD_FORMATION_STACK_CREATE_BODY)
            .data(templateBody)
            .stackNameSuffix(stackNameSuffix)
            .build();
    Stack testStack = new Stack().withStackStatus("CREATE_COMPLETE").withStackName(stackName + stackNameSuffix);
    List<Stack> exitingList = singletonList(testStack);
    ExecutionLogCallback logCallback = mock(ExecutionLogCallback.class);
    StackEvent stackEvent = new StackEvent()
                                .withStackName(stackName)
                                .withEventId("id")
                                .withResourceStatusReason("statusReason")
                                .withTimestamp(timeStamp);
    when(mockAwsHelperService.getAllStackEvents(any(), any(), any())).thenReturn(singletonList(stackEvent));

    long resStackEventTs = createStackHandler.printStackEvents(request, stackEventTs, testStack, logCallback);
    String message = format("[%s] [%s] [%s] [%s] [%s]", stackEvent.getResourceStatus(), stackEvent.getResourceType(),
        stackEvent.getLogicalResourceId(), "statusReason", stackEvent.getPhysicalResourceId());
    verify(logCallback).saveExecutionLog(message);
    assertThat(resStackEventTs).isEqualTo(timeStamp.getTime());
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testListStacks() {
    char[] accessKey = "abcd".toCharArray();
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
    doReturn(stacks).when(mockAwsHelperService).getAllStacks(anyString(), any(), any());
    CloudFormationCommandExecutionResponse response = listStacksHandler.execute(request, null);
    assertThat(response).isNotNull();
    assertThat(response.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
    CloudFormationCommandResponse formationCommandResponse = response.getCommandResponse();
    assertThat(formationCommandResponse).isNotNull();
    assertThat(formationCommandResponse instanceof CloudFormationListStacksResponse).isTrue();
    CloudFormationListStacksResponse listStacksResponse = (CloudFormationListStacksResponse) formationCommandResponse;
    List<StackSummaryInfo> summaryInfos = listStacksResponse.getStackSummaryInfos();
    assertThat(summaryInfos).isNotNull();
    assertThat(summaryInfos).hasSize(2);
    validateStackSummaryInfo(summaryInfos.get(0), "sId1", "sName1", "sStatus1", "sReason1");
    validateStackSummaryInfo(summaryInfos.get(1), "sId2", "sName2", "sStatus2", "sReason2");
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testListStacksWithException() {
    char[] accessKey = "abcd".toCharArray();
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
            .stackId("stackId")
            .build();
    Exception ex = new RuntimeException("This is an exception");
    doThrow(ex).when(mockAwsHelperService).getAllStacks(anyString(), any(), any());
    CloudFormationCommandExecutionResponse response = listStacksHandler.execute(request, null);
    assertThat(response).isNotNull();
    assertThat(response.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.FAILURE);
    CloudFormationCommandResponse formationCommandResponse = response.getCommandResponse();
    assertThat(formationCommandResponse).isNull();
  }

  @Test
  @Owner(developers = RAGHVENDRA)
  @Category(UnitTests.class)
  public void testCreateStackCreateTypeUrl() {
    String templateBody = "Template Body";
    char[] accessKey = "abcd".toCharArray();
    char[] secretKey = "pqrs".toCharArray();
    String stackNameSuffix = "Stack Name 00";
    String roleArn = "roleArn";
    CloudFormationCreateStackRequest request =
        CloudFormationCreateStackRequest.builder()
            .commandType(CloudFormationCommandType.CREATE_STACK)
            .accountId(ACCOUNT_ID)
            .appId(APP_ID)
            .activityId(ACTIVITY_ID)
            .cloudFormationRoleArn(roleArn)
            .commandName("Create Stack")
            .awsConfig(AwsConfig.builder().accessKey(accessKey).accountId(ACCOUNT_ID).secretKey(secretKey).build())
            .timeoutInMs(10 * 60 * 1000)
            .createType(CloudFormationCreateStackRequest.CLOUD_FORMATION_STACK_CREATE_URL)
            .data(templateBody)
            .stackNameSuffix(stackNameSuffix)
            .build();
    CreateStackRequest createStackRequest = new CreateStackRequest()
                                                .withStackName("HarnessStack-" + stackNameSuffix)
                                                .withTemplateBody(templateBody)
                                                .withRoleARN(roleArn);
    String stackId = "Stack Id 00";
    CreateStackResult createStackResult = new CreateStackResult().withStackId(stackId);
    doReturn(createStackResult).when(mockAwsHelperService).createStack(anyString(), any(), any());
    List<Stack> createProgressList = singletonList(new Stack().withStackStatus("CREATE_IN_PROGRESS"));
    List<Stack> createCompleteList =
        singletonList(new Stack()
                          .withStackStatus("CREATE_COMPLETE")
                          .withOutputs(new Output().withOutputKey("vpcs").withOutputValue("vpcs"),
                              new Output().withOutputKey("subnets").withOutputValue("subnets"),
                              new Output().withOutputKey("securityGroups").withOutputValue("sgs")));
    doReturn(Collections.emptyList())
        .doReturn(createProgressList)
        .doReturn(createCompleteList)
        .when(mockAwsHelperService)
        .getAllStacks(anyString(), any(), any());

    CloudFormationCommandExecutionResponse response = createStackHandler.execute(request, null);
    assertThat(response).isNotNull();
    assertThat(response.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
    CloudFormationCommandResponse formationCommandResponse = response.getCommandResponse();
    assertThat(formationCommandResponse).isNotNull();
    assertThat(formationCommandResponse instanceof CloudFormationCreateStackResponse).isTrue();
    CloudFormationCreateStackResponse createStackResponse =
        (CloudFormationCreateStackResponse) formationCommandResponse;
    Map<String, Object> outputMap = createStackResponse.getCloudFormationOutputMap();
    assertThat(3).isEqualTo(outputMap.size());
    validateMapContents(outputMap, "vpcs", "vpcs");
    validateMapContents(outputMap, "subnets", "subnets");
    validateMapContents(outputMap, "securityGroups", "sgs");
    ExistingStackInfo existingStackInfo = createStackResponse.getExistingStackInfo();
    assertThat(existingStackInfo).isNotNull();
    assertThat(existingStackInfo.isStackExisted()).isFalse();
  }

  @Test
  @Owner(developers = RAGHVENDRA)
  @Category(UnitTests.class)
  public void testCreateStackCreateTypeUnknown() {
    String templateBody = "Template Body";
    char[] accessKey = "abcd".toCharArray();
    char[] secretKey = "pqrs".toCharArray();
    String stackNameSuffix = "Stack Name 00";
    String roleArn = "roleArn";
    CloudFormationCreateStackRequest request =
        CloudFormationCreateStackRequest.builder()
            .commandType(CloudFormationCommandType.CREATE_STACK)
            .accountId(ACCOUNT_ID)
            .appId(APP_ID)
            .activityId(ACTIVITY_ID)
            .cloudFormationRoleArn(roleArn)
            .commandName("Create Stack")
            .awsConfig(AwsConfig.builder().accessKey(accessKey).accountId(ACCOUNT_ID).secretKey(secretKey).build())
            .timeoutInMs(10 * 60 * 1000)
            .createType("Unknown")
            .data(templateBody)
            .stackNameSuffix(stackNameSuffix)
            .build();
    CreateStackRequest createStackRequest = new CreateStackRequest()
                                                .withStackName("HarnessStack-" + stackNameSuffix)
                                                .withTemplateBody(templateBody)
                                                .withRoleARN(roleArn);
    String stackId = "Stack Id 00";
    CreateStackResult createStackResult = new CreateStackResult().withStackId(stackId);
    doReturn(createStackResult).when(mockAwsHelperService).createStack(anyString(), any(), any());
    List<Stack> createProgressList = singletonList(new Stack().withStackStatus("CREATE_IN_PROGRESS"));
    List<Stack> createCompleteList =
        singletonList(new Stack()
                          .withStackStatus("CREATE_COMPLETE")
                          .withOutputs(new Output().withOutputKey("vpcs").withOutputValue("vpcs"),
                              new Output().withOutputKey("subnets").withOutputValue("subnets"),
                              new Output().withOutputKey("securityGroups").withOutputValue("sgs")));
    doReturn(Collections.emptyList())
        .doReturn(createProgressList)
        .doReturn(createCompleteList)
        .when(mockAwsHelperService)
        .getAllStacks(anyString(), any(), any());

    CloudFormationCommandExecutionResponse response = createStackHandler.execute(request, null);
    assertThat(response).isNotNull();
    assertThat(response.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.FAILURE);
    CloudFormationCommandResponse formationCommandResponse = response.getCommandResponse();
    assertThat(formationCommandResponse).isNull();
  }

  @Test
  @Owner(developers = RAGHVENDRA)
  @Category(UnitTests.class)
  public void testUpdateStackUpdateTypeUnknown() {
    String templateBody = "Template Body";
    char[] accessKey = "abcd".toCharArray();
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
            .createType("Unknown")
            .data(templateBody)
            .cloudFormationRoleArn("testRole")
            .stackNameSuffix(stackNameSuffix)
            .build();
    List<Stack> exitingList =
        singletonList(new Stack().withStackStatus("CREATE_COMPLETE").withStackName("HarnessStack-" + stackNameSuffix));
    List<Stack> updateProgressList = singletonList(new Stack().withStackStatus("UPDATE_IN_PROGRESS"));
    List<Stack> updateCompleteList = singletonList(new Stack().withStackStatus("UPDATE_COMPLETE"));
    doReturn(exitingList)
        .doReturn(updateProgressList)
        .doReturn(updateCompleteList)
        .when(mockAwsHelperService)
        .getAllStacks(anyString(), any(), any());
    doReturn("Body").when(mockAwsCFHelperServiceDelegate).getStackBody(any(), anyString(), anyString());

    CloudFormationCommandExecutionResponse response = createStackHandler.execute(request, null);
    assertThat(response).isNotNull();
    assertThat(response.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.FAILURE);
    CloudFormationCommandResponse formationCommandResponse = response.getCommandResponse();
    assertThat(formationCommandResponse).isNull();
  }

  @Test
  @Owner(developers = RAGHVENDRA)
  @Category(UnitTests.class)
  public void testUpdateStackUpdateTypeUrl() {
    String templateBody = "Template Body";
    char[] accessKey = "abcd".toCharArray();
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
            .createType(CloudFormationCreateStackRequest.CLOUD_FORMATION_STACK_CREATE_URL)
            .data(templateBody)
            .stackNameSuffix(stackNameSuffix)
            .build();
    List<Stack> exitingList =
        singletonList(new Stack().withStackStatus("CREATE_COMPLETE").withStackName("HarnessStack-" + stackNameSuffix));
    List<Stack> updateProgressList = singletonList(new Stack().withStackStatus("UPDATE_IN_PROGRESS"));
    List<Stack> updateCompleteList = singletonList(new Stack().withStackStatus("UPDATE_COMPLETE"));
    doReturn(exitingList)
        .doReturn(updateProgressList)
        .doReturn(updateCompleteList)
        .when(mockAwsHelperService)
        .getAllStacks(anyString(), any(), any());

    UpdateStackResult updateStackResult = new UpdateStackResult();
    updateStackResult.setStackId("StackId1");
    doReturn(updateStackResult).when(mockAwsHelperService).updateStack(anyString(), any(), any());
    doReturn("Body").when(mockAwsCFHelperServiceDelegate).getStackBody(any(), anyString(), anyString());

    CloudFormationCommandExecutionResponse response = createStackHandler.execute(request, null);
    assertThat(response).isNotNull();
    assertThat(response.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
    CloudFormationCommandResponse formationCommandResponse = response.getCommandResponse();
    assertThat(formationCommandResponse).isNotNull();
    assertThat(formationCommandResponse instanceof CloudFormationCreateStackResponse).isTrue();
    CloudFormationCreateStackResponse createStackResponse =
        (CloudFormationCreateStackResponse) formationCommandResponse;
    ExistingStackInfo existingStackInfo = createStackResponse.getExistingStackInfo();
    assertThat(existingStackInfo).isNotNull();
    assertThat(existingStackInfo.isStackExisted()).isTrue();
    assertThat("Body").isEqualTo(existingStackInfo.getOldStackBody());
  }

  @Test
  @Owner(developers = RAGHVENDRA)
  @Category(UnitTests.class)
  public void testCreateStackWithException() {
    String templateBody = "Template Body";
    char[] accessKey = "abcd".toCharArray();
    char[] secretKey = "pqrs".toCharArray();
    String stackNameSuffix = "Stack Name 00";
    String roleArn = "roleArn";
    CloudFormationCreateStackRequest request =
        CloudFormationCreateStackRequest.builder()
            .commandType(CloudFormationCommandType.CREATE_STACK)
            .accountId(ACCOUNT_ID)
            .appId(APP_ID)
            .activityId(ACTIVITY_ID)
            .cloudFormationRoleArn(roleArn)
            .commandName("Create Stack")
            .awsConfig(AwsConfig.builder().accessKey(accessKey).accountId(ACCOUNT_ID).secretKey(secretKey).build())
            .timeoutInMs(10 * 60 * 1000)
            .createType(CloudFormationCreateStackRequest.CLOUD_FORMATION_STACK_CREATE_BODY)
            .data(templateBody)
            .stackNameSuffix(stackNameSuffix)
            .build();
    RuntimeException ex = new RuntimeException("errorMessage");
    doThrow(ex).when(mockEncryptionService).decrypt(any(), any(), eq(false));
    String errorMessage = format("Exception: %s while executing CF task.", ExceptionUtils.getMessage(ex));
    CloudFormationCommandExecutionResponse response = createStackHandler.execute(request, null);
    assertThat(response.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.FAILURE);
    assertThat(response.getErrorMessage()).isEqualTo(errorMessage);
  }

  @Test
  @Owner(developers = RAGHVENDRA)
  @Category(UnitTests.class)
  public void testCreateStackRollbackProgressStatus() {
    testFailureForCreateStackStatus("ROLLBACK_IN_PROGRESS");
  }

  @Test
  @Owner(developers = RAGHVENDRA)
  @Category(UnitTests.class)
  public void testCreateStackCreateFailedStatus() {
    testFailureForCreateStackStatus("CREATE_FAILED");
  }

  @Test
  @Owner(developers = RAGHVENDRA)
  @Category(UnitTests.class)
  public void testCreateStackRollbackFailedStatus() {
    testFailureForCreateStackStatus("ROLLBACK_FAILED");
  }

  @Test
  @Owner(developers = RAGHVENDRA)
  @Category(UnitTests.class)
  public void testCreateStackRollbackCompleteStatus() {
    testFailureForCreateStackStatus("ROLLBACK_COMPLETE");
  }

  @Test
  @Owner(developers = RAGHVENDRA)
  @Category(UnitTests.class)
  public void testCreateStackDefaultStatus() {
    testFailureForCreateStackStatus("Unknown");
  }

  private void testFailureForDeleteStackStatus(String status) {
    char[] accessKey = "abcd".toCharArray();
    char[] secretKey = "pqrs".toCharArray();
    String stackNameSuffix = "Stack Name 01";
    String roleArn = "RoleArn";
    CloudFormationDeleteStackRequest request =
        CloudFormationDeleteStackRequest.builder()
            .commandType(CloudFormationCommandType.DELETE_STACK)
            .accountId(ACCOUNT_ID)
            .appId(APP_ID)
            .activityId(ACTIVITY_ID)
            .commandName("Delete Stack")
            .cloudFormationRoleArn(roleArn)
            .awsConfig(AwsConfig.builder().accessKey(accessKey).accountId(ACCOUNT_ID).secretKey(secretKey).build())
            .timeoutInMs(10 * 60 * 1000)
            .stackNameSuffix(stackNameSuffix)
            .build();
    String stackId = "Stack Id 01";

    List<Stack> existingStackList =
        singletonList(new Stack().withStackName("HarnessStack-" + stackNameSuffix).withStackId(stackId));
    List<Stack> stackList = singletonList(new Stack().withStackId(stackId).withStackStatus(status));
    doReturn(existingStackList).doReturn(stackList).when(mockAwsHelperService).getAllStacks(anyString(), any(), any());
    CloudFormationCommandExecutionResponse response = deleteStackHandler.execute(request, null);
    assertThat(response).isNotNull();
    assertThat(response.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.FAILURE);
    verify(mockAwsHelperService).deleteStack(anyString(), any(), any());
  }

  private void testFailureForCreateStackStatus(String status) {
    String templateBody = "Template Body";
    char[] accessKey = "abcd".toCharArray();
    char[] secretKey = "pqrs".toCharArray();
    String stackNameSuffix = "Stack Name 00";
    String roleArn = "roleArn";
    CloudFormationCreateStackRequest request =
        CloudFormationCreateStackRequest.builder()
            .commandType(CloudFormationCommandType.CREATE_STACK)
            .accountId(ACCOUNT_ID)
            .appId(APP_ID)
            .activityId(ACTIVITY_ID)
            .cloudFormationRoleArn(roleArn)
            .commandName("Create Stack")
            .awsConfig(AwsConfig.builder().accessKey(accessKey).accountId(ACCOUNT_ID).secretKey(secretKey).build())
            .timeoutInMs(10 * 60 * 1000)
            .createType(CloudFormationCreateStackRequest.CLOUD_FORMATION_STACK_CREATE_BODY)
            .data(templateBody)
            .stackNameSuffix(stackNameSuffix)
            .build();
    CreateStackRequest createStackRequest = new CreateStackRequest()
                                                .withStackName("HarnessStack-" + stackNameSuffix)
                                                .withTemplateBody(templateBody)
                                                .withRoleARN(roleArn);
    String stackId = "Stack Id 00";
    CreateStackResult createStackResult = new CreateStackResult().withStackId(stackId);
    doReturn(createStackResult).when(mockAwsHelperService).createStack(anyString(), any(), any());
    List<Stack> rollbackList =
        singletonList(new Stack().withStackStatus(status).withStackName("HarnessStack-" + stackNameSuffix));
    List<Stack> rollbackCompleteList = singletonList(
        new Stack().withStackStatus("ROLLBACK_COMPLETE").withStackName("HarnessStack-" + stackNameSuffix));

    if ("ROLLBACK_IN_PROGRESS".equalsIgnoreCase(status)) {
      doReturn(Collections.emptyList())
          .doReturn(rollbackList)
          .doReturn(rollbackCompleteList)
          .when(mockAwsHelperService)
          .getAllStacks(anyString(), any(), any());
    } else {
      doReturn(Collections.emptyList())
          .doReturn(rollbackList)
          .when(mockAwsHelperService)
          .getAllStacks(anyString(), any(), any());
    }

    CloudFormationCommandExecutionResponse response = createStackHandler.execute(request, null);
    assertThat(response).isNotNull();
    assertThat(response.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.FAILURE);
    CloudFormationCommandResponse formationCommandResponse = response.getCommandResponse();
    CloudFormationCreateStackResponse expectedCreateStackResponse =
        CloudFormationCreateStackResponse.builder().stackStatus(status).build();
    if ("ROLLBACK_IN_PROGRESS".equalsIgnoreCase(status)) {
      expectedCreateStackResponse =
          CloudFormationCreateStackResponse.builder().stackStatus("ROLLBACK_COMPLETE").build();
    }

    assertThat(formationCommandResponse).isEqualTo(expectedCreateStackResponse);
  }

  private void validateStackSummaryInfo(
      StackSummaryInfo info, String stackId, String stackName, String stackStatus, String stackReason) {
    assertThat(info).isNotNull();
    assertThat(stackId).isEqualTo(info.getStackId());
    assertThat(stackName).isEqualTo(info.getStackName());
    assertThat(stackStatus).isEqualTo(info.getStackStatus());
    assertThat(stackReason).isEqualTo(info.getStackStatusReason());
  }
}
