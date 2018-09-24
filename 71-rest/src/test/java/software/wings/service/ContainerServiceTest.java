package software.wings.service;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.CLUSTER_NAME;
import static software.wings.utils.WingsTestConstants.ECS_SERVICE_NAME;

import com.google.common.collect.ImmutableMap;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.ecs.model.ListTasksResult;
import com.amazonaws.services.ecs.model.Service;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.PodTemplateSpecBuilder;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.ReplicationControllerBuilder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.AwsConfig;
import software.wings.beans.GcpConfig;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.infrastructure.instance.info.ContainerInfo;
import software.wings.cloudprovider.aws.AwsClusterService;
import software.wings.cloudprovider.gke.GkeClusterService;
import software.wings.cloudprovider.gke.KubernetesContainerService;
import software.wings.service.impl.AwsHelperService;
import software.wings.service.impl.ContainerServiceImpl;
import software.wings.service.impl.ContainerServiceParams;
import software.wings.service.intfc.ContainerService;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public class ContainerServiceTest extends WingsBaseTest {
  private static final String KUBERNETES_REPLICATION_CONTROLLER_NAME = "kubernetes-rc-name.0";
  private static final String KUBERNETES_SERVICE_NAME = "kubernetes-service-name";

  @Mock private GkeClusterService gkeClusterService;
  @Mock private KubernetesContainerService kubernetesContainerService;
  @Mock private AwsClusterService awsClusterService;
  @Mock private AwsHelperService awsHelperService;

  @InjectMocks private ContainerService containerService = new ContainerServiceImpl();

  @Mock private ListTasksResult listTasksResult;

  private KubernetesConfig kubernetesConfig = KubernetesConfig.builder()
                                                  .masterUrl("masterUrl")
                                                  .namespace("default")
                                                  .username("user")
                                                  .password("pass".toCharArray())
                                                  .accountId(ACCOUNT_ID)
                                                  .build();

  private ContainerServiceParams gcpParams =
      ContainerServiceParams.builder()
          .settingAttribute(aSettingAttribute()
                                .withValue(GcpConfig.builder()
                                               .serviceAccountKeyFileContent("keyFileContent".toCharArray())
                                               .accountId(ACCOUNT_ID)
                                               .build())
                                .build())
          .encryptionDetails(emptyList())
          .clusterName(CLUSTER_NAME)
          .namespace("default")
          .containerServiceName(KUBERNETES_REPLICATION_CONTROLLER_NAME)
          .build();

  private ContainerServiceParams awsParams =
      ContainerServiceParams.builder()
          .settingAttribute(aSettingAttribute()
                                .withValue(AwsConfig.builder()
                                               .accessKey("accessKey")
                                               .secretKey("secretKey".toCharArray())
                                               .accountId(ACCOUNT_ID)
                                               .build())
                                .build())
          .encryptionDetails(emptyList())
          .clusterName(CLUSTER_NAME)
          .containerServiceName(ECS_SERVICE_NAME)
          .region("us-east-1")
          .build();

  private ContainerServiceParams kubernetesConfigParams =
      ContainerServiceParams.builder()
          .settingAttribute(aSettingAttribute().withValue(kubernetesConfig).build())
          .encryptionDetails(emptyList())
          .clusterName(CLUSTER_NAME)
          .namespace("default")
          .containerServiceName(KUBERNETES_REPLICATION_CONTROLLER_NAME)
          .build();

  @Before
  public void setup() {
    ReplicationController replicationController = new ReplicationControllerBuilder()
                                                      .withApiVersion("v1")
                                                      .withNewMetadata()
                                                      .withName(KUBERNETES_REPLICATION_CONTROLLER_NAME)
                                                      .endMetadata()
                                                      .withNewSpec()
                                                      .withReplicas(2)
                                                      .endSpec()
                                                      .build();
    io.fabric8.kubernetes.api.model.Service kubernetesService = new io.fabric8.kubernetes.api.model.ServiceBuilder()
                                                                    .withApiVersion("v1")
                                                                    .withNewMetadata()
                                                                    .withName(KUBERNETES_SERVICE_NAME)
                                                                    .endMetadata()
                                                                    .build();
    Pod pod = new PodBuilder()
                  .withApiVersion("v1")
                  .withNewStatus()
                  .withPhase("Running")
                  .endStatus()
                  .withNewMetadata()
                  .addToLabels("app", "MyApp")
                  .endMetadata()
                  .build();
    PodTemplateSpec podTemplateSpec = new PodTemplateSpecBuilder()
                                          .withNewMetadata()
                                          .withLabels(ImmutableMap.of("app", "MyApp"))
                                          .endMetadata()
                                          .build();
    when(gkeClusterService.getCluster(gcpParams.getSettingAttribute(), emptyList(), CLUSTER_NAME, "default"))
        .thenReturn(kubernetesConfig);
    when(kubernetesContainerService.listControllers(kubernetesConfig, emptyList()))
        .thenReturn((List) singletonList(replicationController));
    when(kubernetesContainerService.getController(eq(kubernetesConfig), anyObject(), anyString()))
        .thenReturn(replicationController);
    when(kubernetesContainerService.getServices(eq(kubernetesConfig), anyObject(), anyObject()))
        .thenReturn(singletonList(kubernetesService));
    when(kubernetesContainerService.getPods(eq(kubernetesConfig), anyObject(), anyObject()))
        .thenReturn(singletonList(pod));
    when(kubernetesContainerService.getControllers(eq(kubernetesConfig), anyObject(), anyObject()))
        .thenReturn((List) singletonList(replicationController));
    when(kubernetesContainerService.getControllerPodCount(eq(kubernetesConfig), anyObject(), anyString()))
        .thenReturn(Optional.of(2));
    when(kubernetesContainerService.getControllerPodCount(any(ReplicationController.class))).thenReturn(2);
    when(kubernetesContainerService.getPodTemplateSpec(replicationController)).thenReturn(podTemplateSpec);

    Service ecsService = new Service();
    ecsService.setServiceName(ECS_SERVICE_NAME);
    ecsService.setCreatedAt(new Date());
    ecsService.setDesiredCount(2);
    AwsConfig awsConfig =
        AwsConfig.builder().accessKey("accessKey").secretKey("secretKey".toCharArray()).accountId(ACCOUNT_ID).build();
    SettingAttribute awsSettingAttribute = aSettingAttribute().withValue(awsConfig).build();
    when(awsClusterService.getServices(
             Regions.US_EAST_1.getName(), awsSettingAttribute, Collections.emptyList(), CLUSTER_NAME))
        .thenReturn(singletonList(ecsService));
    when(awsHelperService.validateAndGetAwsConfig(eq(awsSettingAttribute), anyObject())).thenReturn(awsConfig);
    when(awsHelperService.listTasks(eq("us-east-1"), eq(awsConfig), anyObject(), anyObject()))
        .thenReturn(listTasksResult);
    when(listTasksResult.getTaskArns()).thenReturn(emptyList());
  }

  @Test
  public void shouldGetContainerInfos_Gcp() {
    List<ContainerInfo> result = containerService.getContainerInfos(gcpParams);

    assertThat(result.size()).isEqualTo(1);
  }
  @Test
  public void shouldGetContainerInfos_Aws() {
    List<ContainerInfo> result = containerService.getContainerInfos(awsParams);

    assertThat(result.size()).isEqualTo(0);
  }
  @Test
  public void shouldGetContainerInfos_DirectKube() {
    List<ContainerInfo> result = containerService.getContainerInfos(kubernetesConfigParams);

    assertThat(result.size()).isEqualTo(1);
  }
}
