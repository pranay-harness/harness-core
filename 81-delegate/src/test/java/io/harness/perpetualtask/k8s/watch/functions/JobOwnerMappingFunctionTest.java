package io.harness.perpetualtask.k8s.watch.functions;

import static io.harness.perpetualtask.k8s.watch.functions.OwnerMappingUtils.getOwnerReference;
import static io.harness.perpetualtask.k8s.watch.functions.OwnerMappingUtils.getOwnerReferences;
import static io.harness.rule.OwnerRule.VIKAS;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

import io.fabric8.kubernetes.api.model.JobBuilder;
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
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class JobOwnerMappingFunctionTest extends CategoryTest {
  @Rule public final KubernetesServer server = new KubernetesServer(true, true);
  private KubernetesClient kubernetesClient;
  private static final String OWNER_NAME = "ttl-1575975600";
  private static final String PARENT_OWNER_NAME = "ttl";
  private static final String PARENT_KIND_NAME = Kind.CronJob.name();
  private static final String OWNER_KIND_NAME = Kind.Job.name();
  private static final String POD_NAMESPACE = "test";

  @Before
  public void setUp() {
    kubernetesClient = server.getClient();
  }

  @Test
  @OwnerRule.Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void apply() {
    kubernetesClient.extensions()
        .jobs()
        .inNamespace(POD_NAMESPACE)
        .create(new JobBuilder()
                    .withNewMetadata()
                    .withName(OWNER_NAME)
                    .withOwnerReferences(getOwnerReferences(PARENT_KIND_NAME, PARENT_OWNER_NAME))
                    .endMetadata()
                    .build());

    //.withMetadata(getObjectMeta(OWNER_NAME, PARENT_KIND_NAME, PARENT_OWNER_NAME)).build());
    JobOwnerMappingFunction jobOwnerMappingFunction = new JobOwnerMappingFunction();
    Owner owner =
        jobOwnerMappingFunction.apply(getOwnerReference(OWNER_KIND_NAME, OWNER_NAME), kubernetesClient, POD_NAMESPACE);
    assertTrue(owner.getKind() != null);
    assertEquals(PARENT_KIND_NAME, owner.getKind());
    assertEquals(PARENT_OWNER_NAME, owner.getName());
  }

  @Test
  @OwnerRule.Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void applyWhenJobIsNotAvailable() {
    String incorrectParentOwnerName = PARENT_OWNER_NAME.concat("incorrect");
    kubernetesClient.extensions()
        .jobs()
        .inNamespace(POD_NAMESPACE)
        .create(new JobBuilder()
                    .withNewMetadata()
                    .withName(OWNER_NAME)
                    .withOwnerReferences(getOwnerReferences(PARENT_KIND_NAME, incorrectParentOwnerName))
                    .endMetadata()
                    .build());
    JobOwnerMappingFunction jobOwnerMappingFunction = new JobOwnerMappingFunction();
    Owner owner = jobOwnerMappingFunction.apply(
        getOwnerReference(OWNER_KIND_NAME, incorrectParentOwnerName), kubernetesClient, POD_NAMESPACE);
    assertTrue(owner.getKind() != null);
    assertEquals(incorrectParentOwnerName, owner.getName());
  }
}