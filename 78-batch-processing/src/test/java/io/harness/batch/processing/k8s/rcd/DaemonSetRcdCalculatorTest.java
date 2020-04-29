package io.harness.batch.processing.k8s.rcd;

import static io.harness.rule.OwnerRule.AVMOHAN;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.openapi.models.V1DaemonSetBuilder;
import io.kubernetes.client.util.Yaml;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class DaemonSetRcdCalculatorTest extends CategoryTest {
  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldHandleAdd() throws Exception {
    assertThat(new DaemonSetRcdCalculator().computeResourceDiff("", daemonSetYaml("100m", "1200Mi")))
        .isEqualTo(ResourceClaim.builder().cpuNano(100000000L).memBytes(1258291200L).build());
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldHandleDelete() throws Exception {
    assertThat(new DaemonSetRcdCalculator().computeResourceDiff(daemonSetYaml("750m", "1300Mi"), ""))
        .isEqualTo(ResourceClaim.builder().cpuNano(-750000000L).memBytes(-1363148800L).build());
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldHandleUpdate() throws Exception {
    assertThat(new DaemonSetRcdCalculator().computeResourceDiff(
                   daemonSetYaml("120m", "1200Mi"), daemonSetYaml("100m", "1210Mi")))
        .isEqualTo(ResourceClaim.builder().cpuNano(-20000000L).memBytes(10485760L).build());
  }
  private String daemonSetYaml(String cpu, String memory) {
    return Yaml.dump(
        new V1DaemonSetBuilder()
            .withNewSpec()
            .withNewTemplate()
            .withNewSpec()
            .addNewContainer()
            .withNewResources()
            .withRequests(ImmutableMap.of("cpu", Quantity.fromString(cpu), "memory", Quantity.fromString(memory)))
            .endResources()
            .endContainer()
            .endSpec()
            .endTemplate()
            .endSpec()
            .build());
  }
}
