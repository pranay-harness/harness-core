package software.wings.helpers.ext.kustomize;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;

public class KustomizeConfigTest extends WingsBaseTest {
  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void cloneFromNull() {
    KustomizeConfig sourceConfig = null;
    KustomizeConfig destConfig = KustomizeConfig.cloneFrom(sourceConfig);

    assertThat(destConfig).isNull();
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void cloneFromValidConfig() {
    KustomizeConfig sourceConfig = KustomizeConfig.builder().pluginRootDir("/home/wings/").build();
    KustomizeConfig destConfig = KustomizeConfig.cloneFrom(sourceConfig);

    assertThat(sourceConfig != destConfig).isTrue();
    assertThat(destConfig).isEqualTo(sourceConfig);
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void kustomizeDirPathShouldNotBeNull() {
    KustomizeConfig config = new KustomizeConfig();
    assertThat(config.getKustomizeDirPath()).isNotNull();
    assertThat(config.getKustomizeDirPath()).isEmpty();

    config = KustomizeConfig.builder().build();
    assertThat(config.getKustomizeDirPath()).isNotNull();
    assertThat(config.getKustomizeDirPath()).isEmpty();
  }
}