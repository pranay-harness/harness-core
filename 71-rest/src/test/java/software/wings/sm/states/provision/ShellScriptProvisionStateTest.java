package software.wings.sm.states.provision;

import static org.junit.Assert.assertEquals;

import io.harness.category.element.UnitTests;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import software.wings.WingsBaseTest;
import software.wings.beans.InfrastructureProvisionerType;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class ShellScriptProvisionStateTest extends WingsBaseTest {
  @InjectMocks
  private ShellScriptProvisionState state =
      new ShellScriptProvisionState(InfrastructureProvisionerType.SHELL_SCRIPT.name());

  @Test
  @Category(UnitTests.class)
  public void testParseOutput() {
    assertEquals(Collections.emptyMap(), state.parseOutput(null));
    assertEquals(Collections.emptyMap(), state.parseOutput(""));

    String json = "{\n"
        + "\t\"key1\":\"val1\",\n"
        + "\t\"key2\":\"val2\"\n"
        + "}";
    Map<String, Object> expectedMap = new LinkedHashMap<>();
    expectedMap.put("key1", "val1");
    expectedMap.put("key2", "val2");
    assertEquals(expectedMap, state.parseOutput(json));
  }
}
