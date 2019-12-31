package io.harness.perpetualtask.k8s.watch;

import static io.harness.rule.OwnerRule.AVMOHAN;
import static org.assertj.core.api.Assertions.assertThat;

import io.fabric8.kubernetes.api.model.JobBuilder;
import io.fabric8.kubernetes.api.model.OwnerReferenceBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.extensions.DaemonSetBuilder;
import io.fabric8.kubernetes.api.model.extensions.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.extensions.ReplicaSetBuilder;
import io.fabric8.kubernetes.api.model.extensions.StatefulSetBuilder;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.OwnerRule.Owner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class K8sWorkloadUtilsTest extends CategoryTest {
  @Rule public final KubernetesServer server = new KubernetesServer();

  @Before
  public void setUp() throws Exception {
    // Deployment
    server.expect()
        .get()
        .withPath("/apis/extensions/v1beta1/namespaces/ns1/deployments/dep1")
        .andReturn(200,
            new DeploymentBuilder()
                .withNewMetadata()
                .withUid("32f51747-f16f-4a0e-b4b8-05aa69c9a3e3")
                .withName("dep1")
                .withNamespace("ns1")
                .endMetadata()
                .build())
        .always();
    server.expect()
        .get()
        .withPath("/apis/extensions/v1beta1/namespaces/ns1/replicasets/rs1")
        .andReturn(200,
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
                .build())
        .always();
    server.expect()
        .get()
        .withPath("/api/v1/namespaces/ns1/pods/pod1")
        .andReturn(200,
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
                .build())
        .always();

    // DaemonSet
    server.expect()
        .get()
        .withPath("/apis/extensions/v1beta1/namespaces/ns1/daemonsets/ds1")
        .andReturn(200,
            new DaemonSetBuilder()
                .withNewMetadata()
                .withUid("227be4c6-654e-4820-a0ca-c14bfcc02a32")
                .withName("ds1")
                .withNamespace("ns1")
                .endMetadata()
                .build())
        .always();
    server.expect()
        .get()
        .withPath("/api/v1/namespaces/ns1/pods/pod2")
        .andReturn(200,
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
                .build())
        .always();

    // Independent pod
    server.expect()
        .get()
        .withPath("/api/v1/namespaces/ns1/pods/pod3")
        .andReturn(200,
            new PodBuilder()
                .withNewMetadata()
                .withUid("9f6e1c5a-0029-48e0-9725-dad75c8119a1")
                .withName("pod3")
                .withNamespace("ns1")
                .endMetadata()
                .build())
        .always();

    // Job
    server.expect()
        .get()
        .withPath("/api/v1/namespaces/ns1/pods/pod6")
        .andReturn(200,
            new PodBuilder()
                .withNewMetadata()
                .withUid("01bb746e-3c7c-41db-a330-f8bb1a08ff9e")
                .withName("pod6")
                .withNamespace("ns1")
                .withOwnerReferences(new OwnerReferenceBuilder()
                                         .withKind("Job")
                                         .withUid("4c26f640-d683-4024-992d-859b16c7f2d3")
                                         .withName("job2")
                                         .withController(true)
                                         .build())
                .endMetadata()
                .build())
        .always();
    server.expect()
        .get()
        .withPath("/apis/batch/v1/namespaces/ns1/jobs/job2")
        .andReturn(200,
            new JobBuilder()
                .withNewMetadata()
                .withUid("4c26f640-d683-4024-992d-859b16c7f2d3")
                .withName("job2")
                .withNamespace("ns1")
                .endMetadata()
                .build())
        .always();

    // CronJob
    server.expect()
        .get()
        .withPath("/api/v1/namespaces/ns1/pods/pod4")
        .andReturn(200,
            new PodBuilder()
                .withNewMetadata()
                .withUid("f28496a5-a9bd-4190-9696-f8139aa02f14")
                .withName("pod4")
                .withNamespace("ns1")
                .withOwnerReferences(new OwnerReferenceBuilder()
                                         .withKind("Job")
                                         .withUid("690d5860-103a-4ad7-bf99-a5c184bbdf78")
                                         .withName("job1")
                                         .withController(true)
                                         .build())
                .endMetadata()
                .build())
        .always();
    server.expect()
        .get()
        .withPath("/apis/batch/v1/namespaces/ns1/jobs/job1")
        .andReturn(200,
            new JobBuilder()
                .withNewMetadata()
                .withUid("690d5860-103a-4ad7-bf99-a5c184bbdf78")
                .withName("job1")
                .withNamespace("ns1")
                .withOwnerReferences(new OwnerReferenceBuilder()
                                         .withKind("CronJob")
                                         .withUid("502d0a2a-1dfd-4ca2-a4f8-9054a1ea8ddc")
                                         .withName("cj1")
                                         .withController(true)
                                         .build())
                .endMetadata()
                .build())
        .always();
    server.expect()
        .get()
        .withPath("/apis/batch/v1beta1/namespaces/ns1/cronjobs/cj1")
        .andReturn(200,
            "{\n"
                + "  \"apiVersion\" : \"batch/v1beta1\",\n"
                + "  \"kind\" : \"CronJob\",\n"
                + "  \"metadata\" : {\n"
                + "    \"name\" : \"cj1\",\n"
                + "    \"namespace\" : \"ns1\",\n"
                + "    \"uid\" : \"502d0a2a-1dfd-4ca2-a4f8-9054a1ea8ddc\"\n"
                + "  }\n"
                + "}\n")
        .always();

    // StatefulSet
    server.expect()
        .get()
        .withPath("/api/v1/namespaces/ns1/pods/pod5")
        .andReturn(200,
            new PodBuilder()
                .withNewMetadata()
                .withUid("3cf3f31d-fcbf-47ad-b69b-30b6659e8bfc")
                .withName("pod5")
                .withNamespace("ns1")
                .withOwnerReferences(new OwnerReferenceBuilder()
                                         .withKind("StatefulSet")
                                         .withUid("c6befdfc-cb1e-43c3-8faa-803612fcb72a")
                                         .withName("ss1")
                                         .withController(true)
                                         .build())
                .endMetadata()
                .build())
        .always();
    server.expect()
        .get()
        .withPath("/apis/apps/v1beta1/namespaces/ns1/statefulsets/ss1")
        .andReturn(200,
            new StatefulSetBuilder()
                .withNewMetadata()
                .withUid("c6befdfc-cb1e-43c3-8faa-803612fcb72a")
                .withName("ss1")
                .withNamespace("ns1")
                .endMetadata()
                .build())
        .always();
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

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void testWorkLoadForCronJob() throws Exception {
    Pod pod4 = server.getClient().pods().inNamespace("ns1").withName("pod4").get();
    assertThat(K8sWorkloadUtils.getTopLevelOwner(server.getClient(), pod4))
        .isEqualTo(io.harness.perpetualtask.k8s.watch.Owner.newBuilder()
                       .setKind("CronJob")
                       .setName("cj1")
                       .setUid("502d0a2a-1dfd-4ca2-a4f8-9054a1ea8ddc")
                       .build());
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void testWorkLoadForStatefulSet() throws Exception {
    Pod pod5 = server.getClient().pods().inNamespace("ns1").withName("pod5").get();
    assertThat(K8sWorkloadUtils.getTopLevelOwner(server.getClient(), pod5))
        .isEqualTo(io.harness.perpetualtask.k8s.watch.Owner.newBuilder()
                       .setKind("StatefulSet")
                       .setName("ss1")
                       .setUid("c6befdfc-cb1e-43c3-8faa-803612fcb72a")
                       .build());
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void testWorkLoadForJob() throws Exception {
    Pod pod4 = server.getClient().pods().inNamespace("ns1").withName("pod6").get();
    assertThat(K8sWorkloadUtils.getTopLevelOwner(server.getClient(), pod4))
        .isEqualTo(io.harness.perpetualtask.k8s.watch.Owner.newBuilder()
                       .setKind("Job")
                       .setName("job2")
                       .setUid("4c26f640-d683-4024-992d-859b16c7f2d3")
                       .build());
  }
}
