package io.harness.perpetualtask.k8s.watch;

import static io.harness.rule.OwnerRule.AVMOHAN;
import static io.harness.rule.OwnerRule.HANTANG;
import static io.harness.rule.OwnerRule.UTSAV;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.openapi.models.V1Container;
import io.kubernetes.client.openapi.models.V1ContainerBuilder;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1ObjectMetaBuilder;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodBuilder;
import io.kubernetes.client.openapi.models.V1PodCondition;
import io.kubernetes.client.openapi.models.V1PodConditionBuilder;
import io.kubernetes.client.openapi.models.V1ResourceRequirements;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.math.BigDecimal;

public class K8sResourceUtilsTest extends CategoryTest {
  private final V1ResourceRequirements resourceRequirements = new V1ResourceRequirements();
  private V1Container k8sContainer;
  private final DateTime TIMESTAMP = DateTime.now();

  @Before
  public void init() {
    k8sContainer = new V1ContainerBuilder()
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
  public void shouldComputeEffectiveResources() throws Exception {
    V1Pod pod = new V1PodBuilder()
                    .withNewSpec()
                    .withContainers(makeContainer("250m", "1Ki", "1", "50Ki"), makeContainer("500m", "1Mi"))
                    .withInitContainers(makeContainer("0.8", "1Ki"), makeContainer("500m", "2Ki"))
                    .endSpec()
                    .build();
    assertThat(K8sResourceUtils.getEffectiveResources(pod.getSpec()))
        .isEqualTo(Resource.newBuilder()
                       .putRequests("cpu", Resource.Quantity.newBuilder().setAmount(800_000_000).setUnit("n").build())
                       .putRequests("memory", Resource.Quantity.newBuilder().setAmount(1049600).setUnit("").build())
                       .putLimits("cpu", Resource.Quantity.newBuilder().setAmount(1_500_000_000).setUnit("n").build())
                       .putLimits("memory", Resource.Quantity.newBuilder().setAmount(1099776).setUnit("").build())
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

    assertThat(K8sResourceUtils.getResourceMap(
                   ImmutableMap.of("cpu", new Quantity(new BigDecimal("1.9"), Quantity.Format.DECIMAL_SI), "memory",
                       new Quantity(new BigDecimal(1_000L), Quantity.Format.BINARY_SI))))
        .isEqualTo(ImmutableMap.of("cpu", Resource.Quantity.newBuilder().setUnit("n").setAmount(1_900_000_000L).build(),
            "memory", Resource.Quantity.newBuilder().setAmount(1_000L).build()));
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testGetTimestampInMillisByMethod() {
    V1ObjectMeta v1ObjectMeta =
        new V1ObjectMetaBuilder().withCreationTimestamp(TIMESTAMP).withDeletionTimestamp(TIMESTAMP).build();

    assertThat(v1ObjectMeta.getCreationTimestamp().getMillis()).isEqualTo(TIMESTAMP.getMillis());
    assertThat(v1ObjectMeta.getDeletionTimestamp().getMillis()).isEqualTo(TIMESTAMP.getMillis());

    V1PodCondition podScheduledCondition = new V1PodConditionBuilder().withLastTransitionTime(TIMESTAMP).build();
    assertThat(podScheduledCondition.getLastTransitionTime().getMillis()).isEqualTo(TIMESTAMP.getMillis());
  }

  private V1Container makeContainer(String cpuLim, String memLim) {
    return makeContainer(cpuLim, memLim, cpuLim, memLim);
  }
  private V1Container makeContainer(String cpuReq, String memReq, String cpuLim, String memLim) {
    return new V1ContainerBuilder()
        .withNewResources()
        .addToRequests("cpu", new Quantity(cpuReq))
        .addToRequests("memory", new Quantity(memReq))
        .addToLimits("cpu", new Quantity(cpuLim))
        .addToLimits("memory", new Quantity(memLim))
        .endResources()
        .build();
  }
}
