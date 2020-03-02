package software.wings.beans.appmanifest;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.beans.GitFileConfig;
import software.wings.helpers.ext.kustomize.KustomizeConfig;

public class ApplicationManifestTest extends WingsBaseTest {
  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void cloneInternalForBareBoneAppManifest() {
    ApplicationManifest sourceManifest = buildBareBoneAppManifest();
    ApplicationManifest destManifest = sourceManifest.cloneInternal();
    verifyCloning(sourceManifest, destManifest);
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void cloneInternalForKustomizeManifest() {
    ApplicationManifest sourceManifest = buildBareBoneAppManifest();
    sourceManifest.setKustomizeConfig(
        KustomizeConfig.builder().pluginRootDir("./foo").kustomizeDirPath("./home").build());
    sourceManifest.setGitFileConfig(
        GitFileConfig.builder().connectorId("connectedId").branch("master").useBranch(true).build());
    ApplicationManifest destManifest = sourceManifest.cloneInternal();
    verifyCloning(sourceManifest, destManifest);
  }

  private void verifyCloning(ApplicationManifest sourceManifest, ApplicationManifest destManifest) {
    assertThat(sourceManifest != destManifest).isTrue();
    assertThat(sourceManifest.getKustomizeConfig() == null
        || sourceManifest.getKustomizeConfig() != destManifest.getKustomizeConfig())
        .isTrue();
    assertThat(sourceManifest).isEqualTo(destManifest);
  }

  private ApplicationManifest buildBareBoneAppManifest() {
    return ApplicationManifest.builder()
        .storeType(StoreType.KustomizeSourceRepo)
        .kind(AppManifestKind.K8S_MANIFEST)
        .serviceId("serviceId")
        .build();
  }
}