package software.wings.sm.states.pcf;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.pcf.model.PcfConstants.INSTANCE_PLACEHOLDER_TOKEN_DEPRECATED;
import static io.harness.pcf.model.PcfConstants.LEGACY_NAME_PCF_MANIFEST;
import static io.harness.pcf.model.PcfConstants.MANIFEST_YML;
import static io.harness.rule.OwnerRule.ADWAIT;
import static io.harness.rule.OwnerRule.ANSHUL;
import static io.harness.rule.OwnerRule.PRASHANT;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.Environment.EnvironmentType.PROD;
import static software.wings.beans.ResizeStrategy.RESIZE_NEW_FIRST;
import static software.wings.beans.TaskType.COMMAND;
import static software.wings.beans.appmanifest.StoreType.Local;
import static software.wings.beans.appmanifest.StoreType.Remote;
import static software.wings.helpers.ext.pcf.request.PcfCommandRequest.PcfCommandType.UPDATE_ROUTE;
import static software.wings.sm.states.pcf.PcfSwitchBlueGreenRoutes.PCF_BG_SWAP_ROUTE_COMMAND;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.ACTIVITY_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.APP_NAME;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.ENV_NAME;
import static software.wings.utils.WingsTestConstants.INFRA_DEFINITION_ID;
import static software.wings.utils.WingsTestConstants.INFRA_MAPPING_ID;
import static software.wings.utils.WingsTestConstants.PASSWORD;
import static software.wings.utils.WingsTestConstants.PCF_SERVICE_NAME;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.STATE_EXECUTION_ID;
import static software.wings.utils.WingsTestConstants.USER_NAME;
import static software.wings.utils.WingsTestConstants.WORKFLOW_EXECUTION_ID;

import com.google.inject.Inject;

