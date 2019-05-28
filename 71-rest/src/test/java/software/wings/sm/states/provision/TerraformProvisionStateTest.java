package software.wings.sm.states.provision;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import io.harness.category.element.UnitTests;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.TerraformInfrastructureProvisioner;
import software.wings.service.intfc.InfrastructureProvisionerService;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Map;

public class TerraformProvisionStateTest extends WingsBaseTest {
  @Mock InfrastructureProvisionerService infrastructureProvisionerService;
  @InjectMocks private TerraformProvisionState state = new ApplyTerraformProvisionState("tf");

  @Test
  @Category(UnitTests.class)
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
  @Category(UnitTests.class)
  public void shouldUpdateProvisionerWorkspaces() {
    when(infrastructureProvisionerService.update(any())).thenReturn(null);
    TerraformInfrastructureProvisioner provisioner = TerraformInfrastructureProvisioner.builder().build();
    state.updateProvisionerWorkspaces(provisioner, "w1");
    assertTrue(provisioner.getWorkspaces().size() == 1 && provisioner.getWorkspaces().contains("w1"));
    state.updateProvisionerWorkspaces(provisioner, "w2");
    assertTrue(
        provisioner.getWorkspaces().size() == 2 && provisioner.getWorkspaces().equals(Arrays.asList("w1", "w2")));
    state.updateProvisionerWorkspaces(provisioner, "w2");
    assertTrue(
        provisioner.getWorkspaces().size() == 2 && provisioner.getWorkspaces().equals(Arrays.asList("w1", "w2")));
  }

  @Test
  @Category(UnitTests.class)
  public void shouldHandleDefaultWorkspace() {
    assertTrue(state.handleDefaultWorkspace(null) == null);
    assertTrue(state.handleDefaultWorkspace("default") == null);
    assertTrue(state.handleDefaultWorkspace("abc").equals("abc"));
  }
}