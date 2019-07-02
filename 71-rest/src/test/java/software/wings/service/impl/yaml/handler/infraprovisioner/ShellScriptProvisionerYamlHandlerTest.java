package software.wings.service.impl.yaml.handler.infraprovisioner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static software.wings.utils.WingsTestConstants.APP_ID;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.beans.Application;
import software.wings.beans.NameValuePair;
import software.wings.beans.ServiceVariable.Type;
import software.wings.beans.shellscript.provisioner.ShellScriptInfrastructureProvisioner;
import software.wings.beans.shellscript.provisioner.ShellScriptInfrastructureProvisioner.Yaml;
import software.wings.service.impl.yaml.HarnessTagYamlHelper;
import software.wings.service.intfc.AppService;
import software.wings.yaml.handler.BaseYamlHandlerTest;

import java.util.Arrays;
import java.util.List;

public class ShellScriptProvisionerYamlHandlerTest extends BaseYamlHandlerTest {
  private static final String NAME = "yamlTest";
  private static final String SCRIPT_BODY = "script";

  @Mock private AppService appService;
  @Mock private HarnessTagYamlHelper harnessTagYamlHelper;

  @Inject @InjectMocks private ShellScriptProvisionerYamlHandler handler;

  @Before
  public void runBeforeTest() {
    doReturn(Application.Builder.anApplication().uuid(APP_ID).build()).when(appService).get(any());
  }

  @Test
  @Category(UnitTests.class)
  public void testToYaml() {
    ShellScriptInfrastructureProvisioner provisioner = prepareProvisioner();
    Yaml yaml = handler.toYaml(provisioner, "my-app");
    assertEquals(NAME, yaml.getName());
    assertEquals(SCRIPT_BODY, yaml.getScriptBody());
  }

  private ShellScriptInfrastructureProvisioner prepareProvisioner() {
    ShellScriptInfrastructureProvisioner provisioner = ShellScriptInfrastructureProvisioner.builder().build();
    provisioner.setName(NAME);
    provisioner.setScriptBody(SCRIPT_BODY);

    List<NameValuePair> variables =
        Arrays.asList(NameValuePair.builder().name("var1").valueType(Type.ENCRYPTED_TEXT.name()).build(),
            NameValuePair.builder().name("var2").valueType(Type.TEXT.name()).build());
    provisioner.setVariables(variables);
    return provisioner;
  }
}
