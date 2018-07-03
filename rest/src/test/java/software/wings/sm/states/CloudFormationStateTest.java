package software.wings.sm.states;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.joor.Reflect.on;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anySet;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;
import static software.wings.api.CommandStateExecutionData.Builder.aCommandStateExecutionData;
import static software.wings.api.PhaseElement.PhaseElementBuilder.aPhaseElement;
import static software.wings.api.ServiceElement.Builder.aServiceElement;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.AwsInfrastructureMapping.Builder.anAwsInfrastructureMapping;
import static software.wings.beans.CloudFormationSourceType.TEMPLATE_BODY;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.ResizeStrategy.RESIZE_NEW_FIRST;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.WorkflowExecution.WorkflowExecutionBuilder.aWorkflowExecution;
import static software.wings.beans.artifact.Artifact.Builder.anArtifact;
import static software.wings.beans.command.Command.Builder.aCommand;
import static software.wings.beans.command.ServiceCommand.Builder.aServiceCommand;
import static software.wings.common.Constants.BUILD_NO;
import static software.wings.sm.StateExecutionInstance.Builder.aStateExecutionInstance;
import static software.wings.sm.WorkflowStandardParams.Builder.aWorkflowStandardParams;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.ACTIVITY_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.APP_NAME;
import static software.wings.utils.WingsTestConstants.ARTIFACT_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.ENV_NAME;
import static software.wings.utils.WingsTestConstants.INFRA_MAPPING_ID;
import static software.wings.utils.WingsTestConstants.PCF_SERVICE_NAME;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_NAME;
import static software.wings.utils.WingsTestConstants.STATE_NAME;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import com.amazonaws.regions.Regions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.api.ContainerServiceElement;
import software.wings.api.DeploymentType;
import software.wings.api.PhaseElement;
import software.wings.api.ServiceElement;
import software.wings.app.MainConfiguration;
import software.wings.app.PortalConfig;
import software.wings.beans.Activity;
import software.wings.beans.Application;
import software.wings.beans.AwsConfig;
import software.wings.beans.CloudFormationInfrastructureProvisioner;
import software.wings.beans.DelegateTask;
import software.wings.beans.Environment;
import software.wings.beans.InfrastructureMappingBlueprint;
import software.wings.beans.NameValuePair;
import software.wings.beans.Service;
import software.wings.beans.ServiceVariable;
import software.wings.beans.ServiceVariable.Type;
import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.JenkinsArtifactStream;
import software.wings.beans.command.CommandType;
import software.wings.beans.command.ServiceCommand;
import software.wings.common.VariableProcessor;
import software.wings.expression.ExpressionEvaluator;
import software.wings.helpers.ext.cloudformation.request.CloudFormationCommandRequest.CloudFormationCommandType;
import software.wings.helpers.ext.cloudformation.request.CloudFormationCreateStackRequest;
import software.wings.helpers.ext.cloudformation.request.CloudFormationDeleteStackRequest;
import software.wings.service.ServiceHelper;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.InfrastructureProvisionerService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.WorkflowStandardParams;
import software.wings.sm.states.provision.CloudFormationCreateStackState;
import software.wings.sm.states.provision.CloudFormationDeleteStackState;

import java.util.Arrays;
import java.util.List;

public class CloudFormationStateTest extends WingsBaseTest {
  private static final String BASE_URL = "https://env.harness.io/";
  public static final String ENV_ID_CF = "abcdefgh";
  public static final String INFRA_PROV_ID = "12345678";
  public static final String EXPECTED_SUFFIX = "abcdefgh12345678";

  @Mock private SettingsService settingsService;
  @Mock private DelegateService delegateService;
  @Mock private ServiceResourceService serviceResourceService;
  @Mock private ActivityService activityService;
  @Mock private InfrastructureMappingService infrastructureMappingService;
  @Mock private AppService appService;
  @Mock private EnvironmentService environmentService;
  @Mock private ServiceTemplateService serviceTemplateService;
  @Mock private SecretManager secretManager;
  @Mock private WorkflowExecutionService workflowExecutionService;
  @Mock private ArtifactService artifactService;
  @Mock private ArtifactStreamService artifactStreamService;
  @Mock private VariableProcessor variableProcessor;
  @Mock private ExpressionEvaluator evaluator;
  @Mock private ServiceHelper serviceHelper;
  @Mock private InfrastructureProvisionerService infrastructureProvisionerService;

  @InjectMocks
  private CloudFormationCreateStackState cloudFormationCreateStackState = new CloudFormationCreateStackState("name");
  @InjectMocks
  private CloudFormationDeleteStackState cloudFormationDeleteStackState = new CloudFormationDeleteStackState("name");

