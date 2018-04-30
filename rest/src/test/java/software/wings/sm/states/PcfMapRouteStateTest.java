package software.wings.sm.states;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.joor.Reflect.on;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anySet;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;
import static software.wings.api.PhaseElement.PhaseElementBuilder.aPhaseElement;
import static software.wings.api.ServiceElement.Builder.aServiceElement;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.ResizeStrategy.RESIZE_NEW_FIRST;
import static software.wings.beans.ServiceTemplate.Builder.aServiceTemplate;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.WorkflowExecution.WorkflowExecutionBuilder.aWorkflowExecution;
import static software.wings.beans.artifact.Artifact.Builder.anArtifact;
import static software.wings.beans.command.Command.Builder.aCommand;
import static software.wings.beans.command.ServiceCommand.Builder.aServiceCommand;
import static software.wings.common.Constants.BUILD_NO;
import static software.wings.common.Constants.INFRA_ROUTE;
import static software.wings.common.Constants.INFRA_TEMP_ROUTE;
import static software.wings.common.Constants.PCF_APP_NAME;
import static software.wings.common.Constants.PCF_OLD_APP_NAME;
import static software.wings.common.Constants.URL;
import static software.wings.sm.StateExecutionInstance.Builder.aStateExecutionInstance;
import static software.wings.sm.WorkflowStandardParams.Builder.aWorkflowStandardParams;
import static software.wings.sm.states.pcf.MapRouteState.PCF_MAP_ROUTE_COMMAND;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.ACTIVITY_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.APP_NAME;
import static software.wings.utils.WingsTestConstants.ARTIFACT_ID;
import static software.wings.utils.WingsTestConstants.COMPUTE_PROVIDER_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.ENV_NAME;
import static software.wings.utils.WingsTestConstants.INFRA_MAPPING_ID;
import static software.wings.utils.WingsTestConstants.PASSWORD;
import static software.wings.utils.WingsTestConstants.PCF_SERVICE_NAME;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_NAME;
import static software.wings.utils.WingsTestConstants.STATE_NAME;
import static software.wings.utils.WingsTestConstants.TEMPLATE_ID;
import static software.wings.utils.WingsTestConstants.USER_NAME;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mongodb.morphia.Key;
import software.wings.WingsBaseTest;
import software.wings.api.DeploymentType;
import software.wings.api.PhaseElement;
import software.wings.api.PhaseStepExecutionData;
import software.wings.api.ServiceElement;
import software.wings.api.pcf.PcfRouteUpdateStateExecutionData;
import software.wings.api.pcf.PcfSetupContextElement;
import software.wings.app.MainConfiguration;
import software.wings.app.PortalConfig;
import software.wings.beans.Activity;
import software.wings.beans.Application;
import software.wings.beans.DelegateTask;
import software.wings.beans.Environment;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.PcfConfig;
import software.wings.beans.PcfInfrastructureMapping;
import software.wings.beans.Service;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.ServiceVariable;
import software.wings.beans.ServiceVariable.Type;
import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.JenkinsArtifactStream;
import software.wings.beans.command.CommandType;
import software.wings.beans.command.ServiceCommand;
import software.wings.beans.container.PcfServiceSpecification;
import software.wings.common.VariableProcessor;
import software.wings.expression.ExpressionEvaluator;
import software.wings.helpers.ext.pcf.request.PcfCommandRouteUpdateRequest;
import software.wings.helpers.ext.pcf.request.PcfCommandSetupRequest;
import software.wings.service.ServiceHelper;
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
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.WorkflowStandardParams;
import software.wings.sm.states.pcf.MapRouteState;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PcfMapRouteStateTest extends WingsBaseTest {
  private static final String BASE_URL = "https://env.harness.io/";
  public static final String ORG = "ORG";
  public static final String SPACE = "SPACE";

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
  @Mock private ServiceHelper serviceHelper;

  @InjectMocks private MapRouteState pcfRouteSwapState = new MapRouteState("name");

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
                                          .withDeploymentType(DeploymentType.PCF.name())
                                          .build();

  private StateExecutionInstance stateExecutionInstance =
      aStateExecutionInstance()
          .withDisplayName(STATE_NAME)
          .addContextElement(workflowStandardParams)
          .addContextElement(phaseElement)
          .addContextElement(
              PcfSetupContextElement.builder()
                  .uuid(serviceElement.getUuid())
                  .name(PCF_SERVICE_NAME)
                  .maxInstanceCount(10)
                  .pcfCommandRequest(PcfCommandSetupRequest.builder().space(SPACE).organization(ORG).build())
                  .newPcfApplicationName("APP_NAME_SERVICE_NAME_ENV_NAME__2")
                  .newPcfApplicationId("1")
                  .infraMappingId(INFRA_MAPPING_ID)
                  .resizeStrategy(RESIZE_NEW_FIRST)
                  .routeMaps(Arrays.asList("R1", "R2"))
                  .tempRouteMap(Arrays.asList("R3"))
                  .appsToBeDownsized(Arrays.asList("APP_NAME_SERVICE_NAME_ENV_NAME__1"))
                  .build())
          .addStateExecutionData(new PhaseStepExecutionData())
          .build();

  private Application app = anApplication().withUuid(APP_ID).withName(APP_NAME).build();
  private Environment env = anEnvironment().withAppId(APP_ID).withUuid(ENV_ID).withName(ENV_NAME).build();
  private Service service = Service.builder().appId(APP_ID).uuid(SERVICE_ID).name(SERVICE_NAME).build();
  private Artifact artifact = anArtifact()
                                  .withArtifactSourceName("source")
                                  .withMetadata(ImmutableMap.of(BUILD_NO, "bn"))
                                  .withServiceIds(singletonList(SERVICE_ID))
                                  .build();
  private ArtifactStream artifactStream =
      JenkinsArtifactStream.builder().appId(APP_ID).sourceName("").jobname("").artifactPaths(null).build();

  private SettingAttribute pcfConfig =
      aSettingAttribute()
          .withValue(
              PcfConfig.builder().endpointUrl(URL).password(PASSWORD).username(USER_NAME).accountId(ACCOUNT_ID).build())
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

    when(artifactService.get(any(), any())).thenReturn(artifact);
    when(artifactStreamService.get(any(), any())).thenReturn(artifactStream);

    InfrastructureMapping infrastructureMapping = PcfInfrastructureMapping.builder()
                                                      .organization(ORG)
                                                      .space(SPACE)
                                                      .routeMaps(Arrays.asList("R1", "R2"))
                                                      .tempRouteMap(Arrays.asList("R3"))
                                                      .computeProviderSettingId(COMPUTE_PROVIDER_ID)
                                                      .build();
    when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID)).thenReturn(infrastructureMapping);

    Activity activity = Activity.builder().build();
    activity.setUuid(ACTIVITY_ID);
    when(activityService.save(any(Activity.class))).thenReturn(activity);

    when(settingsService.get(any())).thenReturn(pcfConfig);

    when(serviceTemplateService.getTemplateRefKeysByService(APP_ID, SERVICE_ID, ENV_ID))
        .thenReturn(singletonList(new Key<>(ServiceTemplate.class, "serviceTemplate", TEMPLATE_ID)));

    when(serviceTemplateService.get(APP_ID, TEMPLATE_ID)).thenReturn(aServiceTemplate().withUuid(TEMPLATE_ID).build());
    when(serviceTemplateService.computeServiceVariables(APP_ID, ENV_ID, TEMPLATE_ID, null, false))
        .thenReturn(serviceVariableList);
    when(serviceTemplateService.computeServiceVariables(APP_ID, ENV_ID, TEMPLATE_ID, null, true))
        .thenReturn(safeDisplayServiceVariableList);
    when(secretManager.getEncryptionDetails(anyObject(), anyString(), anyString())).thenReturn(Collections.emptyList());
    setInternalState(pcfRouteSwapState, "secretManager", secretManager);
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
    when(serviceResourceService.getPcfServiceSpecification(anyString(), anyString()))
        .thenReturn(PcfServiceSpecification.builder()
                        .manifestYaml("  applications:\n"
                            + "  - name : ${APPLICATION_NAME}\n"
                            + "    memory: 850M\n"
                            + "    instances : ${INSTANCE_COUNT}\n"
                            + "    buildpack: https://github.com/cloudfoundry/java-buildpack.git\n"
                            + "    path: ${FILE_LOCATION}\n"
                            + "    routes:\n"
                            + "      - route: ${ROUTE_MAP}\n"
                            + "serviceName: SERV\n")
                        .serviceId(service.getUuid())
                        .build());
    doNothing().when(serviceHelper).addPlaceholderTexts(any());
  }

  @Test
  public void testExecute_pcfAPP_infra_route() {
    pcfRouteSwapState.setPcfAppName("${" + PCF_APP_NAME + "}");
    pcfRouteSwapState.setRoute("${" + INFRA_ROUTE + "}");

    on(context).set("serviceTemplateService", serviceTemplateService);
    ExecutionResponse executionResponse = pcfRouteSwapState.execute(context);

    assertEquals(ExecutionStatus.SUCCESS, executionResponse.getExecutionStatus());
    PcfRouteUpdateStateExecutionData stateExecutionData =
        (PcfRouteUpdateStateExecutionData) executionResponse.getStateExecutionData();
    assertEquals(PCF_MAP_ROUTE_COMMAND, stateExecutionData.getCommandName());
    PcfCommandRouteUpdateRequest pcfCommandRouteUpdateRequest =
        (PcfCommandRouteUpdateRequest) stateExecutionData.getPcfCommandRequest();

    assertNotNull(pcfCommandRouteUpdateRequest.getAppsToBeUpdated());
    assertEquals(1, pcfCommandRouteUpdateRequest.getAppsToBeUpdated().size());
    assertEquals("APP_NAME_SERVICE_NAME_ENV_NAME__2", pcfCommandRouteUpdateRequest.getAppsToBeUpdated().get(0));

    assertNotNull(pcfCommandRouteUpdateRequest.getRouteMaps());
    assertEquals(2, pcfCommandRouteUpdateRequest.getRouteMaps().size());
    assertTrue(pcfCommandRouteUpdateRequest.getRouteMaps().contains("R1"));
    assertTrue(pcfCommandRouteUpdateRequest.getRouteMaps().contains("R2"));

    ArgumentCaptor<DelegateTask> captor = ArgumentCaptor.forClass(DelegateTask.class);
    verify(delegateService).queueTask(captor.capture());
    DelegateTask delegateTask = captor.getValue();

    pcfCommandRouteUpdateRequest = (PcfCommandRouteUpdateRequest) delegateTask.getParameters()[0];
    assertNotNull(pcfCommandRouteUpdateRequest.getAppsToBeUpdated());
    assertEquals(1, pcfCommandRouteUpdateRequest.getAppsToBeUpdated().size());
    assertEquals("APP_NAME_SERVICE_NAME_ENV_NAME__2", pcfCommandRouteUpdateRequest.getAppsToBeUpdated().get(0));

    assertNotNull(pcfCommandRouteUpdateRequest.getRouteMaps());
    assertEquals(2, pcfCommandRouteUpdateRequest.getRouteMaps().size());
    assertTrue(pcfCommandRouteUpdateRequest.getRouteMaps().contains("R1"));
    assertTrue(pcfCommandRouteUpdateRequest.getRouteMaps().contains("R2"));
  }

  @Test
  public void testExecute_pcfAPP_infra_tempRoute() {
    pcfRouteSwapState.setPcfAppName("${" + PCF_APP_NAME + "}");
    pcfRouteSwapState.setRoute("${" + INFRA_TEMP_ROUTE + "}");

    on(context).set("serviceTemplateService", serviceTemplateService);
    ExecutionResponse executionResponse = pcfRouteSwapState.execute(context);

    assertEquals(ExecutionStatus.SUCCESS, executionResponse.getExecutionStatus());
    PcfRouteUpdateStateExecutionData stateExecutionData =
        (PcfRouteUpdateStateExecutionData) executionResponse.getStateExecutionData();
    assertEquals(PCF_MAP_ROUTE_COMMAND, stateExecutionData.getCommandName());
    PcfCommandRouteUpdateRequest pcfCommandRouteUpdateRequest =
        (PcfCommandRouteUpdateRequest) stateExecutionData.getPcfCommandRequest();

    assertNotNull(pcfCommandRouteUpdateRequest.getAppsToBeUpdated());
    assertEquals(1, pcfCommandRouteUpdateRequest.getAppsToBeUpdated().size());
    assertEquals("APP_NAME_SERVICE_NAME_ENV_NAME__2", pcfCommandRouteUpdateRequest.getAppsToBeUpdated().get(0));

    assertNotNull(pcfCommandRouteUpdateRequest.getRouteMaps());
    assertEquals(1, pcfCommandRouteUpdateRequest.getRouteMaps().size());
    assertTrue(pcfCommandRouteUpdateRequest.getRouteMaps().contains("R3"));

    ArgumentCaptor<DelegateTask> captor = ArgumentCaptor.forClass(DelegateTask.class);
    verify(delegateService).queueTask(captor.capture());
    DelegateTask delegateTask = captor.getValue();

    pcfCommandRouteUpdateRequest = (PcfCommandRouteUpdateRequest) delegateTask.getParameters()[0];
    assertNotNull(pcfCommandRouteUpdateRequest.getAppsToBeUpdated());
    assertEquals(1, pcfCommandRouteUpdateRequest.getAppsToBeUpdated().size());
    assertEquals("APP_NAME_SERVICE_NAME_ENV_NAME__2", pcfCommandRouteUpdateRequest.getAppsToBeUpdated().get(0));

    assertNotNull(pcfCommandRouteUpdateRequest.getRouteMaps());
    assertNotNull(pcfCommandRouteUpdateRequest.getRouteMaps());
    assertEquals(1, pcfCommandRouteUpdateRequest.getRouteMaps().size());
    assertTrue(pcfCommandRouteUpdateRequest.getRouteMaps().contains("R3"));
  }

  @Test
  public void testExecute_pcfOldAPP_infra_route() {
    pcfRouteSwapState.setPcfAppName("${" + PCF_OLD_APP_NAME + "}");
    pcfRouteSwapState.setRoute("${" + INFRA_ROUTE + "}");

    on(context).set("serviceTemplateService", serviceTemplateService);
    ExecutionResponse executionResponse = pcfRouteSwapState.execute(context);

    assertEquals(ExecutionStatus.SUCCESS, executionResponse.getExecutionStatus());
    PcfRouteUpdateStateExecutionData stateExecutionData =
        (PcfRouteUpdateStateExecutionData) executionResponse.getStateExecutionData();
    assertEquals(PCF_MAP_ROUTE_COMMAND, stateExecutionData.getCommandName());
    PcfCommandRouteUpdateRequest pcfCommandRouteUpdateRequest =
        (PcfCommandRouteUpdateRequest) stateExecutionData.getPcfCommandRequest();

    assertNotNull(pcfCommandRouteUpdateRequest.getAppsToBeUpdated());
    assertEquals(1, pcfCommandRouteUpdateRequest.getAppsToBeUpdated().size());
    assertEquals("APP_NAME_SERVICE_NAME_ENV_NAME__1", pcfCommandRouteUpdateRequest.getAppsToBeUpdated().get(0));

    assertNotNull(pcfCommandRouteUpdateRequest.getRouteMaps());
    assertEquals(2, pcfCommandRouteUpdateRequest.getRouteMaps().size());
    assertTrue(pcfCommandRouteUpdateRequest.getRouteMaps().contains("R1"));
    assertTrue(pcfCommandRouteUpdateRequest.getRouteMaps().contains("R2"));

    ArgumentCaptor<DelegateTask> captor = ArgumentCaptor.forClass(DelegateTask.class);
    verify(delegateService).queueTask(captor.capture());
    DelegateTask delegateTask = captor.getValue();

    pcfCommandRouteUpdateRequest = (PcfCommandRouteUpdateRequest) delegateTask.getParameters()[0];
    assertNotNull(pcfCommandRouteUpdateRequest.getAppsToBeUpdated());
    assertEquals(1, pcfCommandRouteUpdateRequest.getAppsToBeUpdated().size());
    assertEquals("APP_NAME_SERVICE_NAME_ENV_NAME__1", pcfCommandRouteUpdateRequest.getAppsToBeUpdated().get(0));

    assertNotNull(pcfCommandRouteUpdateRequest.getRouteMaps());
    assertEquals(2, pcfCommandRouteUpdateRequest.getRouteMaps().size());
    assertTrue(pcfCommandRouteUpdateRequest.getRouteMaps().contains("R1"));
    assertTrue(pcfCommandRouteUpdateRequest.getRouteMaps().contains("R2"));
  }

  @Test
  public void testExecute_pcfOldAPP_infra_tempRoute() {
    pcfRouteSwapState.setPcfAppName("${" + PCF_OLD_APP_NAME + "}");
    pcfRouteSwapState.setRoute("${" + INFRA_TEMP_ROUTE + "}");

    on(context).set("serviceTemplateService", serviceTemplateService);
    ExecutionResponse executionResponse = pcfRouteSwapState.execute(context);

    assertEquals(ExecutionStatus.SUCCESS, executionResponse.getExecutionStatus());
    PcfRouteUpdateStateExecutionData stateExecutionData =
        (PcfRouteUpdateStateExecutionData) executionResponse.getStateExecutionData();
    assertEquals(PCF_MAP_ROUTE_COMMAND, stateExecutionData.getCommandName());
    PcfCommandRouteUpdateRequest pcfCommandRouteUpdateRequest =
        (PcfCommandRouteUpdateRequest) stateExecutionData.getPcfCommandRequest();

    assertNotNull(pcfCommandRouteUpdateRequest.getAppsToBeUpdated());
    assertEquals(1, pcfCommandRouteUpdateRequest.getAppsToBeUpdated().size());
    assertEquals("APP_NAME_SERVICE_NAME_ENV_NAME__1", pcfCommandRouteUpdateRequest.getAppsToBeUpdated().get(0));

    assertNotNull(pcfCommandRouteUpdateRequest.getRouteMaps());
    assertEquals(1, pcfCommandRouteUpdateRequest.getRouteMaps().size());
    assertTrue(pcfCommandRouteUpdateRequest.getRouteMaps().contains("R3"));

    ArgumentCaptor<DelegateTask> captor = ArgumentCaptor.forClass(DelegateTask.class);
    verify(delegateService).queueTask(captor.capture());
    DelegateTask delegateTask = captor.getValue();

    pcfCommandRouteUpdateRequest = (PcfCommandRouteUpdateRequest) delegateTask.getParameters()[0];
    assertNotNull(pcfCommandRouteUpdateRequest.getAppsToBeUpdated());
    assertEquals(1, pcfCommandRouteUpdateRequest.getAppsToBeUpdated().size());
    assertEquals("APP_NAME_SERVICE_NAME_ENV_NAME__1", pcfCommandRouteUpdateRequest.getAppsToBeUpdated().get(0));

    assertNotNull(pcfCommandRouteUpdateRequest.getRouteMaps());
    assertNotNull(pcfCommandRouteUpdateRequest.getRouteMaps());
    assertEquals(1, pcfCommandRouteUpdateRequest.getRouteMaps().size());
    assertTrue(pcfCommandRouteUpdateRequest.getRouteMaps().contains("R3"));
  }
}
