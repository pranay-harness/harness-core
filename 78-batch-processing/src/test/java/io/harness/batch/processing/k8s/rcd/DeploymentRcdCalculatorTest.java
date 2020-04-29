package io.harness.batch.processing.k8s.rcd;

import static io.harness.rule.OwnerRule.AVMOHAN;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.openapi.models.V1DeploymentBuilder;
import io.kubernetes.client.util.Yaml;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class DeploymentRcdCalculatorTest extends CategoryTest {
  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldHandleAdd() throws Exception {
    assertThat(new DeploymentRcdCalculator().computeResourceDiff("", deploymentYaml("100m", "1200Mi", 2)))
        .isEqualTo(ResourceClaim.builder().cpuNano(200000000L).memBytes(2516582400L).build());
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldHandleDelete() throws Exception {
    assertThat(new DeploymentRcdCalculator().computeResourceDiff(deploymentYaml("750m", "1300Mi", 3), ""))
        .isEqualTo(ResourceClaim.builder().cpuNano(-2250000000L).memBytes(-4089446400L).build());
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldHandleUpdate() throws Exception {
    assertThat(new DeploymentRcdCalculator().computeResourceDiff(
                   deploymentYaml("100m", "1200Mi", 2), deploymentYaml("300m", "1.5G", 3)))
        .isEqualTo(ResourceClaim.builder().cpuNano(700000000).memBytes(1983417600L).build());
  }
  private String deploymentYaml(String cpu, String memory, int replicas) {
    return Yaml.dump(new V1DeploymentBuilder()
                         .withNewSpec()
                         .withReplicas(replicas)
                         .withNewTemplate()
                         .withNewSpec()
                         .addNewContainer()
                         .withNewResources()
                         .addToRequests("cpu", Quantity.fromString(cpu))
                         .addToRequests("memory", Quantity.fromString(memory))
                         .endResources()
                         .endContainer()
                         .endSpec()
                         .endTemplate()
                         .endSpec()
                         .build());
  }
}
