package io.harness.checks;

import com.puppycrawl.tools.checkstyle.AbstractModuleTestSupport;
import com.puppycrawl.tools.checkstyle.DefaultConfiguration;
import com.puppycrawl.tools.checkstyle.TreeWalker;
import org.junit.Test;

public class ExclusiveStaticImportCheckTest extends AbstractModuleTestSupport {
  @Override
  protected String getPackageLocation() {
    return "io.harness.checks";
  }

  public DefaultConfiguration config() {
    DefaultConfiguration config = createModuleConfig(ExclusiveStaticImportCheck.class);

    config.addAttribute("staticImports", "java.util.Arrays.asList");

    DefaultConfiguration twConf = createModuleConfig(TreeWalker.class);
    twConf.addChild(config);
    twConf.addAttribute("fileExtensions", "jv");

    return twConf;
  }

  @Test
  public void testNonStaticLoggerIssues() throws Exception {
    final String[] expected = {"3:1: Use asList method from package java.util.Arrays."};
    verify(config(), getPath("ExclusiveStaticImportIssues.jv"), expected);
  }

  @Test
  public void testFalsePositive() throws Exception {
    final String[] expected = {};
    verify(config(), getPath("ExclusiveStaticImportNonIssues.jv"), expected);
  }
}
