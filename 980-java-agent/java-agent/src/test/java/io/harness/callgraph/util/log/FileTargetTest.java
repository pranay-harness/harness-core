package io.harness.callgraph.util.log;

import static io.harness.rule.OwnerRule.SHIVAKUMAR;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import junit.framework.AssertionFailedError;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;

public class FileTargetTest {
  private static final String TEST = "test#+*'!§$%&/()=?\n@@€";

  @Rule public TemporaryFolder tmp = new TemporaryFolder();

  private FileTarget reset(int level) {
    try {
      FileTarget.debug = null;
      FileTarget.error = null;

      File file = new File(tmp.getRoot(), "error.log");
      if (file.exists()) {
        file.delete();
      }
      file = new File(tmp.getRoot(), "debug.log");
      if (file.exists()) {
        file.delete();
      }

      return new FileTarget(tmp.getRoot().getCanonicalPath() + "/", level);
    } catch (IOException e) {
      throw new AssertionFailedError("Error creating new FileTarget: " + e.getMessage());
    }
  }

  private String readErr() throws IOException {
    final File file = new File(tmp.getRoot(), "error.log");
    if (!file.exists()) {
      return null;
    }
    byte[] encoded = Files.readAllBytes(file.toPath());
    return new String(encoded, Charset.defaultCharset());
  }

  private String readOut() throws IOException {
    final File file = new File(tmp.getRoot(), "debug.log");
    if (!file.exists()) {
      return null;
    }
    byte[] encoded = Files.readAllBytes(file.toPath());
    return new String(encoded, Charset.defaultCharset());
  }

  @Test
  @Owner(developers = SHIVAKUMAR)
  @Category(UnitTests.class)
  public void testConstructor() {
    reset(0);
    assertThat(FileTarget.debug).isNull();
    assertThat(FileTarget.error).isNull();

    reset(1);
    assertThat(FileTarget.debug).isNull();
    assertThat(FileTarget.error).isNotNull();

    reset(3);
    assertThat(FileTarget.debug).isNotNull();
    assertThat(FileTarget.error).isNotNull();
  }

  @Test
  @Owner(developers = SHIVAKUMAR)
  @Category(UnitTests.class)
  public void testPrint() throws IOException {
    FileTarget target = reset(0);
    target.print(TEST, 5);
    target.flush();
    assertThat(readErr()).isNull();
    assertThat(readOut()).isNull();

    target = reset(1);
    target.print(TEST, 1);
    target.flush();
    assertThat(readErr()).isEqualTo(TEST);
    assertThat(readOut()).isNull();

    target = reset(1);
    target.print(TEST, 3);
    target.flush();
    assertThat(readErr()).isEmpty();
    assertThat(readOut()).isNull();

    target = reset(3);
    target.print(TEST, 1);
    target.flush();
    assertThat(readErr()).isEqualTo(TEST);
    assertThat(readOut()).isEqualTo(TEST);

    target = reset(3);
    target.print(TEST, 3);
    target.flush();
    assertThat(readErr()).isEmpty();
    assertThat(readOut()).isEqualTo(TEST);
  }

  @Test
  @Owner(developers = SHIVAKUMAR)
  @Category(UnitTests.class)
  public void testPrintTrace() throws IOException {
    Throwable e = new Throwable() {
      @Override
      public void printStackTrace(PrintWriter writer) {
        writer.write(TEST);
        writer.flush();
      }
    };

    // No logging when switched off
    FileTarget target = reset(0);
    target.printTrace(e, 3);
    assertThat(readErr()).isNull();
    assertThat(readOut()).isNull();

    // No logging to err when level = 3
    target = reset(1);
    target.printTrace(e, 3);
    assertThat(readErr()).isEmpty();
    assertThat(readOut()).isNull();

    // Only logging to err if level = 2
    target = reset(1);
    target.printTrace(e, 2);
    assertThat(readErr()).isEqualTo(TEST);
    assertThat(readOut()).isNull();

    // Logging to err and out
    target = reset(3);
    target.printTrace(e, 2);
    assertThat(readErr()).isEqualTo(TEST);
    assertThat(readOut()).isEqualTo(TEST);

    // Only logging to out
    target = reset(3);
    target.printTrace(e, 3);
    assertThat(readErr()).isEmpty();
    assertThat(readOut()).isEqualTo(TEST);
  }
}
