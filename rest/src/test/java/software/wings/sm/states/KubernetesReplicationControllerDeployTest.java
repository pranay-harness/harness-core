package software.wings.sm.states;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.api.CommandStateExecutionData.Builder.aCommandStateExecutionData;
import static software.wings.api.ContainerServiceElement.ContainerServiceElementBuilder.aContainerServiceElement;
import static software.wings.api.PhaseElement.PhaseElementBuilder.aPhaseElement;
import static software.wings.api.ServiceElement.Builder.aServiceElement;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.GcpKubernetesInfrastructureMapping.Builder.aGcpKubernetesInfrastructureMapping;
import static software.wings.beans.Service.Builder.aService;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.command.Command.Builder.aCommand;
import static software.wings.beans.command.CommandExecutionResult.Builder.aCommandExecutionResult;
import static software.wings.beans.command.ServiceCommand.Builder.aServiceCommand;
import static software.wings.common.UUIDGenerator.getUuid;
import static software.wings.sm.StateExecutionInstance.Builder.aStateExecutionInstance;
import static software.wings.sm.WorkflowStandardParams.Builder.aWorkflowStandardParams;
import static software.wings.sm.states.KubernetesReplicationControllerDeploy.KubernetesReplicationControllerDeployBuilder.aKubernetesReplicationControllerDeploy;
import static software.wings.utils.WingsTestConstants.ACTIVITY_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.APP_NAME;
import static software.wings.utils.WingsTestConstants.ARTIFACT_ID;
import static software.wings.utils.WingsTestConstants.CLUSTER_NAME;
import static software.wings.utils.WingsTestConstants.COMMAND_NAME;
import static software.wings.utils.WingsTestConstants.COMPUTE_PROVIDER_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.ENV_NAME;
import static software.wings.utils.WingsTestConstants.INFRA_MAPPING_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_NAME;
import static software.wings.utils.WingsTestConstants.STATE_NAME;
import static software.wings.utils.WingsTestConstants.TEMPLATE_ID;

import com.google.common.collect.Lists;

import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.ReplicationControllerBuilder;
import io.fabric8.kubernetes.api.model.ReplicationControllerListBuilder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mongodb.morphia.Key;
import software.wings.WingsBaseTest;
import software.wings.api.CommandStateExecutionData;
import software.wings.api.DeploymentType;
import software.wings.api.PhaseElement;
import software.wings.api.PhaseStepExecutionData;
import software.wings.api.ServiceElement;
import software.wings.beans.Activity;
import software.wings.beans.Application;
import software.wings.beans.DelegateTask;
import software.wings.beans.Environment;
import software.wings.beans.ErrorCode;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.Service;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.SettingAttribute;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.command.CommandType;
import software.wings.beans.command.ServiceCommand;
import software.wings.cloudprovider.gke.GkeClusterService;
import software.wings.cloudprovider.gke.KubernetesContainerService;
import software.wings.common.VariableProcessor;
import software.wings.exception.WingsException;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.SettingsService;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.WorkflowStandardParams;
import software.wings.waitnotify.NotifyResponseData;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by brett on 3/10/17
 */
public class KubernetesReplicationControllerDeployTest extends WingsBaseTest {
  private static final String KUBERNETES_REPLICATION_CONTROLLER_NAME = "kubernetes-rc-name.1";
  private static final String KUBERNETES_REPLICATION_CONTROLLER_OLD_NAME = "kubernetes-rc-name.0";

  @Mock private SettingsService settingsService;
  @Mock private DelegateService delegateService;
  @Mock private ServiceResourceService serviceResourceService;
  @Mock private ActivityService activityService;
  @Mock private InfrastructureMappingService infrastructureMappingService;
  @Mock private GkeClusterService gkeClusterService;
  @Mock private KubernetesContainerService kubernetesContainerService;
  @Mock private AppService appService;
  @Mock private EnvironmentService environmentService;
  @Mock private VariableProcessor variableProcessor;
  @Mock private KubernetesConfig kubernetesConfig;
  @Mock private ServiceTemplateService serviceTemplateService;

