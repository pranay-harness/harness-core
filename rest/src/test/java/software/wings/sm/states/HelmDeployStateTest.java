package software.wings.sm.states;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anySet;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.api.PhaseElement.PhaseElementBuilder.aPhaseElement;
import static software.wings.api.ServiceElement.Builder.aServiceElement;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.GcpKubernetesInfrastructureMapping.Builder.aGcpKubernetesInfrastructureMapping;
import static software.wings.beans.ResizeStrategy.RESIZE_NEW_FIRST;
import static software.wings.beans.ServiceTemplate.Builder.aServiceTemplate;
import static software.wings.beans.WorkflowExecution.WorkflowExecutionBuilder.aWorkflowExecution;
import static software.wings.sm.StateExecutionInstance.Builder.aStateExecutionInstance;
import static software.wings.sm.WorkflowStandardParams.Builder.aWorkflowStandardParams;
import static software.wings.utils.WingsTestConstants.ACTIVITY_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.APP_NAME;
import static software.wings.utils.WingsTestConstants.ARTIFACT_ID;
import static software.wings.utils.WingsTestConstants.CLUSTER_NAME;
import static software.wings.utils.WingsTestConstants.COMPUTE_PROVIDER_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.ENV_NAME;
import static software.wings.utils.WingsTestConstants.INFRA_MAPPING_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_NAME;
import static software.wings.utils.WingsTestConstants.STATE_NAME;
import static software.wings.utils.WingsTestConstants.TEMPLATE_ID;

import com.google.common.collect.Lists;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mongodb.morphia.Key;
import software.wings.WingsBaseTest;
import software.wings.api.ContainerServiceElement;
import software.wings.api.DeploymentType;
import software.wings.api.HelmDeployStateExecutionData;
import software.wings.api.PhaseElement;
import software.wings.api.PhaseStepExecutionData;
import software.wings.api.ServiceElement;
import software.wings.app.MainConfiguration;
import software.wings.app.PortalConfig;
import software.wings.beans.Activity;
import software.wings.beans.Application;
import software.wings.beans.DelegateTask;
import software.wings.beans.Environment;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.Service;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.container.HelmChartSpecification;
import software.wings.beans.container.ImageDetails;
import software.wings.common.VariableProcessor;
import software.wings.delegatetasks.RemoteMethodReturnValueData;
import software.wings.exception.InvalidRequestException;
import software.wings.expression.ExpressionEvaluator;
import software.wings.helpers.ext.container.ContainerDeploymentManagerHelper;
import software.wings.helpers.ext.helm.HelmCommandExecutionResponse;
import software.wings.helpers.ext.helm.request.HelmCommandRequest.HelmCommandType;
import software.wings.helpers.ext.helm.request.HelmInstallCommandRequest;
import software.wings.helpers.ext.helm.response.HelmReleaseHistoryCommandResponse;
import software.wings.service.impl.ContainerServiceParams;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.WorkflowStandardParams;

import java.util.Collections;

public class HelmDeployStateTest extends WingsBaseTest {
  private static final String HELM_CONTROLLER_NAME = "helm-controller-name";
  private static final String HELM_RELEASE_NAME_PREFIX = "helm-release-name-prefix";
  private static final String CHART_NAME = "chart-name";
  private static final String CHART_VERSION = "0.1.0";
  private static final String CHART_URL = "http://google.com";

  @Mock private AppService appService;
  @Mock private ArtifactService artifactService;
  @Mock private ActivityService activityService;
  @Mock private DelegateService delegateService;
  @Mock private EnvironmentService environmentService;
  @Mock private ExpressionEvaluator evaluator;
  @Mock private InfrastructureMappingService infrastructureMappingService;
  @Mock private MainConfiguration configuration;
  @Mock private PortalConfig portalConfig;
  @Mock private ServiceResourceService serviceResourceService;
  @Mock private ServiceTemplateService serviceTemplateService;
  @Mock private VariableProcessor variableProcessor;
  @Mock private WorkflowExecutionService workflowExecutionService;
  @Mock private SecretManager secretManager;
  @Mock private ContainerDeploymentManagerHelper containerDeploymentHelper;