import io.harness.beans.DelegateTask;
import io.harness.beans.EmbeddedUser;
import io.harness.beans.ExecutionStatus;
import io.harness.category.element.UnitTests;
import io.harness.context.ContextElementType;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.task.pcf.PcfManifestsPackage;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import software.wings.WingsBaseTest;
import software.wings.api.PhaseElement;
import software.wings.api.PhaseExecutionData;
import software.wings.api.PhaseExecutionData.PhaseExecutionDataBuilder;
import software.wings.api.ServiceElement;
import software.wings.api.pcf.DeploySweepingOutputPcf;
import software.wings.api.pcf.PcfRouteUpdateStateExecutionData;
import software.wings.api.pcf.PcfSetupStateExecutionData;
import software.wings.api.pcf.SetupSweepingOutputPcf;
import software.wings.api.pcf.SwapRouteRollbackSweepingOutputPcf;
import software.wings.beans.Activity;
import software.wings.beans.Activity.ActivityBuilder;
import software.wings.beans.Activity.Type;
import software.wings.beans.FeatureName;
import software.wings.beans.PcfConfig;
import software.wings.beans.PcfInfrastructureMapping;
import software.wings.beans.Service;
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
import software.wings.helpers.ext.pcf.request.PcfCommandSetupRequest;
import software.wings.helpers.ext.pcf.request.PcfRouteUpdateRequestConfigData;
import software.wings.helpers.ext.pcf.response.PcfAppSetupTimeDetails;
import software.wings.service.intfc.ApplicationManifestService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.StateExecutionService;
import software.wings.service.intfc.SweepingOutputService;
import software.wings.service.intfc.SweepingOutputService.SweepingOutputInquiry;
import software.wings.service.intfc.SweepingOutputService.SweepingOutputInquiry.SweepingOutputInquiryBuilder;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateExecutionInstance.Builder;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PcfStateHelperTest extends WingsBaseTest {
  public static final String REPLACE_ME = "REPLACE_ME";
  public static final ServiceElement SERVICE_ELEMENT = ServiceElement.builder().uuid(SERVICE_ID).build();
  private String SERVICE_MANIFEST_YML = "applications:\n"
      + "- name: ((PCF_APP_NAME))\n"
      + "  memory: ((PCF_APP_MEMORY))\n"
      + "  instances : ((INSTANCES))\n"
      + "  random-route: true\n"
      + "  level: Service";

  private String TEST_APP_MANIFEST = "applications:\n"
      + "- name: " + REPLACE_ME + "\n"
      + "  memory: ((PCF_APP_MEMORY))\n"
      + "  instances : ((INSTANCES))\n";

  private String TEST_VAR = "  MY: order\n"
      + "  PCF_APP_NAME : prod\n"
      + "  INSTANCES : 3";

  private String TEST_VAR_1 = "  MY: login\n"
      + "  DUMMY : dummy\n"
      + "  REPLACE_ROUTE_1 : qa.io\n"
      + "  REPLACE_ROUTE_2 : prod.io";

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
  @Mock private SweepingOutputService sweepingOutputService;
  @Mock private StateExecutionService stateExecutionService;
  @Mock private WorkflowExecutionService workflowExecutionService;
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

  public static final String INVALID_ROUTES_MANIFEST_YAML_CONTENT = "  applications:\n"
      + "  - name : ${APPLICATION_NAME}\n"
      + "    memory: 850M\n"
      + "    instances : ${INSTANCE_COUNT}\n"
      + "    buildpack: https://github.com/cloudfoundry/java-buildpack.git\n"
      + "    path: ${FILE_LOCATION}\n"
      + "    routes:\n"
      + "      -route: app.harness.io\n";

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

    when(context.renderExpression(anyString())).thenAnswer(new Answer<String>() {
      @Override
      public String answer(InvocationOnMock invocation) throws Throwable {
        Object[] args = invocation.getArguments();
        return (String) args[0];
      }
    });

    when(applicationManifestService.getByServiceId(anyString(), anyString(), any())).thenReturn(manifest);
    when(applicationManifestService.getManifestFileByFileName(anyString(), anyString()))
        .thenReturn(
            ManifestFile.builder().fileName(MANIFEST_YML).fileContent(PcfSetupStateTest.MANIFEST_YAML_CONTENT).build());

    doReturn("").when(delegateService).queueTask(any());
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testFetchManifestYmlString() throws Exception {
    when(featureFlagService.isEnabled(FeatureName.INFRA_MAPPING_REFACTOR, ACCOUNT_ID))
        .thenReturn(true)
        .thenReturn(false);

    doReturn(PcfServiceSpecification.builder()
                 .serviceId(SERVICE_ID)
                 .manifestYaml(PcfSetupStateTest.MANIFEST_YAML_CONTENT)
                 .build())
        .when(serviceResourceService)
        .getPcfServiceSpecification(anyString(), anyString());

    String yaml = pcfStateHelper.fetchManifestYmlString(context, SERVICE_ELEMENT);
    assertThat(yaml).isNotNull();
    assertThat(yaml).isEqualTo(PcfSetupStateTest.MANIFEST_YAML_CONTENT);

    yaml = pcfStateHelper.fetchManifestYmlString(context, SERVICE_ELEMENT);
    assertThat(yaml).isNotNull();
    assertThat(yaml).isEqualTo(PcfSetupStateTest.MANIFEST_YAML_CONTENT);
  }

  @Test
  @Owner(developers = ANSHUL)
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
    PcfManifestsPackage pcfManifestsPackage = pcfStateHelper.getFinalManifestFilesMap(appManifestMap, fetchFilesResult);
    assertThat(pcfManifestsPackage).isNotNull();
    assertThat(pcfManifestsPackage.getManifestYml()).isEqualTo(SERVICE_MANIFEST_YML);

    // Remote overrides in environment
    ApplicationManifest envApplicationManifest = generateAppManifest(StoreType.Remote, ENV_ID);
    appManifestMap.put(K8sValuesLocation.EnvironmentGlobal, envApplicationManifest);
    GitFetchFilesResult filesResult = GitFetchFilesResult.builder()
                                          .files(Arrays.asList(GitFile.builder().fileContent(ENV_MANIFEST_YML).build()))
                                          .build();
    filesFromMultipleRepo.put(K8sValuesLocation.EnvironmentGlobal.name(), filesResult);
    fetchFilesResult.setFilesFromMultipleRepo(filesFromMultipleRepo);
    pcfManifestsPackage = pcfStateHelper.getFinalManifestFilesMap(appManifestMap, fetchFilesResult);
    assertThat(pcfManifestsPackage).isNotNull();
    assertThat(pcfManifestsPackage.getManifestYml()).isEqualTo(ENV_MANIFEST_YML);

    // Local Environment Service manifest files
    ApplicationManifest serviceEnvApplicationManifest = generateAppManifest(StoreType.Local, envServiceId);
    appManifestMap.put(K8sValuesLocation.Environment, serviceEnvApplicationManifest);

    when(applicationManifestService.getManifestFilesByAppManifestId(APP_ID, envServiceId))
        .thenReturn(Arrays.asList(ManifestFile.builder().fileContent(ENV_SERVICE_MANIFEST_YML).build()));
    pcfManifestsPackage = pcfStateHelper.getFinalManifestFilesMap(appManifestMap, fetchFilesResult);
    assertThat(pcfManifestsPackage).isNotNull();
    assertThat(pcfManifestsPackage.getManifestYml()).isEqualTo(ENV_SERVICE_MANIFEST_YML);
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testGetFinalManifestFilesMapWithNullGitFetchFileResponse() {
    Map<K8sValuesLocation, ApplicationManifest> appManifestMap = new HashMap<>();
    Map<String, GitFetchFilesResult> filesFromMultipleRepo = new HashMap<>();
    GitFetchFilesFromMultipleRepoResult fetchFilesResult = GitFetchFilesFromMultipleRepoResult.builder().build();

    ApplicationManifest serviceApplicationManifest = generateAppManifest(StoreType.Remote, SERVICE_ID);
    appManifestMap.put(K8sValuesLocation.Service, serviceApplicationManifest);

    PcfManifestsPackage pcfManifestsPackage = pcfStateHelper.getFinalManifestFilesMap(appManifestMap, fetchFilesResult);
    assertThat(pcfManifestsPackage.getManifestYml()).isNull();
    assertThat(pcfManifestsPackage.getVariableYmls()).isNull();

    filesFromMultipleRepo.put(K8sValuesLocation.Service.name(), null);
    fetchFilesResult.setFilesFromMultipleRepo(filesFromMultipleRepo);
    pcfManifestsPackage = pcfStateHelper.getFinalManifestFilesMap(appManifestMap, fetchFilesResult);
    assertThat(pcfManifestsPackage.getManifestYml()).isNull();
    assertThat(pcfManifestsPackage.getVariableYmls()).isNull();
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testGetFinalManifestFilesMapWithInvalidContent() {
    Map<K8sValuesLocation, ApplicationManifest> appManifestMap = new HashMap<>();
    GitFetchFilesFromMultipleRepoResult fetchFilesResult = GitFetchFilesFromMultipleRepoResult.builder().build();

    // Local Service manifest files
    ApplicationManifest serviceApplicationManifest = generateAppManifest(StoreType.Local, SERVICE_ID);
    appManifestMap.put(K8sValuesLocation.Service, serviceApplicationManifest);

    when(applicationManifestService.getManifestFilesByAppManifestId(APP_ID, SERVICE_ID))
        .thenReturn(Arrays.asList(ManifestFile.builder().fileContent("abc").build()));
    PcfManifestsPackage pcfManifestsPackage = pcfStateHelper.getFinalManifestFilesMap(appManifestMap, fetchFilesResult);
    assertThat(pcfManifestsPackage).isNotNull();
    assertThat(pcfManifestsPackage.getManifestYml()).isBlank();
  }

  private ApplicationManifest generateAppManifest(StoreType storeType, String id) {
    ApplicationManifest applicationManifest = ApplicationManifest.builder().storeType(storeType).build();
    applicationManifest.setUuid(id);
    applicationManifest.setAppId(APP_ID);

    return applicationManifest;
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testGetDelegateTask() throws Exception {
    String waitId = generateUuid();
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
  @Owner(developers = ADWAIT)
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
  @Owner(developers = ADWAIT)
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

    ExecutionResponse response = pcfStateHelper.queueDelegateTaskForRouteUpdate(requestData,
        SetupSweepingOutputPcf.builder()
            .pcfCommandRequest(PcfCommandSetupRequest.builder().organization("org").space("space").build())
            .build());
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
  @Owner(developers = ADWAIT)
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

    // Test 5: no-route in manifest, routes should be empty, ignore inframap routes
    infrastructureMapping.setRouteMaps(null);
    try {
      pcfStateHelper.getRouteMaps(INVALID_ROUTES_MANIFEST_YAML_CONTENT, infrastructureMapping);
      fail("Exception expected");
    } catch (Exception e) {
      assertThat(e instanceof InvalidRequestException).isTrue();
    }
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testApplyVarsYmlSubstitutionIfApplicable() {
    PcfManifestsPackage pcfManifestsPackage =
        PcfManifestsPackage.builder()
            .manifestYml(TEST_APP_MANIFEST.replace(REPLACE_ME, "TEST_((MY))__((PCF_APP_NAME))"))
            .variableYmls(Arrays.asList(TEST_VAR_1))
            .build();

    List<String> routes = pcfStateHelper.applyVarsYmlSubstitutionIfApplicable(
        Arrays.asList("((REPLACE_ROUTE_1))", "((REPLACE_ROUTE_2))", "test.io"), pcfManifestsPackage);

    assertThat(routes).containsExactly("qa.io", "prod.io", "test.io");
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testFetchMaxCountFromManifest() {
    PcfManifestsPackage pcfManifestsPackage =
        PcfManifestsPackage.builder().manifestYml(TEST_APP_MANIFEST).variableYmls(Arrays.asList(TEST_VAR)).build();

    assertThat(pcfStateHelper.fetchMaxCountFromManifest(pcfManifestsPackage, 1)).isEqualTo(3);

    pcfManifestsPackage.setManifestYml(TEST_APP_MANIFEST.replace("((INSTANCES))", "2"));
    assertThat(pcfStateHelper.fetchMaxCountFromManifest(pcfManifestsPackage, 3)).isEqualTo(2);

    pcfManifestsPackage.setManifestYml(
        TEST_APP_MANIFEST.replace("((INSTANCES))", INSTANCE_PLACEHOLDER_TOKEN_DEPRECATED));
    assertThat(pcfStateHelper.fetchMaxCountFromManifest(pcfManifestsPackage, 4)).isEqualTo(4);

    pcfManifestsPackage.setManifestYml(TEST_APP_MANIFEST.replace("((INSTANCES))", INSTANCE_PLACEHOLDER_TOKEN_DEPRECATED)
                                           .replace("instances", "INSTANCES"));
    assertThat(pcfStateHelper.fetchMaxCountFromManifest(pcfManifestsPackage, 4)).isEqualTo(4);

    pcfManifestsPackage.setVariableYmls(emptyList());
    pcfManifestsPackage.setManifestYml(TEST_APP_MANIFEST);
    try {
      assertThat(pcfStateHelper.fetchMaxCountFromManifest(pcfManifestsPackage, 1)).isEqualTo(3);
    } catch (InvalidRequestException e) {
      assertThat(e.getMessage())
          .isEqualTo("No Valid Variable file Found, please verify var file is present and has valid structure");
    }
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testFetchPcfApplicationName() {
    PcfManifestsPackage pcfManifestsPackage =
        PcfManifestsPackage.builder()
            .manifestYml(TEST_APP_MANIFEST.replace(REPLACE_ME, "TEST_((MY))__((PCF_APP_NAME))"))
            .variableYmls(Arrays.asList(TEST_VAR))
            .build();

    String appName = pcfStateHelper.fetchPcfApplicationName(pcfManifestsPackage, "app");
    assertThat(appName).isEqualTo("TEST_order__prod");

    pcfManifestsPackage.setManifestYml(TEST_APP_MANIFEST.replace(REPLACE_ME, "TEST_((PCF_APP_NAME))"));
    appName = pcfStateHelper.fetchPcfApplicationName(pcfManifestsPackage, "app");
    assertThat(appName).isEqualTo("TEST_prod");

    pcfManifestsPackage.setManifestYml(TEST_APP_MANIFEST.replace(REPLACE_ME, "TEST"));
    appName = pcfStateHelper.fetchPcfApplicationName(pcfManifestsPackage, "app");
    assertThat(appName).isEqualTo("TEST");

    pcfManifestsPackage.setManifestYml(TEST_APP_MANIFEST.replace(REPLACE_ME, "TEST_((PCF_APP_NAME))__((DUMMY))"));
    pcfManifestsPackage.setVariableYmls(Arrays.asList(TEST_VAR, TEST_VAR_1));
    appName = pcfStateHelper.fetchPcfApplicationName(pcfManifestsPackage, "app");
    assertThat(appName).isEqualTo("TEST_prod__dummy");

    // manifest is pcf legacy manifest, and contains ${APPLICATION_NAME}
    pcfManifestsPackage.setManifestYml(TEST_APP_MANIFEST.replace(REPLACE_ME, LEGACY_NAME_PCF_MANIFEST));
    appName = pcfStateHelper.fetchPcfApplicationName(pcfManifestsPackage, "TEST");
    assertThat(appName).isEqualTo("TEST");
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testGetManifestFromPcfServiceSpecification() {
    String ymlContent = "yml";
    doReturn(null)
        .doReturn(PcfServiceSpecification.builder().manifestYaml(ymlContent).build())
        .when(serviceResourceService)
        .getPcfServiceSpecification(anyString(), any());

    try {
      pcfStateHelper.getManifestFromPcfServiceSpecification(context, SERVICE_ELEMENT);
      fail("Exception was expected");
    } catch (Exception e) {
      assertThat(e instanceof InvalidArgumentsException).isTrue();
    }

    assertThat(pcfStateHelper.getManifestFromPcfServiceSpecification(context, SERVICE_ELEMENT)).isEqualTo(ymlContent);
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testAddToPcfManifestFilesMap() {
    PcfManifestsPackage pcfManifestsPackage = PcfManifestsPackage.builder().build();
    pcfStateHelper.addToPcfManifestFilesMap(TEST_APP_MANIFEST, pcfManifestsPackage);

    // 1
    assertThat(pcfManifestsPackage.getManifestYml()).isEqualTo(TEST_APP_MANIFEST);

    // 2
    pcfStateHelper.addToPcfManifestFilesMap(TEST_VAR, pcfManifestsPackage);
    assertThat(pcfManifestsPackage.getVariableYmls()).isNotEmpty();
    assertThat(pcfManifestsPackage.getVariableYmls().get(0)).isEqualTo(TEST_VAR);

    // 3
    pcfStateHelper.addToPcfManifestFilesMap(TEST_VAR_1, pcfManifestsPackage);
    assertThat(pcfManifestsPackage.getVariableYmls()).isNotEmpty();
    assertThat(pcfManifestsPackage.getVariableYmls().get(0)).isEqualTo(TEST_VAR);
    assertThat(pcfManifestsPackage.getVariableYmls().get(1)).isEqualTo(TEST_VAR_1);
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testUseNoRoute() {
    assertThat(pcfStateHelper.getRouteMaps(TEST_APP_MANIFEST, PcfInfrastructureMapping.builder().build())).isEmpty();

    String route = "url1";
    List<String> routeMaps = pcfStateHelper.getRouteMaps(
        TEST_APP_MANIFEST, PcfInfrastructureMapping.builder().routeMaps(Arrays.asList(route)).build());
    assertThat(routeMaps).isNotEmpty();
    assertThat(routeMaps.get(0)).isEqualTo(route);
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testEvaluateExpressionsInManifestTypes() {
    String v1 = "app:app1";
    String v2 = "count:1";

    PcfManifestsPackage pcfManifestsPackage = PcfManifestsPackage.builder()
                                                  .manifestYml(TEST_APP_MANIFEST)
                                                  .variableYmls(Arrays.asList(TEST_VAR, TEST_VAR_1))
                                                  .build();
    doReturn(v1).doReturn(v2).when(context).renderExpression(anyString());

    pcfStateHelper.evaluateExpressionsInManifestTypes(context, pcfManifestsPackage);
    assertThat(pcfManifestsPackage.getManifestYml()).isEqualTo(TEST_APP_MANIFEST);
    assertThat(pcfManifestsPackage.getVariableYmls()).isNotEmpty();
    assertThat(pcfManifestsPackage.getVariableYmls().size()).isEqualTo(2);
    assertThat(pcfManifestsPackage.getVariableYmls().get(0)).isEqualTo(v1);
    assertThat(pcfManifestsPackage.getVariableYmls().get(1)).isEqualTo(v2);
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testGenerateManifestMap() {
    doReturn(true).when(featureFlagService).isEnabled(eq(FeatureName.INFRA_MAPPING_REFACTOR), anyString());
    Service service = Service.builder().uuid(SERVICE_ID).isPcfV2(true).build();
    doReturn(service).when(serviceResourceService).get(anyString());

    Map<K8sValuesLocation, ApplicationManifest> map = new HashMap<>();
    map.put(K8sValuesLocation.Service,
        ApplicationManifest.builder()
            .storeType(StoreType.Local)
            .serviceId(SERVICE_ID)
            .kind(AppManifestKind.PCF_OVERRIDE)
            .build());

    doReturn(PcfSetupStateExecutionData.builder().appManifestMap(map).build())
        .doReturn(null)
        .when(context)
        .getStateExecutionData();

    doReturn(Arrays.asList(ManifestFile.builder().fileContent(TEST_APP_MANIFEST).build()))
        .when(applicationManifestService)
        .getManifestFilesByAppManifestId(anyString(), anyString());

    PcfManifestsPackage pcfManifestsPackage = pcfStateHelper.generateManifestMap(
        context, map, anApplication().accountId(ACCOUNT_ID).build(), SERVICE_ELEMENT);

    assertThat(pcfManifestsPackage.getManifestYml()).isEqualTo(TEST_APP_MANIFEST);

    pcfManifestsPackage = pcfStateHelper.generateManifestMap(
        context, map, anApplication().accountId(ACCOUNT_ID).build(), SERVICE_ELEMENT);
    assertThat(pcfManifestsPackage.getManifestYml()).isEqualTo(TEST_APP_MANIFEST);
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testIsManifestInGit() throws Exception {
    Map<K8sValuesLocation, ApplicationManifest> appManifestMap = new HashMap<>();
    appManifestMap.put(K8sValuesLocation.Service, ApplicationManifest.builder().storeType(Remote).build());
    appManifestMap.put(K8sValuesLocation.Environment, ApplicationManifest.builder().storeType(Local).build());
    assertThat(pcfStateHelper.isManifestInGit(appManifestMap)).isTrue();

    appManifestMap.remove(K8sValuesLocation.Service);
    assertThat(pcfStateHelper.isManifestInGit(appManifestMap)).isFalse();
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void testFindSetupSweepingOutputForNewExecution() throws Exception {
    final String infraDefinitionId = generateUuid();
    final String setupSweepingOutputPcfId = generateUuid();
    final String phaseName = "Phase 1";
    PhaseElement phaseElement =
        PhaseElement.builder().infraDefinitionId(infraDefinitionId).phaseName(phaseName).build();
    when(context.getContextElement(ContextElementType.PARAM, PhaseElement.PHASE_PARAM)).thenReturn(phaseElement);
    when(context.prepareSweepingOutputInquiryBuilder())
        .thenReturn(SweepingOutputInquiry.builder()
                        .appId(APP_ID)
                        .workflowExecutionId(WORKFLOW_EXECUTION_ID)
                        .stateExecutionId(STATE_EXECUTION_ID));
    SetupSweepingOutputPcf setupSweepingOutputPcf =
        SetupSweepingOutputPcf.builder()
            .uuid(setupSweepingOutputPcfId)
            .name(PCF_SERVICE_NAME)
            .maxInstanceCount(10)
            .desiredActualFinalCount(10)
            .pcfCommandRequest(PcfCommandSetupRequest.builder().space("SPACE").organization("ORG").build())
            .newPcfApplicationDetails(PcfAppSetupTimeDetails.builder()
                                          .applicationName("APP_NAME_SERVICE_NAME_ENV_NAME__1")
                                          .applicationGuid("1")
                                          .build())
            .infraMappingId(INFRA_MAPPING_ID)
            .resizeStrategy(RESIZE_NEW_FIRST)
            .routeMaps(Arrays.asList("R1", "R2"))
            .build();

    when(sweepingOutputService.findSweepingOutput(any())).thenReturn(setupSweepingOutputPcf);
    SetupSweepingOutputPcf sweepingOutputPcf = pcfStateHelper.findSetupSweepingOutputPcf(context, false);
    assertThat(sweepingOutputPcf).isNotNull();
    assertThat(sweepingOutputPcf.getUuid()).isEqualTo(setupSweepingOutputPcfId);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void testFindSetupSweepingOutputSecondPhase() throws Exception {
    final String infraDefinitionId = generateUuid();
    final String phaseName = "Phase 1";
    SweepingOutputInquiryBuilder sweepingOutputInquiryBuilder = SweepingOutputInquiry.builder()
                                                                    .appId(APP_ID)
                                                                    .workflowExecutionId(WORKFLOW_EXECUTION_ID)
                                                                    .stateExecutionId(STATE_EXECUTION_ID);
    PhaseElement phaseElement =
        PhaseElement.builder().infraDefinitionId(infraDefinitionId).phaseName(phaseName).build();
    when(context.getContextElement(ContextElementType.PARAM, PhaseElement.PHASE_PARAM)).thenReturn(phaseElement);
    when(context.prepareSweepingOutputInquiryBuilder()).thenReturn(sweepingOutputInquiryBuilder);
    when(sweepingOutputService.findSweepingOutput(
             sweepingOutputInquiryBuilder.name(SetupSweepingOutputPcf.SWEEPING_OUTPUT_NAME + phaseName).build()))
        .thenReturn(null);
    when(stateExecutionService.fetchPreviousPhaseStateExecutionInstance(
             APP_ID, WORKFLOW_EXECUTION_ID, STATE_EXECUTION_ID))
        .thenReturn(null);
    SetupSweepingOutputPcf sweepingOutputPcf = pcfStateHelper.findSetupSweepingOutputPcf(context, false);
    assertThat(sweepingOutputPcf).isNotNull();
    assertThat(sweepingOutputPcf.getUuid()).isNull();
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void testFindSetupSweepingOutputSecondPhaseServiceRepeat() throws Exception {
    final String infraDefinitionId = generateUuid();
    final String firstPhaseName = "Phase 1";
    final String secondPhaseName = "Phase 2";
    final String currentStateExecutionId = generateUuid();
    final String previousStateExecutionId = generateUuid();
    final String serviceId = generateUuid();
    final String setupSweepingOutputPcfId = generateUuid();

    SetupSweepingOutputPcf setupSweepingOutputPcf =
        SetupSweepingOutputPcf.builder()
            .uuid(setupSweepingOutputPcfId)
            .name(PCF_SERVICE_NAME)
            .maxInstanceCount(10)
            .desiredActualFinalCount(10)
            .pcfCommandRequest(PcfCommandSetupRequest.builder().space("SPACE").organization("ORG").build())
            .newPcfApplicationDetails(PcfAppSetupTimeDetails.builder()
                                          .applicationName("APP_NAME_SERVICE_NAME_ENV_NAME__1")
                                          .applicationGuid("1")
                                          .build())
            .infraMappingId(INFRA_MAPPING_ID)
            .resizeStrategy(RESIZE_NEW_FIRST)
            .routeMaps(Arrays.asList("R1", "R2"))
            .build();

    PhaseExecutionData phaseExecutionData = PhaseExecutionDataBuilder.aPhaseExecutionData()
                                                .withInfraDefinitionId(INFRA_DEFINITION_ID)
                                                .withServiceId(serviceId)
                                                .build();
    SweepingOutputInquiryBuilder sweepingOutputInquiryBuilder1 = SweepingOutputInquiry.builder()
                                                                     .appId(APP_ID)
                                                                     .workflowExecutionId(WORKFLOW_EXECUTION_ID)
                                                                     .stateExecutionId(currentStateExecutionId);
    SweepingOutputInquiryBuilder sweepingOutputInquiryBuilder2 = SweepingOutputInquiry.builder()
                                                                     .appId(APP_ID)
                                                                     .workflowExecutionId(WORKFLOW_EXECUTION_ID)
                                                                     .stateExecutionId(previousStateExecutionId);
    PhaseElement phaseElement =
        PhaseElement.builder().infraDefinitionId(infraDefinitionId).phaseName(secondPhaseName).build();

    when(context.getContextElement(ContextElementType.PARAM, PhaseElement.PHASE_PARAM)).thenReturn(phaseElement);
    when(context.prepareSweepingOutputInquiryBuilder()).thenReturn(sweepingOutputInquiryBuilder1);
    when(sweepingOutputService.findSweepingOutput(
             sweepingOutputInquiryBuilder1.name(SetupSweepingOutputPcf.SWEEPING_OUTPUT_NAME + secondPhaseName).build()))
        .thenReturn(null);

    when(sweepingOutputService.findSweepingOutput(
             sweepingOutputInquiryBuilder2.name(SetupSweepingOutputPcf.SWEEPING_OUTPUT_NAME + firstPhaseName).build()))
        .thenReturn(setupSweepingOutputPcf);

    StateExecutionInstance currentStateExecutionInstance = Builder.aStateExecutionInstance()
                                                               .uuid(currentStateExecutionId)
                                                               .displayName(secondPhaseName)
                                                               .stateName(secondPhaseName)
                                                               .appId(APP_ID)
                                                               .executionUuid(WORKFLOW_EXECUTION_ID)
                                                               .stateType(StateType.PHASE.name())
                                                               .addContextElement(phaseElement)
                                                               .build();

    StateExecutionInstance previousStateExecutionInstance = Builder.aStateExecutionInstance()
                                                                .uuid(previousStateExecutionId)
                                                                .displayName(firstPhaseName)
                                                                .stateName(firstPhaseName)
                                                                .appId(APP_ID)
                                                                .executionUuid(WORKFLOW_EXECUTION_ID)
                                                                .stateType(StateType.PHASE.name())
                                                                .addContextElement(phaseElement)
                                                                .build();

    when(stateExecutionService.fetchPreviousPhaseStateExecutionInstance(
             APP_ID, WORKFLOW_EXECUTION_ID, currentStateExecutionId))
        .thenReturn(previousStateExecutionInstance);
    when(stateExecutionService.fetchCurrentPhaseStateExecutionInstance(
             APP_ID, WORKFLOW_EXECUTION_ID, currentStateExecutionId))
        .thenReturn(currentStateExecutionInstance);

    when(stateExecutionService.fetchPhaseExecutionDataSweepingOutput(currentStateExecutionInstance))
        .thenReturn(phaseExecutionData);
    when(stateExecutionService.fetchPhaseExecutionDataSweepingOutput(previousStateExecutionInstance))
        .thenReturn(phaseExecutionData);
    when(workflowExecutionService.checkIfOnDemand(anyString(), anyString())).thenReturn(false);

    SetupSweepingOutputPcf sweepingOutputPcf = pcfStateHelper.findSetupSweepingOutputPcf(context, false);
    assertThat(sweepingOutputPcf).isNotNull();
    assertThat(sweepingOutputPcf.getUuid()).isEqualTo(setupSweepingOutputPcf.getUuid());
    assertThat(sweepingOutputPcf.getName()).isEqualTo(setupSweepingOutputPcf.getName());
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void testObtainDeploySweepingOutputName() {
    PhaseElement phaseElement =
        PhaseElement.builder().phaseNameForRollback("Rollback Phase 1").phaseName("Phase 1").build();
    when(context.getContextElement(ContextElementType.PARAM, PhaseElement.PHASE_PARAM)).thenReturn(phaseElement);
    String outputName = pcfStateHelper.obtainDeploySweepingOutputName(context, false);
    assertThat(outputName).isNotNull();
    assertThat(outputName).isEqualTo(DeploySweepingOutputPcf.SWEEPING_OUTPUT_NAME + "Phase 1");
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void testObtainDeploySweepingOutputNameWithWhiteSpaces() {
    PhaseElement phaseElement =
        PhaseElement.builder().phaseNameForRollback("Rollback Phase 1").phaseName(" Phase 1 ").build();
    when(context.getContextElement(ContextElementType.PARAM, PhaseElement.PHASE_PARAM)).thenReturn(phaseElement);
    String outputName = pcfStateHelper.obtainDeploySweepingOutputName(context, false);
    assertThat(outputName).isNotNull();
    assertThat(outputName).isEqualTo(DeploySweepingOutputPcf.SWEEPING_OUTPUT_NAME + "Phase 1");
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void testObtainSetupSweepingOutputName() {
    PhaseElement phaseElement =
        PhaseElement.builder().phaseNameForRollback("Rollback Phase 1").phaseName("Phase 1").build();
    when(context.getContextElement(ContextElementType.PARAM, PhaseElement.PHASE_PARAM)).thenReturn(phaseElement);
    String outputName = pcfStateHelper.obtainSetupSweepingOutputName(context, false);
    assertThat(outputName).isNotNull();
    assertThat(outputName).isEqualTo(SetupSweepingOutputPcf.SWEEPING_OUTPUT_NAME + "Phase 1");
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void testObtainSetupSweepingOutputNameWithWhiteSpaces() {
    PhaseElement phaseElement =
        PhaseElement.builder().phaseNameForRollback("Rollback Phase 1").phaseName(" Phase 1 ").build();
    when(context.getContextElement(ContextElementType.PARAM, PhaseElement.PHASE_PARAM)).thenReturn(phaseElement);
    String outputName = pcfStateHelper.obtainSetupSweepingOutputName(context, false);
    assertThat(outputName).isNotNull();
    assertThat(outputName).isEqualTo(SetupSweepingOutputPcf.SWEEPING_OUTPUT_NAME + "Phase 1");
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void testObtainSwapRouteRollbackSweepingOutputName() {
    PhaseElement phaseElement =
        PhaseElement.builder().phaseNameForRollback("Rollback Phase 1").phaseName("Phase 1").build();
    when(context.getContextElement(ContextElementType.PARAM, PhaseElement.PHASE_PARAM)).thenReturn(phaseElement);
    String outputName = pcfStateHelper.obtainSwapRouteSweepingOutputName(context, false);
    assertThat(outputName).isNotNull();
    assertThat(outputName).isEqualTo(SwapRouteRollbackSweepingOutputPcf.SWEEPING_OUTPUT_NAME + "Phase 1");
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void testObtainSwapRouteRollbackSweepingOutputNameWithWhiteSpaces() {
    PhaseElement phaseElement =
        PhaseElement.builder().phaseNameForRollback("Rollback Phase 1").phaseName(" Phase 1 ").build();
    when(context.getContextElement(ContextElementType.PARAM, PhaseElement.PHASE_PARAM)).thenReturn(phaseElement);
    String outputName = pcfStateHelper.obtainSwapRouteSweepingOutputName(context, false);
    assertThat(outputName).isNotNull();
    assertThat(outputName).isEqualTo(SwapRouteRollbackSweepingOutputPcf.SWEEPING_OUTPUT_NAME + "Phase 1");
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void testGetPhaseNameForQuery() {
    final String phaseName = "Phase 1";
    when(workflowExecutionService.checkIfOnDemand(APP_ID, WORKFLOW_EXECUTION_ID)).thenReturn(false);
    String queryPhaseName = pcfStateHelper.getPhaseNameForQuery(APP_ID, WORKFLOW_EXECUTION_ID, phaseName);
    assertThat(queryPhaseName).isNotNull();
    assertThat(queryPhaseName).isEqualTo(phaseName);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void testGetPhaseNameForQueryRollback() {
    final String phaseName = "Staging Execution Phase 1";
    when(workflowExecutionService.checkIfOnDemand(APP_ID, WORKFLOW_EXECUTION_ID)).thenReturn(true);
    String queryPhaseName = pcfStateHelper.getPhaseNameForQuery(APP_ID, WORKFLOW_EXECUTION_ID, phaseName);
    assertThat(queryPhaseName).isNotNull();
    assertThat(queryPhaseName).isEqualTo("Phase 1");
  }
}
