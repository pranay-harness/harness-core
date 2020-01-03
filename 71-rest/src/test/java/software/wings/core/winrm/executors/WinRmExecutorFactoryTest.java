package software.wings.core.winrm.executors;

import static io.harness.rule.OwnerRule.UNKNOWN;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;

public class WinRmExecutorFactoryTest extends CategoryTest {
  @Mock WinRmExecutorFactory winRmExecutorFactory = new WinRmExecutorFactory();
  @Mock WinRmSessionConfig winRmSessionConfig;

  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void shouldGetWinRmExecutor() {
    assertThat(winRmExecutorFactory.getExecutor(winRmSessionConfig)).isNotNull().isInstanceOf(WinRmExecutor.class);
  }
}
