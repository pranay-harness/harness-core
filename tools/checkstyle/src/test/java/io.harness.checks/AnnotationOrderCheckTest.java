package io.harness.checks;

import com.puppycrawl.tools.checkstyle.AbstractModuleTestSupport;
import com.puppycrawl.tools.checkstyle.DefaultConfiguration;
import com.puppycrawl.tools.checkstyle.TreeWalker;
import org.junit.Test;

public class AnnotationOrderCheckTest extends AbstractModuleTestSupport {
  @Override
  protected String getPackageLocation() {
    return "io.harness.checks";
  }

  public DefaultConfiguration config() {
    DefaultConfiguration config = createModuleConfig(AnnotationOrderCheck.class);

    DefaultConfiguration twConf = createModuleConfig(TreeWalker.class);
    twConf.addChild(config);
    twConf.addAttribute("fileExtensions", "jv");

    return twConf;
  }

  @Test
  public void testIssues() throws Exception {
    final String[] expected = {"7:1: Annotation Value must be placed before annotation Builder",
        "12:1: Annotation Data must be placed before annotation Builder",
        "17:1: Annotation Data must be specified before any modifier"};

    verify(config(), getPath("AnnotationOrderCheckIssues.jv"), expected);
  }

  @Test
  public void testFalsePositive() throws Exception {
    //    final String[] expected = {};
    //    verify(config(), getPath("WingsExceptionCheckNonIssues.jv"), expected);
  }
}