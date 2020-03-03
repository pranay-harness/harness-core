package io.harness.perpetualtask.k8s.watch;

import static io.harness.rule.OwnerRule.AVMOHAN;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.fabric8.kubernetes.api.model.EventBuilder;
import io.fabric8.kubernetes.api.model.extensions.DeploymentBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.dsl.FilterWatchListMultiDeletable;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.event.client.EventPublisher;
import io.harness.grpc.utils.HTimestamps;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ClusterEventWatcherTest extends CategoryTest {
  @Rule public final KubernetesServer server = new KubernetesServer();

  private ClusterEventWatcher watcher;
  private KubernetesClient client;
  @Mock private EventPublisher eventPublisher;

  private K8sClusterEvent expectedEventPrototype = K8sClusterEvent.newBuilder()
                                                       .setCloudProviderId("cloud-provider-id")
                                                       .setClusterId("cluster-id")
                                                       .setClusterName("cluster-name")
                                                       .build();

  @Before
  public void setUp() throws Exception {
    client = spy(server.getClient());
    MixedOperation op = mock(MixedOperation.class);
    FilterWatchListMultiDeletable op1 = mock(FilterWatchListMultiDeletable.class);
    when(client.events()).thenReturn(op);
    when(op.inAnyNamespace()).thenReturn(op1);
    watcher = new ClusterEventWatcher(client,
        K8sWatchTaskParams.newBuilder()
            .setCloudProviderId("cloud-provider-id")
            .setClusterName("cluster-name")
            .setClusterId("cluster-id")
            .build(),
        eventPublisher);

    server.expect()
        .get()
        .withPath("/apis/extensions/v1beta1/namespaces/harness/deployments/learning-engine")
        .andReturn(200,
            new DeploymentBuilder()
                .withNewMetadata()
                .withUid("7b9cd6f4-495e-11e9-a04a-4201ac10040a")
                .withName("learning-engine")
                .withNamespace("harness")
                .withResourceVersion("160985299")
                .endMetadata()
                .build())
        .always();
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldHandleDeploymentScalingReplicaSet() throws Exception {
    watcher.eventReceived(Watcher.Action.ADDED,
        new EventBuilder()
            .withNewSource()
            .withComponent("deployment-controller")
            .endSource()
            .withReason("ScalingReplicaSet")
            .withMessage("Scaled up replica set learning-engine-66f497cf87 to 3")
            .withNewInvolvedObject()
            .withApiVersion("apps/v1")
            .withKind("Deployment")
            .withName("learning-engine")
            .withNamespace("harness")
            .withUid("7b9cd6f4-495e-11e9-a04a-4201ac10040a")
            .withResourceVersion("160985299")
            .endInvolvedObject()
            .withLastTimestamp("2020-01-22T09:29:51Z")
            .build());
    verify(eventPublisher)
        .publishMessage(eq(K8sClusterEvent.newBuilder(expectedEventPrototype)
                                .setSourceComponent("deployment-controller")
                                .setReason("ScalingReplicaSet")
                                .setMessage("Scaled up replica set learning-engine-66f497cf87 to 3")
                                .setInvolvedObject(K8sObjectReference.newBuilder()
                                                       .setKind("Deployment")
                                                       .setName("learning-engine")
                                                       .setNamespace("harness")
                                                       .setUid("7b9cd6f4-495e-11e9-a04a-4201ac10040a")
                                                       .setResourceVersion("160985299")
                                                       .build())
                                .build()),
            eq(HTimestamps.parse("2020-01-22T09:29:51Z")), anyMapOf(String.class, String.class));
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldHandleDeploymentScalingReplicaSetWithMismatchedResourceVersion() throws Exception {
    watcher.eventReceived(Watcher.Action.ADDED,
        new EventBuilder()
            .withNewSource()
            .withComponent("deployment-controller")
            .endSource()
            .withReason("ScalingReplicaSet")
            .withMessage("Scaled up replica set learning-engine-66f497cf87 to 3")
            .withNewInvolvedObject()
            .withApiVersion("apps/v1")
            .withKind("Deployment")
            .withName("learning-engine")
            .withNamespace("harness")
            .withUid("7b9cd6f4-495e-11e9-a04a-4201ac10040a")
            .withResourceVersion("160985342")
            .endInvolvedObject()
            .withLastTimestamp("2020-01-22T09:29:51Z")
            .build());
    verify(eventPublisher)
        .publishMessage(eq(K8sClusterEvent.newBuilder(expectedEventPrototype)
                                .setSourceComponent("deployment-controller")
                                .setReason("ScalingReplicaSet")
                                .setMessage("Scaled up replica set learning-engine-66f497cf87 to 3")
                                .setInvolvedObject(K8sObjectReference.newBuilder()
                                                       .setKind("Deployment")
                                                       .setName("learning-engine")
                                                       .setNamespace("harness")
                                                       .setUid("7b9cd6f4-495e-11e9-a04a-4201ac10040a")
                                                       .setResourceVersion("160985342")
                                                       .build())
                                .build()),
            eq(HTimestamps.parse("2020-01-22T09:29:51Z")), anyMapOf(String.class, String.class));
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldHandleHpaSuccessfulRescale() throws Exception {
    watcher.eventReceived(Watcher.Action.ADDED,
        new EventBuilder()
            .withNewSource()
            .withComponent("horizontal-pod-autoscaler")
            .endSource()
            .withReason("SuccessfulRescale")
            .withMessage("New size: 2; reason: All metrics below target")
            .withNewInvolvedObject()
            .withApiVersion("autoscaling/v2beta2")
            .withKind("HorizontalPodAutoscaler")
            .withName("le-analysis-service-guard")
            .withNamespace("harness")
            .withUid("a0593d4b-1667-11ea-bdc8-4201ac104805")
            .withResourceVersion("62983483")
            .endInvolvedObject()
            .withLastTimestamp("2020-01-22T09:53:34Z")
            .build());
    verify(eventPublisher)
        .publishMessage(eq(K8sClusterEvent.newBuilder(expectedEventPrototype)
                                .setSourceComponent("horizontal-pod-autoscaler")
                                .setReason("SuccessfulRescale")
                                .setMessage("New size: 2; reason: All metrics below target")
                                .setInvolvedObject(K8sObjectReference.newBuilder()
                                                       .setKind("HorizontalPodAutoscaler")
                                                       .setName("le-analysis-service-guard")
                                                       .setNamespace("harness")
                                                       .setUid("a0593d4b-1667-11ea-bdc8-4201ac104805")
                                                       .setResourceVersion("62983483")
                                                       .build())
                                .build()),
            eq(HTimestamps.parse("2020-01-22T09:53:34Z")), anyMapOf(String.class, String.class));
  }
}
