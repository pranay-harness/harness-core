package software.wings.sm.states.pcf;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.ADWAIT;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.FeatureName.USE_PCF_CLI;
import static software.wings.beans.InstanceUnitType.COUNT;
import static software.wings.beans.InstanceUnitType.PERCENTAGE;
import static software.wings.beans.ResizeStrategy.RESIZE_NEW_FIRST;
import static software.wings.beans.ServiceTemplate.Builder.aServiceTemplate;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.artifact.Artifact.Builder.anArtifact;
import static software.wings.beans.command.Command.Builder.aCommand;
import static software.wings.beans.command.ServiceCommand.Builder.aServiceCommand;
import static software.wings.service.intfc.ServiceTemplateService.EncryptedFieldComputeMode.OBTAIN_VALUE;
import static software.wings.sm.states.pcf.PcfStateTestHelper.PHASE_NAME;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.ACTIVITY_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.APP_NAME;
import static software.wings.utils.WingsTestConstants.COMMAND_NAME;
import static software.wings.utils.WingsTestConstants.COMPUTE_PROVIDER_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.ENV_NAME;
import static software.wings.utils.WingsTestConstants.INFRA_MAPPING_ID;
import static software.wings.utils.WingsTestConstants.PCF_SERVICE_NAME;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_NAME;
import static software.wings.utils.WingsTestConstants.TEMPLATE_ID;
import static software.wings.utils.WingsTestConstants.URL;
import static software.wings.utils.WingsTestConstants.USER_NAME;
import static software.wings.utils.WingsTestConstants.WORKFLOW_EXECUTION_ID;

import io.harness.beans.DelegateTask;
import io.harness.beans.EmbeddedUser;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.SweepingOutputInstance;
import io.harness.category.element.UnitTests;
import io.harness.expression.VariableResolverTracker;
import io.harness.rule.Owner;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mongodb.morphia.Key;
import software.wings.WingsBaseTest;
import software.wings.api.PhaseElement;
import software.wings.api.ServiceElement;
import software.wings.api.pcf.SetupSweepingOutputPcf;
import software.wings.app.MainConfiguration;
import software.wings.app.PortalConfig;
import software.wings.beans.Activity;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.InfraMappingSweepingOutput;
import software.wings.beans.PcfConfig;
import software.wings.beans.Service;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.SettingAttribute;
import software.wings.beans.command.CommandType;
import software.wings.beans.command.ServiceCommand;
import software.wings.common.InfrastructureConstants;
import software.wings.common.VariableProcessor;
import software.wings.expression.ManagerExpressionEvaluator;
import software.wings.helpers.ext.pcf.request.PcfCommandDeployRequest;
import software.wings.helpers.ext.pcf.request.PcfCommandSetupRequest;
import software.wings.helpers.ext.pcf.response.PcfAppSetupTimeDetails;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.SweepingOutputService;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.WorkflowStandardParams;

import java.util.Arrays;
import java.util.Collections;

public class PcfDeployStateTest extends WingsBaseTest {
  @Mock private SettingsService settingsService;
  @Mock private DelegateService delegateService;
  @Mock private ServiceResourceService serviceResourceService;
  @Mock private ActivityService activityService;
  @Mock private InfrastructureMappingService infrastructureMappingService;
  @Mock private AppService appService;
  @Mock private EnvironmentService environmentService;
  @Mock private ServiceTemplateService serviceTemplateService;
  @Mock private SecretManager secretManager;
  @Mock private MainConfiguration configuration;
  @Mock private PortalConfig portalConfig;
  @Mock private ArtifactService artifactService;
  @Mock private VariableProcessor variableProcessor;
  @Mock private ManagerExpressionEvaluator evaluator;
  @Mock private EncryptionService encryptionService;
  @Mock private SweepingOutputService sweepingOutputService;
  @Mock private FeatureFlagService featureFlagService;
  private PcfStateTestHelper pcfStateTestHelper = new PcfStateTestHelper();
  public static final String ORG = "ORG";
  public static final String SPACE = "SPACE";

  @InjectMocks private PcfDeployState pcfDeployState;
  @InjectMocks private WorkflowStandardParams workflowStandardParams = pcfStateTestHelper.getWorkflowStandardParams();

  private ServiceElement serviceElement = pcfStateTestHelper.getServiceElement();
  @InjectMocks private PhaseElement phaseElement = pcfStateTestHelper.getPhaseElement(serviceElement);

  private StateExecutionInstance stateExecutionInstance =
      pcfStateTestHelper.getStateExecutionInstanceForDeployState(workflowStandardParams, phaseElement, serviceElement);

  private Application app = anApplication().uuid(APP_ID).name(APP_NAME).build();
  private Environment env = anEnvironment().appId(APP_ID).uuid(ENV_ID).name(ENV_NAME).build();
  private Service service = Service.builder().appId(APP_ID).uuid(SERVICE_ID).name(SERVICE_NAME).build();
  private SettingAttribute computeProvider =
      aSettingAttribute()
          .withValue(PcfConfig.builder().accountId(ACCOUNT_ID).endpointUrl(URL).username(USER_NAME).build())
          .build();
  private ExecutionContextImpl context;

