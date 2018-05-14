package software.wings.sm.states.provision;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import software.wings.WingsBaseTest;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
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
}