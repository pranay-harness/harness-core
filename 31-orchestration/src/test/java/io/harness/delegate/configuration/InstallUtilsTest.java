package io.harness.delegate.configuration;

import static io.harness.rule.OwnerRule.ANSHUL;
import static io.harness.rule.OwnerRule.AVMOHAN;
import static io.harness.rule.OwnerRule.YOGESH;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.MockableTestMixin;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.apache.commons.lang3.SystemUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.File;

public class InstallUtilsTest extends CategoryTest implements MockableTestMixin {
  DelegateConfiguration delegateConfiguration =
      DelegateConfiguration.builder().managerUrl("localhost").maxCachedArtifacts(10).build();

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void testGetOsPathWindows() throws Exception {
    setStaticFieldValue(SystemUtils.class, "IS_OS_WINDOWS", true);
    assertThat(InstallUtils.getOsPath()).isEqualTo("windows");
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void testGetOsPathMac() throws Exception {
    setStaticFieldValue(SystemUtils.class, "IS_OS_WINDOWS", false);
    setStaticFieldValue(SystemUtils.class, "IS_OS_MAC", true);
    assertThat(InstallUtils.getOsPath()).isEqualTo("darwin");
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void testGetOsPathLinux() throws Exception {
    setStaticFieldValue(SystemUtils.class, "IS_OS_WINDOWS", false);
    setStaticFieldValue(SystemUtils.class, "IS_OS_MAC", false);
    assertThat(InstallUtils.getOsPath()).isEqualTo("linux");
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testGetOcPath() throws Exception {
    setStaticFieldValue(InstallUtils.class, "ocPath", "oc");
    assertThat(InstallUtils.getOcPath()).isEqualTo("oc");

    setStaticFieldValue(InstallUtils.class, "ocPath", "path_to_oc");
    assertThat(InstallUtils.getOcPath()).isEqualTo("path_to_oc");
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testInstallOc() throws Exception {
    setStaticFieldValue(SystemUtils.class, "IS_OS_WINDOWS", false);
    setStaticFieldValue(SystemUtils.class, "IS_OS_MAC", true);
    deleteOcDirectory();
    assertThat(InstallUtils.installOc(delegateConfiguration)).isFalse();

    setStaticFieldValue(SystemUtils.class, "IS_OS_WINDOWS", false);
    setStaticFieldValue(SystemUtils.class, "IS_OS_MAC", false);
    deleteOcDirectory();
    assertThat(InstallUtils.installOc(delegateConfiguration)).isTrue();

    deleteOcDirectory();
    assertThat(InstallUtils.installOc(delegateConfiguration)).isTrue();
    assertThat(InstallUtils.installOc(delegateConfiguration)).isTrue();

    deleteOcDirectory();
    setStaticFieldValue(SystemUtils.class, "IS_OS_WINDOWS", true);
    assertThat(InstallUtils.installOc(delegateConfiguration)).isTrue();

    deleteOcDirectory();
    delegateConfiguration.setOcPath("oc");
    assertThat(InstallUtils.installOc(delegateConfiguration)).isTrue();
  }

  private void deleteOcDirectory() throws Exception {
    File file = new File("./client-tools/oc/");
    if (file.exists()) {
      org.apache.commons.io.FileUtils.deleteDirectory(file);
    }
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testisHelmV2() {
    assertThat(InstallUtils.isHelmV2("V2")).isTrue();
    assertThat(InstallUtils.isHelmV2("v2")).isTrue();
    assertThat(InstallUtils.isHelmV2("V3")).isFalse();
    assertThat(InstallUtils.isHelmV2("v3")).isFalse();
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testisHelmV3() {
    assertThat(InstallUtils.isHelmV3("V3")).isTrue();
    assertThat(InstallUtils.isHelmV3("v3")).isTrue();
    assertThat(InstallUtils.isHelmV3("V2")).isFalse();
    assertThat(InstallUtils.isHelmV3("v2")).isFalse();
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testGetHelmPath() {
    assertThat(InstallUtils.getHelm2Path()).isNotNull();
    assertThat(InstallUtils.getHelm3Path()).isNotNull();
  }
}