  private String outputName = InfrastructureConstants.PHASE_INFRA_MAPPING_KEY_NAME + phaseElement.getUuid();
  private SweepingOutputInstance sweepingOutputInstance =
      SweepingOutputInstance.builder()
          .appId(APP_ID)
          .name(outputName)
          .uuid(generateUuid())
          .workflowExecutionId(WORKFLOW_EXECUTION_ID)
          .stateExecutionId(null)
          .pipelineExecutionId(null)
          .value(InfraMappingSweepingOutput.builder().infraMappingId(INFRA_MAPPING_ID).build())
          .build();
  private SetupSweepingOutputPcf setupSweepingOutputPcf =
      SetupSweepingOutputPcf.builder()
          .uuid(serviceElement.getUuid())
          .name(PCF_SERVICE_NAME)
          .maxInstanceCount(10)
          .desiredActualFinalCount(10)
          .pcfCommandRequest(PcfCommandSetupRequest.builder().space(SPACE).organization(ORG).build())
          .newPcfApplicationDetails(PcfAppSetupTimeDetails.builder()
                                        .applicationName("APP_NAME_SERVICE_NAME_ENV_NAME__1")
                                        .applicationGuid("1")
                                        .build())
          .infraMappingId(INFRA_MAPPING_ID)
          .resizeStrategy(RESIZE_NEW_FIRST)
          .routeMaps(Arrays.asList("R1", "R2"))
          .build();

