package io.harness.checks;

import com.puppycrawl.tools.checkstyle.AbstractModuleTestSupport;
import com.puppycrawl.tools.checkstyle.DefaultConfiguration;
import org.junit.Test;

public class UseIsEmptyCheckTest extends AbstractModuleTestSupport {
  @Override
  protected String getPackageLocation() {
    return "io.harness.checks";
  }

  @Test
  public void testNullOrIsEmptyDetectIssues() throws Exception {
    final DefaultConfiguration checkConfig = createModuleConfig(UseIsEmptyCheck.class);

    final String[] expected1 = {"7:22: Use isEmpty utility method instead.",
        "9:22: Use isEmpty utility method instead.", "12:22: Use isNotEmpty utility method instead.",
        "14:22: Use isNotEmpty utility method instead.", "16:22: Use isNotEmpty utility method instead.",
        "18:22: Use isNotEmpty utility method instead."};

    verify(checkConfig, getPath("UseIsEmptyCheckIssues.java"), expected1);
  }

  @Test
  public void testSizeDetectIssues() throws Exception {
    final DefaultConfiguration checkConfig = createModuleConfig(UseIsEmptyCheck.class);

    final String[] expected1 = {"7:21: Use isEmpty instead.", "9:11: Use isEmpty instead.",
        "11:37: Use isEmpty instead.", "13:27: Use isEmpty instead."};

    verify(checkConfig, getPath("UseIsEmptyCheckSizeIssues.java"), expected1);
  }

  @Test
  public void testFalsePositive() throws Exception {
    final DefaultConfiguration checkConfig = createModuleConfig(UseIsEmptyCheck.class);
    final String[] expected1 = {};
    verify(checkConfig, getPath("UseIsEmptyCheckNonIssues.java"), expected1);
  }
}