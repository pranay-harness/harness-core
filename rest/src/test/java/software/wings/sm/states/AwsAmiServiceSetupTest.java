package software.wings.sm.states;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static software.wings.api.PhaseElement.PhaseElementBuilder.aPhaseElement;
import static software.wings.api.ServiceElement.Builder.aServiceElement;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.artifact.Artifact.Builder.anArtifact;
import static software.wings.beans.command.Command.Builder.aCommand;
import static software.wings.beans.command.ServiceCommand.Builder.aServiceCommand;
import static software.wings.common.Constants.BUILD_NO;
import static software.wings.sm.StateExecutionInstance.Builder.aStateExecutionInstance;
import static software.wings.sm.WorkflowStandardParams.Builder.aWorkflowStandardParams;
import static software.wings.sm.states.AwsAmiServiceSetup.AUTOSCALING_GROUP_RESOURCE_TYPE;
import static software.wings.sm.states.AwsAmiServiceSetup.HARNESS_AUTOSCALING_GROUP_TAG;
import static software.wings.sm.states.AwsAmiServiceSetup.NAME_TAG;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.ACTIVITY_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.APP_NAME;
import static software.wings.utils.WingsTestConstants.ARTIFACT_ID;
import static software.wings.utils.WingsTestConstants.COMPUTE_PROVIDER_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.ENV_NAME;
import static software.wings.utils.WingsTestConstants.INFRA_MAPPING_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_NAME;
import static software.wings.utils.WingsTestConstants.STATE_NAME;
import static software.wings.utils.WingsTestConstants.WORKFLOW_EXECUTION_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_NAME;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import com.amazonaws.services.autoscaling.model.AmazonAutoScalingException;
import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.CreateAutoScalingGroupRequest;
import com.amazonaws.services.autoscaling.model.LaunchConfiguration;
import com.amazonaws.services.autoscaling.model.Tag;
import com.amazonaws.services.autoscaling.model.TagDescription;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import software.wings.WingsBaseTest;
import software.wings.api.AmiServiceSetupElement;
import software.wings.api.DeploymentType;
import software.wings.api.PhaseElement;
import software.wings.api.PhaseStepExecutionData;
import software.wings.api.ServiceElement;
import software.wings.beans.Activity;
import software.wings.beans.Application;
import software.wings.beans.AwsAmiInfrastructureMapping;
import software.wings.beans.AwsConfig;
import software.wings.beans.Environment;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.ResizeStrategy;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.WorkflowType;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.command.AmiCommandUnit;
import software.wings.beans.command.CommandType;
import software.wings.beans.command.ServiceCommand;
import software.wings.beans.container.ContainerDefinition;
import software.wings.common.Constants;
import software.wings.service.impl.AwsHelperService;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.LogService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.WorkflowStandardParams;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class AwsAmiServiceSetupTest extends WingsBaseTest {
  @Mock private transient AwsHelperService awsHelperService;
  @Mock protected transient SettingsService settingsService;
  @Mock protected transient ServiceResourceService serviceResourceService;
  @Mock protected transient InfrastructureMappingService infrastructureMappingService;
  @Mock protected transient SecretManager secretManager;
  @Mock protected transient ActivityService activityService;
  @Mock protected transient ExecutorService executorService;
  @Mock protected transient LogService logService;
  @Mock protected transient ArtifactService artifactService;

  @InjectMocks
  private WorkflowStandardParams workflowStandardParams = aWorkflowStandardParams()
                                                              .withAppId(APP_ID)
                                                              .withEnvId(ENV_ID)
                                                              .withArtifactIds(Lists.newArrayList(ARTIFACT_ID))
                                                              .build();
  private ServiceElement serviceElement = aServiceElement().withUuid(SERVICE_ID).withName(SERVICE_NAME).build();
  private PhaseElement phaseElement = aPhaseElement()
                                          .withUuid(generateUuid())
                                          .withServiceElement(serviceElement)
                                          .withInfraMappingId(INFRA_MAPPING_ID)
                                          .withDeploymentType(DeploymentType.AMI.name())
                                          .build();

  private Application app = anApplication().withUuid(APP_ID).withName(APP_NAME).build();
  private Environment env = anEnvironment().withAppId(APP_ID).withUuid(ENV_ID).withName(ENV_NAME).build();
  private Service service = Service.builder().appId(APP_ID).uuid(SERVICE_ID).name(SERVICE_NAME).build();
  private Artifact artifact = anArtifact()
                                  .withArtifactSourceName("source")
                                  .withMetadata(ImmutableMap.of(BUILD_NO, "bn"))
                                  .withServiceIds(singletonList(SERVICE_ID))
                                  .build();

  private StateExecutionInstance stateExecutionInstance = aStateExecutionInstance()
                                                              .withDisplayName(STATE_NAME)
                                                              .addContextElement(workflowStandardParams)
                                                              .addContextElement(phaseElement)
                                                              .withStateExecutionData(new PhaseStepExecutionData())
                                                              .build();

  @Mock private ExecutionContextImpl context;

  @InjectMocks private AwsAmiServiceSetup amiServiceSetup = new AwsAmiServiceSetup("name");

  InfrastructureMapping infrastructureMapping = AwsAmiInfrastructureMapping.Builder.anAwsAmiInfrastructureMapping()
                                                    .withAutoScalingGroupName("TestAutoscalingGroup")
                                                    .withRegion("us-east1")
                                                    .withComputeProviderSettingId(COMPUTE_PROVIDER_ID)
                                                    .withUuid("TestUUID")
                                                    .build();

  @Before
  public void setup() {
    when(serviceResourceService.get(APP_ID, SERVICE_ID)).thenReturn(service);

    ServiceCommand serviceCommand =
        aServiceCommand()
            .withCommand(
                aCommand().withCommandType(CommandType.SETUP).withName(Constants.AMI_SETUP_COMMAND_NAME).build())
            .build();
    when(serviceResourceService.getCommandByName(APP_ID, SERVICE_ID, ENV_ID, Constants.AMI_SETUP_COMMAND_NAME))
        .thenReturn(serviceCommand);

    AmiCommandUnit amiCommandUnit = new AmiCommandUnit();
    amiCommandUnit.setName("TestAMISetup");
    when(serviceResourceService.getFlattenCommandUnitList(APP_ID, SERVICE_ID, ENV_ID, Constants.AMI_SETUP_COMMAND_NAME))
        .thenReturn(asList(amiCommandUnit));

    ContainerDefinition containerDefinition = ContainerDefinition.builder().memory(256).cpu(1).build();

    on(workflowStandardParams).set("app", app);
    on(workflowStandardParams).set("env", env);
    on(workflowStandardParams).set("artifactService", artifactService);

    when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID)).thenReturn(infrastructureMapping);

    when(artifactService.get(anyString(), anyString())).thenReturn(artifact);

    Activity activity = Activity.builder().build();
    activity.setUuid(ACTIVITY_ID);
    when(activityService.save(any(Activity.class))).thenReturn(activity);

    SettingAttribute awsConfig = aSettingAttribute()
                                     .withValue(AwsConfig.builder()
                                                    .accessKey("dummyAccess")
                                                    .secretKey("pass".toCharArray())
                                                    .encryptedSecretKey("encryptedSecretKey")
                                                    .accountId(ACCOUNT_ID)
                                                    .build())
                                     .build();

    when(settingsService.get(any())).thenReturn(awsConfig);
    when(secretManager.getEncryptionDetails(anyObject(), anyString(), anyString())).thenReturn(Collections.emptyList());

    when(context.getServiceVariables()).thenReturn(new HashMap<>());
    when(context.getStateExecutionInstance()).thenReturn(stateExecutionInstance);
    when(context.getContextElement(any(ContextElementType.class), anyString())).thenReturn(phaseElement);
    when(context.getContextElement(any(ContextElementType.class))).thenReturn(workflowStandardParams);
    when(context.getArtifactForService(anyString())).thenReturn(artifact);
    when(context.getAppId()).thenReturn(APP_ID);
    when(context.getWorkflowExecutionId()).thenReturn(WORKFLOW_EXECUTION_ID);
    when(context.getWorkflowExecutionName()).thenReturn(WORKFLOW_NAME);
    when(context.getWorkflowType()).thenReturn(WorkflowType.SIMPLE);
    when(context.renderExpression(anyString())).thenReturn("resolvedExpression");

    AutoScalingGroup mockAutoScalingGrp = Mockito.mock(AutoScalingGroup.class);
    when(awsHelperService.getAutoScalingGroup(any(AwsConfig.class), any(List.class), anyString(), anyString()))
        .thenReturn(mockAutoScalingGrp);
    LaunchConfiguration mockLaunchConfiguration = Mockito.mock(LaunchConfiguration.class);
    when(awsHelperService.getLaunchConfiguration(any(AwsConfig.class), any(List.class), anyString(), anyString()))
        .thenReturn(mockLaunchConfiguration);
    when(awsHelperService.listAutoScalingGroups(any(AwsConfig.class), any(List.class), anyString()))
        .thenReturn(new ArrayList<AutoScalingGroup>());
  }

  @Test
  public void testExecutionContext() {
    ExecutionResponse executionResponse = amiServiceSetup.execute(context);
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(ExecutionStatus.SUCCESS);
    assertThat(executionResponse.getContextElements().size()).isEqualTo(1);
    assertThat(executionResponse.getContextElements().get(0)).isInstanceOf(AmiServiceSetupElement.class);
    AmiServiceSetupElement amiServiceSetupElement =
        (AmiServiceSetupElement) executionResponse.getContextElements().get(0);
    assertThat(amiServiceSetupElement.getNewAutoScalingGroupName()).isEqualTo("APP_NAME__SERVICE_NAME__ENV_NAME__1");
    assertThat(amiServiceSetupElement.getOldAutoScalingGroupName()).isNull();
    assertThat(amiServiceSetupElement.getMaxInstances()).isEqualTo(10);
    assertThat(amiServiceSetupElement.getAutoScalingSteadyStateTimeout()).isEqualTo(10);
    assertThat(amiServiceSetupElement.getInstanceCount()).isEqualTo(0);
    assertThat(amiServiceSetupElement.getResizeStrategy()).isEqualTo(ResizeStrategy.RESIZE_NEW_FIRST);
    assertThat(amiServiceSetupElement.getCommandName()).isEqualTo(Constants.AMI_SETUP_COMMAND_NAME);
  }

  @Test
  public void testExecutionWithAwsServiceException() {
    when(awsHelperService.listAutoScalingGroups(any(AwsConfig.class), any(List.class), anyString()))
        .thenThrow(new AmazonAutoScalingException("TestException"));
    ExecutionResponse executionResponse = amiServiceSetup.execute(context);
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(ExecutionStatus.FAILED);
    assertThat(executionResponse.getContextElements().size()).isEqualTo(1);
    assertThat(executionResponse.getContextElements().get(0)).isInstanceOf(AmiServiceSetupElement.class);
    AmiServiceSetupElement serviceElement = (AmiServiceSetupElement) executionResponse.getContextElements().get(0);
    assertThat(serviceElement.getNewAutoScalingGroupName()).isNull();
    assertThat(serviceElement.getOldAutoScalingGroupName()).isNull();
    assertThat(serviceElement.getMaxInstances()).isNull();
    assertThat(serviceElement.getAutoScalingSteadyStateTimeout()).isNull();
    assertThat(serviceElement.getInstanceCount()).isEqualTo(0);
    assertThat(serviceElement.getResizeStrategy()).isNull();
    assertThat(serviceElement.getCommandName()).isNull();
  }

  @Test
  public void testCreateNewAutoScalingGroupRequestWithZeroTags() {
    AutoScalingGroup baseAutoScalingGroup = new AutoScalingGroup();
    Collection<TagDescription> tags = new ArrayList<>();
    baseAutoScalingGroup.setTags(tags);

    CreateAutoScalingGroupRequest request = amiServiceSetup.createNewAutoScalingGroupRequest(
        (AwsAmiInfrastructureMapping) infrastructureMapping, "NewAutoScalingGroup", baseAutoScalingGroup, 10);

    assertThat(request.getTags().size()).isEqualTo(2);
    Tag autoScalingGroupTag = request.getTags().get(0);
    assertThat(autoScalingGroupTag.getKey()).isEqualTo(HARNESS_AUTOSCALING_GROUP_TAG);
    assertThat(autoScalingGroupTag.getValue()).isEqualTo("TestUUID__10");
    assertThat(autoScalingGroupTag.isPropagateAtLaunch()).isTrue();
    assertThat(autoScalingGroupTag.getResourceType()).isEqualTo(AUTOSCALING_GROUP_RESOURCE_TYPE);

    Tag nameTag = request.getTags().get(1);
    assertThat(nameTag.getKey()).isEqualTo(NAME_TAG);
    assertThat(nameTag.getValue()).isEqualTo("NewAutoScalingGroup");
    assertThat(nameTag.isPropagateAtLaunch()).isTrue();
  }

  @Test
  public void testCreateNewAutocalingGroupRequestWithAutoScalingAndNameTags() {
    AutoScalingGroup baseAutoScalingGroup = new AutoScalingGroup();
    baseAutoScalingGroup.getTags().add(
        new TagDescription().withKey(HARNESS_AUTOSCALING_GROUP_TAG).withValue("OldValue"));
    baseAutoScalingGroup.getTags().add(new TagDescription().withKey(NAME_TAG).withValue("OldName"));

    CreateAutoScalingGroupRequest request = amiServiceSetup.createNewAutoScalingGroupRequest(
        (AwsAmiInfrastructureMapping) infrastructureMapping, "NewAutoScalingGroup", baseAutoScalingGroup, 10);

    assertThat(request.getTags().size()).isEqualTo(2);
    Tag autoScalingGroupTag = request.getTags().get(0);
    assertThat(autoScalingGroupTag.getKey()).isEqualTo(HARNESS_AUTOSCALING_GROUP_TAG);
    assertThat(autoScalingGroupTag.getValue()).isEqualTo("TestUUID__10");
    assertThat(autoScalingGroupTag.isPropagateAtLaunch()).isTrue();
    assertThat(autoScalingGroupTag.getResourceType()).isEqualTo(AUTOSCALING_GROUP_RESOURCE_TYPE);

    Tag nameTag = request.getTags().get(1);
    assertThat(nameTag.getKey()).isEqualTo(NAME_TAG);
    assertThat(nameTag.getValue()).isEqualTo("NewAutoScalingGroup");
    assertThat(nameTag.isPropagateAtLaunch()).isTrue();
  }

  @Test
  public void testCreateNewAutocalingGroupRequestWithExtraTags() {
    AutoScalingGroup baseAutoScalingGroup = new AutoScalingGroup();
    baseAutoScalingGroup.getTags().add(
        new TagDescription().withKey(HARNESS_AUTOSCALING_GROUP_TAG).withValue("OldValue"));
    baseAutoScalingGroup.getTags().add(new TagDescription().withKey(NAME_TAG).withValue("OldName"));
    baseAutoScalingGroup.getTags().add(new TagDescription().withKey("TestKey").withValue("TestValue"));

    CreateAutoScalingGroupRequest request = amiServiceSetup.createNewAutoScalingGroupRequest(
        (AwsAmiInfrastructureMapping) infrastructureMapping, "NewAutoScalingGroup", baseAutoScalingGroup, 10);

    assertThat(request.getTags().size()).isEqualTo(3);

    Tag testTag = request.getTags().get(0);
    assertThat(testTag.getKey()).isEqualTo("TestKey");
    assertThat(testTag.getValue()).isEqualTo("TestValue");

    Tag autoScalingGroupTag = request.getTags().get(1);
    assertThat(autoScalingGroupTag.getKey()).isEqualTo(HARNESS_AUTOSCALING_GROUP_TAG);
    assertThat(autoScalingGroupTag.getValue()).isEqualTo("TestUUID__10");
    assertThat(autoScalingGroupTag.isPropagateAtLaunch()).isTrue();
    assertThat(autoScalingGroupTag.getResourceType()).isEqualTo(AUTOSCALING_GROUP_RESOURCE_TYPE);

    Tag nameTag = request.getTags().get(2);
    assertThat(nameTag.getKey()).isEqualTo(NAME_TAG);
    assertThat(nameTag.getValue()).isEqualTo("NewAutoScalingGroup");
    assertThat(nameTag.isPropagateAtLaunch()).isTrue();
  }
}
