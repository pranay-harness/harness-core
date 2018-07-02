package software.wings.sm.states;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.joor.Reflect.on;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;
import static software.wings.api.PhaseElement.PhaseElementBuilder.aPhaseElement;
import static software.wings.api.ServiceElement.Builder.aServiceElement;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.artifact.Artifact.Builder.anArtifact;
import static software.wings.common.Constants.BUILD_NO;
import static software.wings.common.Constants.URL;
import static software.wings.sm.StateExecutionInstance.Builder.aStateExecutionInstance;
import static software.wings.sm.WorkflowStandardParams.Builder.aWorkflowStandardParams;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.ACTIVITY_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.APP_NAME;
import static software.wings.utils.WingsTestConstants.ARTIFACT_ID;
import static software.wings.utils.WingsTestConstants.COMPUTE_PROVIDER_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.ENV_NAME;
import static software.wings.utils.WingsTestConstants.INFRA_MAPPING_ID;
import static software.wings.utils.WingsTestConstants.PCF_SERVICE_NAME;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_NAME;
import static software.wings.utils.WingsTestConstants.STATE_NAME;
import static software.wings.utils.WingsTestConstants.USER_NAME;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.api.DeploymentType;
import software.wings.api.PhaseElement;
import software.wings.api.PhaseStepExecutionData;
import software.wings.api.ServiceElement;
import software.wings.api.pcf.PcfDeployContextElement;
import software.wings.api.pcf.PcfDeployStateExecutionData;
import software.wings.api.pcf.PcfServiceData;
import software.wings.api.pcf.PcfSetupContextElement;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.PcfConfig;
import software.wings.beans.PcfInfrastructureMapping;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.Artifact;
import software.wings.common.VariableProcessor;
import software.wings.expression.ExpressionEvaluator;
import software.wings.helpers.ext.pcf.request.PcfCommandRequest;
import software.wings.helpers.ext.pcf.request.PcfCommandRollbackRequest;
import software.wings.helpers.ext.pcf.request.PcfCommandSetupRequest;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.WorkflowStandardParams;
import software.wings.sm.states.pcf.PcfRollbackState;

import java.util.Arrays;

/**
 * Created by rishi on 2/27/17.
 */
public class PcfRollbackStateTest extends WingsBaseTest {
  @Mock private VariableProcessor variableProcessor;
  @Mock private ExpressionEvaluator evaluator;
  @Mock private SecretManager secretManager;
  public static final String ORG = "ORG";
  public static final String SPACE = "SPACE";

  @InjectMocks private PcfRollbackState pcfRollbackState;

  @InjectMocks
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
                                          .withDeploymentType(DeploymentType.PCF.name())
                                          .build();

  PcfDeployContextElement pcfDeployContextElement =
      PcfDeployContextElement.builder()
          .uuid(serviceElement.getUuid())
          .name(PCF_SERVICE_NAME)
          .instanceData(Arrays.asList(
              PcfServiceData.builder().previousCount(1).desiredCount(0).name("APP_SERVICE_ENV__1").build(),
              PcfServiceData.builder().previousCount(1).desiredCount(0).name("APP_SERVICE_ENV__2").build(),
              PcfServiceData.builder().previousCount(0).desiredCount(2).name("APP_SERVICE_ENV__3").build()))
          //.resizeStrategy(RESIZE_NEW_FIRST)
          .build();
  private StateExecutionInstance stateExecutionInstance = aStateExecutionInstance()
                                                              .withDisplayName(STATE_NAME)
                                                              .addContextElement(workflowStandardParams)
                                                              .addContextElement(phaseElement)
                                                              .addContextElement(pcfDeployContextElement)
                                                              .withStateExecutionData(new PhaseStepExecutionData())
                                                              .build();

  private Application app = anApplication().withUuid(APP_ID).withName(APP_NAME).build();
  private Environment env = anEnvironment().withAppId(APP_ID).withUuid(ENV_ID).withName(ENV_NAME).build();
  private Service service = Service.builder().appId(APP_ID).uuid(SERVICE_ID).name(SERVICE_NAME).build();
  private SettingAttribute computeProvider =
      aSettingAttribute()
          .withValue(PcfConfig.builder().accountId(ACCOUNT_ID).endpointUrl(URL).username(USER_NAME).build())
          .build();

  private Artifact artifact = anArtifact()
                                  .withArtifactSourceName("source")
                                  .withMetadata(ImmutableMap.of(BUILD_NO, "bn"))
                                  .withServiceIds(singletonList(SERVICE_ID))
                                  .build();

  private ExecutionContextImpl context;

  /**
   * Set up.
   */
  @Before
  public void setup() {
    setInternalState(pcfRollbackState, "secretManager", secretManager);
    context = new ExecutionContextImpl(stateExecutionInstance);

    InfrastructureMapping infrastructureMapping = PcfInfrastructureMapping.builder()
                                                      .organization(ORG)
                                                      .space(SPACE)
                                                      .routeMaps(Arrays.asList("R1"))
                                                      .tempRouteMap(Arrays.asList("R2"))
                                                      .computeProviderSettingId(COMPUTE_PROVIDER_ID)
                                                      .build();
    when(variableProcessor.getVariables(any(), any())).thenReturn(emptyMap());
    when(evaluator.substitute(any(), any(), any())).thenAnswer(i -> i.getArguments()[0]);
  }

  @Test
  public void testExecute() throws Exception {
    on(context).set("variableProcessor", variableProcessor);
    on(context).set("evaluator", evaluator);

    PcfDeployStateExecutionData stateExecutionData = PcfDeployStateExecutionData.builder().build();

    PcfSetupContextElement pcfSetupContextElement =
        PcfSetupContextElement.builder()
            .newPcfApplicationName("APP_SERVICE_ENV_NAME__1")
            .pcfCommandRequest(PcfCommandSetupRequest.builder().organization(ORG).space(SPACE).build())
            .routeMaps(Arrays.asList("R1", "R2"))
            .build();

    PcfCommandRequest pcfCommandRequest =
        pcfRollbackState.getPcfCommandRequest(context, anApplication().withName(APP_NAME).withUuid(APP_ID).build(),
            ACTIVITY_ID, pcfSetupContextElement, (PcfConfig) computeProvider.getValue(), -1, -1, stateExecutionData);
    PcfCommandRollbackRequest commandRollbackRequest = (PcfCommandRollbackRequest) pcfCommandRequest;

    assertEquals(ACTIVITY_ID, stateExecutionData.getActivityId());
    assertNotNull(commandRollbackRequest);
    for (PcfServiceData pcfServiceData : commandRollbackRequest.getInstanceData()) {
      if (pcfServiceData.getName().equals("APP_SERVICE_ENV__1")) {
        assertTrue(1 == pcfServiceData.getDesiredCount());
        assertTrue(0 == pcfServiceData.getPreviousCount());
      } else if (pcfServiceData.getName().equals("APP_SERVICE_ENV__2")) {
        assertTrue(1 == pcfServiceData.getDesiredCount());
        assertTrue(0 == pcfServiceData.getPreviousCount());
      } else if (pcfServiceData.getName().equals("APP_SERVICE_ENV__3")) {
        assertTrue(0 == pcfServiceData.getDesiredCount());
        assertTrue(2 == pcfServiceData.getPreviousCount());
      }
    }
    assertEquals(2, commandRollbackRequest.getRouteMaps().size());
    assertTrue(commandRollbackRequest.getRouteMaps().contains("R1"));
    assertTrue(commandRollbackRequest.getRouteMaps().contains("R2"));
  }
}
