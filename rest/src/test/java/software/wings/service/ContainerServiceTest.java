package software.wings.service;

import static com.google.common.truth.Truth.assertThat;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static software.wings.api.ContainerServiceElement.ContainerServiceElementBuilder.aContainerServiceElement;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.CLUSTER_NAME;
import static software.wings.utils.WingsTestConstants.ECS_SERVICE_NAME;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.ecs.model.Service;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.ReplicationControllerBuilder;
import io.fabric8.kubernetes.api.model.ReplicationControllerListBuilder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.api.ContainerServiceElement;
import software.wings.beans.AwsConfig;
import software.wings.beans.GcpConfig;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.SettingAttribute;
import software.wings.cloudprovider.aws.AwsClusterService;
import software.wings.cloudprovider.gke.GkeClusterService;
import software.wings.cloudprovider.gke.KubernetesContainerService;
import software.wings.service.impl.ContainerServiceImpl;
import software.wings.service.intfc.ContainerService;

import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Optional;

public class ContainerServiceTest extends WingsBaseTest {
  private static final String KUBERNETES_REPLICATION_CONTROLLER_NAME = "kubernetes-rc-name.0";

  @Mock private GkeClusterService gkeClusterService;
  @Mock private KubernetesContainerService kubernetesContainerService;
  @Mock private AwsClusterService awsClusterService;

  @InjectMocks private ContainerService containerService = new ContainerServiceImpl();

  private KubernetesConfig kubernetesConfig = KubernetesConfig.builder()
                                                  .masterUrl("masterUrl")
                                                  .namespace("default")
                                                  .username("user")
                                                  .password("pass".toCharArray())
                                                  .accountId(ACCOUNT_ID)
                                                  .build();

  private SettingAttribute gcpConfigSetting =
      aSettingAttribute()
          .withValue(GcpConfig.builder()
                         .serviceAccountKeyFileContent("keyFileContent".toCharArray())
                         .accountId(ACCOUNT_ID)
                         .build())
          .build();
  private SettingAttribute awsConfigSetting = aSettingAttribute()
                                                  .withValue(AwsConfig.builder()
                                                                 .accessKey("accessKey")
                                                                 .secretKey("secretKey".toCharArray())
                                                                 .accountId(ACCOUNT_ID)
                                                                 .build())
                                                  .build();
  private SettingAttribute kubernetesConfigSetting = aSettingAttribute().withValue(kubernetesConfig).build();

  private ContainerServiceElement containerServiceElement = aContainerServiceElement()
                                                                .withName(ECS_SERVICE_NAME)
                                                                .withNamespace("default")
                                                                .withClusterName(CLUSTER_NAME)
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
    when(gkeClusterService.getCluster(gcpConfigSetting, emptyList(), CLUSTER_NAME, "default"))
        .thenReturn(kubernetesConfig);
    when(kubernetesContainerService.listControllers(kubernetesConfig, emptyList()))
        .thenReturn(new ReplicationControllerListBuilder().addToItems(replicationController).build());
    when(kubernetesContainerService.getController(eq(kubernetesConfig), anyObject(), anyString()))
        .thenReturn(replicationController);

    Service ecsService = new Service();
    ecsService.setServiceName(ECS_SERVICE_NAME);
    ecsService.setCreatedAt(new Date());
    ecsService.setDesiredCount(2);
    when(awsClusterService.getServices(
             Regions.US_EAST_1.getName(), awsConfigSetting, Collections.emptyList(), CLUSTER_NAME))
        .thenReturn(singletonList(ecsService));
  }

  @Test
  public void shouldGetServiceDesiredCount_Gcp() {
    Optional<Integer> result =
        containerService.getServiceDesiredCount(gcpConfigSetting, emptyList(), containerServiceElement, null);

    assertThat(result.isPresent()).isTrue();
    assertThat(result.get()).isEqualTo(2);
  }
  @Test
  public void shouldGetServiceDesiredCount_Aws() {
    Optional<Integer> result =
        containerService.getServiceDesiredCount(awsConfigSetting, emptyList(), containerServiceElement, "us-east-1");

    assertThat(result.isPresent()).isTrue();
    assertThat(result.get()).isEqualTo(2);
  }
  @Test
  public void shouldGetServiceDesiredCount_DirectKube() {
    Optional<Integer> result =
        containerService.getServiceDesiredCount(kubernetesConfigSetting, emptyList(), containerServiceElement, null);

    assertThat(result.isPresent()).isTrue();
    assertThat(result.get()).isEqualTo(2);
  }

  @Test
  public void shouldGetActiveServiceCounts_Gcp() {
    LinkedHashMap<String, Integer> result =
        containerService.getActiveServiceCounts(gcpConfigSetting, emptyList(), containerServiceElement, null);

    assertThat(result.size()).isEqualTo(1);
    assertThat(result.get(KUBERNETES_REPLICATION_CONTROLLER_NAME)).isEqualTo(2);
  }

  @Test
  public void shouldGetActiveServiceCounts_Aws() {
    LinkedHashMap<String, Integer> result =
        containerService.getActiveServiceCounts(awsConfigSetting, emptyList(), containerServiceElement, "us-east-1");

    assertThat(result.size()).isEqualTo(1);
    assertThat(result.get(ECS_SERVICE_NAME)).isEqualTo(2);
  }
  @Test
  public void shouldGetActiveServiceCounts_DirectKube() {
    LinkedHashMap<String, Integer> result =
        containerService.getActiveServiceCounts(kubernetesConfigSetting, emptyList(), containerServiceElement, null);

    assertThat(result.size()).isEqualTo(1);
    assertThat(result.get(KUBERNETES_REPLICATION_CONTROLLER_NAME)).isEqualTo(2);
  }
}