  @Mock private MainConfiguration configuration;

  private ExecutionContext context;

  private WorkflowStandardParams workflowStandardParams = aWorkflowStandardParams()
                                                              .withAppId(APP_ID)
                                                              .withEnvId(ENV_ID)
                                                              .withArtifactIds(Lists.newArrayList(ARTIFACT_ID))
                                                              .build();

  private ServiceElement serviceElement = aServiceElement().withUuid(SERVICE_ID).withName(SERVICE_NAME).build();

  @InjectMocks
  private PhaseElement phaseElement = aPhaseElement()
                                          .withUuid(generateUuid())
                                          .withServiceElement(serviceElement)
                                          .withInfraMappingId(INFRA_MAPPING_ID)
                                          .withAppId(APP_ID)
                                          .withDeploymentType(DeploymentType.SSH.name())
                                          .build();

  private StateExecutionInstance stateExecutionInstance =
      aStateExecutionInstance()
          .withDisplayName(STATE_NAME)
          .addContextElement(workflowStandardParams)
          .addContextElement(phaseElement)
          .addContextElement(ContainerServiceElement.builder()
                                 .uuid(serviceElement.getUuid())
                                 .maxInstances(10)
                                 .name(PCF_SERVICE_NAME)
                                 .resizeStrategy(RESIZE_NEW_FIRST)
                                 .infraMappingId(INFRA_MAPPING_ID)
                                 .deploymentType(DeploymentType.PCF)
                                 .build())
          .addStateExecutionData(aCommandStateExecutionData().build())
          .build();

  private Application app = anApplication().withUuid(APP_ID).withName(APP_NAME).withAccountId(ACCOUNT_ID).build();
  private Environment env = anEnvironment().withAppId(APP_ID).withUuid(ENV_ID_CF).withName(ENV_NAME).build();
  private Service service = Service.builder().appId(APP_ID).uuid(SERVICE_ID).name(SERVICE_NAME).build();
  private Artifact artifact = anArtifact()
                                  .withArtifactSourceName("source")
                                  .withMetadata(ImmutableMap.of(BUILD_NO, "bn"))
                                  .withServiceIds(singletonList(SERVICE_ID))
                                  .build();
  private ArtifactStream artifactStream =
      JenkinsArtifactStream.builder().appId(APP_ID).sourceName("").jobname("").artifactPaths(null).build();

  private SettingAttribute awsConfig = aSettingAttribute().withValue(AwsConfig.builder().build()).build();

  private List<ServiceVariable> serviceVariableList =
      asList(ServiceVariable.builder().type(Type.TEXT).name("VAR_1").value("value1".toCharArray()).build(),
          ServiceVariable.builder().type(Type.ENCRYPTED_TEXT).name("VAR_2").value("value2".toCharArray()).build());

  private List<ServiceVariable> safeDisplayServiceVariableList =
      asList(ServiceVariable.builder().type(Type.TEXT).name("VAR_1").value("value1".toCharArray()).build(),
          ServiceVariable.builder().type(Type.ENCRYPTED_TEXT).name("VAR_2").value("*******".toCharArray()).build());

