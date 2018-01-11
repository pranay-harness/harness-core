package io.harness.checks;

import com.puppycrawl.tools.checkstyle.AbstractModuleTestSupport;
import com.puppycrawl.tools.checkstyle.DefaultConfiguration;
import com.puppycrawl.tools.checkstyle.TreeWalker;
import org.junit.Test;

public class UseIsEmptyCheckTest extends AbstractModuleTestSupport {
  @Override
  protected String getPackageLocation() {
    return "io.harness.checks";
  }

  public DefaultConfiguration config() {
    DefaultConfiguration config = createModuleConfig(UseIsEmptyCheck.class);

    DefaultConfiguration twConf = createModuleConfig(TreeWalker.class);
    twConf.addChild(config);
    twConf.addAttribute("fileExtensions", "jv");

    return twConf;
  }

  @Test
  public void testNullOrIsEmptyDetectIssues() throws Exception {
    final String[] expected = {"7:22: Use isEmpty utility method instead.", "9:22: Use isEmpty utility method instead.",
        "12:22: Use isNotEmpty utility method instead.", "14:22: Use isNotEmpty utility method instead.",
        "16:22: Use isNotEmpty utility method instead.", "18:22: Use isNotEmpty utility method instead."};

    verify(config(), getPath("UseIsEmptyCheckIssues.jv"), expected);
  }

  @Test
  public void testSizeDetectIssues() throws Exception {
    final String[] expected = {"7:21: Use isEmpty instead.", "9:11: Use isEmpty instead.",
        "11:37: Use isEmpty instead.", "13:27: Use isEmpty instead."};

    verify(config(), getPath("UseIsEmptyCheckSizeIssues.jv"), expected);
  }

  @Test
  public void testFalsePositive() throws Exception {
    final String[] expected = {};
    verify(config(), getPath("UseIsEmptyCheckNonIssues.jv"), expected);
  }
}