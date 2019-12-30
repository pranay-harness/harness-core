package io.harness.perpetualtask.k8s.watch;

import static io.harness.rule.OwnerRule.AVMOHAN;
import static org.assertj.core.api.Assertions.assertThat;

import io.fabric8.kubernetes.api.model.OwnerReferenceBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.extensions.DaemonSetBuilder;
import io.fabric8.kubernetes.api.model.extensions.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.extensions.ReplicaSetBuilder;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.OwnerRule.Owner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class K8sWorkloadUtilsTest extends CategoryTest {
  @Rule public final KubernetesServer server = new KubernetesServer(true, true);

  @Before
  public void setUp() throws Exception {
    server.getClient().extensions().deployments().inNamespace("ns1").create(
        new DeploymentBuilder()
            .withNewMetadata()
            .withUid("32f51747-f16f-4a0e-b4b8-05aa69c9a3e3")
            .withName("dep1")
            .withNamespace("ns1")
            .endMetadata()
            .build());
    server.getClient().extensions().replicaSets().inNamespace("ns1").create(
        new ReplicaSetBuilder()
            .withNewMetadata()
            .withUid("30da7fb4-5396-442e-b405-0f3826571fdc")
            .withName("rs1")
            .withNamespace("ns1")
            .withOwnerReferences(new OwnerReferenceBuilder()
                                     .withKind("Deployment")
                                     .withUid("32f51747-f16f-4a0e-b4b8-05aa69c9a3e3")
                                     .withName("dep1")
                                     .withController(true)
                                     .build())
            .endMetadata()
            .build());
    server.getClient().extensions().daemonSets().inNamespace("ns1").create(
        new DaemonSetBuilder()
            .withNewMetadata()
            .withUid("227be4c6-654e-4820-a0ca-c14bfcc02a32")
            .withName("ds1")
            .withNamespace("ns1")
            .endMetadata()
            .build());
    server.getClient().pods().inNamespace("ns1").create(
        new PodBuilder()
            .withNewMetadata()
            .withUid("41fd2a56-d551-4689-aeb8-47bbddda4414")
            .withName("pod1")
            .withNamespace("ns1")
            .withOwnerReferences(new OwnerReferenceBuilder()
                                     .withKind("ReplicaSet")
                                     .withUid("30da7fb4-5396-442e-b405-0f3826571fdc")
                                     .withName("rs1")
                                     .withController(true)
                                     .build())
            .endMetadata()
            .build());
    server.getClient().pods().inNamespace("ns1").create(
        new PodBuilder()
            .withNewMetadata()
            .withUid("59000911-7687-4e3c-a576-452138eb2cce")
            .withName("pod2")
            .withNamespace("ns1")
            .withOwnerReferences(new OwnerReferenceBuilder()
                                     .withKind("DaemonSet")
                                     .withUid("227be4c6-654e-4820-a0ca-c14bfcc02a32")
                                     .withName("ds1")
                                     .withController(true)
                                     .build())
            .endMetadata()
            .build());
    server.getClient().pods().inNamespace("ns1").create(new PodBuilder()
                                                            .withNewMetadata()
                                                            .withUid("9f6e1c5a-0029-48e0-9725-dad75c8119a1")
                                                            .withName("pod3")
                                                            .withNamespace("ns1")
                                                            .endMetadata()
                                                            .build());
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void testWorkLoadForDeployment() throws Exception {
    Pod pod1 = server.getClient().pods().inNamespace("ns1").withName("pod1").get();
    assertThat(K8sWorkloadUtils.getTopLevelOwner(server.getClient(), pod1))
        .isEqualTo(io.harness.perpetualtask.k8s.watch.Owner.newBuilder()
                       .setKind("Deployment")
                       .setName("dep1")
                       .setUid("32f51747-f16f-4a0e-b4b8-05aa69c9a3e3")
                       .build());
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void testWorkLoadForDaemonSet() throws Exception {
    Pod pod2 = server.getClient().pods().inNamespace("ns1").withName("pod2").get();
    assertThat(K8sWorkloadUtils.getTopLevelOwner(server.getClient(), pod2))
        .isEqualTo(io.harness.perpetualtask.k8s.watch.Owner.newBuilder()
                       .setKind("DaemonSet")
                       .setName("ds1")
                       .setUid("227be4c6-654e-4820-a0ca-c14bfcc02a32")
                       .build());
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void testWorkLoadIndependentPod() throws Exception {
    Pod pod3 = server.getClient().pods().inNamespace("ns1").withName("pod3").get();
    assertThat(K8sWorkloadUtils.getTopLevelOwner(server.getClient(), pod3))
        .isEqualTo(io.harness.perpetualtask.k8s.watch.Owner.newBuilder()
                       .setKind("Pod")
                       .setName("pod3")
                       .setUid("9f6e1c5a-0029-48e0-9725-dad75c8119a1")
                       .build());
  }
}