  @Before
  public void setup() {
    when(infrastructureProvisionerService.get(anyString(), anyString()))
        .thenReturn(
            CloudFormationInfrastructureProvisioner.builder()
                .uuid(INFRA_PROV_ID)
                .appId(APP_ID)
                .awsConfigId("id")
                .name("InfraMaappingProvisioner")
                .templateBody("Template Body")
                .sourceType(TEMPLATE_BODY.name())
                .mappingBlueprints(Arrays.asList(InfrastructureMappingBlueprint.builder()
                                                     .serviceId(SERVICE_ID)
                                                     .properties(Arrays.asList(NameValuePair.builder()
                                                                                   .name("$(cloudformation.region)")
                                                                                   .value(Regions.US_EAST_1.name())
                                                                                   .build()))
                                                     .build()))
                .build());

    when(appService.get(APP_ID)).thenReturn(app);
    when(appService.getApplicationWithDefaults(APP_ID)).thenReturn(app);
    when(serviceResourceService.get(APP_ID, SERVICE_ID)).thenReturn(service);
    when(environmentService.get(APP_ID, ENV_ID, false)).thenReturn(env);

    ServiceCommand serviceCommand =
        aServiceCommand()
            .withCommand(aCommand().withCommandType(CommandType.SETUP).withName("Setup Service Cluster").build())
            .build();
    when(serviceResourceService.getCommandByName(APP_ID, SERVICE_ID, ENV_ID, "Setup Service Cluster"))
        .thenReturn(serviceCommand);

    on(workflowStandardParams).set("appService", appService);
    on(workflowStandardParams).set("environmentService", environmentService);
    on(workflowStandardParams).set("artifactService", artifactService);
    on(workflowStandardParams).set("serviceTemplateService", serviceTemplateService);
    on(workflowStandardParams).set("configuration", configuration);
    on(workflowStandardParams).set("artifactStreamService", artifactStreamService);

    when(artifactService.get(any(), any())).thenReturn(artifact);
    when(artifactStreamService.get(any(), any())).thenReturn(artifactStream);

    when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID)).thenReturn(anAwsInfrastructureMapping().build());

    Activity activity = Activity.builder().build();
    activity.setUuid(ACTIVITY_ID);
    when(activityService.save(any(Activity.class))).thenReturn(activity);

    when(settingsService.get(any())).thenReturn(awsConfig);

    setInternalState(cloudFormationCreateStackState, "secretManager", secretManager);
    when(workflowExecutionService.getExecutionDetails(anyString(), anyString(), anyBoolean(), anySet()))
        .thenReturn(aWorkflowExecution().build());
    context = new ExecutionContextImpl(stateExecutionInstance);
    on(context).set("variableProcessor", variableProcessor);
    on(context).set("evaluator", evaluator);
    when(variableProcessor.getVariables(any(), any())).thenReturn(emptyMap());
    when(evaluator.substitute(any(), any(), any())).thenAnswer(i -> i.getArguments()[0]);
    PortalConfig portalConfig = new PortalConfig();
    portalConfig.setUrl(BASE_URL);
    when(configuration.getPortal()).thenReturn(portalConfig);
    doNothing().when(serviceHelper).addPlaceholderTexts(any());
  }

  @Test
  public void testExecute_createStackState() {
    cloudFormationCreateStackState.setRegion(Regions.US_EAST_1.name());
    ExecutionResponse executionResponse = cloudFormationCreateStackState.execute(context);
    assertEquals(ExecutionStatus.SUCCESS, executionResponse.getExecutionStatus());

    ArgumentCaptor<DelegateTask> captor = ArgumentCaptor.forClass(DelegateTask.class);
    verify(delegateService).queueTask(captor.capture());
    DelegateTask delegateTask = captor.getValue();

    CloudFormationCreateStackRequest cloudFormationCreateStackRequest =
        (CloudFormationCreateStackRequest) delegateTask.getParameters()[0];
    assertNotNull(cloudFormationCreateStackRequest);
    assertEquals(Regions.US_EAST_1.name(), cloudFormationCreateStackRequest.getRegion());
    assertEquals(CloudFormationCommandType.CREATE_STACK, cloudFormationCreateStackRequest.getCommandType());
    assertEquals(APP_ID, cloudFormationCreateStackRequest.getAppId());
    assertEquals(ACCOUNT_ID, cloudFormationCreateStackRequest.getAccountId());
    assertEquals("Create Stack", cloudFormationCreateStackRequest.getCommandName());
    assertEquals(CloudFormationCreateStackRequest.CLOUD_FORMATION_STACK_CREATE_BODY,
        cloudFormationCreateStackRequest.getCreateType());
    assertEquals("Template Body", cloudFormationCreateStackRequest.getData());
  }

  @Test
  public void testExecute_deleteStackState() {
    cloudFormationDeleteStackState.setRegion(Regions.US_EAST_1.name());
    ExecutionResponse executionResponse = cloudFormationDeleteStackState.execute(context);
    assertEquals(ExecutionStatus.SUCCESS, executionResponse.getExecutionStatus());

    ArgumentCaptor<DelegateTask> captor = ArgumentCaptor.forClass(DelegateTask.class);
    verify(delegateService).queueTask(captor.capture());
    DelegateTask delegateTask = captor.getValue();

    CloudFormationDeleteStackRequest cloudFormationDeleteStackRequest =
        (CloudFormationDeleteStackRequest) delegateTask.getParameters()[0];
    assertNotNull(cloudFormationDeleteStackRequest);
    assertEquals(Regions.US_EAST_1.name(), cloudFormationDeleteStackRequest.getRegion());
    assertEquals(CloudFormationCommandType.DELETE_STACK, cloudFormationDeleteStackRequest.getCommandType());
    assertEquals(APP_ID, cloudFormationDeleteStackRequest.getAppId());
    assertEquals(ACCOUNT_ID, cloudFormationDeleteStackRequest.getAccountId());
    assertEquals("Delete Stack", cloudFormationDeleteStackRequest.getCommandName());
    assertEquals(EXPECTED_SUFFIX, cloudFormationDeleteStackRequest.getStackNameSuffix());
  }
}
