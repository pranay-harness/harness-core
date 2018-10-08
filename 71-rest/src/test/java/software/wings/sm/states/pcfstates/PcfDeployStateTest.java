package software.wings.sm.states.pcfstates;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.joor.Reflect.on;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.ServiceTemplate.Builder.aServiceTemplate;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.artifact.Artifact.Builder.anArtifact;
import static software.wings.beans.command.Command.Builder.aCommand;
import static software.wings.beans.command.ServiceCommand.Builder.aServiceCommand;
import static software.wings.common.Constants.URL;
import static software.wings.service.intfc.ServiceTemplateService.EncryptedFieldComputeMode.OBTAIN_VALUE;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.ACTIVITY_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.APP_NAME;
import static software.wings.utils.WingsTestConstants.COMMAND_NAME;
import static software.wings.utils.WingsTestConstants.COMPUTE_PROVIDER_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.ENV_NAME;
import static software.wings.utils.WingsTestConstants.INFRA_MAPPING_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_NAME;
import static software.wings.utils.WingsTestConstants.TEMPLATE_ID;
import static software.wings.utils.WingsTestConstants.USER_NAME;

import org.apache.commons.lang3.reflect.MethodUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mongodb.morphia.Key;
import software.wings.WingsBaseTest;
import software.wings.api.PhaseElement;
import software.wings.api.ServiceElement;
import software.wings.api.pcf.PcfSetupContextElement;
import software.wings.app.MainConfiguration;
import software.wings.app.PortalConfig;
import software.wings.beans.Activity;
import software.wings.beans.Application;
import software.wings.beans.DelegateTask;
import software.wings.beans.Environment;
import software.wings.beans.InstanceUnitType;
import software.wings.beans.PcfConfig;
import software.wings.beans.Service;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.SettingAttribute;
import software.wings.beans.command.CommandType;
import software.wings.beans.command.ServiceCommand;
import software.wings.common.VariableProcessor;
import software.wings.expression.ManagerExpressionEvaluator;
import software.wings.helpers.ext.pcf.request.PcfCommandDeployRequest;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.WorkflowStandardParams;
import software.wings.sm.states.pcf.PcfDeployState;
import software.wings.sm.states.pcf.PcfStateHelper;

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
  private PcfStateTestHelper pcfStateTestHelper = new PcfStateTestHelper();
  public static final String ORG = "ORG";
  public static final String SPACE = "SPACE";

  @InjectMocks private PcfDeployState pcfDeployState;
  @InjectMocks private WorkflowStandardParams workflowStandardParams = pcfStateTestHelper.getWorkflowStandardParams();

  private ServiceElement serviceElement = pcfStateTestHelper.getServiceElement();
  @InjectMocks private PhaseElement phaseElement = pcfStateTestHelper.getPhaseElement(serviceElement);

  private StateExecutionInstance stateExecutionInstance =
      pcfStateTestHelper.getStateExecutionInstanceForDeployState(workflowStandardParams, phaseElement, serviceElement);

  private Application app = anApplication().withUuid(APP_ID).withName(APP_NAME).build();
  private Environment env = anEnvironment().withAppId(APP_ID).withUuid(ENV_ID).withName(ENV_NAME).build();
  private Service service = Service.builder().appId(APP_ID).uuid(SERVICE_ID).name(SERVICE_NAME).build();
  private SettingAttribute computeProvider =
      aSettingAttribute()
          .withValue(PcfConfig.builder().accountId(ACCOUNT_ID).endpointUrl(URL).username(USER_NAME).build())
          .build();
  private ExecutionContextImpl context;

  /**
   * Set up.
   */
  @Before
  public void setup() {
    when(secretManager.getEncryptionDetails(anyObject(), anyString(), anyString())).thenReturn(Collections.emptyList());
    setInternalState(pcfDeployState, "secretManager", secretManager);
    setInternalState(pcfDeployState, "pcfStateHelper", new PcfStateHelper());

    context = new ExecutionContextImpl(stateExecutionInstance);

    when(appService.get(APP_ID)).thenReturn(app);
    when(appService.getApplicationWithDefaults(APP_ID)).thenReturn(app);
    when(serviceResourceService.get(APP_ID, SERVICE_ID)).thenReturn(service);
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
    when(artifactService.get(any(), any())).thenReturn(anArtifact().build());
    when(variableProcessor.getVariables(any(), any())).thenReturn(emptyMap());
    when(evaluator.substitute(any(), any(), any())).thenAnswer(i -> i.getArguments()[0]);
    doReturn(null).when(encryptionService).decrypt(any(), any());
  }

  @Test
  public void testExecute() {
    on(context).set("serviceTemplateService", serviceTemplateService);
    on(context).set("variableProcessor", variableProcessor);
    on(context).set("evaluator", evaluator);

    pcfDeployState.setInstanceCount(50);
    pcfDeployState.setInstanceUnitType(InstanceUnitType.PERCENTAGE);
    ExecutionResponse response = pcfDeployState.execute(context);
    assertEquals(ExecutionStatus.SUCCESS, response.getExecutionStatus());
    assertThat(response).isNotNull().hasFieldOrPropertyWithValue("async", true);
    assertThat(response.getCorrelationIds()).isNotNull().hasSize(1);
    verify(activityService).save(any(Activity.class));
    verify(delegateService).queueTask(any(DelegateTask.class));

    ArgumentCaptor<DelegateTask> captor = ArgumentCaptor.forClass(DelegateTask.class);
    verify(delegateService).queueTask(captor.capture());
    DelegateTask delegateTask = captor.getValue();

    PcfCommandDeployRequest pcfCommandRequest = (PcfCommandDeployRequest) delegateTask.getParameters()[0];
    assertTrue(5 == pcfCommandRequest.getUpdateCount());
    assertEquals("APP_NAME_SERVICE_NAME_ENV_NAME__1", pcfCommandRequest.getNewReleaseName());
    assertEquals(URL, pcfCommandRequest.getPcfConfig().getEndpointUrl());
    assertEquals(USER_NAME, pcfCommandRequest.getPcfConfig().getUsername());
    assertEquals(ORG, pcfCommandRequest.getOrganization());
    assertEquals(SPACE, pcfCommandRequest.getSpace());
    assertEquals(2, pcfCommandRequest.getRouteMaps().size());
    assertTrue(pcfCommandRequest.getRouteMaps().contains("R1"));
    assertTrue(pcfCommandRequest.getRouteMaps().contains("R2"));
  }

  @Test
  public void testGetDownsizeUpdateCount() throws Exception {
    // PERCENT
    pcfDeployState.setDownsizeInstanceUnitType(InstanceUnitType.PERCENTAGE);
    pcfDeployState.setDownsizeInstanceCount(30);
    Integer answer = (Integer) MethodUtils.invokeMethod(pcfDeployState, true, "getDownsizeUpdateCount",
        new Object[] {50, PcfSetupContextElement.builder().maxInstanceCount(100).build()});
    assertEquals(70, answer.intValue());

    pcfDeployState.setDownsizeInstanceCount(80);
    answer = (Integer) MethodUtils.invokeMethod(pcfDeployState, true, "getDownsizeUpdateCount",
        new Object[] {50, PcfSetupContextElement.builder().maxInstanceCount(100).build()});
    assertEquals(20, answer.intValue());

    pcfDeployState.setDownsizeInstanceCount(100);
    answer = (Integer) MethodUtils.invokeMethod(pcfDeployState, true, "getDownsizeUpdateCount",
        new Object[] {50, PcfSetupContextElement.builder().maxInstanceCount(100).build()});
    assertEquals(0, answer.intValue());

    pcfDeployState.setDownsizeInstanceCount(0);
    answer = (Integer) MethodUtils.invokeMethod(pcfDeployState, true, "getDownsizeUpdateCount",
        new Object[] {50, PcfSetupContextElement.builder().maxInstanceCount(100).build()});
    assertEquals(100, answer.intValue());

    // COUNT
    pcfDeployState.setDownsizeInstanceUnitType(InstanceUnitType.COUNT);
    pcfDeployState.setDownsizeInstanceCount(90);
    answer = (Integer) MethodUtils.invokeMethod(pcfDeployState, true, "getDownsizeUpdateCount",
        new Object[] {50, PcfSetupContextElement.builder().maxInstanceCount(100).build()});
    assertEquals(10, answer.intValue());

    pcfDeployState.setDownsizeInstanceCount(60);
    answer = (Integer) MethodUtils.invokeMethod(pcfDeployState, true, "getDownsizeUpdateCount",
        new Object[] {50, PcfSetupContextElement.builder().maxInstanceCount(100).build()});
    assertEquals(40, answer.intValue());

    pcfDeployState.setDownsizeInstanceCount(100);
    answer = (Integer) MethodUtils.invokeMethod(pcfDeployState, true, "getDownsizeUpdateCount",
        new Object[] {50, PcfSetupContextElement.builder().maxInstanceCount(100).build()});
    assertEquals(0, answer.intValue());

    pcfDeployState.setDownsizeInstanceCount(0);
    answer = (Integer) MethodUtils.invokeMethod(pcfDeployState, true, "getDownsizeUpdateCount",
        new Object[] {50, PcfSetupContextElement.builder().maxInstanceCount(100).build()});
    assertEquals(100, answer.intValue());
  }
}
