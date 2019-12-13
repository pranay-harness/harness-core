package io.harness.perpetualtask.k8s.watch.functions;

import static io.harness.perpetualtask.k8s.watch.functions.OwnerMappingUtils.getObjectMeta;
import static io.harness.perpetualtask.k8s.watch.functions.OwnerMappingUtils.getOwnerReference;
import static io.harness.rule.OwnerRule.VIKAS;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

import io.fabric8.kubernetes.api.model.extensions.ReplicaSetBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.k8s.model.Kind;
import io.harness.perpetualtask.k8s.watch.Owner;
import io.harness.rule.OwnerRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ReplicaSetOwnerMappingFunctionTest extends CategoryTest {
  @Rule public final KubernetesServer server = new KubernetesServer(true, true);
  private KubernetesClient kubernetesClient;

  private static final String OWNER_NAME = "cattle-cluster-agent-5c59669d7";
  private static final String PARENT_OWNER_NAME = "cattle-cluster-agent\n";
  private static final String PARENT_KIND_NAME = Kind.Deployment.name();
  private static final String OWNER_KIND_NAME = Kind.ReplicaSet.name();
  private static final String POD_NAMESPACE = "harness-test";

  @Before
  public void setUp() {
    kubernetesClient = server.getClient();
  }

  @Test
  @OwnerRule.Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void apply() {
    kubernetesClient.extensions()
        .replicaSets()
        .inNamespace(POD_NAMESPACE)
        .create(new ReplicaSetBuilder()
                    .withMetadata(getObjectMeta(OWNER_NAME, PARENT_KIND_NAME, PARENT_OWNER_NAME))
                    .build());
    ReplicaSetOwnerMappingFunction replicaSetOwnerMappingFunction = new ReplicaSetOwnerMappingFunction();
    Owner owner = replicaSetOwnerMappingFunction.apply(
        getOwnerReference(OWNER_KIND_NAME, OWNER_NAME), kubernetesClient, POD_NAMESPACE);
    assertTrue(owner.getKind() != null);
    assertEquals(PARENT_KIND_NAME, owner.getKind());
    assertEquals(PARENT_OWNER_NAME, owner.getName());
  }

  @Test
  @OwnerRule.Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void applyWithIncorrectNamespace() {
    kubernetesClient.extensions()
        .replicaSets()
        .inNamespace(PARENT_OWNER_NAME)
        .create(new ReplicaSetBuilder()
                    .withMetadata(getObjectMeta(OWNER_NAME, PARENT_KIND_NAME, PARENT_OWNER_NAME))
                    .build());
    ReplicaSetOwnerMappingFunction replicaSetOwnerMappingFunction = new ReplicaSetOwnerMappingFunction();
    Owner owner = replicaSetOwnerMappingFunction.apply(
        getOwnerReference(OWNER_KIND_NAME, OWNER_NAME), kubernetesClient, POD_NAMESPACE);
    assertTrue(owner.getKind() != null);
    assertEquals(OWNER_KIND_NAME, owner.getKind());
    assertEquals(OWNER_NAME, owner.getName());
  }
}