package io.harness.delegate.configuration;

import static io.harness.rule.OwnerRule.AVMOHAN;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.MockableTestMixin;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.apache.commons.lang3.SystemUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class InstallUtilsTest extends CategoryTest implements MockableTestMixin {
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
}