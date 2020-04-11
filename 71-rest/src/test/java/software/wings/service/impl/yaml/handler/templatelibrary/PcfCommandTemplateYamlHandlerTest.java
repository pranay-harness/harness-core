package software.wings.service.impl.yaml.handler.templatelibrary;

import static io.harness.rule.OwnerRule.AADITI;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static software.wings.beans.Account.GLOBAL_ACCOUNT_ID;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.common.TemplateConstants.PCF_PLUGIN;
import static software.wings.service.impl.yaml.handler.templatelibrary.TemplateLibaryYamlConstants.INVALID_PCF_COMMAND_TEMPLATE_VALID_YAML_FILE_PATH;
import static software.wings.service.impl.yaml.handler.templatelibrary.TemplateLibaryYamlConstants.INVALID_PCF_COMMAND_TEMPLATE_WITH_VARIABLE;
import static software.wings.service.impl.yaml.handler.templatelibrary.TemplateLibaryYamlConstants.PCF_COMMAND_TEMPLATE_VALID_YAML_FILE_PATH;
import static software.wings.service.impl.yaml.handler.templatelibrary.TemplateLibaryYamlConstants.PCfCommandTemplateForSetup;
import static software.wings.service.impl.yaml.handler.templatelibrary.TemplateLibaryYamlConstants.VALID_PCF_COMMAND_TEMPLATE_WITH_VARIABLE;
import static software.wings.service.impl.yaml.handler.templatelibrary.TemplateLibaryYamlConstants.expectedPcfCommandTemplate;
import static software.wings.service.impl.yaml.handler.templatelibrary.TemplateLibaryYamlConstants.expectedPcfCommandTemplateWithoutVariable;
import static software.wings.service.impl.yaml.handler.templatelibrary.TemplateLibaryYamlConstants.rootTemplateFolder;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.wings.beans.template.Template;
import software.wings.beans.yaml.ChangeContext;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.template.TemplateService;
import software.wings.yaml.templatelibrary.PcfCommandTemplateYaml;

import java.io.IOException;

public class PcfCommandTemplateYamlHandlerTest extends TemplateLibraryYamlHandlerTest {
  private String templateName = "test-pcf-command";

  @InjectMocks @Inject private PcfCommandTemplateYamlHandler yamlHandler;
  @Mock private TemplateService templateService;
  @Mock private AppService appService;

  @Before
  public void runBeforeTest() {
    MockitoAnnotations.initMocks(this);
    setup(PCF_COMMAND_TEMPLATE_VALID_YAML_FILE_PATH, templateName);
  }

  @Test
  @Owner(developers = AADITI)
  @Category(UnitTests.class)
  public void testRUDAndGetForExistingTemplate() throws Exception {
    when(yamlHelper.ensureTemplateFolder(GLOBAL_ACCOUNT_ID, PCF_COMMAND_TEMPLATE_VALID_YAML_FILE_PATH, GLOBAL_APP_ID))
        .thenReturn(rootTemplateFolder);
    when(templateService.findByFolder(rootTemplateFolder, templateName, GLOBAL_APP_ID))
        .thenReturn(PCfCommandTemplateForSetup);
    when(templateService.update(expectedPcfCommandTemplate)).thenReturn(expectedPcfCommandTemplate);

    ChangeContext<PcfCommandTemplateYaml> changeContext = getChangeContext(
        VALID_PCF_COMMAND_TEMPLATE_WITH_VARIABLE, PCF_COMMAND_TEMPLATE_VALID_YAML_FILE_PATH, yamlHandler);
    PcfCommandTemplateYaml yamlObject =
        (PcfCommandTemplateYaml) getYaml(VALID_PCF_COMMAND_TEMPLATE_WITH_VARIABLE, PcfCommandTemplateYaml.class);
    changeContext.setYaml(yamlObject);
    changeContext.getChange().setAccountId(GLOBAL_ACCOUNT_ID);
    Template template = yamlHandler.upsertFromYaml(changeContext, asList(changeContext));
    assertThat(template).isNotNull();
    assertThat(template.getTemplateObject()).isNotNull();
    assertThat(template.getName()).isEqualTo(templateName);

    PcfCommandTemplateYaml yaml = yamlHandler.toYaml(template, GLOBAL_APP_ID);
    assertThat(yaml).isNotNull();
    assertThat(yaml.getType()).isEqualTo(PCF_PLUGIN);

    String yamlContent = getYamlContent(yaml);
    assertThat(yamlContent).isNotNull();
    assertThat(yamlContent).isEqualTo(VALID_PCF_COMMAND_TEMPLATE_WITH_VARIABLE);

    Template savedTemplate = yamlHandler.get(GLOBAL_ACCOUNT_ID, PCF_COMMAND_TEMPLATE_VALID_YAML_FILE_PATH);
    assertThat(savedTemplate).isNotNull();
    assertThat(savedTemplate.getName()).isEqualTo(templateName);

    yamlHandler.delete(changeContext);
  }

  @Test
  @Owner(developers = AADITI)
  @Category(UnitTests.class)
  public void testCreateForNewTemplate() throws Exception {
    when(yamlHelper.ensureTemplateFolder(GLOBAL_ACCOUNT_ID, PCF_COMMAND_TEMPLATE_VALID_YAML_FILE_PATH, GLOBAL_APP_ID))
        .thenReturn(rootTemplateFolder);
    when(templateService.findByFolder(rootTemplateFolder, templateName, GLOBAL_APP_ID)).thenReturn(null);
    when(templateService.save(expectedPcfCommandTemplateWithoutVariable))
        .thenReturn(expectedPcfCommandTemplateWithoutVariable);
    ChangeContext<PcfCommandTemplateYaml> changeContext = getChangeContext(
        VALID_PCF_COMMAND_TEMPLATE_WITH_VARIABLE, PCF_COMMAND_TEMPLATE_VALID_YAML_FILE_PATH, yamlHandler);
    PcfCommandTemplateYaml yamlObject =
        (PcfCommandTemplateYaml) getYaml(VALID_PCF_COMMAND_TEMPLATE_WITH_VARIABLE, PcfCommandTemplateYaml.class);
    changeContext.setYaml(yamlObject);
    changeContext.getChange().setAccountId(GLOBAL_ACCOUNT_ID);
    Template template = yamlHandler.upsertFromYaml(changeContext, asList(changeContext));
    assertThat(template).isNotNull();
    assertThat(template.getTemplateObject()).isNotNull();
    assertThat(template.getName()).isEqualTo(templateName);

    PcfCommandTemplateYaml yaml = yamlHandler.toYaml(template, GLOBAL_APP_ID);
    assertThat(yaml).isNotNull();
  }

  @Test
  @Owner(developers = AADITI)
  @Category(UnitTests.class)
  public void testFailures() throws IOException {
    testFailures(VALID_PCF_COMMAND_TEMPLATE_WITH_VARIABLE, PCF_COMMAND_TEMPLATE_VALID_YAML_FILE_PATH,
        INVALID_PCF_COMMAND_TEMPLATE_WITH_VARIABLE, INVALID_PCF_COMMAND_TEMPLATE_VALID_YAML_FILE_PATH, yamlHandler,
        PcfCommandTemplateYaml.class);
  }
}
