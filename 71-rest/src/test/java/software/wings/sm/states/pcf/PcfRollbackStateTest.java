package software.wings.sm.states.pcf;

import static io.harness.rule.OwnerRule.ADWAIT;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.ACTIVITY_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.APP_NAME;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.ENV_NAME;
import static software.wings.utils.WingsTestConstants.PCF_SERVICE_NAME;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_NAME;
import static software.wings.utils.WingsTestConstants.URL;
import static software.wings.utils.WingsTestConstants.USER_NAME;

import io.harness.category.element.UnitTests;
import io.harness.expression.VariableResolverTracker;
import io.harness.rule.Owner;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import software.wings.WingsBaseTest;
import software.wings.api.PhaseElement;
import software.wings.api.ServiceElement;
import software.wings.api.pcf.DeploySweepingOutputPcf;
import software.wings.api.pcf.PcfDeployStateExecutionData;
import software.wings.api.pcf.PcfServiceData;
import software.wings.api.pcf.SetupSweepingOutputPcf;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.PcfConfig;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.common.VariableProcessor;
import software.wings.expression.ManagerExpressionEvaluator;
import software.wings.helpers.ext.pcf.request.PcfCommandRequest;
import software.wings.helpers.ext.pcf.request.PcfCommandRollbackRequest;
import software.wings.helpers.ext.pcf.request.PcfCommandSetupRequest;
import software.wings.helpers.ext.pcf.response.PcfAppSetupTimeDetails;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.service.intfc.sweepingoutput.SweepingOutputService;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.WorkflowStandardParams;

import java.util.Arrays;

public class PcfRollbackStateTest extends WingsBaseTest {
  @Mock private VariableProcessor variableProcessor;
  @Mock private ManagerExpressionEvaluator evaluator;
  @Mock private SecretManager secretManager;
  @Mock private SweepingOutputService sweepingOutputService;
  @Mock private PcfStateHelper pcfStateHelper;
  @Mock private FeatureFlagService featureFlagService;
  public static final String ORG = "ORG";
  public static final String SPACE = "SPACE";

  @Spy @InjectMocks private PcfRollbackState pcfRollbackState;
  private PcfStateTestHelper pcfStateTestHelper = new PcfStateTestHelper();

  @InjectMocks private WorkflowStandardParams workflowStandardParams = pcfStateTestHelper.getWorkflowStandardParams();
  private ServiceElement serviceElement = pcfStateTestHelper.getServiceElement();
  @InjectMocks private PhaseElement phaseElement = pcfStateTestHelper.getPhaseElement(serviceElement);

  private StateExecutionInstance stateExecutionInstance = pcfStateTestHelper.getStateExecutionInstanceForRollbackState(
      workflowStandardParams, phaseElement, serviceElement);

  private Application app = anApplication().uuid(APP_ID).name(APP_NAME).build();
  private Environment env = anEnvironment().appId(APP_ID).uuid(ENV_ID).name(ENV_NAME).build();
  private Service service = Service.builder().appId(APP_ID).uuid(SERVICE_ID).name(SERVICE_NAME).build();
  private SettingAttribute computeProvider =
      aSettingAttribute()
          .withValue(PcfConfig.builder().accountId(ACCOUNT_ID).endpointUrl(URL).username(USER_NAME).build())
          .build();

  private ExecutionContextImpl context;

  private DeploySweepingOutputPcf deploySweepingOutputPcf =
      DeploySweepingOutputPcf.builder()
          .uuid(serviceElement.getUuid())
          .name(PCF_SERVICE_NAME)
          .instanceData(Arrays.asList(
              PcfServiceData.builder().previousCount(1).desiredCount(0).name("APP_SERVICE_ENV__1").build(),
              PcfServiceData.builder().previousCount(1).desiredCount(0).name("APP_SERVICE_ENV__2").build(),
              PcfServiceData.builder().previousCount(0).desiredCount(2).name("APP_SERVICE_ENV__3").build()))
          //.resizeStrategy(RESIZE_NEW_FIRST)
          .build();

  @Before
  public void setup() throws IllegalAccessException {
    FieldUtils.writeField(pcfRollbackState, "secretManager", secretManager, true);
    context = new ExecutionContextImpl(stateExecutionInstance);
    when(variableProcessor.getVariables(any(), any())).thenReturn(emptyMap());
    when(evaluator.substitute(anyString(), anyMap(), any(VariableResolverTracker.class), anyString()))
        .thenAnswer(i -> i.getArguments()[0]);
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testExecute() throws Exception {
    on(context).set("variableProcessor", variableProcessor);
    on(context).set("evaluator", evaluator);
    on(context).set("sweepingOutputService", sweepingOutputService);
    when(sweepingOutputService.findSweepingOutput(any())).thenReturn(deploySweepingOutputPcf);

    PcfDeployStateExecutionData stateExecutionData = PcfDeployStateExecutionData.builder().build();

    SetupSweepingOutputPcf setupSweepingOutputPcf =
        SetupSweepingOutputPcf.builder()
            .newPcfApplicationDetails(
                PcfAppSetupTimeDetails.builder().applicationName("APP_SERVICE_ENV_NAME__1").build())
            .pcfCommandRequest(PcfCommandSetupRequest.builder().organization(ORG).space(SPACE).build())
            .routeMaps(Arrays.asList("R1", "R2"))
            .build();

    PcfCommandRequest pcfCommandRequest =
        pcfRollbackState.getPcfCommandRequest(context, anApplication().name(APP_NAME).uuid(APP_ID).build(), ACTIVITY_ID,
            setupSweepingOutputPcf, (PcfConfig) computeProvider.getValue(), -1, -1, stateExecutionData,
            pcfStateTestHelper.getPcfInfrastructureMapping(Arrays.asList("R1", "R2"), Arrays.asList("R3")));

    PcfCommandRollbackRequest commandRollbackRequest = (PcfCommandRollbackRequest) pcfCommandRequest;

    assertThat(stateExecutionData.getActivityId()).isEqualTo(ACTIVITY_ID);
    assertThat(commandRollbackRequest).isNotNull();
    for (PcfServiceData pcfServiceData : commandRollbackRequest.getInstanceData()) {
      if (pcfServiceData.getName().equals("APP_SERVICE_ENV__1")) {
        assertThat(1 == pcfServiceData.getDesiredCount()).isTrue();
        assertThat(0 == pcfServiceData.getPreviousCount()).isTrue();
      } else if (pcfServiceData.getName().equals("APP_SERVICE_ENV__2")) {
        assertThat(1 == pcfServiceData.getDesiredCount()).isTrue();
        assertThat(0 == pcfServiceData.getPreviousCount()).isTrue();
      } else if (pcfServiceData.getName().equals("APP_SERVICE_ENV__3")) {
        assertThat(0 == pcfServiceData.getDesiredCount()).isTrue();
        assertThat(2 == pcfServiceData.getPreviousCount()).isTrue();
      }
    }
    assertThat(commandRollbackRequest.getRouteMaps()).hasSize(2);
    assertThat(commandRollbackRequest.getRouteMaps().contains("R1")).isTrue();
    assertThat(commandRollbackRequest.getRouteMaps().contains("R2")).isTrue();
  }
}
