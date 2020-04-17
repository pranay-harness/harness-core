package io.harness.perpetualtask.k8s.watch;

import static io.harness.rule.OwnerRule.AVMOHAN;
import static io.harness.rule.OwnerRule.HANTANG;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class K8sResourceUtilsTest extends CategoryTest {
  private ResourceRequirements resourceRequirements = new ResourceRequirements();
  private Container k8sContainer;

  @Before
  public void init() {
    k8sContainer = new io.fabric8.kubernetes.api.model.ContainerBuilder()
                       .withName("init-mydb")
                       .withImage("busybox")
                       .withResources(resourceRequirements)
                       .addToCommand("sh")
                       .addToCommand("-c")
                       .addToCommand("until nslookup mydb; do echo waiting for mydb; sleep 2; done;")
                       .build();
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void testGetResource() {
    Resource actualResource = K8sResourceUtils.getResource(k8sContainer);
    assertThat(actualResource).isNotNull();
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldComputeEffectiveRequest() throws Exception {
    Pod pod = new PodBuilder()
                  .withNewSpec()
                  .withContainers(makeContainer("250m", "1Ki"), makeContainer("500m", "1Mi"))
                  .withInitContainers(makeContainer("0.8", "1Ki"), makeContainer("500m", "2Ki"))
                  .endSpec()
                  .build();
    assertThat(K8sResourceUtils.getTotalResourceRequest(pod.getSpec()))
        .isEqualTo(Resource.newBuilder()
                       .putRequests("cpu", Resource.Quantity.newBuilder().setAmount(800_000_000).setUnit("n").build())
                       .putRequests("memory", Resource.Quantity.newBuilder().setAmount(1049600).setUnit("").build())
                       .build());
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void testGetResourceMapNoCpu() throws Exception {
    assertThat(K8sResourceUtils.getResourceMap(ImmutableMap.of("memory", new Quantity("0.5Mi"))))
        .isEqualTo(ImmutableMap.of("cpu", Resource.Quantity.newBuilder().setUnit("n").setAmount(0).build(), "memory",
            Resource.Quantity.newBuilder().setAmount(512 * 1024).build()));
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void testGetResourceMapNoMemory() throws Exception {
    assertThat(K8sResourceUtils.getResourceMap(ImmutableMap.of("cpu", new Quantity("100m"))))
        .isEqualTo(ImmutableMap.of("cpu", Resource.Quantity.newBuilder().setUnit("n").setAmount(100_000_000L).build(),
            "memory", Resource.Quantity.newBuilder().setAmount(0).build()));
  }

  private Container makeContainer(String cpu, String memory) {
    return new ContainerBuilder()
        .withNewResources()
        .addToRequests("cpu", new Quantity(cpu))
        .addToRequests("memory", new Quantity(memory))
        .endResources()
        .build();
  }
}
