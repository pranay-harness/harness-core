package io.harness.perpetualtask.k8s.watch;

import static io.harness.rule.OwnerRule.AVMOHAN;
import static io.harness.rule.OwnerRule.HANTANG;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.ArrayList;
import java.util.List;

public class K8sResourceUtilsTest extends CategoryTest {
  private ResourceRequirements resourceRequirements = new ResourceRequirements();
  private Container k8sContainer;
  private List<Container> k8sContainers = new ArrayList<>();

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

    k8sContainers.add(k8sContainer);
    k8sContainers.add(k8sContainer);
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void testGetResource() {
    Resource actualResource = K8sResourceUtils.getResource(k8sContainer);
    assertThat(actualResource).isNotNull();
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void testGetTotalResourceRequest() {
    Resource actualResource = K8sResourceUtils.getTotalResourceRequest(k8sContainers);
    assertThat(actualResource).isNotNull();
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
}