  private WorkflowStandardParams workflowStandardParams = aWorkflowStandardParams()
                                                              .withAppId(APP_ID)
                                                              .withEnvId(ENV_ID)
                                                              .withArtifactIds(Lists.newArrayList(ARTIFACT_ID))
                                                              .build();
  private ServiceElement serviceElement = aServiceElement().withUuid(SERVICE_ID).withName(SERVICE_NAME).build();
  private PhaseElement phaseElement = aPhaseElement()
                                          .withUuid(getUuid())
                                          .withServiceElement(serviceElement)
                                          .withInfraMappingId(INFRA_MAPPING_ID)
                                          .withDeploymentType(DeploymentType.KUBERNETES.name())
                                          .build();
  private StateExecutionInstance stateExecutionInstance =
      aStateExecutionInstance()
          .withStateName(STATE_NAME)
          .addContextElement(workflowStandardParams)
          .addContextElement(phaseElement)
          .addContextElement(aContainerServiceElement()
                                 .withUuid(serviceElement.getUuid())
                                 .withClusterName(CLUSTER_NAME)
                                 .withName(KUBERNETES_REPLICATION_CONTROLLER_NAME)
                                 .withOldName(KUBERNETES_REPLICATION_CONTROLLER_OLD_NAME)
                                 .withInfraMappingId(INFRA_MAPPING_ID)
                                 .withDeploymentType(DeploymentType.KUBERNETES)
                                 .build())
          .addStateExecutionData(new PhaseStepExecutionData())
          .build();

  private Application app = anApplication().withUuid(APP_ID).withName(APP_NAME).build();
  private Environment env = anEnvironment().withAppId(APP_ID).withUuid(ENV_ID).withName(ENV_NAME).build();
  private Service service = aService().withAppId(APP_ID).withUuid(SERVICE_ID).withName(SERVICE_NAME).build();
  private SettingAttribute computeProvider = aSettingAttribute().build();

  private KubernetesReplicationControllerDeploy kubernetesReplicationControllerDeploy =
      aKubernetesReplicationControllerDeploy(STATE_NAME).withCommandName(COMMAND_NAME).withInstanceCount(1).build();

