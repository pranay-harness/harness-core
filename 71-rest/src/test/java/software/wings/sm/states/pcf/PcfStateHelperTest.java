package software.wings.sm.states.pcf;

import static io.harness.pcf.model.PcfConstants.MANIFEST_YML;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.Environment.EnvironmentType.PROD;
import static software.wings.beans.TaskType.COMMAND;
import static software.wings.helpers.ext.pcf.request.PcfCommandRequest.PcfCommandType.UPDATE_ROUTE;
import static software.wings.sm.states.pcf.PcfSwitchBlueGreenRoutes.PCF_BG_SWAP_ROUTE_COMMAND;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.ACTIVITY_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.APP_NAME;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.ENV_NAME;
import static software.wings.utils.WingsTestConstants.INFRA_MAPPING_ID;
import static software.wings.utils.WingsTestConstants.PASSWORD;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.USER_NAME;

import com.google.inject.Inject;

import io.harness.beans.DelegateTask;
import io.harness.beans.EmbeddedUser;
import io.harness.beans.ExecutionStatus;
import io.harness.category.element.UnitTests;
import io.harness.context.ContextElementType;
import io.harness.data.structure.UUIDGenerator;
import io.harness.delegate.beans.TaskData;
import io.harness.pcf.ManifestType;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.api.ServiceElement;
import software.wings.api.pcf.PcfRouteUpdateStateExecutionData;
import software.wings.beans.Activity;
import software.wings.beans.Activity.ActivityBuilder;
import software.wings.beans.Activity.Type;
import software.wings.beans.FeatureName;
import software.wings.beans.PcfConfig;
import software.wings.beans.PcfInfrastructureMapping;
import software.wings.beans.appmanifest.AppManifestKind;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.appmanifest.ManifestFile;
import software.wings.beans.appmanifest.StoreType;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.command.CommandUnitDetails.CommandUnitType;
import software.wings.beans.command.PcfDummyCommandUnit;
import software.wings.beans.container.PcfServiceSpecification;
import software.wings.beans.yaml.GitFetchFilesFromMultipleRepoResult;
import software.wings.beans.yaml.GitFetchFilesResult;
import software.wings.beans.yaml.GitFile;
import software.wings.helpers.ext.k8s.request.K8sValuesLocation;
import software.wings.helpers.ext.pcf.request.PcfCommandRouteUpdateRequest;
import software.wings.helpers.ext.pcf.request.PcfRouteUpdateRequestConfigData;
import software.wings.helpers.ext.pcf.response.PcfAppSetupTimeDetails;
import software.wings.service.intfc.ApplicationManifestService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.WorkflowStandardParams;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PcfStateHelperTest extends WingsBaseTest {
  private String SERVICE_MANIFEST_YML = "applications:\n"
      + "- name: ((PCF_APP_NAME))\n"
      + "  memory: ((PCF_APP_MEMORY))\n"
      + "  instances : ((INSTANCES))\n"
      + "  random-route: true\n"
      + "  level: Service";

  private String ENV_MANIFEST_YML = "applications:\n"
      + "- name: ((PCF_APP_NAME))\n"
      + "  memory: ((PCF_APP_MEMORY))\n"
      + "  instances : ((INSTANCES))\n"
      + "  random-route: true\n"
      + "  level: Environment";

  private String ENV_SERVICE_MANIFEST_YML = "applications:\n"
      + "- name: ((PCF_APP_NAME))\n"
      + "  memory: ((PCF_APP_MEMORY))\n"
      + "  instances : ((INSTANCES))\n"
      + "  random-route: true\n"
      + "  level: EnvironmentService";

  private String envServiceId = "envServiceId";

  @Mock private ApplicationManifestService applicationManifestService;
  @Mock private FeatureFlagService featureFlagService;
  @Mock private ServiceResourceService serviceResourceService;
  @Mock private DelegateService delegateService;
  @InjectMocks @Inject private PcfStateHelper pcfStateHelper;
  @Mock private ExecutionContext context;

  public static final String MANIFEST_YAML_CONTENT_With_RouteMap = "  applications:\n"
      + "  - name : ${APPLICATION_NAME}\n"
      + "    memory: 850M\n"
      + "    instances : ${INSTANCE_COUNT}\n"
      + "    buildpack: https://github.com/cloudfoundry/java-buildpack.git\n"
      + "    path: ${FILE_LOCATION}\n"
      + "    routes:\n"
      + "      - route: app.harness.io\n"
      + "      - route: qa.harness.io\n";

  public static final String MANIFEST_YAML_CONTENT_With_NO_ROUTE = "  applications:\n"
      + "  - name : ${APPLICATION_NAME}\n"
      + "    memory: 850M\n"
      + "    instances : ${INSTANCE_COUNT}\n"
      + "    buildpack: https://github.com/cloudfoundry/java-buildpack.git\n"
      + "    path: ${FILE_LOCATION}\n"
      + "    no-route: true\n";

  @Before
  public void setup() throws IllegalAccessException {
    ApplicationManifest manifest = ApplicationManifest.builder()
                                       .serviceId(SERVICE_ID)
                                       .kind(AppManifestKind.K8S_MANIFEST)
                                       .storeType(StoreType.Local)
                                       .build();
    manifest.setUuid("1234");

    when(context.getAppId()).thenReturn(APP_ID);
    when(applicationManifestService.getByServiceId(anyString(), anyString(), any())).thenReturn(manifest);
    when(applicationManifestService.getManifestFileByFileName(anyString(), anyString()))
        .thenReturn(
            ManifestFile.builder().fileName(MANIFEST_YML).fileContent(PcfSetupStateTest.MANIFEST_YAML_CONTENT).build());

    doReturn("").when(delegateService).queueTask(any());
  }

  @Test
  @Category(UnitTests.class)
  public void testFetchManifestYmlString() throws Exception {
    when(featureFlagService.isEnabled(FeatureName.PCF_MANIFEST_REDESIGN, ACCOUNT_ID))
        .thenReturn(true)
        .thenReturn(false);
    String yaml = pcfStateHelper.fetchManifestYmlString(context,
        anApplication().uuid(APP_ID).appId(APP_ID).accountId(ACCOUNT_ID).name(APP_NAME).build(),
        ServiceElement.builder().uuid(SERVICE_ID).build());
    assertThat(yaml).isNotNull();
    assertThat(yaml).isEqualTo(PcfSetupStateTest.MANIFEST_YAML_CONTENT);

    when(serviceResourceService.getPcfServiceSpecification(APP_ID, SERVICE_ID))
        .thenReturn(PcfServiceSpecification.builder()
                        .serviceId(SERVICE_ID)
                        .manifestYaml(PcfSetupStateTest.MANIFEST_YAML_CONTENT)
                        .build());
    yaml = pcfStateHelper.fetchManifestYmlString(context,
        anApplication().uuid(APP_ID).appId(APP_ID).accountId(ACCOUNT_ID).name(APP_NAME).build(),
        ServiceElement.builder().uuid(SERVICE_ID).build());
    assertThat(yaml).isNotNull();
    assertThat(yaml).isEqualTo(PcfSetupStateTest.MANIFEST_YAML_CONTENT);
  }

  @Test
  @Category(UnitTests.class)
  public void testGetFinalManifestFilesMap() {
    Map<K8sValuesLocation, ApplicationManifest> appManifestMap = new HashMap<>();
    Map<String, GitFetchFilesResult> filesFromMultipleRepo = new HashMap<>();
    GitFetchFilesFromMultipleRepoResult fetchFilesResult = GitFetchFilesFromMultipleRepoResult.builder().build();

    // Local Service manifest files
    ApplicationManifest serviceApplicationManifest = generateAppManifest(StoreType.Local, SERVICE_ID);
    appManifestMap.put(K8sValuesLocation.Service, serviceApplicationManifest);

    when(applicationManifestService.getManifestFilesByAppManifestId(APP_ID, SERVICE_ID))
        .thenReturn(Arrays.asList(ManifestFile.builder().fileContent(SERVICE_MANIFEST_YML).build()));
    Map<ManifestType, String> finalManifestFilesMap =
        pcfStateHelper.getFinalManifestFilesMap(appManifestMap, fetchFilesResult);
    assertThat(finalManifestFilesMap).isNotEmpty();
    assertThat(finalManifestFilesMap.get(ManifestType.APPLICATION_MANIFEST)).isEqualTo(SERVICE_MANIFEST_YML);

    // Remote overrides in environment
    ApplicationManifest envApplicationManifest = generateAppManifest(StoreType.Remote, ENV_ID);
    appManifestMap.put(K8sValuesLocation.EnvironmentGlobal, envApplicationManifest);
    GitFetchFilesResult filesResult = GitFetchFilesResult.builder()
                                          .files(Arrays.asList(GitFile.builder().fileContent(ENV_MANIFEST_YML).build()))
                                          .build();
    filesFromMultipleRepo.put(K8sValuesLocation.EnvironmentGlobal.name(), filesResult);
    fetchFilesResult.setFilesFromMultipleRepo(filesFromMultipleRepo);
    finalManifestFilesMap = pcfStateHelper.getFinalManifestFilesMap(appManifestMap, fetchFilesResult);
    assertThat(finalManifestFilesMap).isNotEmpty();
    assertThat(finalManifestFilesMap.get(ManifestType.APPLICATION_MANIFEST)).isEqualTo(ENV_MANIFEST_YML);

    // Local Environment Service manifest files
    ApplicationManifest serviceEnvApplicationManifest = generateAppManifest(StoreType.Local, envServiceId);
    appManifestMap.put(K8sValuesLocation.Environment, serviceEnvApplicationManifest);

    when(applicationManifestService.getManifestFilesByAppManifestId(APP_ID, envServiceId))
        .thenReturn(Arrays.asList(ManifestFile.builder().fileContent(ENV_SERVICE_MANIFEST_YML).build()));
    finalManifestFilesMap = pcfStateHelper.getFinalManifestFilesMap(appManifestMap, fetchFilesResult);
    assertThat(finalManifestFilesMap).isNotEmpty();
    assertThat(finalManifestFilesMap.get(ManifestType.APPLICATION_MANIFEST)).isEqualTo(ENV_SERVICE_MANIFEST_YML);
  }

  @Test
  @Category(UnitTests.class)
  public void testGetFinalManifestFilesMapWithNullGitFetchFileResponse() {
    Map<K8sValuesLocation, ApplicationManifest> appManifestMap = new HashMap<>();
    Map<String, GitFetchFilesResult> filesFromMultipleRepo = new HashMap<>();
    GitFetchFilesFromMultipleRepoResult fetchFilesResult = GitFetchFilesFromMultipleRepoResult.builder().build();

    ApplicationManifest serviceApplicationManifest = generateAppManifest(StoreType.Remote, SERVICE_ID);
    appManifestMap.put(K8sValuesLocation.Service, serviceApplicationManifest);

    Map<ManifestType, String> finalManifestFilesMap =
        pcfStateHelper.getFinalManifestFilesMap(appManifestMap, fetchFilesResult);
    assertThat(finalManifestFilesMap).isEmpty();

    filesFromMultipleRepo.put(K8sValuesLocation.Service.name(), null);
    fetchFilesResult.setFilesFromMultipleRepo(filesFromMultipleRepo);
    finalManifestFilesMap = pcfStateHelper.getFinalManifestFilesMap(appManifestMap, fetchFilesResult);
    assertThat(finalManifestFilesMap).isEmpty();
  }

  @Test
  @Category(UnitTests.class)
  public void testGetFinalManifestFilesMapWithInvalidContent() {
    Map<K8sValuesLocation, ApplicationManifest> appManifestMap = new HashMap<>();
    GitFetchFilesFromMultipleRepoResult fetchFilesResult = GitFetchFilesFromMultipleRepoResult.builder().build();

    // Local Service manifest files
    ApplicationManifest serviceApplicationManifest = generateAppManifest(StoreType.Local, SERVICE_ID);
    appManifestMap.put(K8sValuesLocation.Service, serviceApplicationManifest);

    when(applicationManifestService.getManifestFilesByAppManifestId(APP_ID, SERVICE_ID))
        .thenReturn(Arrays.asList(ManifestFile.builder().fileContent("abc").build()));
    Map<ManifestType, String> finalManifestFilesMap =
        pcfStateHelper.getFinalManifestFilesMap(appManifestMap, fetchFilesResult);
    assertThat(finalManifestFilesMap).isEmpty();
  }

  private ApplicationManifest generateAppManifest(StoreType storeType, String id) {
    ApplicationManifest applicationManifest = ApplicationManifest.builder().storeType(storeType).build();
    applicationManifest.setUuid(id);
    applicationManifest.setAppId(APP_ID);

    return applicationManifest;
  }

  @Test
  @Category(UnitTests.class)
  public void testGetDelegateTask() throws Exception {
    String waitId = UUIDGenerator.generateUuid();
    PcfDelegateTaskCreationData pcfDelegateTaskCreationData = PcfDelegateTaskCreationData.builder()
                                                                  .appId(APP_ID)
                                                                  .accountId(ACCOUNT_ID)
                                                                  .envId(ENV_ID)
                                                                  .infrastructureMappingId(INFRA_MAPPING_ID)
                                                                  .taskType(COMMAND)
                                                                  .timeout(5l)
                                                                  .waitId(waitId)
                                                                  .parameters(new Object[] {"1"})
                                                                  .build();

    DelegateTask delegateTask = pcfStateHelper.getDelegateTask(pcfDelegateTaskCreationData);

    assertThat(delegateTask).isNotNull();
    assertThat(delegateTask.getAppId()).isEqualTo(APP_ID);
    assertThat(delegateTask.getAccountId()).isEqualTo(ACCOUNT_ID);
    assertThat(delegateTask.getEnvId()).isEqualTo(ENV_ID);
    assertThat(delegateTask.getInfrastructureMappingId()).isEqualTo(INFRA_MAPPING_ID);
    assertThat(delegateTask.getWaitId()).isEqualTo(waitId);
    assertThat(delegateTask.isAsync()).isTrue();

    TaskData taskData = delegateTask.getData();
    assertThat(taskData).isNotNull();
    assertThat(taskData.getTaskType()).isEqualTo(COMMAND.name());
    assertThat(taskData.getTimeout()).isEqualTo(300000L);

    Object[] parameters = taskData.getParameters();
    assertThat(parameters).isNotNull();
    assertThat(parameters).isNotEmpty();
    assertThat(parameters.length).isEqualTo(1);
    assertThat(parameters[0]).isEqualTo("1");
  }

  @Test
  @Category(UnitTests.class)
  public void testGetActivityBuilder() throws Exception {
    final String commandName = "C1";
    final String commandType = "CType";
    final String user = "test";
    final String email = "email";
    PcfDummyCommandUnit pcfDummyCommandUnit = new PcfDummyCommandUnit("dummy");
    WorkflowStandardParams workflowStandardParams = new WorkflowStandardParams();
    workflowStandardParams.setCurrentUser(EmbeddedUser.builder().name(user).email(email).build());

    ExecutionContext context = mock(ExecutionContext.class);
    doReturn(workflowStandardParams).when(context).getContextElement(ContextElementType.STANDARD);

    PcfActivityBuilderCreationData creationData =
        PcfActivityBuilderCreationData.builder()
            .appId(APP_ID)
            .appName(APP_NAME)
            .commandName(commandName)
            .commandType(commandType)
            .commandUnits(Arrays.asList(pcfDummyCommandUnit))
            .commandUnitType(CommandUnitType.COMMAND)
            .environment(anEnvironment().uuid(ENV_ID).name(ENV_NAME).environmentType(PROD).build())
            .executionContext(context)
            .type(Type.Command)
            .build();

    ActivityBuilder activityBuilder = pcfStateHelper.getActivityBuilder(creationData);
    assertThat(activityBuilder).isNotNull();
    Activity activity = activityBuilder.build();
    assertThat(activity.getAppId()).isEqualTo(APP_ID);
    assertThat(activity.getApplicationName()).isEqualTo(APP_NAME);
    assertThat(activity.getEnvironmentId()).isEqualTo(ENV_ID);
    assertThat(activity.getEnvironmentName()).isEqualTo(ENV_NAME);
    assertThat(activity.getEnvironmentType()).isEqualTo(PROD);
    assertThat(activity.getCommandName()).isEqualTo(commandName);
    assertThat(activity.getCommandType()).isEqualTo(commandType);
    assertThat(activity.getStatus()).isEqualTo(ExecutionStatus.RUNNING);
    assertThat(activity.getTriggeredBy()).isNotNull();
    assertThat(activity.getTriggeredBy().getName()).isEqualTo(user);
    assertThat(activity.getTriggeredBy().getEmail()).isEqualTo(email);
    assertThat(activity.getStatus()).isEqualTo(ExecutionStatus.RUNNING);
    assertThat(activity.getCommandUnitType()).isEqualTo(CommandUnitType.COMMAND);

    List<CommandUnit> unitList = activity.getCommandUnits();
    assertThat(unitList).isNotNull();
    assertThat(unitList).containsExactly(pcfDummyCommandUnit);
  }

  @Test
  @Category(UnitTests.class)
  public void testQueueDelegateTaskForRouteUpdate() {
    PcfRouteUpdateQueueRequestData requestData =
        PcfRouteUpdateQueueRequestData.builder()
            .timeoutIntervalInMinutes(5)
            .app(anApplication().name(APP_NAME).appId(APP_ID).uuid(APP_ID).accountId(ACCOUNT_ID).build())
            .activityId(ACTIVITY_ID)
            .pcfInfrastructureMapping(PcfInfrastructureMapping.builder()
                                          .tempRouteMap(Arrays.asList("temproute.io"))
                                          .routeMaps(Arrays.asList("route.io"))
                                          .organization("org")
                                          .space("space")
                                          .build())
            .commandName(PCF_BG_SWAP_ROUTE_COMMAND)
            .pcfConfig(PcfConfig.builder().endpointUrl("pcfUrl").username(USER_NAME).password(PASSWORD).build())
            .requestConfigData(PcfRouteUpdateRequestConfigData.builder()
                                   .newApplicatiaonName("newApp")
                                   .downsizeOldApplication(false)
                                   .isRollback(false)
                                   .existingApplicationNames(Arrays.asList("oldApp"))

                                   .existingApplicationDetails(Arrays.asList(PcfAppSetupTimeDetails.builder()
                                                                                 .applicationGuid("AppGuid1")
                                                                                 .applicationName("pcfApp")
                                                                                 .initialInstanceCount(1)
                                                                                 .urls(Arrays.asList("url1"))
                                                                                 .build()))
                                   .build())
            .build();

    ExecutionResponse response = pcfStateHelper.queueDelegateTaskForRouteUpdate(requestData);
    assertThat(response).isNotNull();
    assertThat(response.isAsync()).isTrue();
    assertThat(response.getCorrelationIds()).containsExactly(ACTIVITY_ID);
    assertThat(response.getStateExecutionData()).isNotNull();
    assertThat(response.getStateExecutionData() instanceof PcfRouteUpdateStateExecutionData).isTrue();

    PcfRouteUpdateStateExecutionData stateExecutionData =
        (PcfRouteUpdateStateExecutionData) response.getStateExecutionData();
    assertThat(stateExecutionData.getAppId()).isNotNull();
    assertThat(stateExecutionData.getAppId()).isEqualTo(APP_ID);
    assertThat(stateExecutionData.getAccountId()).isEqualTo(ACCOUNT_ID);
    assertThat(stateExecutionData.getActivityId()).isEqualTo(ACTIVITY_ID);
    assertThat("PCF BG Swap Route").isEqualTo(stateExecutionData.getCommandName());

    assertThat(stateExecutionData.getPcfCommandRequest() instanceof PcfCommandRouteUpdateRequest).isTrue();
    PcfCommandRouteUpdateRequest request = (PcfCommandRouteUpdateRequest) stateExecutionData.getPcfCommandRequest();
    assertThat(stateExecutionData.getAppId()).isEqualTo(APP_ID);
    assertThat("PCF BG Swap Route").isEqualTo(stateExecutionData.getCommandName());
    assertThat(request.getAccountId()).isEqualTo(ACCOUNT_ID);
    assertThat(request.getActivityId()).isEqualTo(ACTIVITY_ID);
    assertThat(request.getAppId()).isEqualTo(APP_ID);
    assertThat("PCF BG Swap Route").isEqualTo(request.getCommandName());
    assertThat(request.getPcfCommandType()).isEqualTo(UPDATE_ROUTE);
    assertThat(request.getOrganization()).isEqualTo("org");
    assertThat(request.getSpace()).isEqualTo("space");

    assertThat(request.getPcfConfig().getEndpointUrl()).isEqualTo("pcfUrl");
    assertThat(request.getPcfConfig().getUsername()).isEqualTo(USER_NAME);
    assertThat(request.getPcfConfig().getPassword()).isEqualTo(PASSWORD);

    PcfRouteUpdateRequestConfigData pcfRouteUpdateRequestConfigData =
        stateExecutionData.getPcfRouteUpdateRequestConfigData();
    assertThat(pcfRouteUpdateRequestConfigData).isNotNull();
    assertThat(pcfRouteUpdateRequestConfigData.getNewApplicatiaonName()).isEqualTo("newApp");
    assertThat(pcfRouteUpdateRequestConfigData.isDownsizeOldApplication()).isFalse();
    assertThat(pcfRouteUpdateRequestConfigData.isRollback()).isFalse();
    assertThat(pcfRouteUpdateRequestConfigData.getExistingApplicationDetails().size()).isEqualTo(1);
    assertThat(pcfRouteUpdateRequestConfigData.getExistingApplicationNames().size()).isEqualTo(1);
  }

  @Test
  @Category(UnitTests.class)
  public void testGetRouteMaps() {
    PcfInfrastructureMapping infrastructureMapping = PcfInfrastructureMapping.builder().routeMaps(null).build();

    // Test 1: Read from manifest
    List<String> routes = pcfStateHelper.getRouteMaps(MANIFEST_YAML_CONTENT_With_RouteMap, infrastructureMapping);
    assertThat(routes).isNotNull();
    assertThat(routes.size()).isEqualTo(2);
    assertThat(routes).containsExactly("app.harness.io", "qa.harness.io");

    // Test 2: Read from manifest, inframapping also contains routes, ignore them
    infrastructureMapping.setRouteMaps(Arrays.asList("stage.harness.io"));
    routes = pcfStateHelper.getRouteMaps(MANIFEST_YAML_CONTENT_With_RouteMap, infrastructureMapping);
    assertThat(routes).isNotNull();
    assertThat(routes.size()).isEqualTo(2);
    assertThat(routes).containsExactly("app.harness.io", "qa.harness.io");

    // Test 3: Routes not metinoed in manifest, read infraMaaping 1
    infrastructureMapping.setRouteMaps(Arrays.asList("stage.harness.io"));
    routes = pcfStateHelper.getRouteMaps(PcfSetupStateTest.MANIFEST_YAML_CONTENT, infrastructureMapping);
    assertThat(routes).isNotNull();
    assertThat(routes.size()).isEqualTo(1);
    assertThat(routes).containsExactly("stage.harness.io");

    // Test 4: NPE check
    infrastructureMapping.setRouteMaps(null);
    routes = pcfStateHelper.getRouteMaps(PcfSetupStateTest.MANIFEST_YAML_CONTENT, infrastructureMapping);
    assertThat(routes).isNotNull();
    assertThat(routes.size()).isEqualTo(0);

    // Test 5: no-route in manifest, routes should be empty, ignore inframap routes
    infrastructureMapping.setRouteMaps(Arrays.asList("app.harness.io"));
    routes = pcfStateHelper.getRouteMaps(MANIFEST_YAML_CONTENT_With_NO_ROUTE, infrastructureMapping);
    assertThat(routes).isNotNull();
    assertThat(routes.size()).isEqualTo(0);
  }
}
