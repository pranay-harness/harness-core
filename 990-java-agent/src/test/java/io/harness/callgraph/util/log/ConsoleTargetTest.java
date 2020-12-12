package io.harness.callgraph.util.log;

import static io.harness.rule.OwnerRule.SHIVAKUMAR;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.callgraph.helper.Console;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mockito;

public class ConsoleTargetTest {
  private static final String TEST = "test#+*'!§$%&/()=?\n@@€";

  @Test
  @Owner(developers = SHIVAKUMAR)
  @Category(UnitTests.class)
  public void testPrint() throws UnsupportedEncodingException {
    Console console = new Console();
    ConsoleTarget target = new ConsoleTarget();

    console.startCapture();
    target.print(TEST, 3);
    assertThat(console.getOut()).isEqualTo(TEST);
    assertThat(console.getErr()).isEmpty();

    console.clear();
    target.print(TEST, 2);
    assertThat(console.getErr()).isEqualTo(TEST);
    assertThat(console.getOut()).isEmpty();

    console.reset();
  }

  @Test
  @Owner(developers = SHIVAKUMAR)
  @Category(UnitTests.class)
  public void testPrintTrace() throws IOException {
    Throwable e = Mockito.mock(Throwable.class);
    ConsoleTarget target = new ConsoleTarget();

    target.printTrace(e, 3);
    Mockito.verify(e).printStackTrace(System.out);

    target.printTrace(e, 2);
    Mockito.verify(e).printStackTrace(System.err);
    Mockito.verifyNoMoreInteractions(e);
  }

  @Test
  @Owner(developers = SHIVAKUMAR)
  @Category(UnitTests.class)
  public void testFlush() throws IOException {
    ConsoleTarget target = new ConsoleTarget();
    target.flush();
  }
}
