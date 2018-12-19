package software.wings.core.winrm.executors;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.mockito.Mock;

public class WinRmExecutorFactoryTest {
  @Mock WinRmExecutorFactory winRmExecutorFactory = new WinRmExecutorFactory();
  @Mock WinRmSessionConfig winRmSessionConfig;

  @Test
  public void shouldGetWinRmExecutor() {
    assertThat(winRmExecutorFactory.getExecutor(winRmSessionConfig)).isNotNull().isInstanceOf(WinRmExecutor.class);
  }
}
