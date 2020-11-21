package io.harness.delegate.beans.executioncapability;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import software.wings.WingsBaseTest;
import software.wings.helpers.ext.kustomize.KustomizeConfig;

import org.junit.Test;
import org.junit.experimental.categories.Category;

public class KustomizeCapabilityTest extends WingsBaseTest {
  private KustomizeCapability capability =
      KustomizeCapability.builder()
          .kustomizeConfig(new KustomizeConfig("/home/kustomize_plugins/", "examples/"))
          .build();

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void getCapabilityType() {
    assertThat(capability.getCapabilityType()).isEqualTo(CapabilityType.KUSTOMIZE);
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void fetchCapabilityBasis() {
    assertThat(capability.fetchCapabilityBasis()).isEqualTo("kustomizePluginDir:/home/kustomize_plugins/");
  }
}
