package io.harness.delegate.configuration;

import static io.harness.rule.OwnerRule.ANSHUL;
import static io.harness.rule.OwnerRule.AVMOHAN;
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
}