  @InjectMocks HelmDeployState helmDeployState = new HelmDeployState("helmDeployState");

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
                                          .withDeploymentType(DeploymentType.HELM.name())
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
                                 .name(HELM_CONTROLLER_NAME)
                                 .resizeStrategy(RESIZE_NEW_FIRST)
                                 .infraMappingId(INFRA_MAPPING_ID)
                                 .deploymentType(DeploymentType.KUBERNETES)
                                 .build())
          .addStateExecutionData(new PhaseStepExecutionData())
          .build();

  private InfrastructureMapping infrastructureMapping = aGcpKubernetesInfrastructureMapping()
                                                            .withUuid(INFRA_MAPPING_ID)
                                                            .withClusterName(CLUSTER_NAME)
                                                            .withComputeProviderSettingId(COMPUTE_PROVIDER_ID)
                                                            .withDeploymentType(DeploymentType.KUBERNETES.name())
                                                            .build();

  private Application app = anApplication().withUuid(APP_ID).withName(APP_NAME).build();
  private Environment env = anEnvironment().withAppId(APP_ID).withUuid(ENV_ID).withName(ENV_NAME).build();
  private Service service = Service.builder().appId(APP_ID).uuid(SERVICE_ID).name(SERVICE_NAME).build();
  private ExecutionContextImpl context;

  @Before
  public void setup() throws InterruptedException {
    context = new ExecutionContextImpl(stateExecutionInstance);
    helmDeployState.setHelmReleaseNamePrefix(HELM_RELEASE_NAME_PREFIX);

    when(appService.get(APP_ID)).thenReturn(app);
    when(appService.getApplicationWithDefaults(APP_ID)).thenReturn(app);
    when(serviceResourceService.get(APP_ID, SERVICE_ID)).thenReturn(service);
    when(environmentService.get(APP_ID, ENV_ID, false)).thenReturn(env);
    when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID)).thenReturn(infrastructureMapping);

    when(activityService.save(any(Activity.class))).thenReturn(Activity.builder().uuid(ACTIVITY_ID).build());
    when(secretManager.getEncryptionDetails(anyObject(), anyString(), anyString())).thenReturn(Collections.emptyList());
    when(configuration.getPortal()).thenReturn(portalConfig);
    when(portalConfig.getUrl()).thenReturn("http://www.url.com");
    when(serviceTemplateService.getTemplateRefKeysByService(APP_ID, SERVICE_ID, ENV_ID))
        .thenReturn(singletonList(new Key<>(ServiceTemplate.class, "serviceTemplate", TEMPLATE_ID)));

    when(serviceTemplateService.get(APP_ID, TEMPLATE_ID)).thenReturn(aServiceTemplate().withUuid(TEMPLATE_ID).build());
    when(serviceTemplateService.computeServiceVariables(APP_ID, ENV_ID, TEMPLATE_ID, null, false))
        .thenReturn(emptyList());
    when(workflowExecutionService.getExecutionDetails(anyString(), anyString(), anyBoolean(), anySet()))
        .thenReturn(aWorkflowExecution().build());
    when(containerDeploymentHelper.getContainerServiceParams(any(), any()))
        .thenReturn(ContainerServiceParams.builder().build());
    when(containerDeploymentHelper.fetchArtifactDetails(any(), anyString(), any()))
        .thenReturn(ImageDetails.builder().build());

    when(delegateService.executeTask(any()))
        .thenReturn(HelmCommandExecutionResponse.builder()
                        .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
                        .helmCommandResponse(HelmReleaseHistoryCommandResponse.builder().build())
                        .build());
    when(variableProcessor.getVariables(any(), any())).thenReturn(emptyMap());
    when(evaluator.substitute(any(), any(), any())).thenAnswer(i -> i.getArguments()[0]);

    on(workflowStandardParams).set("appService", appService);
    on(workflowStandardParams).set("environmentService", environmentService);
    on(context).set("artifactService", artifactService);
    on(context).set("variableProcessor", variableProcessor);
    on(context).set("evaluator", evaluator);
  }

  @Test
  public void testExecute() throws InterruptedException {
    when(serviceResourceService.getHelmChartSpecification(APP_ID, SERVICE_ID))
        .thenReturn(HelmChartSpecification.builder()
                        .chartName(CHART_NAME)
                        .chartUrl(CHART_URL)
                        .chartVersion(CHART_VERSION)
                        .build());

    ExecutionResponse executionResponse = helmDeployState.execute(context);
    assertThat(executionResponse.isAsync()).isEqualTo(true);
    assertThat(executionResponse.getCorrelationIds()).contains(ACTIVITY_ID);
    HelmDeployStateExecutionData helmDeployStateExecutionData =
        (HelmDeployStateExecutionData) executionResponse.getStateExecutionData();
    assertThat(helmDeployStateExecutionData.getActivityId()).isEqualTo(ACTIVITY_ID);
    assertThat(helmDeployStateExecutionData.getChartName()).isEqualTo(CHART_NAME);
    assertThat(helmDeployStateExecutionData.getChartRepositoryUrl()).isEqualTo(CHART_URL);
    assertThat(helmDeployStateExecutionData.getReleaseName()).isEqualTo(HELM_RELEASE_NAME_PREFIX + "-harness-infrazm");
    assertThat(helmDeployStateExecutionData.getReleaseOldVersion()).isEqualTo(0);
    assertThat(helmDeployStateExecutionData.getReleaseNewVersion()).isEqualTo(1);

    ArgumentCaptor<DelegateTask> captor = ArgumentCaptor.forClass(DelegateTask.class);
    verify(delegateService).queueTask(captor.capture());
    DelegateTask delegateTask = captor.getValue();

    HelmInstallCommandRequest helmInstallCommandRequest = (HelmInstallCommandRequest) delegateTask.getParameters()[0];
    assertThat(helmInstallCommandRequest.getHelmCommandType()).isEqualTo(HelmCommandType.INSTALL);
    assertThat(helmInstallCommandRequest.getReleaseName()).isEqualTo(HELM_RELEASE_NAME_PREFIX + "-harness-infrazm");
    assertThat(helmInstallCommandRequest.getRepoName()).isEqualTo("app-name-service-name");

    verify(delegateService).executeTask(any());
  }

  @Test(expected = InvalidRequestException.class)
  public void testExecuteWithNullChartSpec() {
    when(serviceResourceService.getHelmChartSpecification(APP_ID, SERVICE_ID)).thenReturn(null);

    helmDeployState.execute(context);
  }

  @Test(expected = InvalidRequestException.class)
  public void testExecuteWithNullReleaseName() {
    when(serviceResourceService.getHelmChartSpecification(APP_ID, SERVICE_ID)).thenReturn(null);
    helmDeployState.setHelmReleaseNamePrefix(null);

    helmDeployState.execute(context);
  }

  @Test(expected = InvalidRequestException.class)
  public void testErrorResponseFromDelegate() throws InterruptedException {
    when(serviceResourceService.getHelmChartSpecification(APP_ID, SERVICE_ID))
        .thenReturn(HelmChartSpecification.builder()
                        .chartName(CHART_NAME)
                        .chartUrl(CHART_URL)
                        .chartVersion(CHART_VERSION)
                        .build());

    when(delegateService.executeTask(any()))
        .thenReturn(RemoteMethodReturnValueData.Builder.aRemoteMethodReturnValueData().build());

    helmDeployState.execute(context);
  }
}
