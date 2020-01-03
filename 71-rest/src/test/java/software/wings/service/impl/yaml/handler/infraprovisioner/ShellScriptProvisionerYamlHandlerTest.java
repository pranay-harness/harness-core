package software.wings.service.impl.yaml.handler.infraprovisioner;

import static io.harness.rule.OwnerRule.VAIBHAV_SI;
import static io.harness.rule.OwnerRule.YOGESH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.apache.commons.io.FileUtils;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.beans.Application;
import software.wings.beans.InfrastructureProvisioner;
import software.wings.beans.NameValuePair;
import software.wings.beans.Service;
import software.wings.beans.ServiceVariable.Type;
import software.wings.beans.shellscript.provisioner.ShellScriptInfrastructureProvisioner;
import software.wings.beans.shellscript.provisioner.ShellScriptInfrastructureProvisioner.Yaml;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.GitFileChange;
import software.wings.beans.yaml.YamlConstants;
import software.wings.beans.yaml.YamlType;
import software.wings.service.impl.yaml.handler.tag.HarnessTagYamlHelper;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.InfrastructureProvisionerService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.yaml.handler.BaseYamlHandlerTest;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

public class ShellScriptProvisionerYamlHandlerTest extends BaseYamlHandlerTest {
  private static final String NAME = "yamlTest";
  private static final String SCRIPT_BODY = "script";
  private final String resourcePath = "./yaml";
  private final String yamlFilePath = "shellScriptProvisoner.yaml";

  @Mock private AppService appService;
  @Mock private HarnessTagYamlHelper harnessTagYamlHelper;
  @Mock private YamlHelper mockYamlHelper;
  @Mock private ServiceResourceService mockServiceResourceService;
  @Mock private InfrastructureProvisionerService mockInfrastructureProvisionerService;

  @Inject @InjectMocks private ShellScriptProvisionerYamlHandler handler;

  private ArgumentCaptor<InfrastructureProvisioner> captor = ArgumentCaptor.forClass(InfrastructureProvisioner.class);
  private File yamlFile;
  private ChangeContext<ShellScriptInfrastructureProvisioner.Yaml> changeContext;
  private String yamlString;
  private ShellScriptInfrastructureProvisioner.Yaml yaml;

  @Before
  public void runBeforeTest() throws IOException {
    doReturn(Application.Builder.anApplication().uuid(APP_ID).build()).when(appService).get(any());
    doReturn(ACCOUNT_ID).when(appService).getAccountIdByAppId(anyString());
    doReturn(APP_ID).when(mockYamlHelper).getAppId(anyString(), anyString());
    Service service = Service.builder().name("Test").uuid(SERVICE_ID).build();
    doReturn(service).when(mockServiceResourceService).getServiceByName(anyString(), anyString());
    doReturn(service).when(mockServiceResourceService).get(anyString(), anyString());
    doReturn(null).when(mockInfrastructureProvisionerService).save(any());
    readYamlFile();
    doReturn(yaml.getName()).when(mockYamlHelper).getNameFromYamlFilePath(anyString());
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testToYaml() {
    ShellScriptInfrastructureProvisioner provisioner = prepareProvisioner();
    Yaml yaml = handler.toYaml(provisioner, "my-app");
    assertThat(yaml.getName()).isEqualTo(NAME);
    assertThat(yaml.getScriptBody()).isEqualTo(SCRIPT_BODY);
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testCreationFromYaml() {
    handler.upsertFromYaml(changeContext, Arrays.asList(changeContext));

    verify(mockInfrastructureProvisionerService).save(captor.capture());
    ShellScriptInfrastructureProvisioner savedProvisioner = (ShellScriptInfrastructureProvisioner) captor.getValue();

    // convert to Yaml from bean
    Yaml yamlFromBean = handler.toYaml(savedProvisioner, APP_ID);
    assertThat(yamlFromBean).isNotNull();

    String yamlContent = getYamlContent(yamlFromBean);
    assertThat(yamlContent).isNotNull();
    yamlContent = yamlContent.substring(0, yamlContent.length() - 1);
    assertThat(yamlContent).isEqualTo(yamlString);
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testGetSavedProvisioner() {
    ShellScriptInfrastructureProvisioner saved = ShellScriptInfrastructureProvisioner.builder().build();
    doReturn(saved).when(mockYamlHelper).getInfrastructureProvisioner(anyString(), anyString());
    ShellScriptInfrastructureProvisioner provisioner = handler.get(ACCOUNT_ID, yamlFilePath);
    assertThat(provisioner).isNotNull();
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testUpdationFromYaml() {
    ShellScriptInfrastructureProvisioner saved =
        ShellScriptInfrastructureProvisioner.builder().name("name").uuid("uuid").build();
    doReturn(saved).when(mockInfrastructureProvisionerService).getByName(anyString(), anyString());
    handler.upsertFromYaml(changeContext, Arrays.asList(changeContext));
    verify(mockInfrastructureProvisionerService).update(captor.capture());
    ShellScriptInfrastructureProvisioner updated = (ShellScriptInfrastructureProvisioner) captor.getValue();
    assertThat(updated).isNotNull();
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

  private ChangeContext<ShellScriptInfrastructureProvisioner.Yaml> getChangeContext(
      String validYamlContent, String yamlFilePath) {
    GitFileChange gitFileChange = GitFileChange.Builder.aGitFileChange()
                                      .withAccountId(ACCOUNT_ID)
                                      .withFilePath(yamlFilePath)
                                      .withFileContent(validYamlContent)
                                      .build();

    ChangeContext<ShellScriptInfrastructureProvisioner.Yaml> changeContext = new ChangeContext<>();
    changeContext.setChange(gitFileChange);
    changeContext.setYamlType(YamlType.PROVISIONER);
    changeContext.setYamlSyncHandler(handler);
    return changeContext;
  }

  private void readYamlFile() throws IOException {
    try {
      yamlFile = new File(
          getClass().getClassLoader().getResource(resourcePath + YamlConstants.PATH_DELIMITER + yamlFilePath).toURI());
    } catch (URISyntaxException e) {
      Assertions.fail("Unable to find yaml file " + resourcePath);
    }
    assertThat(yamlFile).isNotNull();
    yamlString = FileUtils.readFileToString(yamlFile, "UTF-8");
    changeContext = getChangeContext(yamlString, yamlFilePath);
    yaml = (Yaml) getYaml(yamlString, Yaml.class);
    changeContext.setYaml(yaml);
  }
}
