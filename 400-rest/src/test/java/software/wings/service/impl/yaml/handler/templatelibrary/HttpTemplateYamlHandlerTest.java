/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler.templatelibrary;

import static io.harness.rule.OwnerRule.ABHINAV;

import static software.wings.beans.Account.GLOBAL_ACCOUNT_ID;
import static software.wings.beans.CGConstants.GLOBAL_APP_ID;
import static software.wings.common.TemplateConstants.HTTP;
import static software.wings.service.impl.yaml.handler.templatelibrary.TemplateLibaryYamlConstants.HTTP_TEMPLATE_VALID_YAML_FILE_PATH;
import static software.wings.service.impl.yaml.handler.templatelibrary.TemplateLibaryYamlConstants.INVALID_HTTP_TEMPLATE_VALID_YAML_FILE_PATH;
import static software.wings.service.impl.yaml.handler.templatelibrary.TemplateLibaryYamlConstants.INVALID_HTTP_TEMPLATE_WITH_VARIABLE;
import static software.wings.service.impl.yaml.handler.templatelibrary.TemplateLibaryYamlConstants.VALID_HTTP_TEMPLATE_WITH_VARIABLE;
import static software.wings.service.impl.yaml.handler.templatelibrary.TemplateLibaryYamlConstants.expectedHttpTemplate;
import static software.wings.service.impl.yaml.handler.templatelibrary.TemplateLibaryYamlConstants.httpTemplateForSetup;
import static software.wings.service.impl.yaml.handler.templatelibrary.TemplateLibaryYamlConstants.httpTemplateName;
import static software.wings.service.impl.yaml.handler.templatelibrary.TemplateLibaryYamlConstants.rootTemplateFolder;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.beans.template.Template;
import software.wings.beans.yaml.ChangeContext;
import software.wings.service.intfc.template.TemplateService;
import software.wings.yaml.templatelibrary.HttpTemplateYaml;

import com.google.inject.Inject;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class HttpTemplateYamlHandlerTest extends TemplateLibraryYamlHandlerTestBase {
  @InjectMocks @Inject private HttpTemplateYamlHandler yamlHandler;
  @Mock private TemplateService templateService;

  @Before
  public void runBeforeTest() {
    MockitoAnnotations.initMocks(this);
    setup(HTTP_TEMPLATE_VALID_YAML_FILE_PATH, httpTemplateName);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testCRUDAndGetFoTemplate() throws IOException {
    when(yamlHelper.ensureTemplateFolder(
             GLOBAL_ACCOUNT_ID, HTTP_TEMPLATE_VALID_YAML_FILE_PATH, GLOBAL_APP_ID, TEMPLATE_GALLERY_UUID))
        .thenReturn(rootTemplateFolder);
    when(templateService.findByFolder(rootTemplateFolder, httpTemplateName, GLOBAL_APP_ID))
        .thenReturn(httpTemplateForSetup);
    when(templateService.update(expectedHttpTemplate)).thenReturn(expectedHttpTemplate);

    ChangeContext<HttpTemplateYaml> changeContext =
        getChangeContext(VALID_HTTP_TEMPLATE_WITH_VARIABLE, HTTP_TEMPLATE_VALID_YAML_FILE_PATH, yamlHandler);
    HttpTemplateYaml yamlObject = (HttpTemplateYaml) getYaml(VALID_HTTP_TEMPLATE_WITH_VARIABLE, HttpTemplateYaml.class);
    changeContext.setYaml(yamlObject);
    changeContext.getChange().setAccountId(GLOBAL_ACCOUNT_ID);
    Template template = yamlHandler.upsertFromYaml(changeContext, asList(changeContext));
    assertThat(template).isNotNull();
    assertThat(template.getTemplateObject()).isNotNull();
    assertThat(template.getName()).isEqualTo(httpTemplateName);

    HttpTemplateYaml yaml = yamlHandler.toYaml(template, GLOBAL_APP_ID);
    assertThat(yaml).isNotNull();
    assertThat(yaml.getType()).isEqualTo(HTTP);

    String yamlContent = getYamlContent(yaml);
    assertThat(yamlContent).isNotNull();
    assertThat(yamlContent).isEqualTo(VALID_HTTP_TEMPLATE_WITH_VARIABLE);

    Template savedTemplate = yamlHandler.get(GLOBAL_ACCOUNT_ID, HTTP_TEMPLATE_VALID_YAML_FILE_PATH);
    assertThat(savedTemplate).isNotNull();
    assertThat(savedTemplate.getName()).isEqualTo(httpTemplateName);

    yamlHandler.delete(changeContext);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testFailures() throws IOException {
    testFailures(VALID_HTTP_TEMPLATE_WITH_VARIABLE, HTTP_TEMPLATE_VALID_YAML_FILE_PATH,
        INVALID_HTTP_TEMPLATE_WITH_VARIABLE, INVALID_HTTP_TEMPLATE_VALID_YAML_FILE_PATH, yamlHandler,
        HttpTemplateYaml.class);
  }
}
