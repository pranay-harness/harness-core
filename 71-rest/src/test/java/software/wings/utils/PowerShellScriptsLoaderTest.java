package software.wings.utils;

import static org.junit.Assert.assertFalse;

import io.harness.category.element.UnitTests;
import io.harness.data.structure.EmptyPredicate;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.utils.PowerShellScriptsLoader.PsScript;

import java.util.Map;

public class PowerShellScriptsLoaderTest {
  @Test
  @Category(UnitTests.class)
  public void smokeTest() {
    for (Map.Entry<PsScript, String> entry : PowerShellScriptsLoader.psScriptMap.entrySet()) {
      assertFalse(EmptyPredicate.isEmpty(entry.getValue()));
    }
  }
}
