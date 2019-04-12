package io.harness.checks;

import com.puppycrawl.tools.checkstyle.AbstractModuleTestSupport;
import com.puppycrawl.tools.checkstyle.DefaultConfiguration;
import com.puppycrawl.tools.checkstyle.TreeWalker;
import org.junit.Test;

public class AnnotationParametersCheckTest extends AbstractModuleTestSupport {
  @Override
  protected String getPackageLocation() {
    return "io.harness.checks";
  }

  public DefaultConfiguration config() {
    DefaultConfiguration config = createModuleConfig(AnnotationParametersCheck.class);

    DefaultConfiguration twConf = createModuleConfig(TreeWalker.class);
    twConf.addChild(config);
    twConf.addAttribute("fileExtensions", "jv");

    return twConf;
  }

  @Test
  public void testIssues() throws Exception {
    final String[] expected = {"8:1: The annotation FieldNameConstants is missing required parameter innerTypeName."};

    verify(config(), getPath("AnnotationParametersCheckIssues.jv"), expected);
  }

  @Test
  public void testFalsePositive() throws Exception {
    final String[] expected = {};
    verify(config(), getPath("AnnotationParametersCheckNonIssues.jv"), expected);
  }
}