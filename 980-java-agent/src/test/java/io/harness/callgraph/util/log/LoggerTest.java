package io.harness.callgraph.util.log;

import static io.harness.rule.OwnerRule.SHIVAKUMAR;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import io.harness.callgraph.helper.Console;
import io.harness.callgraph.util.config.ConfigUtils;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import java.io.IOException;
import java.util.regex.Pattern;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

public class LoggerTest {
  @Rule public TemporaryFolder tmp = new TemporaryFolder();

  private Logger init(int logLevel, boolean stdOut) {
    try {
      ConfigUtils.replace(true, "logLevel: " + logLevel, "logConsole: " + stdOut, "outDir: /tmp/");
    } catch (IOException e) {
      fail("Error initializing config");
    }
    Logger.init();
    return new Logger(LoggerTest.class);
  }

  @Test
  @Owner(developers = SHIVAKUMAR)
  @Category(UnitTests.class)
  public void testInit() {
    init(0, false);
    assertThat(Logger.TARGETS.isEmpty()).isTrue();

    init(1, false);
    assertThat(Logger.TARGETS.size()).isEqualTo(1);
    assertThat(Logger.TARGETS.get(0)).isInstanceOf(FileTarget.class);

    init(1, true);
    assertThat(Logger.TARGETS.size()).isEqualTo(2);
    assertThat(Logger.TARGETS.get(0)).isInstanceOf(FileTarget.class);
    assertThat(Logger.TARGETS.get(1)).isInstanceOf(ConsoleTarget.class);

    Logger logger = init(1, false);
    assertThat(logger.prefix).isEqualTo("[LoggerTest]");
  }

  @Test
  @Owner(developers = SHIVAKUMAR)
  @Category(UnitTests.class)
  public void testTrace() {
    Logger logger = Mockito.spy(init(6, false));
    logger.trace("test");
    Mockito.verify(logger).trace("test");
    Mockito.verify(logger).log(6, "test");
    Mockito.verifyNoMoreInteractions(logger);

    logger = Mockito.spy(init(5, false));
    logger.trace("test");
    Mockito.verify(logger).trace("test");
    Mockito.verifyNoMoreInteractions(logger);
  }

  @Test
  @Owner(developers = SHIVAKUMAR)
  @Category(UnitTests.class)
  public void testDebug() {
    Logger logger = Mockito.spy(init(5, false));
    logger.debug("test");
    Mockito.verify(logger).debug("test");
    Mockito.verify(logger).log(5, "test");
    Mockito.verifyNoMoreInteractions(logger);

    logger = Mockito.spy(init(4, false));
    logger.debug("test");
    Mockito.verify(logger).debug("test");
    Mockito.verifyNoMoreInteractions(logger);
  }

  @Test
  @Owner(developers = SHIVAKUMAR)
  @Category(UnitTests.class)
  public void testInfo() {
    Logger logger = Mockito.spy(init(4, false));
    logger.info("test");
    Mockito.verify(logger).info("test");
    Mockito.verify(logger).log(4, "test");
    Mockito.verifyNoMoreInteractions(logger);

    logger = Mockito.spy(init(3, false));
    logger.info("test");
    Mockito.verify(logger).info("test");
    Mockito.verifyNoMoreInteractions(logger);
  }

  @Test
  @Owner(developers = SHIVAKUMAR)
  @Category(UnitTests.class)
  public void testWarn() {
    Logger logger = Mockito.spy(init(3, false));
    logger.warn("test");
    Mockito.verify(logger).warn("test");
    Mockito.verify(logger).log(3, "test");
    Mockito.verifyNoMoreInteractions(logger);

    logger = Mockito.spy(init(2, false));
    logger.warn("test");
    Mockito.verify(logger).warn("test");
    Mockito.verifyNoMoreInteractions(logger);
  }

  @Test
  @Owner(developers = SHIVAKUMAR)
  @Category(UnitTests.class)
  public void testError() {
    Logger logger = Mockito.spy(init(2, false));
    logger.error("test");
    Mockito.verify(logger).error("test");
    Mockito.verify(logger).logE(2, "test");
    Mockito.verify(logger).log(2, "test");
    Mockito.verifyNoMoreInteractions(logger);

    logger = Mockito.spy(init(1, false));
    logger.error("test");
    Mockito.verify(logger).error("test");
    Mockito.verifyNoMoreInteractions(logger);
  }

  @Test
  @Owner(developers = SHIVAKUMAR)
  @Category(UnitTests.class)
  public void testFatal() {
    Logger logger = Mockito.spy(init(1, false));
    logger.fatal("test");
    Mockito.verify(logger).fatal("test");
    Mockito.verify(logger).logE(1, "test");
    Mockito.verify(logger).log(1, "test");
    Mockito.verifyNoMoreInteractions(logger);

    logger = Mockito.spy(init(0, false));
    logger.fatal("test");
    Mockito.verify(logger).fatal("test");
    Mockito.verifyNoMoreInteractions(logger);
  }

  @Test
  @Owner(developers = SHIVAKUMAR)
  @Category(UnitTests.class)
  public void testException() throws IOException {
    Logger logger = init(6, false);
    Logger.TARGETS.clear();
    LogTarget target = Mockito.mock(LogTarget.class);
    Logger.TARGETS.add(target);

    Exception e = new RuntimeException();
    logger.error("test", e);

    Mockito.verify(target).printTrace(e, 2);
    Mockito.verify(target, Mockito.times(2)).print(Mockito.anyString(), Mockito.eq(2));
    Mockito.verify(target, Mockito.times(2)).flush();
    Mockito.verifyNoMoreInteractions(target);
  }

  @Test
  @Owner(developers = SHIVAKUMAR)
  @Category(UnitTests.class)
  public void testReplacement() throws IOException {
    Logger logger = init(6, false);
    Logger.TARGETS.clear();
    LogTarget target = Mockito.mock(LogTarget.class);
    Logger.TARGETS.add(target);

    String arg = "123";
    logger.debug("test {}", arg);
    Mockito.verify(target).print(Mockito.matches("\\[.*] \\[DEBUG] \\[LoggerTest] test 123\n"), Mockito.eq(5));

    arg = null;
    logger.debug("test {}", arg);
    Mockito.verify(target).print(Mockito.matches("\\[.*] \\[DEBUG] \\[LoggerTest] test null\n"), Mockito.eq(5));
  }

  @Test
  @Owner(developers = SHIVAKUMAR)
  @Category(UnitTests.class)
  public void testWrongArgumentCount() {
    Logger logger = init(6, false);
    try {
      logger.debug("test {}", "123", "456");
      fail("Should notice too less {}");
    } catch (IllegalArgumentException e) {
    }

    try {
      logger.debug("test {} {}");
      fail("Should notice missing arguments");
    } catch (IllegalArgumentException e) {
    }
  }

  @Test
  @Owner(developers = SHIVAKUMAR)
  @Category(UnitTests.class)
  public void testTargetError() throws IOException {
    Logger logger = init(6, false);
    Logger.TARGETS.clear();
    LogTarget target = Mockito.mock(LogTarget.class);
    Mockito.doThrow(new IOException("error")).when(target).print(Mockito.anyString(), Mockito.anyInt());
    Logger.TARGETS.add(target);

    Console console = new Console();
    console.startCapture();
    logger.debug("test");
    logger.error("test", new RuntimeException());
    assertThat(console.getOut()).isEmpty();
    String pattern = "Error in logger: error\njava.io.IOException: error\n.*";
    pattern = pattern + pattern + pattern;
    assertThat(console.getErr()).matches(Pattern.compile(pattern, Pattern.DOTALL));

    console.reset();
  }
}
