package software.wings.sm.states.provision;

import static io.harness.logging.CommandExecutionStatus.FAILURE;
import static io.harness.rule.OwnerRule.ARVIND;
import static io.harness.rule.OwnerRule.BOJANA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static software.wings.beans.CloudFormationSourceType.GIT;
import static software.wings.beans.CloudFormationSourceType.TEMPLATE_BODY;
import static software.wings.beans.CloudFormationSourceType.TEMPLATE_URL;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.TaskType.CLOUD_FORMATION_TASK;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.ACTIVITY_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.ENV_NAME;
import static software.wings.utils.WingsTestConstants.TAG_NAME;
import static software.wings.utils.WingsTestConstants.TEMPLATE_FILE_PATH;
import static software.wings.utils.WingsTestConstants.UUID;

import com.google.common.collect.ImmutableMap;

import io.harness.beans.DelegateTask;
import io.harness.beans.ExecutionStatus;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.ResponseData;
import io.harness.logging.CommandExecutionStatus;
import io.harness.rule.Owner;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.stubbing.Answer;
import software.wings.WingsBaseTest;
import software.wings.api.ScriptStateExecutionData;
import software.wings.api.cloudformation.CloudFormationElement;
import software.wings.api.cloudformation.CloudFormationOutputInfoElement;
import software.wings.api.cloudformation.CloudFormationRollbackInfoElement;
import software.wings.beans.Application;
import software.wings.beans.AwsConfig;
import software.wings.beans.CloudFormationInfrastructureProvisioner;
import software.wings.beans.Environment;
import software.wings.beans.GitConfig;
import software.wings.beans.GitFileConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TemplateExpression;
import software.wings.common.TemplateExpressionProcessor;
import software.wings.dl.WingsPersistence;
import software.wings.helpers.ext.cloudformation.request.CloudFormationCommandRequest;
import software.wings.helpers.ext.cloudformation.request.CloudFormationCreateStackRequest;
import software.wings.helpers.ext.cloudformation.response.CloudFormationCommandExecutionResponse;
import software.wings.helpers.ext.cloudformation.response.CloudFormationCreateStackResponse;
import software.wings.helpers.ext.cloudformation.response.CloudFormationRollbackInfo;
import software.wings.helpers.ext.cloudformation.response.ExistingStackInfo;
import software.wings.service.impl.yaml.GitClientHelper;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.InfrastructureProvisionerService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.settings.SettingVariableTypes;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.WorkflowStandardParams;
import software.wings.utils.GitUtilsManager;
import software.wings.utils.WingsTestConstants;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CloudFormationCreateStackStateTest extends WingsBaseTest {
  @Mock private AppService appService;
  @Mock private GitUtilsManager gitUtilsManager;
  @Mock private ExecutionContextImpl mockContext;
  @Mock private InfrastructureProvisionerService mockInfrastructureProvisionerService;
  @Mock private SecretManager mockSecretManager;
  @Mock private TemplateExpressionProcessor templateExpressionProcessor;
  @Mock private WingsPersistence wingsPersistence;
  @Mock private ActivityService activityService;
  @Spy private GitClientHelper gitClientHelper;

  @InjectMocks private CloudFormationCreateStackState state = new CloudFormationCreateStackState("stateName");

  private AwsConfig awsConfig = AwsConfig.builder().tag(TAG_NAME).build();
  private static final String repoUrl = "http://xyz.com/z.git";

  @Before
  public void setUp() {
    WorkflowStandardParams mockParams = mock(WorkflowStandardParams.class);
    doReturn(mockParams).when(mockContext).fetchWorkflowStandardParamsFromContext();
    Environment env = anEnvironment().appId(APP_ID).uuid(ENV_ID).name(ENV_NAME).build();
    doReturn(env).when(mockParams).fetchRequiredEnv();

    Application application = new Application();
    application.setAccountId(ACCOUNT_ID);
    application.setUuid(UUID);
    when(mockContext.getApp()).thenReturn(application);

    Answer<String> doReturnSameValue = invocation -> invocation.getArgumentAt(0, String.class);
    doAnswer(doReturnSameValue).when(mockContext).renderExpression(anyString());
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void buildDelegateTaskProvisionByUrl() {
    CloudFormationInfrastructureProvisioner provisioner = CloudFormationInfrastructureProvisioner.builder()
                                                              .sourceType(TEMPLATE_URL.name())
                                                              .templateFilePath(TEMPLATE_FILE_PATH)
                                                              .build();

    DelegateTask delegateTask = state.buildDelegateTask(mockContext, provisioner, awsConfig, ACTIVITY_ID);
    verifyDelegateTask(delegateTask);
    CloudFormationCreateStackRequest request =
        (CloudFormationCreateStackRequest) delegateTask.getData().getParameters()[0];
    assertThat(request.getData()).isEqualTo(TEMPLATE_FILE_PATH);
    assertThat(request.getCreateType()).isEqualTo(CloudFormationCreateStackRequest.CLOUD_FORMATION_STACK_CREATE_URL);
    assertThat(request.getCustomStackName()).isEqualTo(StringUtils.EMPTY);
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void buildDelegateTaskProvisionByBody() {
    CloudFormationInfrastructureProvisioner provisioner = CloudFormationInfrastructureProvisioner.builder()
                                                              .sourceType(TEMPLATE_BODY.name())
                                                              .templateBody(WingsTestConstants.TEMPLATE_BODY)
                                                              .build();
    state.customStackName = "customStackName";
    state.useCustomStackName = true;
    DelegateTask delegateTask = state.buildDelegateTask(mockContext, provisioner, awsConfig, ACTIVITY_ID);
    verifyDelegateTask(delegateTask);
    CloudFormationCreateStackRequest request =
        (CloudFormationCreateStackRequest) delegateTask.getData().getParameters()[0];
    assertThat(request.getData()).isEqualTo(WingsTestConstants.TEMPLATE_BODY);
    assertThat(request.getCreateType()).isEqualTo(CloudFormationCreateStackRequest.CLOUD_FORMATION_STACK_CREATE_BODY);
    assertThat(request.getCustomStackName()).isEqualTo("customStackName");
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void buildDelegateTaskProvisionByGitRepo() {
    CloudFormationInfrastructureProvisioner provisioner = CloudFormationInfrastructureProvisioner.builder()
                                                              .sourceType(GIT.name())
                                                              .gitFileConfig(GitFileConfig.builder()
                                                                                 .connectorId("sourceRepoSettingId")
                                                                                 .branch("gitBranch")
                                                                                 .commitId("commitId")
                                                                                 .build())
                                                              .build();

    GitConfig gitConfig = GitConfig.builder().urlType(GitConfig.UrlType.REPO).repoUrl(repoUrl).build();
    when(gitUtilsManager.getGitConfig("sourceRepoSettingId")).thenReturn(gitConfig);

    DelegateTask delegateTask = state.buildDelegateTask(mockContext, provisioner, awsConfig, ACTIVITY_ID);
    verifyDelegateTask(delegateTask);
    CloudFormationCreateStackRequest request =
        (CloudFormationCreateStackRequest) delegateTask.getData().getParameters()[0];
    assertThat(request.getGitConfig()).isEqualTo(gitConfig);
    assertThat(request.getGitConfig().getBranch()).isEqualTo("gitBranch");
    assertThat(request.getGitConfig().getReference()).isEqualTo("commitId");
    assertThat(request.getGitConfig().getRepoName()).isNull();
    assertThat(request.getGitConfig().getRepoUrl()).isEqualTo(repoUrl);
    assertThat(request.getCreateType()).isEqualTo(CloudFormationCreateStackRequest.CLOUD_FORMATION_STACK_CREATE_GIT);
  }

  @Test
  @Owner(developers = ARVIND)
  @Category(UnitTests.class)
  public void buildDelegateTaskProvisionByGitAccount() {
    CloudFormationInfrastructureProvisioner provisioner = CloudFormationInfrastructureProvisioner.builder()
                                                              .sourceType(GIT.name())
                                                              .gitFileConfig(GitFileConfig.builder()
                                                                                 .connectorId("sourceRepoSettingId")
                                                                                 .branch("gitBranch")
                                                                                 .commitId("commitId")
                                                                                 .repoName("z.git")
                                                                                 .build())
                                                              .build();

    GitConfig gitConfig = GitConfig.builder().urlType(GitConfig.UrlType.ACCOUNT).repoUrl("http://xyz.com").build();
    when(gitUtilsManager.getGitConfig("sourceRepoSettingId")).thenReturn(gitConfig);

    DelegateTask delegateTask = state.buildDelegateTask(mockContext, provisioner, awsConfig, ACTIVITY_ID);
    verifyDelegateTask(delegateTask);
    CloudFormationCreateStackRequest request =
        (CloudFormationCreateStackRequest) delegateTask.getData().getParameters()[0];
    assertThat(request.getGitConfig()).isEqualTo(gitConfig);
    assertThat(request.getGitConfig().getBranch()).isEqualTo("gitBranch");
    assertThat(request.getGitConfig().getReference()).isEqualTo("commitId");
    assertThat(request.getGitConfig().getRepoName()).isEqualTo("z.git");
    assertThat(request.getGitConfig().getRepoUrl()).isEqualTo(repoUrl);
    assertThat(request.getCreateType()).isEqualTo(CloudFormationCreateStackRequest.CLOUD_FORMATION_STACK_CREATE_GIT);
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void testHandleResponse() {
    CloudFormationRollbackInfo cloudFormationRollbackInfo =
        CloudFormationRollbackInfo.builder().url(TEMPLATE_FILE_PATH).build();
    ExistingStackInfo existingStackInfo = ExistingStackInfo.builder().oldStackBody("oldStackBody").build();
    Map<String, Object> cloudFormationOutputMap = new HashMap<>();
    cloudFormationOutputMap.put("key1", CloudFormationOutputInfoElement.builder().build());
    CloudFormationCreateStackResponse createStackResponse =
        new CloudFormationCreateStackResponse(CommandExecutionStatus.SUCCESS, "output", cloudFormationOutputMap,
            "stackId", existingStackInfo, cloudFormationRollbackInfo);

    TemplateExpression templateExpression = TemplateExpression.builder().build();
    doReturn(ScriptStateExecutionData.builder().build()).when(mockContext).getStateExecutionData();
    when(templateExpressionProcessor.getTemplateExpression(anyList(), anyString())).thenReturn(templateExpression);

    SettingAttribute settingAttribute = mock(SettingAttribute.class);
    when(templateExpressionProcessor.resolveSettingAttributeByNameOrId(
             eq(mockContext), eq(templateExpression), eq(SettingVariableTypes.AWS)))
        .thenReturn(settingAttribute);
    when(settingAttribute.getUuid()).thenReturn(UUID);

    state.setTemplateExpressions(Arrays.asList(templateExpression));
    state.customStackName = "customStackName";
    state.useCustomStackName = true;
    List<CloudFormationElement> cloudFormationElementList = state.handleResponse(createStackResponse, mockContext);

    assertThat(cloudFormationElementList.size()).isEqualTo(2);
    CloudFormationElement cloudFormationElement0 = cloudFormationElementList.get(0);
    assertThat(cloudFormationElement0).isInstanceOf(CloudFormationRollbackInfoElement.class);
    CloudFormationRollbackInfoElement cloudFormationRollbackInfoElement =
        (CloudFormationRollbackInfoElement) cloudFormationElement0;
    assertThat(cloudFormationRollbackInfoElement.getOldStackBody()).isEqualTo("oldStackBody");
    assertThat(cloudFormationRollbackInfoElement.getCustomStackName()).isEqualTo("customStackName");
    assertThat(cloudFormationRollbackInfoElement.getAwsConfigId()).isEqualTo(UUID);

    CloudFormationElement cloudFormationElement1 = cloudFormationElementList.get(1);
    assertThat(cloudFormationElement1).isInstanceOf(CloudFormationOutputInfoElement.class);
    CloudFormationOutputInfoElement cloudFormationOutputInfoElement =
        (CloudFormationOutputInfoElement) cloudFormationElement1;
    assertThat(cloudFormationOutputInfoElement.getNewStackOutputs()).containsKeys("key1");
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void testHandleAsyncResponseFailure() {
    Map<String, ResponseData> delegateResponse = ImmutableMap.of(ACTIVITY_ID,
        CloudFormationCommandExecutionResponse.builder()
            .commandExecutionStatus(FAILURE)
            .commandResponse(CloudFormationCreateStackResponse.builder().commandExecutionStatus(FAILURE).build())
            .build());
    ExecutionResponse response = state.handleAsyncResponse(mockContext, delegateResponse);
    assertThat(response.getExecutionStatus()).isEqualTo(ExecutionStatus.FAILED);
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void testHandleResponseFailure() {
    CloudFormationCreateStackResponse cloudFormationCreateStackResponse =
        CloudFormationCreateStackResponse.builder().commandExecutionStatus(FAILURE).build();
    List<CloudFormationElement> cloudFormationElements =
        state.handleResponse(cloudFormationCreateStackResponse, mockContext);
    assertThat(cloudFormationElements).isEqualTo(Collections.emptyList());
  }

  private void verifyDelegateTask(DelegateTask delegateTask) {
    assertThat(delegateTask.getData().getTaskType()).isEqualTo(CLOUD_FORMATION_TASK.name());
    assertThat(delegateTask.getWaitId()).isEqualTo(ACTIVITY_ID);
    assertThat(delegateTask.getTags()).isNotEmpty();
    assertThat(delegateTask.getTags().get(0)).isEqualTo(TAG_NAME);
    assertThat(delegateTask.getAccountId()).isEqualTo(ACCOUNT_ID);

    assertThat(delegateTask.getData().getParameters()).isNotNull();
    assertThat(delegateTask.getData().getParameters().length).isEqualTo(2);
    CloudFormationCreateStackRequest request =
        (CloudFormationCreateStackRequest) delegateTask.getData().getParameters()[0];
    assertThat(request.getCommandType()).isEqualTo(CloudFormationCommandRequest.CloudFormationCommandType.CREATE_STACK);
    assertThat(request.getAccountId()).isEqualTo(ACCOUNT_ID);
    assertThat(request.getAwsConfig()).isEqualTo(awsConfig);
  }
}
