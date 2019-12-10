package io.harness.perpetualtask.k8s.watch.functions;

import static junit.framework.TestCase.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.fabric8.kubernetes.api.model.JobBuilder;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.k8s.model.Kind;
import io.harness.perpetualtask.k8s.watch.Owner;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;
import java.util.UUID;

@RunWith(MockitoJUnitRunner.class)
public class JobOwnerMappingFunctionTest extends CategoryTest {
  @Rule public final KubernetesServer server = new KubernetesServer(true, true);
  private KubernetesClient kubernetesClient;
  private OwnerReference ownerReference;
  private static final String OWNER_NAME = "ttl-1575975600";
  private static final String PARENT_OWNER_NAME = "ttl";
  private static final String PARENT_KIND_NAME = Kind.CronJob.name();
  private static final String OWNER_KIND_NAME = Kind.Job.name();
  private static final String POD_NAMESPACE = "harness-test";

  @Before
  @Category(UnitTests.class)
  public void setUp() {
    ownerReference = mock(OwnerReference.class);
    when(ownerReference.getName()).thenReturn(OWNER_NAME);

    kubernetesClient = server.getClient();

    kubernetesClient.extensions()
        .jobs()
        .inNamespace(POD_NAMESPACE)
        .create(new JobBuilder().withMetadata(getObjectMeta(OWNER_NAME, PARENT_KIND_NAME, PARENT_OWNER_NAME)).build());
  }

  @Test
  @Category(UnitTests.class)
  public void apply() {
    JobOwnerMappingFunction jobOwnerMappingFunction = new JobOwnerMappingFunction();
    Owner owner =
        jobOwnerMappingFunction.apply(getOwnerReference(OWNER_KIND_NAME, OWNER_NAME), kubernetesClient, POD_NAMESPACE);
    assertEquals(PARENT_KIND_NAME, owner.getKind());
    assertEquals(PARENT_OWNER_NAME, owner.getName());
  }

  private OwnerReference getOwnerReference(String kind, String name) {
    OwnerReference ownerReference = new OwnerReference();
    ownerReference.setUid(UUID.randomUUID().toString());
    ownerReference.setKind(kind);
    ownerReference.setName(name);
    return ownerReference;
  }

  private ObjectMeta getObjectMeta(String name, String parentKindName, String parentOwnerName) {
    List<OwnerReference> ownerReferenceList = Lists.newArrayList();
    ownerReferenceList.add(getOwnerReference(parentKindName, parentOwnerName));
    ObjectMeta ownerObjectMeta = new ObjectMeta();
    ownerObjectMeta.setName(name);
    ownerObjectMeta.setOwnerReferences(ownerReferenceList);
    return ownerObjectMeta;
  }
}