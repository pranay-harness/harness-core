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
  public void testDetectIssues() throws Exception {
    final DefaultConfiguration checkConfig = createModuleConfig(UseIsEmptyCheck.class);

    final String[] expected1 = {
        "7:22: Use isEmpty utility method instead.", "9:22: Use isEmpty utility method instead."};

    verify(checkConfig, getPath("UseIsEmptyCheckIssues.java"), expected1);
  }

  @Test
  public void testFalsePositive() throws Exception {
    final DefaultConfiguration checkConfig = createModuleConfig(UseIsEmptyCheck.class);
    final String[] expected1 = {};
    verify(checkConfig, getPath("UseIsEmptyCheckNonIssues.java"), expected1);
  }
}