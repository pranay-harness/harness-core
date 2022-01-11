/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.sm.states.provision;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;
import static io.harness.rule.OwnerRule.BOJANA;

import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.helpers.ext.cloudformation.request.CloudFormationCommandRequest.CloudFormationCommandType.DELETE_STACK;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.ACTIVITY_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.ENV_NAME;
import static software.wings.utils.WingsTestConstants.TAG_NAME;
import static software.wings.utils.WingsTestConstants.UUID;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DelegateTask;
import io.harness.beans.ExecutionStatus;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.tasks.ResponseData;

import software.wings.WingsBaseTest;
import software.wings.api.cloudformation.CloudFormationElement;
import software.wings.beans.Application;
import software.wings.beans.AwsConfig;
import software.wings.beans.CloudFormationInfrastructureProvisioner;
import software.wings.beans.Environment;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TemplateExpression;
import software.wings.beans.infrastructure.CloudFormationRollbackConfig;
import software.wings.common.TemplateExpressionProcessor;
import software.wings.dl.WingsPersistence;
import software.wings.helpers.ext.cloudformation.request.CloudFormationDeleteStackRequest;
import software.wings.helpers.ext.cloudformation.response.CloudFormationCommandExecutionResponse;
import software.wings.helpers.ext.cloudformation.response.CloudFormationCommandResponse;
import software.wings.helpers.ext.cloudformation.response.CloudFormationCreateStackResponse;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.InfrastructureProvisionerService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.StateExecutionService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.settings.SettingVariableTypes;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.WorkflowStandardParams;

import com.google.common.collect.ImmutableMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;
import org.mongodb.morphia.query.Query;

@OwnedBy(CDP)
public class CloudFormationDeleteStackStateTest extends WingsBaseTest {
  @Mock private ExecutionContextImpl mockContext;
  @Mock private InfrastructureProvisionerService infrastructureProvisionerService;
  @Mock private TemplateExpressionProcessor templateExpressionProcessor;
  @Mock private SecretManager secretManager;
  @Mock private DelegateService delegateService;
  @Mock private WingsPersistence wingsPersistence;
  @Mock private ActivityService activityService;
  @Mock private SettingsService settingsService;
  @Mock private StateExecutionService stateExecutionService;

  @InjectMocks private CloudFormationDeleteStackState state = new CloudFormationDeleteStackState("stateName");

