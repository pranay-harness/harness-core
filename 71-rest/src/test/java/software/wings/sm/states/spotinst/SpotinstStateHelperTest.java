package software.wings.sm.states.spotinst;

import static io.harness.beans.OrchestrationWorkflowType.BASIC;
import static io.harness.beans.OrchestrationWorkflowType.BLUE_GREEN;
import static io.harness.beans.OrchestrationWorkflowType.CANARY;
import static io.harness.beans.OrchestrationWorkflowType.MULTI_SERVICE;
import static io.harness.beans.OrchestrationWorkflowType.ROLLING;
import static io.harness.rule.OwnerRule.ADWAIT;
import static io.harness.rule.OwnerRule.SATYAM;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.when;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.AwsAmiInfrastructureMapping.Builder.anAwsAmiInfrastructureMapping;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.TaskType.SPOTINST_COMMAND_TASK;
import static software.wings.beans.artifact.Artifact.Builder.anArtifact;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.ACTIVITY_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.ENV_NAME;
import static software.wings.utils.WingsTestConstants.INFRA_MAPPING_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_NAME;
import static software.wings.utils.WingsTestConstants.SETTING_ID;
import static software.wings.utils.WingsTestConstants.SPOTINST_SETTING_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_EXECUTION_ID;

import com.google.inject.Inject;

import io.harness.beans.DelegateTask;
import io.harness.beans.EmbeddedUser;
import io.harness.category.element.UnitTests;
import io.harness.delegate.task.aws.LoadBalancerDetailsForBGDeployment;
import io.harness.delegate.task.spotinst.request.SpotInstSetupTaskParameters;
import io.harness.delegate.task.spotinst.request.SpotInstTaskParameters;
import io.harness.rule.Owner;
import io.harness.spotinst.model.ElastiGroup;
import io.harness.spotinst.model.ElastiGroupCapacity;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import software.wings.WingsBaseTest;
import software.wings.api.PhaseElement;
import software.wings.api.ServiceElement;
import software.wings.beans.Activity;
import software.wings.beans.Application;
import software.wings.beans.AwsAmiInfrastructureMapping;
import software.wings.beans.AwsConfig;
import software.wings.beans.Environment;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SpotInstConfig;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.container.UserDataSpecification;
import software.wings.delegatetasks.aws.AwsCommandHelper;
import software.wings.service.impl.spotinst.SpotInstCommandRequest;
import software.wings.service.impl.spotinst.SpotInstCommandRequest.SpotInstCommandRequestBuilder;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.WorkflowStandardParams;

import java.util.Arrays;
import java.util.List;

public class SpotinstStateHelperTest extends WingsBaseTest {
  @Mock private AppService mockAppService;
  @Mock private InfrastructureMappingService mockInfrastructureMappingService;
  @Mock private SettingsService mockSettingsService;
  @Mock private SecretManager mockSecretManager;
  @Mock private ActivityService mockActivityService;
  @Mock private ServiceResourceService mockServiceResourceService;
  @Mock private AwsCommandHelper mockCommandHelper;

