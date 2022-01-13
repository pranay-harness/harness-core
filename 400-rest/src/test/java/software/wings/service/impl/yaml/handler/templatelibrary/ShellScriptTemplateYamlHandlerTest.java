/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler.templatelibrary;

import static io.harness.rule.OwnerRule.ABHINAV;
import static io.harness.rule.OwnerRule.ROHIT_KUMAR;

import static software.wings.beans.Account.GLOBAL_ACCOUNT_ID;
import static software.wings.beans.CGConstants.GLOBAL_APP_ID;
import static software.wings.common.TemplateConstants.SHELL_SCRIPT;
import static software.wings.service.impl.yaml.handler.templatelibrary.TemplateLibaryYamlConstants.INVALID_SHELL_SCRIPT_TEMPLATE_VALID_YAML_FILE_PATH;
import static software.wings.service.impl.yaml.handler.templatelibrary.TemplateLibaryYamlConstants.INVALID_SHELL_SCRIPT_TEMPLATE_WITH_VARIABLE;
import static software.wings.service.impl.yaml.handler.templatelibrary.TemplateLibaryYamlConstants.SHELL_SCRIPT_TEMPLATE_VALID_YAML_FILE_PATH;
import static software.wings.service.impl.yaml.handler.templatelibrary.TemplateLibaryYamlConstants.VALID_SHELL_SCRIPT_TEMPLATE_WITH_VARIABLE;
import static software.wings.service.impl.yaml.handler.templatelibrary.TemplateLibaryYamlConstants.expectedTemplate;
import static software.wings.service.impl.yaml.handler.templatelibrary.TemplateLibaryYamlConstants.expectedTemplateWithoutVariable;
import static software.wings.service.impl.yaml.handler.templatelibrary.TemplateLibaryYamlConstants.rootTemplateFolder;
import static software.wings.service.impl.yaml.handler.templatelibrary.TemplateLibaryYamlConstants.templateForSetup;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import io.harness.category.element.UnitTests;
import io.harness.exception.NoResultFoundException;
import io.harness.rule.Owner;

import software.wings.beans.Application;
import software.wings.beans.template.Template;
import software.wings.beans.yaml.ChangeContext;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.template.TemplateService;
import software.wings.yaml.templatelibrary.ShellScriptTemplateYaml;