  @Before
  public void setUp() {
    Answer<String> doReturnSameValue = invocation -> invocation.getArgumentAt(0, String.class);
    doAnswer(doReturnSameValue).when(mockContext).renderExpression(anyString());

    WorkflowStandardParams mockParams = mock(WorkflowStandardParams.class);
    doReturn(mockParams).when(mockContext).fetchWorkflowStandardParamsFromContext();
    Environment env = anEnvironment().appId(APP_ID).uuid(ENV_ID).name(ENV_NAME).build();
    doReturn(env).when(mockParams).fetchRequiredEnv();

    Application application = new Application();
    application.setAccountId(ACCOUNT_ID);
    application.setUuid(UUID);
    when(mockContext.getApp()).thenReturn(application);
    doNothing().when(stateExecutionService).appendDelegateTaskDetails(anyString(), any());

    state.useCustomStackName = true;
    state.customStackName = "customStackName";
    state.setCloudFormationRoleArn("cloudFormationRoleArn");
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void testExecuteInternal() {
    CloudFormationInfrastructureProvisioner cloudFormationInfrastructureProvisioner =
        CloudFormationInfrastructureProvisioner.builder().build();
    when(infrastructureProvisionerService.get(anyString(), anyString()))
        .thenReturn(cloudFormationInfrastructureProvisioner);
    TemplateExpression templateExpression = TemplateExpression.builder().build();
    when(templateExpressionProcessor.getTemplateExpression(eq(Arrays.asList(templateExpression)), eq("awsConfigId")))
        .thenReturn(templateExpression);

    SettingAttribute settingAttribute =
        aSettingAttribute().withValue(AwsConfig.builder().tag(TAG_NAME).build()).build();
    when(templateExpressionProcessor.resolveSettingAttributeByNameOrId(
             eq(mockContext), eq(templateExpression), eq(SettingVariableTypes.AWS)))
        .thenReturn(settingAttribute);

    state.setTemplateExpressions(Arrays.asList(templateExpression));
    ExecutionResponse executionResponse = state.executeInternal(mockContext, ACTIVITY_ID);
    verifyDelegate(executionResponse, true, 1);

    // no tags in awsConfig
    settingAttribute.setValue(AwsConfig.builder().build());
    when(templateExpressionProcessor.getTemplateExpression(eq(Arrays.asList(templateExpression)), eq("awsConfigId")))
        .thenReturn(null);
    when(settingsService.get(anyString())).thenReturn(settingAttribute);
    executionResponse = state.executeInternal(mockContext, ACTIVITY_ID);
    verifyDelegate(executionResponse, false, 2);
  }

  private void verifyDelegate(ExecutionResponse executionResponse, boolean checkTags, int i) {
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(ExecutionStatus.SUCCESS);
    ArgumentCaptor<DelegateTask> captor = ArgumentCaptor.forClass(DelegateTask.class);
    verify(delegateService, times(i)).queueTask(captor.capture());
    DelegateTask delegateTask = captor.getValue();
    assertThat(delegateTask).isNotNull();
    assertThat(delegateTask.getAccountId()).isEqualTo(ACCOUNT_ID);
    if (checkTags) {
      assertThat(delegateTask.getTags()).isNotEmpty();
      assertThat(delegateTask.getTags().get(0)).isEqualTo(TAG_NAME);
    }
    assertThat(delegateTask.getWaitId()).isEqualTo(ACTIVITY_ID);
    assertThat(delegateTask.getData().getParameters()).isNotNull();
    assertThat(delegateTask.getData().getParameters().length).isEqualTo(2);
    assertThat(delegateTask.getData().getParameters()[0] instanceof CloudFormationDeleteStackRequest).isTrue();
    CloudFormationDeleteStackRequest deleteStackRequest =
        (CloudFormationDeleteStackRequest) delegateTask.getData().getParameters()[0];
    assertThat(deleteStackRequest.getCommandType()).isEqualTo(DELETE_STACK);
    assertThat(deleteStackRequest.getCustomStackName()).isEqualTo("customStackName");
    assertThat(deleteStackRequest.getAccountId()).isEqualTo(ACCOUNT_ID);
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void testHandleResponse() {
    Query query = mock(Query.class);
    doReturn(query).when(wingsPersistence).createQuery(any());
    when(query.filter(anyString(), any(Object.class))).thenReturn(query);

    CloudFormationCommandResponse commandResponse = mock(CloudFormationCommandResponse.class);
    List<CloudFormationElement> cloudFormationElements = state.handleResponse(commandResponse, mockContext);
    verify(wingsPersistence, times(1)).delete(query);
    verify(wingsPersistence, times(1)).createQuery(CloudFormationRollbackConfig.class);
    assertThat(cloudFormationElements).isEqualTo(emptyList());
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void testHandleAsyncResponseWithRollbackElements() {
    Query query = mock(Query.class);
    doReturn(query).when(wingsPersistence).createQuery(any());
    when(query.filter(anyString(), any(Object.class))).thenReturn(query);

    Map<String, ResponseData> delegateResponse = ImmutableMap.of(ACTIVITY_ID,
        CloudFormationCommandExecutionResponse.builder()
            .commandExecutionStatus(SUCCESS)
            .commandResponse(CloudFormationCreateStackResponse.builder().commandExecutionStatus(SUCCESS).build())
            .build());
    ExecutionResponse response = state.handleAsyncResponse(mockContext, delegateResponse);
    assertThat(response.getExecutionStatus()).isEqualTo(ExecutionStatus.SUCCESS);
    assertThat(response.getContextElements()).isEmpty();
    assertThat(response.getNotifyElements()).isEmpty();
  }
}
