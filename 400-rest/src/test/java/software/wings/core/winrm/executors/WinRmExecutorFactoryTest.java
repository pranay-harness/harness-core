package software.wings.core.winrm.executors;

import static io.harness.rule.OwnerRule.DINESH;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;

public class WinRmExecutorFactoryTest extends CategoryTest {
  @Mock WinRmExecutorFactory winRmExecutorFactory = new WinRmExecutorFactory();
  WinRmSessionConfig winRmSessionConfig;

  @Test
  @Owner(developers = DINESH)
  @Category(UnitTests.class)
  public void shouldGetWinRmExecutor() {
    winRmSessionConfig = WinRmSessionConfig.builder().build();
    assertThat(winRmExecutorFactory.getExecutor(winRmSessionConfig, false))
        .isNotNull()
        .isInstanceOf(WinRmExecutor.class);
  }
}