  /**
   * Set up.
   */
  @Before
  public void setup() throws IllegalAccessException {
    when(secretManager.getEncryptionDetails(anyObject(), anyString(), anyString())).thenReturn(Collections.emptyList());
    PcfStateHelper pcfStateHelper = new PcfStateHelper();
    on(pcfStateHelper).set("sweepingOutputService", sweepingOutputService);
    FieldUtils.writeField(pcfDeployState, "secretManager", secretManager, true);
    FieldUtils.writeField(pcfDeployState, "pcfStateHelper", pcfStateHelper, true);

    EmbeddedUser currentUser = EmbeddedUser.builder().name("test").email("test@harness.io").build();
    workflowStandardParams.setCurrentUser(currentUser);

    context = new ExecutionContextImpl(stateExecutionInstance);

    when(appService.get(APP_ID)).thenReturn(app);
    when(appService.getApplicationWithDefaults(APP_ID)).thenReturn(app);
    when(serviceResourceService.getWithDetails(APP_ID, SERVICE_ID)).thenReturn(service);
    when(environmentService.get(APP_ID, ENV_ID, false)).thenReturn(env);

    ServiceCommand serviceCommand =
        aServiceCommand()
            .withCommand(aCommand().withCommandType(CommandType.RESIZE).withName(COMMAND_NAME).build())
            .build();
    when(serviceResourceService.getCommandByName(APP_ID, SERVICE_ID, ENV_ID, COMMAND_NAME)).thenReturn(serviceCommand);
    on(workflowStandardParams).set("appService", appService);
    on(workflowStandardParams).set("environmentService", environmentService);

    when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID))
        .thenReturn(pcfStateTestHelper.getPcfInfrastructureMapping(Arrays.asList("R1"), Arrays.asList("R2")));

    Activity activity = Activity.builder().build();
    activity.setUuid(ACTIVITY_ID);

    when(activityService.save(any(Activity.class))).thenReturn(activity);

    when(settingsService.get(COMPUTE_PROVIDER_ID)).thenReturn(computeProvider);

    when(serviceTemplateService.getTemplateRefKeysByService(APP_ID, SERVICE_ID, ENV_ID))
        .thenReturn(singletonList(new Key<>(ServiceTemplate.class, "serviceTemplate", TEMPLATE_ID)));

    when(serviceTemplateService.get(APP_ID, TEMPLATE_ID)).thenReturn(aServiceTemplate().withUuid(TEMPLATE_ID).build());
    when(serviceTemplateService.computeServiceVariables(APP_ID, ENV_ID, TEMPLATE_ID, null, OBTAIN_VALUE))
        .thenReturn(emptyList());

    when(configuration.getPortal()).thenReturn(portalConfig);
    when(portalConfig.getUrl()).thenReturn("http://www.url.com");
    when(artifactService.get(any())).thenReturn(anArtifact().build());
    when(variableProcessor.getVariables(any(), any())).thenReturn(emptyMap());
    when(evaluator.substitute(anyString(), anyMap(), any(VariableResolverTracker.class), anyString()))
        .thenAnswer(i -> i.getArguments()[0]);
    doReturn(null).when(encryptionService).decrypt(any(), any());
    when(sweepingOutputService.find(context.prepareSweepingOutputInquiryBuilder().name(outputName).build()))
        .thenReturn(sweepingOutputInstance);
    when(sweepingOutputService.findSweepingOutput(context.prepareSweepingOutputInquiryBuilder()
                                                      .name(SetupSweepingOutputPcf.SWEEPING_OUTPUT_NAME + PHASE_NAME)
                                                      .build()))
        .thenReturn(setupSweepingOutputPcf);
    doReturn(true).when(featureFlagService).isEnabled(eq(USE_PCF_CLI), anyString());
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testExecute() {
    on(context).set("serviceTemplateService", serviceTemplateService);
    on(context).set("variableProcessor", variableProcessor);
    on(context).set("sweepingOutputService", sweepingOutputService);
    on(context).set("evaluator", evaluator);

    pcfDeployState.setInstanceCount(50);
    pcfDeployState.setInstanceUnitType(PERCENTAGE);
    ExecutionResponse response = pcfDeployState.execute(context);
    assertThat(response.getExecutionStatus()).isEqualTo(ExecutionStatus.SUCCESS);
    assertThat(response).isNotNull().hasFieldOrPropertyWithValue("async", true);
    assertThat(response.getCorrelationIds()).isNotNull().hasSize(1);
    verify(activityService).save(any(Activity.class));
    verify(delegateService).queueTask(any(DelegateTask.class));

    ArgumentCaptor<DelegateTask> captor = ArgumentCaptor.forClass(DelegateTask.class);
    verify(delegateService).queueTask(captor.capture());
    DelegateTask delegateTask = captor.getValue();

    PcfCommandDeployRequest pcfCommandRequest = (PcfCommandDeployRequest) delegateTask.getData().getParameters()[0];
    assertThat(5 == pcfCommandRequest.getUpdateCount()).isTrue();
    assertThat(pcfCommandRequest.getNewReleaseName()).isEqualTo("APP_NAME_SERVICE_NAME_ENV_NAME__1");
    assertThat(pcfCommandRequest.getPcfConfig().getEndpointUrl()).isEqualTo(URL);
    assertThat(pcfCommandRequest.getPcfConfig().getUsername()).isEqualTo(USER_NAME);
    assertThat(pcfCommandRequest.getOrganization()).isEqualTo(ORG);
    assertThat(pcfCommandRequest.getSpace()).isEqualTo(SPACE);
    assertThat(pcfCommandRequest.getRouteMaps()).hasSize(2);
    assertThat(pcfCommandRequest.getRouteMaps().contains("R1")).isTrue();
    assertThat(pcfCommandRequest.getRouteMaps().contains("R2")).isTrue();
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testGetDownsizeUpdateCount() {
    // PERCENT
    pcfDeployState.setDownsizeInstanceUnitType(PERCENTAGE);
    pcfDeployState.setDownsizeInstanceCount(40);
    SetupSweepingOutputPcf setupSweepingOutputPcf = SetupSweepingOutputPcf.builder()
                                                        .useCurrentRunningInstanceCount(false)
                                                        .maxInstanceCount(10)
                                                        .desiredActualFinalCount(10)
                                                        .build();

    Integer answer = pcfDeployState.getDownsizeUpdateCount(setupSweepingOutputPcf);
    assertThat(answer.intValue()).isEqualTo(6);

    pcfDeployState.setDownsizeInstanceUnitType(COUNT);
    pcfDeployState.setDownsizeInstanceCount(4);
    answer = pcfDeployState.getDownsizeUpdateCount(setupSweepingOutputPcf);
    assertThat(answer.intValue()).isEqualTo(6);

    pcfDeployState.setDownsizeInstanceCount(null);
    pcfDeployState.setDownsizeInstanceUnitType(COUNT);
    pcfDeployState.setInstanceCount(6);
    answer = pcfDeployState.getDownsizeUpdateCount(setupSweepingOutputPcf);
    assertThat(answer.intValue()).isEqualTo(4);

    pcfDeployState.setDownsizeInstanceCount(null);
    pcfDeployState.setDownsizeInstanceUnitType(null);
    pcfDeployState.setInstanceUnitType(COUNT);
    pcfDeployState.setInstanceCount(6);
    answer = pcfDeployState.getDownsizeUpdateCount(setupSweepingOutputPcf);
    assertThat(answer.intValue()).isEqualTo(4);

    pcfDeployState.setDownsizeInstanceCount(null);
    pcfDeployState.setDownsizeInstanceUnitType(PERCENTAGE);
    pcfDeployState.setInstanceCount(60);
    answer = pcfDeployState.getDownsizeUpdateCount(setupSweepingOutputPcf);
    assertThat(answer.intValue()).isEqualTo(4);

    pcfDeployState.setDownsizeInstanceCount(null);
    pcfDeployState.setDownsizeInstanceUnitType(null);
    pcfDeployState.setInstanceUnitType(PERCENTAGE);
    pcfDeployState.setInstanceCount(40);
    answer = pcfDeployState.getDownsizeUpdateCount(setupSweepingOutputPcf);
    assertThat(answer.intValue()).isEqualTo(6);
  }
}
