package software.wings.utils;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.data.structure.EmptyPredicate;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.utils.PowerShellScriptsLoader.PsScript;

import java.util.Map;

public class PowerShellScriptsLoaderTest extends CategoryTest {
  @Test
  @Category(UnitTests.class)
  public void smokeTest() {
    for (Map.Entry<PsScript, String> entry : PowerShellScriptsLoader.psScriptMap.entrySet()) {
      assertThat(EmptyPredicate.isEmpty(entry.getValue())).isFalse();
    }
  }
}
