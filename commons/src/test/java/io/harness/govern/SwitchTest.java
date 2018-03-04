package io.harness.govern;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.matches;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.slf4j.Logger;

public class SwitchTest {
  @Test
  public void unhandled() {
    Logger mockLogger = mock(Logger.class);
    Switch.setLogger(mockLogger);

    int a = 5;
    Switch.unhandled(a);

    verify(mockLogger, times(1))
        .error(matches("Unhandled switch value \\{\\}: \\{\\}\n\\{\\}"), matches("java.lang.Integer"), anyInt(),
            any(String.class));
  }
}