  @Spy @Inject @InjectMocks SpotInstStateHelper spotInstStateHelper;

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testAddLoadBalancerConfigAfterExpressionEvaluation() throws Exception {
    ExecutionContext context = mock(ExecutionContext.class);
    when(context.renderExpression(anyString())).thenAnswer(new Answer<String>() {
      @Override
      public String answer(InvocationOnMock invocation) throws Throwable {
        Object[] args = invocation.getArguments();
        return (String) args[0];
      }
    });

    List<LoadBalancerDetailsForBGDeployment> lbDetails =
        spotInstStateHelper.addLoadBalancerConfigAfterExpressionEvaluation(
            Arrays.asList(LoadBalancerDetailsForBGDeployment.builder()
                              .loadBalancerName("LB1")
                              .prodListenerPort("8080")
                              .stageListenerPort("80")
                              .build(),
                LoadBalancerDetailsForBGDeployment.builder()
                    .loadBalancerName("LB1")
                    .prodListenerPort("8080")
                    .stageListenerPort("80")
                    .build(),
                LoadBalancerDetailsForBGDeployment.builder()
                    .loadBalancerName("LB2")
                    .prodListenerPort("8080")
                    .stageListenerPort("80")
                    .build()),
            context);

    assertThat(lbDetails.size()).isEqualTo(2);
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testPrepareStateExecutionData() {
    ExecutionContextImpl mockContext = mock(ExecutionContextImpl.class);
    SpotInstServiceSetup mockState = mock(SpotInstServiceSetup.class);
    doReturn(3).when(mockState).getOlderActiveVersionCountToKeep();
    PhaseElement phaseElement =
        PhaseElement.builder()
            .serviceElement(ServiceElement.builder().uuid(SERVICE_ID).name(SERVICE_NAME).build())
            .build();
    doReturn(phaseElement).when(mockContext).getContextElement(any(), anyString());
    WorkflowStandardParams mockWorkflowStandardParams = mock(WorkflowStandardParams.class);
    doReturn(mockWorkflowStandardParams).when(mockContext).getContextElement(any());
    Environment environment = anEnvironment().uuid(ENV_ID).name(ENV_NAME).build();
    doReturn(environment).when(mockWorkflowStandardParams).fetchRequiredEnv();
    doReturn(environment).when(mockContext).fetchRequiredEnvironment();
    Application application = anApplication().appId(APP_ID).name("app-name").build();
    doReturn(application).when(mockContext).fetchRequiredApp();
    doReturn(application).when(mockAppService).get(anyString());
    EmbeddedUser currentUser = EmbeddedUser.builder().name("user").email("user@harness.io").build();
    doReturn(currentUser).when(mockWorkflowStandardParams).getCurrentUser();
    String image = "ami-id";
    Artifact artifact =
        anArtifact().withRevision(image).withDisplayName("ami-display-name").withUuid(ARTIFACT_ID).build();
    doReturn(artifact).when(mockContext).getDefaultArtifactForService(anyString());
    Activity activity = Activity.builder().uuid(ACTIVITY_ID).build();
    doReturn(activity).when(mockActivityService).save(any());
    AwsAmiInfrastructureMapping infrastructureMapping = anAwsAmiInfrastructureMapping()
                                                            .withSpotinstCloudProvider(SPOTINST_SETTING_ID)
                                                            .withComputeProviderSettingId(SETTING_ID)
                                                            .withRegion("us-east-1")
                                                            .build();
    doReturn(infrastructureMapping).when(mockInfrastructureMappingService).get(anyString(), anyString());
    SettingAttribute awsProvider = aSettingAttribute().withValue(AwsConfig.builder().build()).build();
    SettingAttribute spotinst = aSettingAttribute().withValue(SpotInstConfig.builder().build()).build();
    doReturn(awsProvider).doReturn(spotinst).when(mockSettingsService).get(anyString());
    doReturn(emptyList()).when(mockSecretManager).getEncryptionDetails(any(), anyString(), anyString());
    String userData = "userData";
    UserDataSpecification userDataSpecification = UserDataSpecification.builder().data(userData).build();
    doReturn(userDataSpecification).when(mockServiceResourceService).getUserDataSpecification(anyString(), anyString());
    String elastigroupNamePrefix = "cdteam";
    String elastigroupJson = "{\n"
        + "  \"group\":{\n"
        + "    \"name\":\"adwait__11\",\n"
        + "    \"capacity\":{\n"
        + "      \"minimum\":1,\n"
        + "      \"maximum\":3,\n"
        + "      \"target\":1,\n"
        + "      \"unit\":\"instance\"\n"
        + "    }\n"
        + "  }\n"
        + "}";
    doReturn(elastigroupNamePrefix).when(mockState).getElastiGroupNamePrefix();
    doReturn(elastigroupNamePrefix)
        .doReturn(elastigroupJson)
        .doReturn(userData)
        .when(mockContext)
        .renderExpression(anyString());
    SpotInstSetupStateExecutionData executionData =
        spotInstStateHelper.prepareStateExecutionData(mockContext, mockState);
    assertThat(executionData).isNotNull();
    SpotInstCommandRequest spotinstCommandRequest = executionData.getSpotinstCommandRequest();
    assertThat(spotinstCommandRequest).isNotNull();
    SpotInstTaskParameters spotInstTaskParameters = spotinstCommandRequest.getSpotInstTaskParameters();
    assertThat(spotInstTaskParameters instanceof SpotInstSetupTaskParameters).isTrue();
    SpotInstSetupTaskParameters setupParams = (SpotInstSetupTaskParameters) spotInstTaskParameters;
    assertThat(setupParams.getElastiGroupNamePrefix()).isEqualTo(elastigroupNamePrefix);
    assertThat(setupParams.getImage()).isEqualTo(image);
    assertThat(setupParams.getUserData()).isEqualTo("dXNlckRhdGE=");
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testGenerateSpotInstCommandRequest() {
    AwsAmiInfrastructureMapping infrastructureMapping = anAwsAmiInfrastructureMapping()
                                                            .withSpotinstCloudProvider(SPOTINST_SETTING_ID)
                                                            .withComputeProviderSettingId(SETTING_ID)
                                                            .withRegion("us-east-1")
                                                            .build();
    ExecutionContextImpl mockContext = mock(ExecutionContextImpl.class);
    doReturn(APP_ID).when(mockContext).getAppId();
    doReturn(WORKFLOW_EXECUTION_ID).when(mockContext).getWorkflowExecutionId();
    SettingAttribute awsProvider = aSettingAttribute().withValue(AwsConfig.builder().build()).build();
    SettingAttribute spotinstProvider = aSettingAttribute().withValue(SpotInstConfig.builder().build()).build();
    doReturn(spotinstProvider).doReturn(awsProvider).when(mockSettingsService).get(anyString());
    doReturn(emptyList()).when(mockSecretManager).getEncryptionDetails(any(), anyString(), anyString());
    SpotInstCommandRequestBuilder builder =
        spotInstStateHelper.generateSpotInstCommandRequest(infrastructureMapping, mockContext);
    assertThat(builder).isNotNull();
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testPrepareNewElastiGroupConfigForRollback() {
    String id = "newId";
    SpotInstSetupContextElement element =
        SpotInstSetupContextElement.builder()
            .newElastiGroupOriginalConfig(
                ElastiGroup.builder()
                    .id(id)
                    .capacity(ElastiGroupCapacity.builder().minimum(1).maximum(1).target(1).build())
                    .build())
            .build();
    ElastiGroup elastiGroup = spotInstStateHelper.prepareNewElastiGroupConfigForRollback(element);
    assertThat(elastiGroup).isNotNull();
    ElastiGroupCapacity capacity = elastiGroup.getCapacity();
    assertThat(capacity).isNotNull();
    assertThat(capacity.getTarget()).isEqualTo(0);
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testPrepareOldElastiGroupConfigForRollback() {
    String id = "oldId";
    SpotInstSetupContextElement element =
        SpotInstSetupContextElement.builder()
            .oldElastiGroupOriginalConfig(
                ElastiGroup.builder()
                    .id(id)
                    .capacity(ElastiGroupCapacity.builder().minimum(1).maximum(1).target(1).build())
                    .build())
            .build();
    ElastiGroup elastiGroup = spotInstStateHelper.prepareOldElastiGroupConfigForRollback(element);
    assertThat(elastiGroup).isNotNull();
    ElastiGroupCapacity capacity = elastiGroup.getCapacity();
    assertThat(capacity).isNotNull();
    assertThat(capacity.getTarget()).isEqualTo(1);
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testGetDelegateTask() {
    String tag = "tag";
    doReturn(singletonList(tag)).when(mockCommandHelper).nonEmptyTag(any());
    DelegateTask task = spotInstStateHelper.getDelegateTask(ACCOUNT_ID, APP_ID, SPOTINST_COMMAND_TASK, ACTIVITY_ID,
        ENV_ID, INFRA_MAPPING_ID,
        SpotInstCommandRequest.builder()
            .awsConfig(AwsConfig.builder().build())
            .spotInstTaskParameters(SpotInstSetupTaskParameters.builder().build())
            .build());
    assertThat(task).isNotNull();
    assertThat(task.getTags().size()).isEqualTo(1);
    assertThat(task.getTags().get(0)).isEqualTo(tag);
    verify(mockCommandHelper).nonEmptyTag(any());
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testIsBlueGreenWorkflow() {
    ExecutionContext execContext = mock(ExecutionContext.class);
    doReturn(BASIC)
        .doReturn(CANARY)
        .doReturn(ROLLING)
        .doReturn(MULTI_SERVICE)
        .doReturn(BLUE_GREEN)
        .when(execContext)
        .getOrchestrationWorkflowType();

    assertThat(spotInstStateHelper.isBlueGreenWorkflow(execContext)).isFalse();
    assertThat(spotInstStateHelper.isBlueGreenWorkflow(execContext)).isFalse();
    assertThat(spotInstStateHelper.isBlueGreenWorkflow(execContext)).isFalse();
    assertThat(spotInstStateHelper.isBlueGreenWorkflow(execContext)).isFalse();
    assertThat(spotInstStateHelper.isBlueGreenWorkflow(execContext)).isTrue();
  }
}