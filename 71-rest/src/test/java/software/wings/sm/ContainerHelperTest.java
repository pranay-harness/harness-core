package software.wings.sm;

import static io.harness.rule.OwnerRule.YOGESH;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.sm.ContainerHelper.generateInstanceDetails;

import io.harness.category.element.UnitTests;
import io.harness.deployment.InstanceDetails;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.cloudprovider.ContainerInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ContainerHelperTest extends WingsBaseTest {
  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testGenerateInstanceDetails() {
    final List<ContainerInfo> containerInfo = Arrays.asList(ContainerInfo.builder()
                                                                .hostName("hostname-1")
                                                                .containerId("docker-1")
                                                                .newContainer(false)
                                                                .podName("pod-1")
                                                                .ip("ip-1")
                                                                .workloadName("workload-1")
                                                                .build(),
        ContainerInfo.builder()
            .hostName("hostname-2")
            .containerId("docker-2")
            .newContainer(true)
            .podName("pod-2")
            .ip("ip-2")
            .workloadName("workload-2")
            .build());
    final List<InstanceDetails> instanceDetails = generateInstanceDetails(containerInfo);
    assertThat(instanceDetails).hasSize(2);
    assertThat(instanceDetails.get(0).getHostName()).isEqualTo("hostname-1");
    assertThat(instanceDetails.get(0).getWorkloadName()).isEqualTo("workload-1");
    assertThat(instanceDetails.get(0).getInstanceType()).isEqualTo(InstanceDetails.InstanceType.K8s);
    assertThat(instanceDetails.get(0).isNewInstance()).isFalse();
    assertThat(instanceDetails.get(0).getK8s().getDockerId()).isEqualTo("docker-1");
    assertThat(instanceDetails.get(0).getK8s().getIp()).isEqualTo("ip-1");
    assertThat(instanceDetails.get(0).getK8s().getPodName()).isEqualTo("pod-1");

    assertThat(instanceDetails.get(1).getHostName()).isEqualTo("hostname-2");
    assertThat(instanceDetails.get(1).getWorkloadName()).isEqualTo("workload-2");
    assertThat(instanceDetails.get(1).getInstanceType()).isEqualTo(InstanceDetails.InstanceType.K8s);
    assertThat(instanceDetails.get(1).isNewInstance()).isTrue();
    assertThat(instanceDetails.get(1).getK8s().getIp()).isEqualTo("ip-2");
    assertThat(instanceDetails.get(1).getK8s().getPodName()).isEqualTo("pod-2");
    assertThat(instanceDetails.get(1).getK8s().getDockerId()).isEqualTo("docker-2");
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testGenerateInstanceDetailsForEmptyContainerInfo() {
    assertThat(generateInstanceDetails(new ArrayList<>())).isEmpty();
    assertThat(generateInstanceDetails(null)).isEmpty();
  }
}