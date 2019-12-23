package software.wings.sm.states.provision;

import static io.harness.rule.OwnerRule.ABHINAV;
import static io.harness.rule.OwnerRule.VAIBHAV_SI;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.rule.OwnerRule.Owner;
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
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testParseOutput() {
    assertThat(state.parseOutput(null)).isEqualTo(Collections.emptyMap());
    assertThat(state.parseOutput("")).isEqualTo(Collections.emptyMap());

    String json = "{\n"
        + "\t\"key1\":\"val1\",\n"
        + "\t\"key2\":\"val2\"\n"
        + "}";
    Map<String, Object> expectedMap = new LinkedHashMap<>();
    expectedMap.put("key1", "val1");
    expectedMap.put("key2", "val2");
    assertThat(state.parseOutput(json)).isEqualTo(expectedMap);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testValidation() {
    assertThat(state.validateFields().size()).isNotEqualTo(0);
    state.setProvisionerId("test provisioner");
    assertThat(state.validateFields().size()).isEqualTo(0);
  }
}
