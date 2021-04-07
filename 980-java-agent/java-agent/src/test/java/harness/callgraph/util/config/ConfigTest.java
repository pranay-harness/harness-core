package harness.callgraph.util.config;

import static io.harness.rule.OwnerRule.SHIVAKUMAR;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import harness.callgraph.util.Target;
import java.io.IOException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;

public class ConfigTest {
  @Rule public TemporaryFolder tmp = new TemporaryFolder();

  @Test
  @Owner(developers = SHIVAKUMAR)
  @Category(UnitTests.class)
  public void testLoad() throws IOException {
    ConfigUtils.replace(true);
    assertThat(Config.getInst()).isNotEqualTo(null);
  }

  @Test
  @Owner(developers = SHIVAKUMAR)
  @Category(UnitTests.class)
  public void testInvalidConfig() throws IOException {
    try {
      ConfigUtils.replace(true, "outDir test");
      fail("Line without : not detected");
    } catch (IllegalArgumentException e) {
    }
    try {
      ConfigUtils.replace(true, "asdf: 123");
      fail("Invalid option not detected");
    } catch (IllegalArgumentException e) {
    }
  }

  @Test
  @Owner(developers = SHIVAKUMAR)
  @Category(UnitTests.class)
  public void testIgnoreWhitespace() throws IOException {
    ConfigUtils.replace(true, "", "");

    ConfigUtils.replace(true, "outDir: abc/", "#outDir: xyz");
    assertThat(Config.getInst().outDir()).isEqualTo("abc/");

    ConfigUtils.replace(true, "      \t    outDir:    abc/    ", " \t ");
    assertThat(Config.getInst().outDir()).isEqualTo("abc/");
  }

  @Test
  @Owner(developers = SHIVAKUMAR)
  @Category(UnitTests.class)
  public void testOutDir() throws IOException {
    ConfigUtils.replace(true, "outDir: abc/");
    assertThat(Config.getInst().outDir()).isEqualTo("abc/");

    try {
      ConfigUtils.replace(true, "outDir: abc");
      fail("Did not detect missing trailing slash");
    } catch (IllegalArgumentException e) {
    }

    ConfigUtils.replace(true);
    assertThat(Config.getInst().outDir()).isEqualTo("/tmp/callgraph/portal/");
  }

  @Test
  @Owner(developers = SHIVAKUMAR)
  @Category(UnitTests.class)
  public void testLogLevel() throws IOException {
    ConfigUtils.replace(true, "logLevel: 4");
    assertThat(Config.getInst().logLevel()).isEqualTo(4);

    try {
      ConfigUtils.replace(true, "logLevel: -1");
      fail("Did not detect negative log level");
    } catch (IllegalArgumentException e) {
    }
    try {
      ConfigUtils.replace(true, "logLevel: 7");
      fail("Did not detect too high loglevel");
    } catch (IllegalArgumentException e) {
    }
  }

  @Test
  @Owner(developers = SHIVAKUMAR)
  @Category(UnitTests.class)
  public void testLogConsole() throws IOException {
    ConfigUtils.replace(true, "logConsole: true");
    assertThat(Config.getInst().logConsole()).isTrue();

    ConfigUtils.replace(true, "logConsole: false");
    assertThat(Config.getInst().logConsole()).isFalse();
  }

  @Test
  @Owner(developers = SHIVAKUMAR)
  @Category(UnitTests.class)
  public void testWriteTo() throws IOException {
    for (Target t : Target.values()) {
      ConfigUtils.replace(true, "writeTo: " + t.name());
      assertThat(Config.getInst().writeTo()[0]).isEqualTo(t);
    }

    ConfigUtils.replace(true, "writeTo: " + Target.COVERAGE_JSON + "," + Target.GRAPH_DB_CSV);
    assertThat(Config.getInst().writeTo()[0]).isEqualTo(Target.COVERAGE_JSON);
    assertThat(Config.getInst().writeTo()[1]).isEqualTo(Target.GRAPH_DB_CSV);
  }

  @Test
  @Owner(developers = SHIVAKUMAR)
  @Category(UnitTests.class)
  public void testInstrPackages() throws IOException {
    String[] pkgs = {"io.harness.", "software.wings.", "migrations."};
    for (String pkg : pkgs) {
      ConfigUtils.replace(true, "instrPackages: " + pkg);
      assertThat(Config.getInst().instrPackages()[0]).isEqualTo(pkg);
    }

    ConfigUtils.replace(true, "instrPackages: " + pkgs[0] + "," + pkgs[1] + "," + pkgs[2]);
    for (int i = 0; i < pkgs.length; i++) {
      assertThat(Config.getInst().instrPackages()[i]).isEqualTo(pkgs[i]);
    }
  }

  @Test
  @Owner(developers = SHIVAKUMAR)
  @Category(UnitTests.class)
  public void testTestAnnotations() throws IOException {
    String[] annotations = {"io.harness.Test1", "io.harness.Test2", "io.harness.Test3"};
    for (String annotation : annotations) {
      ConfigUtils.replace(true, "testAnnotations: " + annotation);
      assertThat(Config.getInst().testAnnotations()[0]).isEqualTo(annotation);
    }

    ConfigUtils.replace(true, "testAnnotations: " + annotations[0] + "," + annotations[1] + "," + annotations[2]);
    for (int i = 0; i < annotations.length; i++) {
      assertThat(Config.getInst().testAnnotations()[i]).isEqualTo(annotations[i]);
    }
  }
}
