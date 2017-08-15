package software.wings.sm.states;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.api.ContainerServiceElement.ContainerServiceElementBuilder.aContainerServiceElement;
import static software.wings.api.PhaseElement.PhaseElementBuilder.aPhaseElement;
import static software.wings.api.ServiceElement.Builder.aServiceElement;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.DockerConfig.Builder.aDockerConfig;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.GcpKubernetesInfrastructureMapping.Builder.aGcpKubernetesInfrastructureMapping;
import static software.wings.beans.Service.Builder.aService;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.artifact.Artifact.Builder.anArtifact;
import static software.wings.beans.artifact.DockerArtifactStream.Builder.aDockerArtifactStream;
import static software.wings.common.UUIDGenerator.getUuid;
import static software.wings.sm.StateExecutionInstance.Builder.aStateExecutionInstance;
import static software.wings.sm.WorkflowStandardParams.Builder.aWorkflowStandardParams;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.APP_NAME;
import static software.wings.utils.WingsTestConstants.ARTIFACT_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_STREAM_ID;
import static software.wings.utils.WingsTestConstants.CLUSTER_NAME;
import static software.wings.utils.WingsTestConstants.COMPUTE_PROVIDER_ID;
import static software.wings.utils.WingsTestConstants.DOCKER_IMAGE;
import static software.wings.utils.WingsTestConstants.DOCKER_SOURCE;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.ENV_NAME;
import static software.wings.utils.WingsTestConstants.INFRA_MAPPING_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_NAME;
import static software.wings.utils.WingsTestConstants.SETTING_ID;
import static software.wings.utils.WingsTestConstants.STATE_NAME;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.ReplicationControllerBuilder;
import io.fabric8.kubernetes.api.model.ReplicationControllerListBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.api.PhaseElement;
import software.wings.api.PhaseStepExecutionData;
import software.wings.api.ServiceElement;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.ErrorCode;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.cloudprovider.gke.GkeClusterService;
import software.wings.cloudprovider.gke.KubernetesContainerService;
import software.wings.exception.WingsException;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.WorkflowStandardParams;
import software.wings.utils.KubernetesConvention;

import java.util.Date;

/**
 * Created by brett on 3/10/17
 */
public class KubernetesReplicationControllerSetupTest extends WingsBaseTest {
  @Mock private GkeClusterService gkeClusterService;
  @Mock private KubernetesContainerService kubernetesContainerService;
  @Mock private SettingsService settingsService;
  @Mock private ServiceResourceService serviceResourceService;
  @Mock private InfrastructureMappingService infrastructureMappingService;
  @Mock private ArtifactStreamService artifactStreamService;
  @Mock private ArtifactService artifactService;
  @Mock private AppService appService;
  @Mock private EnvironmentService environmentService;
  @Mock private KubernetesConfig kubernetesConfig;

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
                                          .build();
  private StateExecutionInstance stateExecutionInstance =
      aStateExecutionInstance()
          .withStateName(STATE_NAME)
          .addContextElement(workflowStandardParams)
          .addContextElement(phaseElement)
          .addContextElement(aContainerServiceElement().withUuid(serviceElement.getUuid()).build())
          .addStateExecutionData(new PhaseStepExecutionData())
          .build();
  private ExecutionContextImpl context = new ExecutionContextImpl(stateExecutionInstance);

  private KubernetesReplicationControllerSetup kubernetesReplicationControllerSetup =
      new KubernetesReplicationControllerSetup(STATE_NAME);

  private Artifact artifact = anArtifact()
                                  .withAppId(APP_ID)
                                  .withServiceIds(Lists.newArrayList(SERVICE_ID))
                                  .withUuid(ARTIFACT_ID)
                                  .withArtifactStreamId(ARTIFACT_STREAM_ID)
                                  .build();
  private ArtifactStream artifactStream = aDockerArtifactStream()
                                              .withImageName(DOCKER_IMAGE)
                                              .withSourceName(DOCKER_SOURCE)
                                              .withSettingId(SETTING_ID)
                                              .build();
  private Application app = anApplication().withUuid(APP_ID).withName(APP_NAME).build();
  private Environment env = anEnvironment().withAppId(APP_ID).withUuid(ENV_ID).withName(ENV_NAME).build();
  private Service service = aService().withAppId(APP_ID).withUuid(SERVICE_ID).withName(SERVICE_NAME).build();
  private SettingAttribute computeProvider = aSettingAttribute().build();
  private SettingAttribute dockerConfigSettingAttribute = aSettingAttribute()
                                                              .withValue(aDockerConfig()
                                                                             .withDockerRegistryUrl("url")
                                                                             .withUsername("name")
                                                                             .withPassword("pass".toCharArray())
                                                                             .withAccountId(ACCOUNT_ID)
                                                                             .build())
                                                              .build();
  private ReplicationController replicationController;
  private Secret secret;
  private io.fabric8.kubernetes.api.model.Service kubernetesService;