import com.google.inject.Inject;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ShellScriptTemplateYamlHandlerTest extends TemplateLibraryYamlHandlerTestBase {
  private String templateName = "test-shell-script";

  @InjectMocks @Inject private ShellScriptTemplateYamlHandler yamlHandler;
  @Mock private TemplateService templateService;
  @Mock private AppService appService;

  @Before
  public void runBeforeTest() {
    MockitoAnnotations.initMocks(this);
    setup(SHELL_SCRIPT_TEMPLATE_VALID_YAML_FILE_PATH, templateName);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testRUDAndGetForExistingTemplate() throws Exception {
    when(yamlHelper.ensureTemplateFolder(
             GLOBAL_ACCOUNT_ID, SHELL_SCRIPT_TEMPLATE_VALID_YAML_FILE_PATH, GLOBAL_APP_ID, TEMPLATE_GALLERY_UUID))
        .thenReturn(rootTemplateFolder);
    when(templateService.findByFolder(rootTemplateFolder, templateName, GLOBAL_APP_ID)).thenReturn(templateForSetup);
    when(templateService.update(expectedTemplate)).thenReturn(expectedTemplate);

    ChangeContext<ShellScriptTemplateYaml> changeContext = getChangeContext(
        VALID_SHELL_SCRIPT_TEMPLATE_WITH_VARIABLE, SHELL_SCRIPT_TEMPLATE_VALID_YAML_FILE_PATH, yamlHandler);
    ShellScriptTemplateYaml yamlObject =
        (ShellScriptTemplateYaml) getYaml(VALID_SHELL_SCRIPT_TEMPLATE_WITH_VARIABLE, ShellScriptTemplateYaml.class);
    changeContext.setYaml(yamlObject);
    changeContext.getChange().setAccountId(GLOBAL_ACCOUNT_ID);
    Template template = yamlHandler.upsertFromYaml(changeContext, asList(changeContext));
    assertThat(template).isNotNull();
    assertThat(template.getTemplateObject()).isNotNull();
    assertThat(template.getName()).isEqualTo(templateName);

    ShellScriptTemplateYaml yaml = yamlHandler.toYaml(template, GLOBAL_APP_ID);
    assertThat(yaml).isNotNull();
    assertThat(yaml.getType()).isEqualTo(SHELL_SCRIPT);

    String yamlContent = getYamlContent(yaml);
    assertThat(yamlContent).isNotNull();
    assertThat(yamlContent).isEqualTo(VALID_SHELL_SCRIPT_TEMPLATE_WITH_VARIABLE);

    Template savedTemplate = yamlHandler.get(GLOBAL_ACCOUNT_ID, SHELL_SCRIPT_TEMPLATE_VALID_YAML_FILE_PATH);
    assertThat(savedTemplate).isNotNull();
    assertThat(savedTemplate.getName()).isEqualTo(templateName);

    yamlHandler.delete(changeContext);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testCreateForNewTemplate() throws Exception {
    when(yamlHelper.ensureTemplateFolder(
             GLOBAL_ACCOUNT_ID, SHELL_SCRIPT_TEMPLATE_VALID_YAML_FILE_PATH, GLOBAL_APP_ID, TEMPLATE_GALLERY_UUID))
        .thenReturn(rootTemplateFolder);
    when(templateService.findByFolder(rootTemplateFolder, templateName, GLOBAL_APP_ID)).thenReturn(null);
    when(templateService.save(expectedTemplateWithoutVariable)).thenReturn(expectedTemplateWithoutVariable);
    ChangeContext<ShellScriptTemplateYaml> changeContext = getChangeContext(
        VALID_SHELL_SCRIPT_TEMPLATE_WITH_VARIABLE, SHELL_SCRIPT_TEMPLATE_VALID_YAML_FILE_PATH, yamlHandler);
    ShellScriptTemplateYaml yamlObject =
        (ShellScriptTemplateYaml) getYaml(VALID_SHELL_SCRIPT_TEMPLATE_WITH_VARIABLE, ShellScriptTemplateYaml.class);
    changeContext.setYaml(yamlObject);
    changeContext.getChange().setAccountId(GLOBAL_ACCOUNT_ID);
    Template template = yamlHandler.upsertFromYaml(changeContext, asList(changeContext));
    assertThat(template).isNotNull();
    assertThat(template.getTemplateObject()).isNotNull();
    assertThat(template.getName()).isEqualTo(templateName);

    ShellScriptTemplateYaml yaml = yamlHandler.toYaml(template, GLOBAL_APP_ID);
    assertThat(yaml).isNotNull();
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testFailures() throws IOException {
    testFailures(VALID_SHELL_SCRIPT_TEMPLATE_WITH_VARIABLE, SHELL_SCRIPT_TEMPLATE_VALID_YAML_FILE_PATH,
        INVALID_SHELL_SCRIPT_TEMPLATE_WITH_VARIABLE, INVALID_SHELL_SCRIPT_TEMPLATE_VALID_YAML_FILE_PATH, yamlHandler,
        ShellScriptTemplateYaml.class);
  }

  @Test
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void test_getApplicationId() {
    final String accountid = "accountid";
    final String yamlPath = "/Setup/Applications/app1/Template Library/harness/folder1/shell.yaml";
    doReturn("app1").when(yamlHelper).getAppName(yamlPath);
    doReturn(Application.Builder.anApplication().uuid("app_uuid").build())
        .when(appService)
        .getAppByName(accountid, "app1");
    final String applicationId = yamlHandler.getApplicationId(accountid, yamlPath);
    assertThat(applicationId).isEqualTo("app_uuid");
  }

  @Test
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void test_getApplicationId_GLOBAL() {
    final String accountid = "accountid";
    final String yamlPath = "/Setup/Template Library/harness/folder1/shell.yaml";

    doReturn(null).when(yamlHelper).getAppName(yamlPath);
    doReturn(null).when(appService).getAppByName(accountid, "app1");
    final String applicationId = yamlHandler.getApplicationId(accountid, yamlPath);
    assertThat(applicationId).isEqualTo(GLOBAL_APP_ID);
  }

  @Test(expected = NoResultFoundException.class)
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void test_getApplicationId_erro() {
    final String accountid = "accountid";
    final String yamlPath = "/Setup/Applications/app1/Template Library/harness/folder1/shell.yaml";

    doReturn("app1").when(yamlHelper).getAppName(yamlPath);
    doReturn(null).when(appService).getAppByName(accountid, "app1");
    final String applicationId = yamlHandler.getApplicationId(accountid, yamlPath);
  }
}
