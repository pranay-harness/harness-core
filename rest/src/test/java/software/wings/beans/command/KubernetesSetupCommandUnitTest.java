package software.wings.beans.command;

import static com.google.common.truth.Truth.assertThat;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.command.CommandExecutionContext.Builder.aCommandExecutionContext;
import static software.wings.beans.command.KubernetesSetupParams.KubernetesSetupParamsBuilder.aKubernetesSetupParams;
import static software.wings.utils.WingsTestConstants.APP_NAME;
import static software.wings.utils.WingsTestConstants.ENV_NAME;
import static software.wings.utils.WingsTestConstants.INFRA_MAPPING_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_NAME;

import com.google.common.collect.ImmutableMap;

import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.ReplicationControllerBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.GcpConfig;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.container.ImageDetails;
import software.wings.beans.container.KubernetesPortProtocol;
import software.wings.beans.container.KubernetesServiceType;
import software.wings.cloudprovider.gke.GkeClusterService;
import software.wings.cloudprovider.gke.KubernetesContainerService;
import software.wings.utils.KubernetesConvention;

import java.util.Date;
import java.util.List;

public class KubernetesSetupCommandUnitTest extends WingsBaseTest {
  @Mock private GkeClusterService gkeClusterService;
  @Mock private KubernetesContainerService kubernetesContainerService;

  @InjectMocks private KubernetesSetupCommandUnit kubernetesSetupCommandUnit = new KubernetesSetupCommandUnit();

  private KubernetesConfig kubernetesConfig = KubernetesConfig.builder()
                                                  .masterUrl("https://1.1.1.1/")
                                                  .username("admin")
                                                  .password("password".toCharArray())
                                                  .namespace("default")
                                                  .build();
  private KubernetesSetupParams setupParams = aKubernetesSetupParams()
                                                  .withAppName(APP_NAME)
                                                  .withEnvName(ENV_NAME)
                                                  .withServiceName(SERVICE_NAME)
                                                  .withImageDetails(ImageDetails.builder()
                                                                        .registryUrl("gcr.io")
                                                                        .sourceName("GCR")
                                                                        .name("exploration-161417/todolist")
                                                                        .tag("v1")
                                                                        .build())
                                                  .withInfraMappingId(INFRA_MAPPING_ID)
                                                  .withPort(80)
                                                  .withProtocol(KubernetesPortProtocol.TCP)
                                                  .withServiceType(KubernetesServiceType.ClusterIP)
                                                  .withTargetPort(8080)
                                                  .withRcNamePrefix(APP_NAME + "." + ENV_NAME + "." + SERVICE_NAME)
                                                  .withClusterName("cluster")
                                                  .build();
  private SettingAttribute computeProvider = aSettingAttribute().withValue(GcpConfig.builder().build()).build();
  private CommandExecutionContext context = aCommandExecutionContext()
                                                .withCloudProviderSetting(computeProvider)
                                                .withContainerSetupParams(setupParams)
                                                .withCloudProviderCredentials(emptyList())
                                                .build();

  /**
   * Set up.
   */
  @Before
  public void setup() {
    ReplicationController replicationController =
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

    Secret secret = new SecretBuilder()
                        .withApiVersion("v1")
                        .withKind("Secret")
                        .withData(ImmutableMap.of(".dockercfg", "aaa"))
                        .withNewMetadata()
                        .withName("secret-name")
                        .endMetadata()
                        .build();

    io.fabric8.kubernetes.api.model.Service kubernetesService = new ServiceBuilder()
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

    when(gkeClusterService.getCluster(any(SettingAttribute.class), eq(emptyList()), anyString(), anyString()))
        .thenReturn(kubernetesConfig);
    when(kubernetesContainerService.createController(
             eq(kubernetesConfig), eq(emptyList()), any(ReplicationController.class)))
        .thenReturn(replicationController);
    when(kubernetesContainerService.listControllers(kubernetesConfig, emptyList())).thenReturn(null);
    when(kubernetesContainerService.createOrReplaceService(
             eq(kubernetesConfig), eq(emptyList()), any(io.fabric8.kubernetes.api.model.Service.class)))
        .thenReturn(kubernetesService);
    when(kubernetesContainerService.createOrReplaceSecret(eq(kubernetesConfig), eq(emptyList()), any(Secret.class)))
        .thenReturn(secret);
  }

  @Test
  public void shouldExecuteWithLastService() {
    ReplicationController kubernetesReplicationController =
        new ReplicationControllerBuilder()
            .withNewMetadata()
            .withName(KubernetesConvention.getReplicationControllerName(
                KubernetesConvention.getReplicationControllerNamePrefix("app", "service", "env"), 1))
            .withCreationTimestamp(new Date().toString())
            .endMetadata()
            .build();

    when(kubernetesContainerService.listControllers(kubernetesConfig, emptyList()))
        .thenReturn((List) singletonList(kubernetesReplicationController));

    CommandExecutionStatus status = kubernetesSetupCommandUnit.execute(context);
    assertThat(status).isEqualTo(CommandExecutionStatus.SUCCESS);
    verify(gkeClusterService).getCluster(any(SettingAttribute.class), eq(emptyList()), anyString(), anyString());
    verify(kubernetesContainerService).createController(eq(kubernetesConfig), any(), any(ReplicationController.class));
  }
}
