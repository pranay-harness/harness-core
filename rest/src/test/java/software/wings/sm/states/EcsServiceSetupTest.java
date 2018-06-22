package software.wings.sm.states;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anySet;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;
import static software.wings.api.CommandStateExecutionData.Builder.aCommandStateExecutionData;
import static software.wings.api.PhaseElement.PhaseElementBuilder.aPhaseElement;
import static software.wings.api.ServiceElement.Builder.aServiceElement;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.EcsInfrastructureMapping.Builder.anEcsInfrastructureMapping;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.ResizeStrategy.RESIZE_NEW_FIRST;
import static software.wings.beans.ServiceTemplate.Builder.aServiceTemplate;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.WorkflowExecution.WorkflowExecutionBuilder.aWorkflowExecution;
import static software.wings.beans.artifact.Artifact.Builder.anArtifact;
import static software.wings.beans.command.Command.Builder.aCommand;
import static software.wings.beans.command.CommandExecutionResult.Builder.aCommandExecutionResult;
import static software.wings.beans.command.ServiceCommand.Builder.aServiceCommand;
import static software.wings.common.Constants.BUILD_NO;
import static software.wings.sm.StateExecutionInstance.Builder.aStateExecutionInstance;
import static software.wings.sm.WorkflowStandardParams.Builder.aWorkflowStandardParams;
import static software.wings.sm.states.ContainerServiceSetup.DEFAULT_MAX;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.ACTIVITY_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.APP_NAME;
import static software.wings.utils.WingsTestConstants.ARTIFACT_ID;
import static software.wings.utils.WingsTestConstants.CLUSTER_NAME;
import static software.wings.utils.WingsTestConstants.COMPUTE_PROVIDER_ID;
import static software.wings.utils.WingsTestConstants.ECS_SERVICE_NAME;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.ENV_NAME;
import static software.wings.utils.WingsTestConstants.INFRA_MAPPING_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_NAME;
import static software.wings.utils.WingsTestConstants.STATE_NAME;
import static software.wings.utils.WingsTestConstants.TEMPLATE_ID;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mongodb.morphia.Key;
import software.wings.WingsBaseTest;
import software.wings.api.ContainerServiceElement;
import software.wings.api.ContainerServiceElement.ContainerServiceElementBuilder;
import software.wings.api.DeploymentType;
import software.wings.api.PhaseElement;
import software.wings.api.ServiceElement;
import software.wings.app.MainConfiguration;
import software.wings.app.PortalConfig;
import software.wings.beans.Activity;
import software.wings.beans.Application;
import software.wings.beans.DelegateTask;
import software.wings.beans.DockerConfig;
import software.wings.beans.Environment;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.Service;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.ServiceVariable;
import software.wings.beans.ServiceVariable.Type;
import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.DockerArtifactStream;
import software.wings.beans.command.CommandExecutionContext;
import software.wings.beans.command.CommandExecutionResult;
import software.wings.beans.command.CommandType;
import software.wings.beans.command.ContainerSetupCommandUnitExecutionData;
import software.wings.beans.command.ContainerSetupParams;
import software.wings.beans.command.EcsSetupParams;
import software.wings.beans.command.EcsSetupParams.EcsSetupParamsBuilder;
import software.wings.beans.command.ServiceCommand;
import software.wings.beans.container.ContainerDefinition;
import software.wings.beans.container.EcsContainerTask;
import software.wings.beans.container.ImageDetails;
import software.wings.common.VariableProcessor;
import software.wings.expression.ExpressionEvaluator;
import software.wings.helpers.ext.container.ContainerDeploymentManagerHelper;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.WorkflowStandardParams;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class EcsServiceSetupTest extends WingsBaseTest {
  private static final String BASE_URL = "https://env.harness.io/";

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
  @Mock private EncryptionService encryptionService;
  @Mock private VariableProcessor variableProcessor;
  @Mock private ExpressionEvaluator evaluator;
  @Mock private ContainerDeploymentManagerHelper containerDeploymentHelper;

  @InjectMocks private EcsServiceSetup ecsServiceSetup = new EcsServiceSetup("name");

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
                                          .withDeploymentType(DeploymentType.ECS.name())
                                          .build();

  private StateExecutionInstance stateExecutionInstance =
      aStateExecutionInstance()
          .withDisplayName(STATE_NAME)
          .addContextElement(workflowStandardParams)
          .addContextElement(phaseElement)
          .addContextElement(ContainerServiceElement.builder()
                                 .uuid(serviceElement.getUuid())
                                 .maxInstances(10)
                                 .clusterName(CLUSTER_NAME)
                                 .namespace("default")
                                 .name(ECS_SERVICE_NAME)
                                 .resizeStrategy(RESIZE_NEW_FIRST)
                                 .infraMappingId(INFRA_MAPPING_ID)
                                 .deploymentType(DeploymentType.ECS)
                                 .build())
          .addStateExecutionData(aCommandStateExecutionData().build())
          .build();

  private Application app = anApplication().withUuid(APP_ID).withName(APP_NAME).build();
  private Environment env = anEnvironment().withAppId(APP_ID).withUuid(ENV_ID).withName(ENV_NAME).build();
  private Service service = Service.builder().appId(APP_ID).uuid(SERVICE_ID).name(SERVICE_NAME).build();
  private Artifact artifact = anArtifact()
                                  .withArtifactSourceName("source")
                                  .withMetadata(ImmutableMap.of(BUILD_NO, "bn"))
                                  .withServiceIds(singletonList(SERVICE_ID))
                                  .build();
  private ArtifactStream artifactStream = DockerArtifactStream.builder().appId(APP_ID).imageName("imageName").build();

  private SettingAttribute dockerConfig = aSettingAttribute()
                                              .withValue(DockerConfig.builder()
                                                             .dockerRegistryUrl("url")
                                                             .password("pass".toCharArray())
                                                             .username("user")
                                                             .accountId(ACCOUNT_ID)
                                                             .build())
                                              .build();

  private List<ServiceVariable> serviceVariableList =
      asList(ServiceVariable.builder().type(Type.TEXT).name("VAR_1").value("value1".toCharArray()).build(),
          ServiceVariable.builder().type(Type.ENCRYPTED_TEXT).name("VAR_2").value("value2".toCharArray()).build());

  private List<ServiceVariable> safeDisplayServiceVariableList =
      asList(ServiceVariable.builder().type(Type.TEXT).name("VAR_1").value("value1".toCharArray()).build(),
          ServiceVariable.builder().type(Type.ENCRYPTED_TEXT).name("VAR_2").value("*******".toCharArray()).build());

  @Before
  public void setup() {
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

    EcsContainerTask ecsContainerTask = new EcsContainerTask();
    ContainerDefinition containerDefinition = ContainerDefinition.builder().memory(256).cpu(1).build();
    ecsContainerTask.setContainerDefinitions(Lists.newArrayList(containerDefinition));
    when(serviceResourceService.getContainerTaskByDeploymentType(APP_ID, SERVICE_ID, DeploymentType.ECS.name()))
        .thenReturn(ecsContainerTask);
    on(workflowStandardParams).set("appService", appService);
    on(workflowStandardParams).set("environmentService", environmentService);
    on(workflowStandardParams).set("artifactService", artifactService);
    on(workflowStandardParams).set("serviceTemplateService", serviceTemplateService);
    on(workflowStandardParams).set("configuration", configuration);
    on(workflowStandardParams).set("artifactStreamService", artifactStreamService);

    when(artifactService.get(any(), any())).thenReturn(artifact);
    when(artifactStreamService.get(any(), any())).thenReturn(artifactStream);

    InfrastructureMapping infrastructureMapping = anEcsInfrastructureMapping()
                                                      .withClusterName(CLUSTER_NAME)
                                                      .withComputeProviderSettingId(COMPUTE_PROVIDER_ID)
                                                      .build();
    when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID)).thenReturn(infrastructureMapping);

    Activity activity = Activity.builder().build();
    activity.setUuid(ACTIVITY_ID);
    when(activityService.save(any(Activity.class))).thenReturn(activity);

    when(settingsService.get(any())).thenReturn(dockerConfig);

    when(serviceTemplateService.getTemplateRefKeysByService(APP_ID, SERVICE_ID, ENV_ID))
        .thenReturn(singletonList(new Key<>(ServiceTemplate.class, "serviceTemplate", TEMPLATE_ID)));

    when(serviceTemplateService.get(APP_ID, TEMPLATE_ID)).thenReturn(aServiceTemplate().withUuid(TEMPLATE_ID).build());
    when(serviceTemplateService.computeServiceVariables(APP_ID, ENV_ID, TEMPLATE_ID, null, false))
        .thenReturn(serviceVariableList);
    when(serviceTemplateService.computeServiceVariables(APP_ID, ENV_ID, TEMPLATE_ID, null, true))
        .thenReturn(safeDisplayServiceVariableList);
    when(secretManager.getEncryptionDetails(anyObject(), anyString(), anyString())).thenReturn(Collections.emptyList());
    setInternalState(ecsServiceSetup, "secretManager", secretManager);
    when(workflowExecutionService.getExecutionDetails(anyString(), anyString(), anySet()))
        .thenReturn(aWorkflowExecution().build());
    context = new ExecutionContextImpl(stateExecutionInstance);
    on(context).set("variableProcessor", variableProcessor);
    on(context).set("evaluator", evaluator);
    when(variableProcessor.getVariables(any(), any())).thenReturn(emptyMap());
    when(evaluator.substitute(any(), any(), any())).thenAnswer(i -> i.getArguments()[0]);
    PortalConfig portalConfig = new PortalConfig();
    portalConfig.setUrl(BASE_URL);
    when(configuration.getPortal()).thenReturn(portalConfig);
  }

  @Test
  public void shouldExecute() {
    on(context).set("serviceTemplateService", serviceTemplateService);
    when(containerDeploymentHelper.fetchArtifactDetails(artifact, app.getUuid(), context.getWorkflowExecutionId()))
        .thenReturn(ImageDetails.builder().name(artifactStream.getSourceName()).tag(artifact.getBuildNo()).build());

    ecsServiceSetup.execute(context);

    ArgumentCaptor<DelegateTask> captor = ArgumentCaptor.forClass(DelegateTask.class);
    verify(delegateService).queueTask(captor.capture());
    DelegateTask delegateTask = captor.getValue();

    CommandExecutionContext executionContext = (CommandExecutionContext) delegateTask.getParameters()[1];

    Map<String, String> serviceVariables = executionContext.getServiceVariables();
    assertThat(serviceVariables.size()).isEqualTo(2);
    assertThat(serviceVariables.get("VAR_1")).isEqualTo("value1");
    assertThat(serviceVariables.get("VAR_2")).isEqualTo("value2");

    Map<String, String> safeDisplayServiceVariables = executionContext.getSafeDisplayServiceVariables();
    assertThat(safeDisplayServiceVariables.size()).isEqualTo(2);
    assertThat(safeDisplayServiceVariables.get("VAR_1")).isEqualTo("value1");
    assertThat(safeDisplayServiceVariables.get("VAR_2")).isEqualTo("*******");

    ContainerSetupParams params = executionContext.getContainerSetupParams();
    assertThat(params.getAppName()).isEqualTo(APP_NAME);
    assertThat(params.getEnvName()).isEqualTo(ENV_NAME);
    assertThat(params.getServiceName()).isEqualTo(SERVICE_NAME);
    assertThat(params.getClusterName()).isEqualTo(CLUSTER_NAME);
  }

  @Test
  public void shouldBuildContainerServiceElement() {
    ContainerServiceElement containerServiceElement = buildContainerServiceElement("10", "5");

    assertThat(containerServiceElement.getName()).isEqualTo(ECS_SERVICE_NAME);
    assertThat(containerServiceElement.getMaxInstances()).isEqualTo(10);
    assertThat(containerServiceElement.getFixedInstances()).isEqualTo(5);
  }

  @Test
  public void shouldBuildContainerServiceElementEmptyValues() {
    ContainerServiceElement containerServiceElement = buildContainerServiceElement(null, null);

    assertThat(containerServiceElement.getName()).isEqualTo(ECS_SERVICE_NAME);
    assertThat(containerServiceElement.getMaxInstances()).isEqualTo(DEFAULT_MAX);
    assertThat(containerServiceElement.getFixedInstances()).isEqualTo(DEFAULT_MAX);
  }

  @Test
  public void shouldBuildContainerServiceElementEmptyFixed() {
    ContainerServiceElement containerServiceElement = buildContainerServiceElement("10", null);

    assertThat(containerServiceElement.getName()).isEqualTo(ECS_SERVICE_NAME);
    assertThat(containerServiceElement.getMaxInstances()).isEqualTo(10);
    assertThat(containerServiceElement.getFixedInstances()).isEqualTo(10);
  }

  @Test
  public void shouldBuildContainerServiceElementZero() {
    ContainerServiceElement containerServiceElement = buildContainerServiceElement("0", "0");

    assertThat(containerServiceElement.getName()).isEqualTo(ECS_SERVICE_NAME);
    assertThat(containerServiceElement.getMaxInstances()).isEqualTo(DEFAULT_MAX);
    assertThat(containerServiceElement.getFixedInstances()).isEqualTo(DEFAULT_MAX);
  }

  private ContainerServiceElement buildContainerServiceElement(String maxInstances, String fixedInstances) {
    EcsSetupParams setupParams = EcsSetupParamsBuilder.anEcsSetupParams().build();
    ContainerServiceElementBuilder serviceElementBuilder = ContainerServiceElement.builder()
                                                               .uuid(serviceElement.getUuid())
                                                               .clusterName(CLUSTER_NAME)
                                                               .namespace("default")
                                                               .name(ECS_SERVICE_NAME)
                                                               .resizeStrategy(RESIZE_NEW_FIRST)
                                                               .infraMappingId(INFRA_MAPPING_ID)
                                                               .deploymentType(DeploymentType.ECS);
    if (maxInstances != null) {
      serviceElementBuilder.maxInstances(Integer.valueOf(maxInstances));
    }
    if (fixedInstances != null) {
      serviceElementBuilder.maxInstances(Integer.valueOf(fixedInstances));
    }
    StateExecutionInstance stateExecutionInstance =
        aStateExecutionInstance()
            .withDisplayName(STATE_NAME)
            .addContextElement(workflowStandardParams)
            .addContextElement(phaseElement)
            .addContextElement(serviceElementBuilder.build())
            .addStateExecutionData(aCommandStateExecutionData().withContainerSetupParams(setupParams).build())
            .build();
    ExecutionContext context = new ExecutionContextImpl(stateExecutionInstance);
    on(context).set("variableProcessor", variableProcessor);
    on(context).set("evaluator", evaluator);
    CommandExecutionResult result =
        aCommandExecutionResult()
            .withCommandExecutionData(
                ContainerSetupCommandUnitExecutionData.builder().containerServiceName(ECS_SERVICE_NAME).build())
            .build();

    ecsServiceSetup.setMaxInstances(maxInstances);
    ecsServiceSetup.setFixedInstances(fixedInstances);
    return ecsServiceSetup.buildContainerServiceElement(
        context, result, ExecutionStatus.SUCCESS, ImageDetails.builder().name("foo").tag("43").build());
  }
}
