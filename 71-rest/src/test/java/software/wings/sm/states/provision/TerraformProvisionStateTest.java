package software.wings.sm.states.provision;

import static io.harness.rule.OwnerRule.GEORGE;
import static io.harness.rule.OwnerRule.VAIBHAV_SI;
import static io.harness.rule.OwnerRule.YOGESH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.NameValuePair;
import software.wings.beans.TerraformInfrastructureProvisioner;
import software.wings.service.intfc.InfrastructureProvisionerService;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class TerraformProvisionStateTest extends WingsBaseTest {
  @Mock InfrastructureProvisionerService infrastructureProvisionerService;
  @InjectMocks private TerraformProvisionState state = new ApplyTerraformProvisionState("tf");

  @Test
  @Owner(developers = GEORGE)
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
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void shouldUpdateProvisionerWorkspaces() {
    when(infrastructureProvisionerService.update(any())).thenReturn(null);
    TerraformInfrastructureProvisioner provisioner = TerraformInfrastructureProvisioner.builder().build();
    state.updateProvisionerWorkspaces(provisioner, "w1");
    assertThat(provisioner.getWorkspaces().size() == 1 && provisioner.getWorkspaces().contains("w1")).isTrue();
    state.updateProvisionerWorkspaces(provisioner, "w2");
    assertThat(provisioner.getWorkspaces().size() == 2 && provisioner.getWorkspaces().equals(Arrays.asList("w1", "w2")))
        .isTrue();
    state.updateProvisionerWorkspaces(provisioner, "w2");
    assertThat(provisioner.getWorkspaces().size() == 2 && provisioner.getWorkspaces().equals(Arrays.asList("w1", "w2")))
        .isTrue();
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldHandleDefaultWorkspace() {
    assertThat(state.handleDefaultWorkspace(null) == null).isTrue();
    assertThat(state.handleDefaultWorkspace("default") == null).isTrue();
    assertThat(state.handleDefaultWorkspace("abc").equals("abc")).isTrue();
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testValidateAndFilterVariables() {
    NameValuePair prov_var_1 = NameValuePair.builder().name("access_key").valueType("TEXT").build();
    NameValuePair prov_var_2 = NameValuePair.builder().name("secret_key").valueType("TEXT").build();

    NameValuePair wf_var_1 = NameValuePair.builder().name("access_key").valueType("TEXT").value("value-1").build();
    NameValuePair wf_var_2 = NameValuePair.builder().name("secret_key").valueType("TEXT").value("value-2").build();
    NameValuePair wf_var_3 = NameValuePair.builder().name("region").valueType("TEXT").value("value-3").build();

    final List<NameValuePair> workflowVars = Arrays.asList(wf_var_1, wf_var_2, wf_var_3);
    final List<NameValuePair> provVars = Arrays.asList(prov_var_1, prov_var_2);

    List<NameValuePair> filteredVars_1 = TerraformProvisionState.validateAndFilterVariables(workflowVars, provVars);

    final List<NameValuePair> expected_1 = Arrays.asList(wf_var_1, wf_var_2);
    assertThat(filteredVars_1).isEqualTo(expected_1);

    wf_var_1.setValueType("ENCRYPTED_TEXT");

    final List<NameValuePair> filteredVars_2 =
        TerraformProvisionState.validateAndFilterVariables(workflowVars, provVars);

    final List<NameValuePair> expected_2 = Arrays.asList(wf_var_1, wf_var_2);
    assertThat(filteredVars_2).isEqualTo(expected_2);
  }
}