  /**
   * Set up.
   */
  @Before
  public void setup() {
    when(appService.get(APP_ID)).thenReturn(app);
    when(serviceResourceService.get(APP_ID, SERVICE_ID)).thenReturn(service);
    when(environmentService.get(APP_ID, ENV_ID, false)).thenReturn(env);

    ServiceCommand serviceCommand =
        aServiceCommand()
            .withCommand(aCommand().withCommandType(CommandType.RESIZE).withName(COMMAND_NAME).build())
            .build();
    when(serviceResourceService.getCommandByName(APP_ID, SERVICE_ID, ENV_ID, COMMAND_NAME)).thenReturn(serviceCommand);
    on(workflowStandardParams).set("appService", appService);
    on(workflowStandardParams).set("environmentService", environmentService);

    on(kubernetesReplicationControllerDeploy).set("settingsService", settingsService);
    on(kubernetesReplicationControllerDeploy).set("delegateService", delegateService);
    on(kubernetesReplicationControllerDeploy).set("serviceResourceService", serviceResourceService);
    on(kubernetesReplicationControllerDeploy).set("activityService", activityService);
    on(kubernetesReplicationControllerDeploy).set("infrastructureMappingService", infrastructureMappingService);
    on(kubernetesReplicationControllerDeploy).set("gkeClusterService", gkeClusterService);
    on(kubernetesReplicationControllerDeploy).set("kubernetesContainerService", kubernetesContainerService);
    on(kubernetesReplicationControllerDeploy).set("serviceTemplateService", serviceTemplateService);

    InfrastructureMapping infrastructureMapping = aGcpKubernetesInfrastructureMapping()
                                                      .withClusterName(CLUSTER_NAME)
                                                      .withComputeProviderSettingId(COMPUTE_PROVIDER_ID)
                                                      .build();
    when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID)).thenReturn(infrastructureMapping);

    Activity activity = Activity.Builder.anActivity().withUuid(ACTIVITY_ID).build();
    when(activityService.save(any(Activity.class))).thenReturn(activity);

    when(settingsService.get(COMPUTE_PROVIDER_ID)).thenReturn(computeProvider);

    when(serviceTemplateService.getTemplateRefKeysByService(APP_ID, SERVICE_ID, ENV_ID))
        .thenReturn(Arrays.asList(new Key<>(ServiceTemplate.class, "serviceTemplate", TEMPLATE_ID)));
  }

  @Test
  public void shouldExecute() {
    ExecutionContextImpl context = new ExecutionContextImpl(stateExecutionInstance);
    on(context).set("variableProcessor", variableProcessor);

    ReplicationController replicationController = new ReplicationControllerBuilder()
                                                      .withApiVersion("v1")
                                                      .withNewMetadata()
                                                      .withName(KUBERNETES_REPLICATION_CONTROLLER_NAME)
                                                      .endMetadata()
                                                      .withNewSpec()
                                                      .withReplicas(2)
                                                      .endSpec()
                                                      .build();
    when(gkeClusterService.getCluster(computeProvider, CLUSTER_NAME)).thenReturn(kubernetesConfig);
    when(kubernetesContainerService.listControllers(kubernetesConfig))
        .thenReturn(new ReplicationControllerListBuilder().addToItems(replicationController).build());
    when(kubernetesContainerService.getController(eq(kubernetesConfig), anyString())).thenReturn(replicationController);

    ExecutionResponse response = kubernetesReplicationControllerDeploy.execute(context);
    assertThat(response).isNotNull().hasFieldOrPropertyWithValue("async", true);
    assertThat(response).isNotNull().hasFieldOrPropertyWithValue("async", true);
    assertThat(response.getCorrelationIds()).isNotNull().hasSize(1).contains(ACTIVITY_ID);
    verify(activityService).save(any(Activity.class));
    verify(delegateService).queueTask(any(DelegateTask.class));
  }

  @Test
  public void shouldExecuteThrowInvalidRequest() {
    try {
      ExecutionContextImpl context = new ExecutionContextImpl(stateExecutionInstance);
      on(context).set("variableProcessor", variableProcessor);
      kubernetesReplicationControllerDeploy.execute(context);
      failBecauseExceptionWasNotThrown(WingsException.class);
    } catch (WingsException exception) {
      assertThat(exception).hasMessage(ErrorCode.INVALID_REQUEST.getCode());
      assertThat(exception.getParams()).hasSize(1).containsKey("message");
      assertThat(exception.getParams().get("message"))
          .asString()
          .contains("Kubernetes replication controller setup not done, controllerName");
    }
  }

  @Test
  public void shouldExecuteAsync() {
    Map<String, NotifyResponseData> notifyResponse = new HashMap<>();
    notifyResponse.put("key", aCommandExecutionResult().withStatus(CommandExecutionStatus.SUCCESS).build());

    stateExecutionInstance.getStateExecutionMap().put(stateExecutionInstance.getStateName(),
        aCommandStateExecutionData().withOldContainerServiceName("oldService").build());
    ExecutionContextImpl context = new ExecutionContextImpl(stateExecutionInstance);

    ExecutionResponse response = kubernetesReplicationControllerDeploy.handleAsyncResponse(context, notifyResponse);
    assertThat(response)
        .isNotNull()
        .hasFieldOrPropertyWithValue("async", false)
        .hasFieldOrPropertyWithValue("executionStatus", ExecutionStatus.SUCCESS);
  }

  @Test
  public void shouldExecuteAsyncWithOldReplicationController() {
    Map<String, NotifyResponseData> notifyResponse = new HashMap<>();
    notifyResponse.put("key", aCommandExecutionResult().withStatus(CommandExecutionStatus.SUCCESS).build());

    ReplicationController replicationController = new ReplicationControllerBuilder()
                                                      .withApiVersion("v1")
                                                      .withNewMetadata()
                                                      .withName(KUBERNETES_REPLICATION_CONTROLLER_OLD_NAME)
                                                      .endMetadata()
                                                      .withNewSpec()
                                                      .withReplicas(2)
                                                      .endSpec()
                                                      .build();
    when(gkeClusterService.getCluster(computeProvider, CLUSTER_NAME)).thenReturn(kubernetesConfig);
    when(kubernetesContainerService.listControllers(kubernetesConfig))
        .thenReturn(new ReplicationControllerListBuilder().addToItems(replicationController).build());
    when(kubernetesContainerService.getController(eq(kubernetesConfig), anyString())).thenReturn(replicationController);

    CommandStateExecutionData commandStateExecutionData =
        aCommandStateExecutionData().withActivityId(ACTIVITY_ID).build();
    stateExecutionInstance.getStateExecutionMap().put(stateExecutionInstance.getStateName(), commandStateExecutionData);
    ExecutionContextImpl context = new ExecutionContextImpl(stateExecutionInstance);

    ExecutionResponse response = kubernetesReplicationControllerDeploy.handleAsyncResponse(context, notifyResponse);
    assertThat(response).isNotNull().hasFieldOrPropertyWithValue("async", true);
    assertThat(response.getCorrelationIds()).isNotNull().hasSize(1).contains(ACTIVITY_ID);
    assertThat(response.getStateExecutionData()).isNotNull().isEqualTo(commandStateExecutionData);
    verify(delegateService).queueTask(any(DelegateTask.class));
  }

  @Test
  public void shouldExecuteAsyncWithOldReplicationControllerWithNoInstance() {
    Map<String, NotifyResponseData> notifyResponse = new HashMap<>();
    notifyResponse.put("key", aCommandExecutionResult().withStatus(CommandExecutionStatus.SUCCESS).build());
    stateExecutionInstance.getStateExecutionMap().put(
        stateExecutionInstance.getStateName(), aCommandStateExecutionData().build());
    ExecutionContextImpl context = new ExecutionContextImpl(stateExecutionInstance);

    ExecutionResponse response = kubernetesReplicationControllerDeploy.handleAsyncResponse(context, notifyResponse);
    assertThat(response)
        .isNotNull()
        .hasFieldOrPropertyWithValue("async", false)
        .hasFieldOrPropertyWithValue("executionStatus", ExecutionStatus.SUCCESS);
  }
}
