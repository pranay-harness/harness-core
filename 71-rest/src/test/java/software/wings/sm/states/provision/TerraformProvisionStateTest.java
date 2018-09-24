package software.wings.sm.states.provision;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import io.harness.exception.InvalidRequestException;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.beans.NameValuePair;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TerraformProvisionStateTest extends WingsBaseTest {
  @Test
  public void shouldParseOutputs() throws IOException {
    assertThat(TerraformProvisionState.parseOutputs(null).size()).isEqualTo(0);
    assertThat(TerraformProvisionState.parseOutputs("").size()).isEqualTo(0);
    assertThat(TerraformProvisionState.parseOutputs("  ").size()).isEqualTo(0);

    ClassLoader classLoader = getClass().getClassLoader();
    File file = new File(classLoader.getResource("software/wings/sm/states/provision/terraform_output.json").getFile());
    String json = FileUtils.readFileToString(file, Charset.defaultCharset());

    final Map<String, Object> stringObjectMap = TerraformProvisionState.parseOutputs(json);
    assertThat(stringObjectMap.size()).isEqualTo(4);
  }

  @Test
  public void testCalculateVariables() {
    List<NameValuePair> variables = new ArrayList<>();
    List<NameValuePair> provisionerVariables = new ArrayList<>();

    assertThat(TerraformProvisionState.calculateVariables(variables, provisionerVariables)).isEmpty();

    variables.add(NameValuePair.builder().name("foo").valueType("TEXT").build());
    assertThat(TerraformProvisionState.calculateVariables(variables, provisionerVariables)).isEmpty();

    provisionerVariables.add(NameValuePair.builder().name("foo").valueType("TEXT").build());
    assertThat(TerraformProvisionState.calculateVariables(variables, provisionerVariables))
        .containsExactly(variables.get(0));

    variables.add(NameValuePair.builder().name("bar").valueType("ENCYPTED_TEXT").build());
    provisionerVariables.add(NameValuePair.builder().name("bar").valueType("ENCYPTED_TEXT").build());
    assertThat(TerraformProvisionState.calculateVariables(variables, provisionerVariables))
        .containsExactly(variables.get(0), variables.get(1));

    provisionerVariables.add(NameValuePair.builder().name("baz").valueType("TEXT").build());
    assertThatThrownBy(() -> TerraformProvisionState.calculateVariables(variables, provisionerVariables))
        .isInstanceOf(InvalidRequestException.class);

    variables.add(NameValuePair.builder().name("baz").valueType("ENCYPTED_TEXT").build());
    assertThatThrownBy(() -> TerraformProvisionState.calculateVariables(variables, provisionerVariables))
        .isInstanceOf(InvalidRequestException.class);
  }
}