  /**
   * Set up.
   */
  @Before
  public void setup() {
    kubernetesReplicationControllerSetup.setServiceType("ClusterIP");
    kubernetesReplicationControllerSetup.setPort("80");
    kubernetesReplicationControllerSetup.setTargetPort("8080");
    kubernetesReplicationControllerSetup.setProtocol("TCP");

    when(appService.get(APP_ID)).thenReturn(app);
    when(environmentService.get(APP_ID, ENV_ID, false)).thenReturn(env);
    when(artifactService.get(APP_ID, ARTIFACT_ID)).thenReturn(artifact);
    on(workflowStandardParams).set("artifactService", artifactService);
    on(workflowStandardParams).set("appService", appService);
    on(workflowStandardParams).set("environmentService", environmentService);

    when(serviceResourceService.get(APP_ID, SERVICE_ID)).thenReturn(service);
    when(artifactStreamService.get(APP_ID, ARTIFACT_STREAM_ID)).thenReturn(artifactStream);
    on(kubernetesReplicationControllerSetup).set("gkeClusterService", gkeClusterService);
    on(kubernetesReplicationControllerSetup).set("kubernetesContainerService", kubernetesContainerService);
    on(kubernetesReplicationControllerSetup).set("settingsService", settingsService);
    on(kubernetesReplicationControllerSetup).set("serviceResourceService", serviceResourceService);
    on(kubernetesReplicationControllerSetup).set("infrastructureMappingService", infrastructureMappingService);
    on(kubernetesReplicationControllerSetup).set("artifactStreamService", artifactStreamService);

    when(settingsService.get(APP_ID, COMPUTE_PROVIDER_ID)).thenReturn(computeProvider);
    when(settingsService.get(SETTING_ID)).thenReturn(dockerConfigSettingAttribute);

    replicationController =
        new ReplicationControllerBuilder()
            .withApiVersion("v1")
            .withNewMetadata()
            .withName("backend-ctrl")
            .addToLabels("app", "testApp")
            .addToLabels("tier", "backend")
            .endMetadata()
            .withNewSpec()
            .withReplicas(2)
            .withNewTemplate()
            .withNewMetadata()
            .addToLabels("app", "testApp")
            .addToLabels("tier", "backend")
            .endMetadata()
            .withNewSpec()
            .addNewContainer()
            .withName("server")
            .withImage("gcr.io/exploration-161417/todolist")
            .withArgs("8080")
            .withNewResources()
            .withLimits(ImmutableMap.of("cpu", new Quantity("100m"), "memory", new Quantity("100Mi")))
            .endResources()
            .addNewPort()
            .withContainerPort(8080)
            .endPort()
            .endContainer()
            .endSpec()
            .endTemplate()
            .endSpec()
            .build();

    secret = new SecretBuilder()
                 .withApiVersion("v1")
                 .withKind("Secret")
                 .withData(ImmutableMap.of(".dockercfg", "aaa"))
                 .withNewMetadata()
                 .withName("secret-name")
                 .endMetadata()
                 .build();

    kubernetesService = new ServiceBuilder()
                            .withApiVersion("v1")
                            .withNewMetadata()
                            .withName("backend-service")
                            .addToLabels("app", "testApp")
                            .addToLabels("tier", "backend")
                            .endMetadata()
                            .withNewSpec()
                            .addNewPort()
                            .withPort(80)
                            .withNewTargetPort()
                            .withIntVal(8080)
                            .endTargetPort()
                            .endPort()
                            .addToSelector("app", "testApp")
                            .addToSelector("tier", "backend")
                            .withClusterIP("1.2.3.4")
                            .endSpec()
                            .withNewStatus()
                            .withNewLoadBalancer()
                            .addNewIngress()
                            .withIp("5.6.7.8")
                            .endIngress()
                            .endLoadBalancer()
                            .endStatus()
                            .build();

    when(gkeClusterService.getCluster(any(SettingAttribute.class), anyString())).thenReturn(kubernetesConfig);
    when(kubernetesContainerService.createController(eq(kubernetesConfig), any(ReplicationController.class)))
        .thenReturn(replicationController);
    when(kubernetesContainerService.listControllers(kubernetesConfig)).thenReturn(null);
    when(kubernetesContainerService.createOrReplaceService(
             eq(kubernetesConfig), any(io.fabric8.kubernetes.api.model.Service.class)))
        .thenReturn(kubernetesService);
    when(kubernetesContainerService.createOrReplaceSecret(eq(kubernetesConfig), any(Secret.class))).thenReturn(secret);
  }

  @Test
  public void shouldThrowInvalidRequest() {
    try {
      kubernetesReplicationControllerSetup.execute(context);
      failBecauseExceptionWasNotThrown(WingsException.class);
    } catch (WingsException exception) {
      assertThat(exception).hasMessage(ErrorCode.INVALID_REQUEST.getCode());
      assertThat(exception.getParams()).hasSize(1).containsKey("message");
      assertThat(exception.getParams().get("message")).asString().contains("Invalid infrastructure type");
    }
  }

  @Test
  public void shouldExecuteWithLastService() {
    InfrastructureMapping infrastructureMapping = aGcpKubernetesInfrastructureMapping()
                                                      .withClusterName(CLUSTER_NAME)
                                                      .withComputeProviderSettingId(COMPUTE_PROVIDER_ID)
                                                      .build();
    when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID)).thenReturn(infrastructureMapping);
    on(kubernetesReplicationControllerSetup).set("infrastructureMappingService", infrastructureMappingService);

    ReplicationController kubernetesReplicationController =
        new ReplicationControllerBuilder()
            .withNewMetadata()
            .withName(KubernetesConvention.getReplicationControllerName(
                KubernetesConvention.getReplicationControllerNamePrefix("app", "service", "env"), 1))
            .withCreationTimestamp(new Date().toString())
            .endMetadata()
            .build();

    when(kubernetesContainerService.listControllers(kubernetesConfig))
        .thenReturn(new ReplicationControllerListBuilder()
                        .withItems(Lists.newArrayList(kubernetesReplicationController))
                        .build());

    ExecutionResponse response = kubernetesReplicationControllerSetup.execute(context);
    assertThat(response).isNotNull();
    verify(gkeClusterService).getCluster(any(SettingAttribute.class), anyString());
    verify(kubernetesContainerService).createController(eq(kubernetesConfig), any(ReplicationController.class));
  }
}
