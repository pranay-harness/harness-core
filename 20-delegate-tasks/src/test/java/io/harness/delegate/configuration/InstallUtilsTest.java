package io.harness.delegate.configuration;

import static io.harness.delegate.configuration.InstallUtils.helm2Version;
import static io.harness.delegate.configuration.InstallUtils.helm3Version;
import static io.harness.rule.OwnerRule.ANSHUL;
import static io.harness.rule.OwnerRule.AVMOHAN;
import static io.harness.rule.OwnerRule.RIHAZ;
import static io.harness.rule.OwnerRule.VAIBHAV_SI;
import static io.harness.rule.OwnerRule.YOGESH;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.MockableTestMixin;
import io.harness.category.element.FunctionalTests;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

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
  @Ignore("Disable this test until the oc binary is on QA proxy also")
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

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(FunctionalTests.class)
  public void shouldInstallKustomize() throws IOException, IllegalAccessException {
    setStaticFieldValue(SystemUtils.class, "IS_OS_WINDOWS", false);
    FileUtils.deleteDirectory(new File("./client-tools/kustomize/"));
    assertThat(InstallUtils.installKustomize(delegateConfiguration)).isTrue();

    // Won't download this time
    assertThat(InstallUtils.installKustomize(delegateConfiguration)).isTrue();
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void shouldNotInstallWhenPathSetInDelegateConfig() {
    DelegateConfiguration delegateConfiguration = DelegateConfiguration.builder().kustomizePath("RANDOM").build();
    assertThat(InstallUtils.installKustomize(delegateConfiguration)).isTrue();
  }

  @Before
  public void setup() throws Exception {
    setStaticFieldValue(InstallUtils.class, "helmPaths", new HashMap() {
      {
        put(helm2Version, "helm");
        put(helm3Version, "helm");
      }
    });
  }

  @Test
  @Owner(developers = RIHAZ)
  @Category(UnitTests.class)
  public void testHelm2Install() {
    DelegateConfiguration helm2DelegateConfiguration = DelegateConfiguration.builder().helmPath("helm2Path").build();

    assertThat(InstallUtils.delegateConfigHasHelmPath(helm2DelegateConfiguration, helm2Version)).isTrue();
    assertThat(InstallUtils.getHelm2Path()).isEqualTo("helm2Path");
    assertThat(InstallUtils.getHelm3Path()).isEqualTo("helm");
  }

  @Test
  @Owner(developers = RIHAZ)
  @Category(UnitTests.class)
  public void testHelm3Install() {
    DelegateConfiguration helm3DelegateConfiguration = DelegateConfiguration.builder().helm3Path("helm3Path").build();

    assertThat(InstallUtils.delegateConfigHasHelmPath(helm3DelegateConfiguration, helm3Version)).isTrue();
    assertThat(InstallUtils.getHelm3Path()).isEqualTo("helm3Path");
    assertThat(InstallUtils.getHelm2Path()).isEqualTo("helm");
  }
}