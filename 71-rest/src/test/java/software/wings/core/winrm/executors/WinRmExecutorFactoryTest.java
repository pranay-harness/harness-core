package software.wings.core.winrm.executors;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;

public class WinRmExecutorFactoryTest {
  @Mock WinRmExecutorFactory winRmExecutorFactory = new WinRmExecutorFactory();
  @Mock WinRmSessionConfig winRmSessionConfig;

  @Test
  @Category(UnitTests.class)
  public void shouldGetWinRmExecutor() {
    assertThat(winRmExecutorFactory.getExecutor(winRmSessionConfig)).isNotNull().isInstanceOf(WinRmExecutor.class);
  }
}
